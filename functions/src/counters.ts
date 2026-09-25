import { onDocumentCreated, onDocumentDeleted, onDocumentWritten } from "firebase-functions/v2/firestore";
import {
  FieldValue,
  captionTokens,
  claimOnce,
  createNotification,
  db,
  extractHashtags,
  extractMentions,
  log,
  logError,
} from "./common";

/**
 * Trusted counters.
 *
 * Security rules forbid clients from writing likeCount / commentCount /
 * followerCount and friends. These triggers are the only writer, so the numbers
 * the app displays can be trusted and cannot be inflated from a device.
 */

export const onPostLikeCreated = onDocumentCreated(
  "posts/{postId}/likes/{userId}",
  async (event) => {
    const { postId, userId } = event.params;
    try {
      await db.collection("posts").doc(postId).update({ likeCount: FieldValue.increment(1) });

      const post = await db.collection("posts").doc(postId).get();
      const authorId = post.get("authorId") as string | undefined;
      if (!authorId) return;

      await bumpAnalytics(authorId, { likes: 1 });
      await createNotification({
        recipientId: authorId,
        actorId: userId,
        type: "POST_LIKE",
        targetId: postId,
        previewText: ((post.get("caption") as string) ?? "").slice(0, 80),
        idempotencyKey: `post-like:${postId}:${userId}`,
      });
      log("counter.post.like.increment", { postId, userId });
    } catch (error) {
      logError("counter.post.like.failed", error, { postId, userId });
      throw error;
    }
  },
);

export const onPostLikeDeleted = onDocumentDeleted(
  "posts/{postId}/likes/{userId}",
  async (event) => {
    const { postId } = event.params;
    // A missing post simply means the parent was deleted first; nothing to do.
    const ref = db.collection("posts").doc(postId);
    if (!(await ref.get()).exists) return;
    await ref.update({ likeCount: FieldValue.increment(-1) });
    log("counter.post.like.decrement", { postId });
  },
);

export const onReelLikeCreated = onDocumentCreated(
  "reels/{reelId}/likes/{userId}",
  async (event) => {
    const { reelId, userId } = event.params;
    await db.collection("reels").doc(reelId).update({ likeCount: FieldValue.increment(1) });
    const reel = await db.collection("reels").doc(reelId).get();
    const authorId = reel.get("authorId") as string | undefined;
    if (!authorId) return;
    await bumpAnalytics(authorId, { likes: 1 });
    await createNotification({
      recipientId: authorId,
      actorId: userId,
      type: "REEL_LIKE",
      targetId: reelId,
      previewText: ((reel.get("caption") as string) ?? "").slice(0, 80),
      idempotencyKey: `reel-like:${reelId}:${userId}`,
    });
  },
);

export const onReelLikeDeleted = onDocumentDeleted(
  "reels/{reelId}/likes/{userId}",
  async (event) => {
    const ref = db.collection("reels").doc(event.params.reelId);
    if (!(await ref.get()).exists) return;
    await ref.update({ likeCount: FieldValue.increment(-1) });
  },
);

export const onCommentCreated = onDocumentCreated(
  "posts/{postId}/comments/{commentId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;
    const { postId, commentId } = event.params;

    const authorOfComment = snapshot.get("authorId") as string;
    const text = ((snapshot.get("text") as string) ?? "").slice(0, 200);
    const replyTo = snapshot.get("replyToCommentId") as string | null;

    await db.collection("posts").doc(postId).update({ commentCount: FieldValue.increment(1) });

    const post = await db.collection("posts").doc(postId).get();
    const postAuthorId = post.get("authorId") as string | undefined;
    if (postAuthorId) {
      await bumpAnalytics(postAuthorId, { comments: 1 });
      await createNotification({
        recipientId: postAuthorId,
        actorId: authorOfComment,
        type: "COMMENT",
        targetId: postId,
        previewText: text,
        idempotencyKey: `comment:${commentId}`,
      });
    }

    // Replies additionally notify the parent comment's author.
    if (replyTo) {
      const parent = await db
        .collection("posts")
        .doc(postId)
        .collection("comments")
        .doc(replyTo)
        .get();
      const parentAuthor = parent.get("authorId") as string | undefined;
      if (parentAuthor) {
        await createNotification({
          recipientId: parentAuthor,
          actorId: authorOfComment,
          type: "REPLY",
          targetId: postId,
          previewText: text,
          idempotencyKey: `reply:${commentId}`,
        });
      }
    }

    await notifyMentions(text, authorOfComment, postId, `comment-mention:${commentId}`);
  },
);

export const onCommentDeleted = onDocumentDeleted(
  "posts/{postId}/comments/{commentId}",
  async (event) => {
    const { postId, commentId } = event.params;
    const ref = db.collection("posts").doc(postId);
    if ((await ref.get()).exists) {
      await ref.update({ commentCount: FieldValue.increment(-1) });
    }
    // Cascade: replies and likes of the removed comment.
    const replies = await ref
      .collection("comments")
      .where("replyToCommentId", "==", commentId)
      .get();
    const batch = db.batch();
    replies.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();
  },
);

export const onCommentLikeWritten = onDocumentWritten(
  "posts/{postId}/comments/{commentId}/likes/{userId}",
  async (event) => {
    const created = !event.data?.before.exists && event.data?.after.exists;
    const removed = event.data?.before.exists && !event.data?.after.exists;
    if (!created && !removed) return;

    const ref = db
      .collection("posts")
      .doc(event.params.postId)
      .collection("comments")
      .doc(event.params.commentId);
    if (!(await ref.get()).exists) return;
    await ref.update({ likeCount: FieldValue.increment(created ? 1 : -1) });
  },
);

