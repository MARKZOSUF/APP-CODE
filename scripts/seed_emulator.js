#!/usr/bin/env node
/**
 * Seeds the Firebase Emulator Suite with the same bundled demo dataset the
 * Android demo flavor uses.
 *
 * Safety: this script refuses to run unless the Firestore/Auth emulator host
 * variables are present. That makes it impossible to accidentally write the
 * fictional demo dataset into a real production project.
 *
 * Usage:
 *   firebase emulators:start --only auth,firestore,storage,functions
 *   node scripts/seed_emulator.js
 */

const fs = require("fs");
const path = require("path");

const SEED_PATH = path.join(
  __dirname,
  "..",
  "app",
  "src",
  "demo",
  "assets",
  "demo_seed.json",
);

function assertEmulator() {
  const firestoreHost = process.env.FIRESTORE_EMULATOR_HOST;
  const authHost = process.env.FIREBASE_AUTH_EMULATOR_HOST;

  if (!firestoreHost || !authHost) {
    console.error(
      [
        "Refusing to seed: emulator environment variables are not set.",
        "",
        "This guard exists so demo data can never be written to a production",
        "Firebase project. Export both variables first:",
        "",
        "  export FIRESTORE_EMULATOR_HOST=localhost:8080",
        "  export FIREBASE_AUTH_EMULATOR_HOST=localhost:9099",
        "  export FIREBASE_STORAGE_EMULATOR_HOST=localhost:9199",
        "",
      ].join("\n"),
    );
    process.exit(1);
  }

  if (!/^(localhost|127\.0\.0\.1|0\.0\.0\.0)/.test(firestoreHost)) {
    console.error(`Refusing to seed: ${firestoreHost} is not a local emulator.`);
    process.exit(1);
  }
}

