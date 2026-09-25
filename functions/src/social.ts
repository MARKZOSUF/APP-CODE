import { onDocumentCreated, onDocumentDeleted, onDocumentUpdated } from "firebase-functions/v2/firestore";
import { onCall } from "firebase-functions/v2/https";
import { HttpsError } from "firebase-functions/v2/https";
import {
  FieldValue,
  createNotification,
  db,
  enforceRateLimit,
  log,
  normalizeUsername,
  requireAuth,
  requireString,
  usernamePrefixes,
} from "./common";
import { bumpAnalytics } from "./counters";

/**
 * Follow graph maintenance.
 *
 * The relationship document id is `{followerId}_{followeeId}`, so duplicate
 * follows and duplicate requests are impossible by construction. These triggers
 * keep the follower/following counters trustworthy.
 */

export const onFollowCreated = onDocumentCreated("follows/{relationshipId}", async (event) => {
  const snapshot = event.data;
  if (!snapshot) return;

  const followerId = snapshot.get("followerId") as string;
  const followeeId = snapshot.get("followeeId") as string;
  const state = snapshot.get("state") as string;

  if (state === "REQUESTED") {
    await createNotification({
      recipientId: followeeId,
      actorId: followerId,
      type: "FOLLOW_REQUEST",
      targetId: followerId,
      previewText: "wants to follow you",
      idempotencyKey: `follow-request:${followerId}:${followeeId}`,
    });
    return;
  }

  const batch = db.batch();
  batch.set(
    db.collection("users").doc(followerId),
    { followingCount: FieldValue.increment(1) },
    { merge: true },
  );
  batch.set(
    db.collection("users").doc(followeeId),
    { followerCount: FieldValue.increment(1) },
    { merge: true },
  );
  await batch.commit();

  await bumpAnalytics(followeeId, { followers: 1 });
  await createNotification({
    recipientId: followeeId,
    actorId: followerId,
    type: "NEW_FOLLOWER",
    targetId: followerId,
    previewText: "started following you",
    idempotencyKey: `follow:${followerId}:${followeeId}`,
  });
});

/** Approving a request is the REQUESTED -> FOLLOWING transition. */
export const onFollowUpdated = onDocumentUpdated("follows/{relationshipId}", async (event) => {
  const before = event.data?.before;
  const after = event.data?.after;
  if (!before || !after) return;

  const wasRequested = before.get("state") === "REQUESTED";
  const isFollowing = after.get("state") === "FOLLOWING";
  if (!wasRequested || !isFollowing) return;

  const followerId = after.get("followerId") as string;
  const followeeId = after.get("followeeId") as string;

  const batch = db.batch();
  batch.set(
    db.collection("users").doc(followerId),
    { followingCount: FieldValue.increment(1) },
    { merge: true },
  );
  batch.set(
    db.collection("users").doc(followeeId),
    { followerCount: FieldValue.increment(1) },
    { merge: true },
  );
  // The paired request document is no longer needed.
  batch.delete(db.collection("followRequests").doc(`${followerId}_${followeeId}`));
  await batch.commit();

  await bumpAnalytics(followeeId, { followers: 1 });
  await createNotification({
    recipientId: followerId,
    actorId: followeeId,
    type: "REQUEST_ACCEPTED",
    targetId: followeeId,
    previewText: "accepted your follow request",
    idempotencyKey: `follow-accepted:${followerId}:${followeeId}`,
  });
});

export const onFollowDeleted = onDocumentDeleted("follows/{relationshipId}", async (event) => {
  const snapshot = event.data;
  if (!snapshot) return;
  if (snapshot.get("state") !== "FOLLOWING") return;

  const followerId = snapshot.get("followerId") as string;
  const followeeId = snapshot.get("followeeId") as string;

  const batch = db.batch();
  batch.set(
    db.collection("users").doc(followerId),
    { followingCount: FieldValue.increment(-1) },
    { merge: true },
  );
  batch.set(
    db.collection("users").doc(followeeId),
    { followerCount: FieldValue.increment(-1) },
    { merge: true },
  );
  await batch.commit();
  log("follow.removed", { followerId, followeeId });
});

/**
 * Atomically reserves a username.
 *
 * Uniqueness cannot be expressed in security rules alone, so the reservation
 * document and the profile field are written in one transaction. Rate limited
 * to stop enumeration and squatting.
 */
export const reserveUsername = onCall(async (request) => {
  const uid = requireAuth(request.auth);
  const requested = normalizeUsername(requireString(request.data?.username, "username", 30));

  if (requested.length < 3) {
    throw new HttpsError("invalid-argument", "Usernames need at least 3 characters.");
  }
  await enforceRateLimit(uid, "reserveUsername", 10, 60 * 60 * 1000);

  const reservationRef = db.collection("usernames").doc(requested);
  const userRef = db.collection("users").doc(uid);

  await db.runTransaction(async (tx) => {
    const existing = await tx.get(reservationRef);
    if (existing.exists && existing.get("userId") !== uid) {
      throw new HttpsError("already-exists", "That username is taken.");
    }

    const current = await tx.get(userRef);
    const previous = current.get("normalizedUsername") as string | undefined;
    if (previous && previous !== requested) {
      tx.delete(db.collection("usernames").doc(previous));
    }

    tx.set(reservationRef, {
      normalizedUsername: requested,
      userId: uid,
      reservedAt: FieldValue.serverTimestamp(),
    });
    tx.set(
      userRef,
      {
        username: requested,
        normalizedUsername: requested,
        usernamePrefixes: usernamePrefixes(requested),
      },
      { merge: true },
    );
  });

  log("username.reserved", { uid });
  return { username: requested };
});

