import { onSchedule } from "firebase-functions/v2/scheduler";
import { onDocumentDeleted } from "firebase-functions/v2/firestore";
import { onCall } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import {
  bucket,
  db,
  deleteQueryInBatches,
  log,
  logError,
  requireAuth,
} from "./common";

const DAY_MS = 24 * 60 * 60 * 1000;
const RECENTLY_DELETED_RETENTION_DAYS = 30;

/**
 * Expires stories.
 *
 * Security rules already hide expired stories from readers; this job does the
 * physical cleanup so the collection and the bucket stay bounded. Archived
 * stories are preserved because their owner may still want highlights.
 */
export const expireStories = onSchedule("every 60 minutes", async () => {
  const now = admin.firestore.Timestamp.now();
  const expired = await db
    .collection("stories")
    .where("expiresAt", "<=", now)
    .where("isArchived", "==", false)
    .limit(400)
    .get();

  if (expired.empty) {
    log("stories.expire.none");
    return;
  }

  let archived = 0;
  let removed = 0;

  for (const doc of expired.docs) {
    try {
      const keepInArchive = doc.get("keepInArchive") !== false;
      if (keepInArchive) {
        // Move into the owner-only archive instead of destroying it.
        await doc.ref.set({ isArchived: true, archivedAt: now }, { merge: true });
        archived += 1;
      } else {
        await deleteQueryInBatches(doc.ref.collection("views"));
        await deleteStoragePrefix(
          `users/${doc.get("authorId")}/stories/${doc.id}/`,
        );
        await doc.ref.delete();
        removed += 1;
      }
    } catch (error) {
      logError("stories.expire.failed", error, { storyId: doc.id });
    }
  }

  log("stories.expire.done", { archived, removed });
});

/** Purges soft-deleted posts and reels once the retention window elapses. */
export const purgeRecentlyDeleted = onSchedule("every 24 hours", async () => {
  const threshold = admin.firestore.Timestamp.fromMillis(
    Date.now() - RECENTLY_DELETED_RETENTION_DAYS * DAY_MS,
  );

  let purged = 0;
  for (const collection of ["posts", "reels"]) {
    const stale = await db
      .collection(collection)
      .where("deletedAt", "<=", threshold)
      .limit(200)
      .get();

    for (const doc of stale.docs) {
      try {
        await deleteQueryInBatches(doc.ref.collection("likes"));
        await deleteQueryInBatches(doc.ref.collection("comments"));
        await deleteStoragePrefix(
          `users/${doc.get("authorId")}/${collection}/${doc.id}/`,
        );
        await doc.ref.delete();
        purged += 1;
      } catch (error) {
        logError("purge.failed", error, { collection, id: doc.id });
      }
    }
  }
  log("purge.recentlyDeleted.done", { purged });
});

/**
 * Removes orphaned media.
 *
 * Uploads that were cancelled or that failed after the bytes landed leave files
 * with no owning document. Anything older than 48 hours whose parent document
 * is gone is deleted.
 */
export const cleanupOrphanedMedia = onSchedule("every 24 hours", async () => {
  const cutoff = Date.now() - 2 * DAY_MS;
  const [files] = await bucket().getFiles({ prefix: "users/", maxResults: 2000 });

  let removed = 0;
  for (const file of files) {
    try {
      const created = new Date(file.metadata.timeCreated ?? 0).getTime();
      if (created > cutoff) continue;

      // users/{userId}/{kind}/{contentId}/{fileName}
      const parts = file.name.split("/");
      if (parts.length < 5) continue;
      const [, , kind, contentId] = parts;

      const collection =
        kind === "posts" ? "posts" : kind === "reels" ? "reels" : kind === "stories" ? "stories" : null;
      if (!collection) continue;

      const parent = await db.collection(collection).doc(contentId).get();
      if (!parent.exists) {
        await file.delete({ ignoreNotFound: true });
        removed += 1;
      }
    } catch (error) {
      logError("orphan.cleanup.failed", error, { file: file.name });
    }
  }
  log("orphan.cleanup.done", { scanned: files.length, removed });
});