export const onSavedPostWritten = onDocumentWritten(
  "users/{userId}/savedPosts/{postId}",
  async (event) => {
    const created = !event.data?.before.exists && event.data?.after.exists;
    const removed = event.data?.before.exists && !event.data?.after.exists;
    if (!created && !removed) return;

    const ref = db.collection("posts").doc(event.params.postId);
    if (!(await ref.get()).exists) return;
    await ref.update({ saveCount: FieldValue.increment(created ? 1 : -1) });
    if (created) {
      const authorId = (await ref.get()).get("authorId") as string | undefined;
      if (authorId) await bumpAnalytics(authorId, { saves: 1 });
    }
  },
);

/**
 * Derives searchable fields and hashtag documents whenever a post changes.
 *
 * Firestore has no full-text search, so search is made possible by writing
 * bounded, indexable arrays (`captionTokens`) and per-hashtag documents here on
 * the server rather than trusting the client to maintain them.
 */
export const onPostWritten = onDocumentWritten("posts/{postId}", async (event) => {
  const after = event.data?.after;
  const before = event.data?.before;
  const { postId } = event.params;

  if (!after?.exists) {
    // Post removed: decrement hashtag counters and the author's post count.
    const oldTags = (before?.get("hashtags") as string[] | undefined) ?? [];
    const authorId = before?.get("authorId") as string | undefined;
    const batch = db.batch();
    oldTags.forEach((tag) => {
      batch.set(
        db.collection("hashtags").doc(tag),
        { postCount: FieldValue.increment(-1) },
        { merge: true },
      );
    });
    if (authorId) {
      batch.set(
        db.collection("users").doc(authorId),
        { postCount: FieldValue.increment(-1) },
        { merge: true },
      );
    }
    await batch.commit();
    return;
  }

  const caption = (after.get("caption") as string) ?? "";
  const tokens = captionTokens(caption);
  const hashtags = extractHashtags(caption);
  const authorId = after.get("authorId") as string;

  const existingTokens = (after.get("captionTokens") as string[] | undefined) ?? [];
  const tokensChanged = existingTokens.join(",") !== tokens.join(",");
  if (tokensChanged) {
    // Idempotent: writing identical derived data is a no-op we skip entirely.
    await after.ref.set({ captionTokens: tokens, hashtags }, { merge: true });
  }

  if (!before?.exists) {
    const batch = db.batch();
    batch.set(
      db.collection("users").doc(authorId),
      { postCount: FieldValue.increment(1) },
      { merge: true },
    );
    hashtags.forEach((tag) => {
      batch.set(
        db.collection("hashtags").doc(tag),
        {
          normalizedHashtag: tag,
          displayTag: tag,
          postCount: FieldValue.increment(1),
          recentEngagement: FieldValue.increment(1),
          lastUsedAt: FieldValue.serverTimestamp(),
        },
        { merge: true },
      );
    });
    await batch.commit();

    await notifyMentions(caption, authorId, postId, `post-mention:${postId}`);

    const tagged = (after.get("taggedUserIds") as string[] | undefined) ?? [];
    await Promise.all(
      tagged.map((taggedId) =>
        createNotification({
          recipientId: taggedId,
          actorId: authorId,
          type: "TAG",
          targetId: postId,
          previewText: caption.slice(0, 80),
          idempotencyKey: `post-tag:${postId}:${taggedId}`,
        }),
      ),
    );
  }
});

async function notifyMentions(
  text: string,
  actorId: string,
  targetId: string,
  keyPrefix: string,
): Promise<void> {
  const mentions = extractMentions(text);
  if (mentions.length === 0) return;

  // Bounded: at most 10 mentions are honoured per piece of content.
  await Promise.all(
    mentions.slice(0, 10).map(async (username) => {
      const match = await db
        .collection("users")
        .where("normalizedUsername", "==", username)
        .limit(1)
        .get();
      if (match.empty) return;
      await createNotification({
        recipientId: match.docs[0].id,
        actorId,
        type: "MENTION",
        targetId,
        previewText: text.slice(0, 80),
        idempotencyKey: `${keyPrefix}:${username}`,
      });
    }),
  );
}

/** Bounded aggregate document, one per creator - never an unbounded scan. */
export async function bumpAnalytics(
  userId: string,
  delta: Partial<Record<
    "postViews" | "reelViews" | "likes" | "comments" | "saves" | "shares" | "followers" | "profileVisits",
    number
  >> & { watchTimeMs?: number },
): Promise<void> {
  const increments: Record<string, unknown> = { updatedAt: FieldValue.serverTimestamp() };
  Object.entries(delta).forEach(([key, value]) => {
    if (typeof value === "number" && value !== 0) {
      increments[key] = FieldValue.increment(value);
    }
  });
  await db.collection("analytics").doc(userId).set(increments, { merge: true });
}

export const onStoryViewCreated = onDocumentCreated(
  "stories/{storyId}/views/{viewerId}",
  async (event) => {
    const { storyId, viewerId } = event.params;
    if (!(await claimOnce(`story-view:${storyId}:${viewerId}`))) return;
    const ref = db.collection("stories").doc(storyId);
    if (!(await ref.get()).exists) return;
    await ref.update({ viewCount: FieldValue.increment(1) });
  },
);