/** Message fan-out: unread counters plus a push for every other member. */
export const onMessageCreated = onDocumentCreated(
  "conversations/{conversationId}/messages/{messageId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;
    const { conversationId, messageId } = event.params;

    const senderId = snapshot.get("senderId") as string;
    const text = ((snapshot.get("text") as string) ?? "").slice(0, 120);

    const conversationRef = db.collection("conversations").doc(conversationId);
    const conversation = await conversationRef.get();
    if (!conversation.exists) return;

    const memberIds = ((conversation.get("memberIds") as string[]) ?? []).filter(Boolean);

    await conversationRef.set(
      {
        lastMessagePreview: text,
        lastMessageSenderId: senderId,
        lastMessageAt: FieldValue.serverTimestamp(),
      },
      { merge: true },
    );

    await Promise.all(
      memberIds
        .filter((memberId) => memberId !== senderId)
        .map(async (memberId) => {
          await conversationRef
            .collection("members")
            .doc(memberId)
            .set({ unreadCount: FieldValue.increment(1) }, { merge: true });

          await createNotification({
            recipientId: memberId,
            actorId: senderId,
            type: "NEW_MESSAGE",
            targetId: conversationId,
            previewText: text,
            idempotencyKey: `message:${messageId}:${memberId}`,
          });
        }),
    );
  },
);

export const onStoryReactionCreated = onDocumentCreated(
  "stories/{storyId}/views/{viewerId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;
    const reaction = snapshot.get("reactionEmoji") as string | undefined;
    if (!reaction) return;

    const { storyId, viewerId } = event.params;
    const story = await db.collection("stories").doc(storyId).get();
    const authorId = story.get("authorId") as string | undefined;
    if (!authorId) return;

    await createNotification({
      recipientId: authorId,
      actorId: viewerId,
      type: "STORY_REACTION",
      targetId: storyId,
      previewText: reaction,
      idempotencyKey: `story-reaction:${storyId}:${viewerId}`,
    });
  },
);

/** Report intake hook: validates and records, never auto-punishes. */
export const onReportCreated = onDocumentCreated("reports/{reportId}", async (event) => {
  const snapshot = event.data;
  if (!snapshot) return;

  await snapshot.ref.set(
    { state: "SUBMITTED", receivedAt: FieldValue.serverTimestamp() },
    { merge: true },
  );

  log("report.received", {
    reportId: event.params.reportId,
    targetType: snapshot.get("targetType"),
  });
});

/**
 * Records a view/watch-time event.
 *
 * Clients cannot write counters directly, so they call this instead. The
 * increments are clamped so a malicious client cannot report a 10 hour view of
 * a 15 second reel.
 */
export const recordView = onCall(async (request) => {
  const uid = requireAuth(request.auth);
  const contentId = requireString(request.data?.contentId, "contentId", 200);
  const kind = request.data?.kind === "REEL" ? "REEL" : "POST";
  const watchedMs = Math.max(0, Math.min(Number(request.data?.watchedMs ?? 0), 600_000));

  await enforceRateLimit(uid, "recordView", 600, 60 * 60 * 1000);

  const ref = db.collection(kind === "REEL" ? "reels" : "posts").doc(contentId);
  const doc = await ref.get();
  if (!doc.exists) throw new HttpsError("not-found", "That content no longer exists.");

  const update: Record<string, unknown> = { viewCount: FieldValue.increment(1) };
  if (kind === "REEL" && watchedMs > 0) {
    update.totalWatchTimeMs = FieldValue.increment(watchedMs);
  }
  await ref.update(update);

  const authorId = doc.get("authorId") as string | undefined;
  if (authorId) {
    await bumpAnalytics(
      authorId,
      kind === "REEL"
        ? { reelViews: 1, watchTimeMs: watchedMs }
        : { postViews: 1 },
    );
  }
  return { ok: true };
});

/** Records a share; the Sharesheet itself happens entirely on device. */
export const recordShare = onCall(async (request) => {
  const uid = requireAuth(request.auth);
  const contentId = requireString(request.data?.contentId, "contentId", 200);
  const kind = request.data?.kind === "REEL" ? "reels" : "posts";

  await enforceRateLimit(uid, "recordShare", 120, 60 * 60 * 1000);

  const ref = db.collection(kind).doc(contentId);
  if (!(await ref.get()).exists) {
    throw new HttpsError("not-found", "That content no longer exists.");
  }
  await ref.update({ shareCount: FieldValue.increment(1) });
  return { ok: true };
});

/** Profile visit counter, owner-visible only through analytics/{userId}. */
export const recordProfileVisit = onCall(async (request) => {
  const uid = requireAuth(request.auth);
  const profileId = requireString(request.data?.profileId, "profileId", 200);
  if (profileId === uid) return { ok: true };

  await enforceRateLimit(uid, "recordProfileVisit", 300, 60 * 60 * 1000);
  await bumpAnalytics(profileId, { profileVisits: 1 });
  return { ok: true };
});
