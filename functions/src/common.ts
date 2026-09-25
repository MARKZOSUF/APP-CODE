import * as admin from "firebase-admin";
import { HttpsError } from "firebase-functions/v2/https";
import * as logger from "firebase-functions/logger";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

export const db = admin.firestore();
export const bucket = () => admin.storage().bucket();
export const messaging = admin.messaging();
export const FieldValue = admin.firestore.FieldValue;

/** Notification categories mirrored by the Android notification channels. */
export type NotificationType =
  | "NEW_FOLLOWER"
  | "FOLLOW_REQUEST"
  | "REQUEST_ACCEPTED"
  | "POST_LIKE"
  | "REEL_LIKE"
  | "COMMENT"
  | "REPLY"
  | "MENTION"
  | "TAG"
  | "NEW_MESSAGE"
  | "STORY_REACTION"
  | "STORY_REPLY";

const SOCIAL_TYPES: NotificationType[] = [
  "POST_LIKE",
  "REEL_LIKE",
  "COMMENT",
  "REPLY",
  "MENTION",
  "TAG",
  "STORY_REACTION",
  "STORY_REPLY",
];

export function channelFor(type: NotificationType): string {
  if (type === "NEW_MESSAGE") return "insangram_messages";
  if (type === "NEW_FOLLOWER" || type === "FOLLOW_REQUEST" || type === "REQUEST_ACCEPTED") {
    return "insangram_follows";
  }
  return SOCIAL_TYPES.includes(type) ? "insangram_social" : "insangram_social";
}

/**
 * Structured logging helper.
 *
 * Never pass tokens, passwords or raw request bodies through here - only ids
 * and counts, so production logs stay free of secrets.
 */
export function log(event: string, fields: Record<string, unknown> = {}): void {
  logger.info(event, { structuredData: true, ...fields });
}

export function logError(event: string, error: unknown, fields: Record<string, unknown> = {}): void {
  logger.error(event, {
    structuredData: true,
    message: error instanceof Error ? error.message : String(error),
    ...fields,
  });
}

export function requireAuth(auth: { uid?: string } | undefined): string {
  if (!auth?.uid) {
    throw new HttpsError("unauthenticated", "Sign in to perform this action.");
  }
  return auth.uid;
}

export function requireString(value: unknown, field: string, maxLength = 2200): string {
  if (typeof value !== "string" || value.trim().length === 0) {
    throw new HttpsError("invalid-argument", `"${field}" must be a non-empty string.`);
  }
  if (value.length > maxLength) {
    throw new HttpsError("invalid-argument", `"${field}" exceeds ${maxLength} characters.`);
  }
  return value.trim();
}

/**
 * Marks a unit of work as done exactly once.
 *
 * Firestore triggers are at-least-once, so every side effect that must not be
 * duplicated (a push notification, a counter increment) is guarded by a
 * document create in `idempotency/{key}`. The create fails if the key already
 * exists, which tells us a retry is in progress.
 */
export async function claimOnce(key: string): Promise<boolean> {
  const ref = db.collection("idempotency").doc(key);
  try {
    await ref.create({ claimedAt: FieldValue.serverTimestamp() });
    return true;
  } catch {
    log("idempotency.skipped", { key });
    return false;
  }
}

/** Writes a trusted notification document and pushes it to every device. */
export async function createNotification(params: {
  recipientId: string;
  actorId: string;
  type: NotificationType;
  targetId?: string | null;
  previewText?: string;
  previewImageUrl?: string | null;
  idempotencyKey: string;
}): Promise<void> {
  const { recipientId, actorId, type, idempotencyKey } = params;

  // Never notify yourself, and never notify across a block.
  if (recipientId === actorId) return;
  if (await isBlocked(recipientId, actorId)) {
    log("notification.blocked", { recipientId, actorId, type });
    return;
  }
  if (!(await claimOnce(`notify:${idempotencyKey}`))) return;

  const recipient = await db.collection("users").doc(recipientId).get();
  if (!recipient.exists) return;

  const prefs = (recipient.get("notificationPreferences") ?? {}) as Record<string, boolean>;
  if (prefs[type] === false) {
    log("notification.muted", { recipientId, type });
    return;
  }

  const actor = await db.collection("users").doc(actorId).get();
  const actorName = (actor.get("username") as string | undefined) ?? "Someone";

  await db
    .collection("notifications")
    .doc(recipientId)
    .collection("items")
    .add({
      recipientId,
      actorId,
      type,
      targetId: params.targetId ?? null,
      previewText: params.previewText ?? "",
      previewImageUrl: params.previewImageUrl ?? null,
      isRead: false,
      createdAt: FieldValue.serverTimestamp(),
    });

  const showPreview = recipient.get("showNotificationPreviews") !== false;
  await pushToUser(recipientId, {
    title: titleFor(type, actorName),
    body: showPreview ? params.previewText ?? "" : "New activity on Insangram",
    type,
    targetId: params.targetId ?? "",
  });
}

function titleFor(type: NotificationType, actorName: string): string {
  switch (type) {
    case "NEW_FOLLOWER":
      return `${actorName} started following you`;
    case "FOLLOW_REQUEST":
      return `${actorName} requested to follow you`;
    case "REQUEST_ACCEPTED":
      return `${actorName} accepted your follow request`;
    case "POST_LIKE":
      return `${actorName} liked your post`;
    case "REEL_LIKE":
      return `${actorName} liked your reel`;
    case "COMMENT":
      return `${actorName} commented on your post`;
    case "REPLY":
      return `${actorName} replied to your comment`;
    case "MENTION":
      return `${actorName} mentioned you`;
    case "TAG":
      return `${actorName} tagged you`;
    case "NEW_MESSAGE":
      return `${actorName} sent you a message`;
    case "STORY_REACTION":
      return `${actorName} reacted to your story`;
    case "STORY_REPLY":
      return `${actorName} replied to your story`;
    default:
      return "Insangram";
  }
}