async function main() {
  assertEmulator();

  if (!fs.existsSync(SEED_PATH)) {
    console.error(`Seed file not found at ${SEED_PATH}`);
    process.exit(1);
  }

  // firebase-admin is a dependency of ./functions.
  const admin = require(path.join(__dirname, "..", "functions", "node_modules", "firebase-admin"));

  const projectId = process.env.GCLOUD_PROJECT || "insangram-demo";
  admin.initializeApp({ projectId });

  const db = admin.firestore();
  const auth = admin.auth();
  const seed = JSON.parse(fs.readFileSync(SEED_PATH, "utf8"));

  console.log(`Seeding emulator project "${projectId}" (seedVersion ${seed.seedVersion})`);

  // ---- Auth users. Passwords are only ever the documented demo password.
  const DEMO_PASSWORD = "Demo@12345";
  for (const account of seed.accounts ?? []) {
    const user = (seed.users ?? []).find((candidate) => candidate.userId === account.userId);
    try {
      await auth.createUser({
        uid: account.userId,
        email: account.email,
        emailVerified: true,
        password: DEMO_PASSWORD,
        displayName: user?.fullName,
      });
    } catch (error) {
      if (error.code !== "auth/uid-already-exists") throw error;
    }
  }
  console.log(`  auth users: ${(seed.accounts ?? []).length}`);

  const commit = async (label, items, toRef) => {
    let batch = db.batch();
    let pending = 0;
    let written = 0;
    for (const item of items) {
      const { ref, data } = toRef(item);
      batch.set(ref, data, { merge: true });
      pending += 1;
      written += 1;
      if (pending === 400) {
        await batch.commit();
        batch = db.batch();
        pending = 0;
      }
    }
    if (pending > 0) await batch.commit();
    console.log(`  ${label}: ${written}`);
  };

  const ts = (millis) => admin.firestore.Timestamp.fromMillis(millis);

  await commit("users", seed.users ?? [], (user) => ({
    ref: db.collection("users").doc(user.userId),
    data: {
      username: user.username,
      normalizedUsername: user.username.toLowerCase(),
      usernamePrefixes: prefixes(user.username),
      fullName: user.fullName,
      bio: user.bio ?? "",
      website: user.website ?? "",
      pronouns: user.pronouns ?? "",
      photoUrl: "",
      isPrivate: Boolean(user.isPrivate),
      followerCount: 0,
      followingCount: 0,
      postCount: 0,
      showActivityStatus: true,
      showNotificationPreviews: true,
      createdAt: ts(user.createdAt ?? Date.now()),
    },
  }));

  await commit("usernames", seed.users ?? [], (user) => ({
    ref: db.collection("usernames").doc(user.username.toLowerCase()),
    data: {
      normalizedUsername: user.username.toLowerCase(),
      userId: user.userId,
      reservedAt: ts(user.createdAt ?? Date.now()),
    },
  }));

  await commit("posts", seed.posts ?? [], (post) => ({
    ref: db.collection("posts").doc(post.postId),
    data: {
      authorId: post.authorId,
      caption: post.caption ?? "",
      captionTokens: tokens(post.caption ?? ""),
      hashtags: post.hashtags ?? [],
      taggedUserIds: post.taggedUserIds ?? [],
      locationName: post.locationName ?? "",
      media: (post.media ?? []).map((item, index) => ({
        position: index,
        mediaType: item.mediaType ?? "IMAGE",
        altText: item.altText ?? "",
        // Emulator seeding does not upload bytes; the Android demo flavor
        // reads the same images straight from its bundled assets.
        mediaUrl: `asset://${item.asset ?? ""}`,
      })),
      commentsEnabled: post.commentsEnabled !== false,
      hideLikeCount: false,
      isArchived: false,
      deletedAt: null,
      likeCount: 0,
      commentCount: 0,
      saveCount: 0,
      shareCount: 0,
      viewCount: 0,
      feedScore: 0,
      createdAt: ts(post.createdAt ?? Date.now()),
    },
  }));

  await commit("reels", seed.reels ?? [], (reel) => ({
    ref: db.collection("reels").doc(reel.reelId),
    data: {
      authorId: reel.authorId,
      caption: reel.caption ?? "",
      hashtags: reel.hashtags ?? [],
      audioLabel: reel.audioLabel ?? "Original audio",
      videoUrl: "",
      thumbnailUrl: `asset://${reel.thumbnailAsset ?? ""}`,
      durationMs: reel.durationMs ?? 15000,
      commentsEnabled: true,
      deletedAt: null,
      likeCount: 0,
      commentCount: 0,
      saveCount: 0,
      shareCount: 0,
      viewCount: 0,
      totalWatchTimeMs: 0,
      feedScore: 0,
      createdAt: ts(reel.createdAt ?? Date.now()),
    },
  }));

  const now = Date.now();
  await commit(
    "stories",
    (seed.stories ?? []).filter((story) => (story.expiresAt ?? 0) > now),
    (story) => ({
      ref: db.collection("stories").doc(story.storyId),
      data: {
        authorId: story.authorId,
        storyType: story.storyType ?? "IMAGE",
        textContent: story.textContent ?? "",
        backgroundColor: story.backgroundColor ?? "",
        mediaUrl: story.asset ? `asset://${story.asset}` : "",
        audience: story.audience ?? "FOLLOWERS",
        allowReplies: true,
        isArchived: false,
        viewCount: 0,
        createdAt: ts(story.createdAt ?? now),
        expiresAt: ts(story.expiresAt ?? now + 86400000),
      },
    }),
  );

  await commit("follows", seed.follows ?? [], (follow) => ({
    ref: db.collection("follows").doc(`${follow.followerId}_${follow.followeeId}`),
    data: {
      followerId: follow.followerId,
      followeeId: follow.followeeId,
      state: follow.state ?? "FOLLOWING",
      isCloseFriend: Boolean(follow.isCloseFriend),
      mutedPosts: false,
      mutedStories: false,
      createdAt: ts(follow.createdAt ?? now),
    },
  }));

  await commit("conversations", seed.conversations ?? [], (conversation) => ({
    ref: db.collection("conversations").doc(conversation.conversationId),
    data: {
      isGroup: Boolean(conversation.isGroup),
      title: conversation.title ?? "",
      imageUrl: "",
      memberIds: conversation.memberIds ?? [],
      adminIds: conversation.adminIds ?? [conversation.memberIds?.[0]].filter(Boolean),
      lastMessagePreview: "",
      lastMessageAt: ts(conversation.createdAt ?? now),
      typingUserIds: [],
      createdAt: ts(conversation.createdAt ?? now),
    },
  }));

  await commit("messages", seed.messages ?? [], (message) => ({
    ref: db
      .collection("conversations")
      .doc(message.conversationId)
      .collection("messages")
      .doc(message.messageId),
    data: {
      senderId: message.senderId,
      text: message.text ?? "",
      mediaUrl: null,
      replyToMessageId: message.replyToMessageId ?? null,
      reactions: {},
      readByIds: [message.senderId],
      deliveryState: "DELIVERED",
      isUnsent: false,
      isPinned: false,
      createdAt: ts(message.createdAt ?? now),
    },
  }));

  await commit("hashtags", seed.hashtags ?? [], (hashtag) => ({
    ref: db.collection("hashtags").doc(hashtag.normalizedTag),
    data: {
      normalizedHashtag: hashtag.normalizedTag,
      displayTag: hashtag.displayTag ?? hashtag.normalizedTag,
      postCount: hashtag.postCount ?? 0,
      recentEngagement: hashtag.recentEngagement ?? 0,
      lastUsedAt: ts(hashtag.lastUsedAt ?? now),
    },
  }));

  console.log("");
  console.log("Done. Counters (likeCount, followerCount, ...) intentionally start");
  console.log("at zero: the Cloud Functions triggers own those values, so run the");
  console.log("functions emulator and interact with the app to see them move.");
  console.log("");
  console.log(`Demo sign-in: ${(seed.accounts ?? [])[0]?.email} / ${DEMO_PASSWORD}`);
}

function prefixes(username) {
  const normalized = username.toLowerCase();
  const out = [];
  for (let i = 1; i <= Math.min(normalized.length, 20); i += 1) {
    out.push(normalized.slice(0, i));
  }
  return out;
}

function tokens(caption) {
  return Array.from(
    new Set(
      caption
        .toLowerCase()
        .replace(/[^a-z0-9\s#]/g, " ")
        .split(/\s+/)
        .map((token) => token.replace(/^#/, ""))
        .filter((token) => token.length >= 3),
    ),
  ).slice(0, 40);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