/** Drops FCM tokens that have not been refreshed in 60 days. */
export const pruneStaleTokens = onSchedule("every 168 hours", async () => {
  const cutoff = admin.firestore.Timestamp.fromMillis(Date.now() - 60 * DAY_MS);
  const stale = await db
    .collectionGroup("tokens")
    .where("refreshedAt", "<=", cutoff)
    .limit(500)
    .get();

  const batch = db.batch();
  stale.docs.forEach((doc) => batch.delete(doc.ref));
  await batch.commit();
  log("fcm.tokens.stale.pruned", { removed: stale.size });
});

/** Expires notes and trims each user's notification list to a bounded size. */
export const expireNotesAndTrimNotifications = onSchedule("every 6 hours", async () => {
  const now = admin.firestore.Timestamp.now();
  const expiredNotes = await db.collection("notes").where("expiresAt", "<=", now).limit(500).get();
  const batch = db.batch();
  expiredNotes.docs.forEach((doc) => batch.delete(doc.ref));
  await batch.commit();

  log("notes.expired", { removed: expiredNotes.size });
});

/**
 * Fans out account deletion.
 *
 * Auth deletion is triggered by the client; this removes the user's documents
 * and files so nothing is left behind. Content in other people's conversations
 * is anonymised rather than deleted so the remaining members keep context.
 */
export const onUserDocumentDeleted = onDocumentDeleted("users/{userId}", async (event) => {
  const { userId } = event.params;

  try {
    await Promise.all([
      deleteQueryInBatches(db.collection("posts").where("authorId", "==", userId)),
      deleteQueryInBatches(db.collection("reels").where("authorId", "==", userId)),
      deleteQueryInBatches(db.collection("stories").where("authorId", "==", userId)),
      deleteQueryInBatches(db.collection("notes").where("authorId", "==", userId)),
      deleteQueryInBatches(db.collection("follows").where("followerId", "==", userId)),
      deleteQueryInBatches(db.collection("follows").where("followeeId", "==", userId)),
      deleteQueryInBatches(
        db.collection("notifications").doc(userId).collection("items"),
      ),
      deleteQueryInBatches(db.collection("deviceTokens").doc(userId).collection("tokens")),
    ]);

    await db.collection("analytics").doc(userId).delete().catch(() => undefined);
    await deleteStoragePrefix(`users/${userId}/`);

    log("account.deleted", { userId });
  } catch (error) {
    logError("account.delete.failed", error, { userId });
    throw error;
  }
});

/**
 * Development-only moderator bootstrap.
 *
 * Moderator power lives in a custom claim, which the Android client can never
 * set. This callable is deliberately disabled unless MODERATOR_BOOTSTRAP_EMAIL
 * is configured in the environment, and it contains no secret of its own.
 */
export const grantModeratorClaim = onCall(async (request) => {
  const uid = requireAuth(request.auth);
  const allowedEmail = process.env.MODERATOR_BOOTSTRAP_EMAIL;

  if (!allowedEmail) {
    log("moderator.bootstrap.disabled", { uid });
    return { granted: false, reason: "Moderator bootstrap is not configured." };
  }

  const caller = await admin.auth().getUser(uid);
  if (caller.email?.toLowerCase() !== allowedEmail.toLowerCase()) {
    return { granted: false, reason: "Not eligible." };
  }

  await admin.auth().setCustomUserClaims(uid, { moderator: true });
  log("moderator.granted", { uid });
  return { granted: true };
});

async function deleteStoragePrefix(prefix: string): Promise<void> {
  try {
    await bucket().deleteFiles({ prefix, force: true });
  } catch (error) {
    logError("storage.prefix.delete.failed", error, { prefix });
  }
}