/**
 * Sends a data+notification message to each registered device and prunes any
 * token the FCM backend reports as permanently invalid.
 */
export async function pushToUser(
  userId: string,
  payload: { title: string; body: string; type: NotificationType; targetId: string },
): Promise<void> {
  const tokensSnapshot = await db
    .collection("deviceTokens")
    .doc(userId)
    .collection("tokens")
    .get();

  if (tokensSnapshot.empty) return;

  const tokens = tokensSnapshot.docs.map((doc) => doc.get("token") as string).filter(Boolean);
  if (tokens.length === 0) return;

  const response = await messaging.sendEachForMulticast({
    tokens,
    notification: { title: payload.title, body: payload.body },
    data: { type: payload.type, targetId: payload.targetId },
    android: {
      priority: "high",
      notification: { channelId: channelFor(payload.type) },
    },
  });

  const stale: string[] = [];
  response.responses.forEach((result, index) => {
    const code = result.error?.code;
    if (
      code === "messaging/registration-token-not-registered" ||
      code === "messaging/invalid-registration-token"
    ) {
      stale.push(tokens[index]);
    }
  });

  if (stale.length > 0) {
    const batch = db.batch();
    tokensSnapshot.docs
      .filter((doc) => stale.includes(doc.get("token")))
      .forEach((doc) => batch.delete(doc.ref));
    await batch.commit();
    log("fcm.tokens.pruned", { userId, removed: stale.length });
  }

  log("fcm.sent", {
    userId,
    success: response.successCount,
    failure: response.failureCount,
    type: payload.type,
  });
}

export async function isBlocked(a: string, b: string): Promise<boolean> {
  const [forward, reverse] = await Promise.all([
    db.collection("users").doc(a).collection("blocks").doc(b).get(),
    db.collection("users").doc(b).collection("blocks").doc(a).get(),
  ]);
  return forward.exists || reverse.exists;
}

/**
 * Fixed-window rate limiter backed by a single document per user + action.
 *
 * Cheap and predictable: one read-modify-write inside a transaction, bounded
 * document size, no unbounded history.
 */
export async function enforceRateLimit(
  userId: string,
  action: string,
  limit: number,
  windowMs: number,
): Promise<void> {
  const ref = db.collection("rateLimits").doc(userId);
  const now = Date.now();

  await db.runTransaction(async (tx) => {
    const snapshot = await tx.get(ref);
    const entry = (snapshot.get(action) ?? {}) as { count?: number; windowStart?: number };
    const windowStart = entry.windowStart ?? 0;
    const withinWindow = now - windowStart < windowMs;
    const count = withinWindow ? (entry.count ?? 0) : 0;

    if (withinWindow && count >= limit) {
      throw new HttpsError(
        "resource-exhausted",
        `Too many ${action} requests. Try again shortly.`,
      );
    }

    tx.set(
      ref,
      { [action]: { count: count + 1, windowStart: withinWindow ? windowStart : now } },
      { merge: true },
    );
  });
}

export function normalizeUsername(raw: string): string {
  return raw.trim().toLowerCase().replace(/[^a-z0-9._]/g, "");
}

/** Prefix list used for Firestore-compatible username search. */
export function usernamePrefixes(username: string, max = 20): string[] {
  const normalized = normalizeUsername(username);
  const prefixes: string[] = [];
  for (let i = 1; i <= Math.min(normalized.length, max); i += 1) {
    prefixes.push(normalized.slice(0, i));
  }
  return prefixes;
}

/** Tokenizes a caption so captions become queryable with array-contains. */
export function captionTokens(caption: string, max = 40): string[] {
  const stop = new Set([
    "the", "and", "for", "with", "this", "that", "from", "you", "your", "our",
    "was", "are", "but", "not", "all", "can", "has", "had", "its", "out",
  ]);
  return Array.from(
    new Set(
      caption
        .toLowerCase()
        .replace(/[^a-z0-9\s#]/g, " ")
        .split(/\s+/)
        .map((token) => token.replace(/^#/, ""))
        .filter((token) => token.length >= 3 && !stop.has(token)),
    ),
  ).slice(0, max);
}

export function extractHashtags(caption: string): string[] {
  const matches = caption.match(/#[\p{L}\p{N}_]+/gu) ?? [];
  return Array.from(new Set(matches.map((tag) => tag.slice(1).toLowerCase())));
}

export function extractMentions(caption: string): string[] {
  const matches = caption.match(/@[a-zA-Z0-9._]+/g) ?? [];
  return Array.from(new Set(matches.map((mention) => mention.slice(1).toLowerCase())));
}

/** Deletes every document matching a query in bounded batches. */
export async function deleteQueryInBatches(
  query: admin.firestore.Query,
  batchSize = 300,
): Promise<number> {
  let deleted = 0;
  for (;;) {
    const snapshot = await query.limit(batchSize).get();
    if (snapshot.empty) return deleted;
    const batch = db.batch();
    snapshot.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();
    deleted += snapshot.size;
    if (snapshot.size < batchSize) return deleted;
  }
}
