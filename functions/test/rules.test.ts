/**
 * Firestore and Storage security-rules tests.
 *
 * Run with the emulator running:
 *   firebase emulators:exec --only firestore,storage "npm test"
 *
 * These assertions encode the guarantees the project claims: private content
 * stays private, counters cannot be forged, notifications cannot be spoofed,
 * and saved content is owner-only.
 */

import { assertFails, assertSucceeds, initializeTestEnvironment, RulesTestEnvironment } from "@firebase/rules-unit-testing";
import { readFileSync } from "fs";
import { expect } from "chai";

let testEnv: RulesTestEnvironment;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: "insangram-rules-test",
    firestore: {
      rules: readFileSync("../firebase/firestore.rules", "utf8"),
    },
    storage: {
      rules: readFileSync("../firebase/storage.rules", "utf8"),
    },
  });
});

after(async () => {
  await testEnv.cleanup();
});

beforeEach(async () => {
  await testEnv.clearFirestore();

  // Seed baseline documents with admin privileges (rules bypassed).
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.doc("users/alice").set({
      username: "alice",
      normalizedUsername: "alice",
      fullName: "Alice A",
      isPrivate: false,
      followerCount: 0,
      followingCount: 0,
      postCount: 0,
    });
    await db.doc("users/mallory").set({
      username: "mallory",
      normalizedUsername: "mallory",
      fullName: "Mallory M",
      isPrivate: true,
      followerCount: 0,
      followingCount: 0,
      postCount: 0,
    });
    await db.doc("posts/post-alice").set({
      authorId: "alice",
      caption: "hello",
      media: [{ mediaUrl: "x", mediaType: "IMAGE" }],
      commentsEnabled: true,
      likeCount: 7,
      commentCount: 0,
      saveCount: 0,
      shareCount: 0,
      viewCount: 0,
      deletedAt: null,
      isArchived: false,
    });
    await db.doc("posts/post-mallory").set({
      authorId: "mallory",
      caption: "private",
      media: [{ mediaUrl: "x", mediaType: "IMAGE" }],
      commentsEnabled: true,
      likeCount: 0,
      commentCount: 0,
      saveCount: 0,
      shareCount: 0,
      viewCount: 0,
      deletedAt: null,
      isArchived: false,
    });
    await db.doc("conversations/conv-1").set({
      isGroup: false,
      memberIds: ["alice", "bob"],
      adminIds: ["alice"],
    });
    await db.doc("conversations/conv-1/messages/m1").set({
      senderId: "alice",
      text: "hi bob",
    });
  });
});

const asAlice = () => testEnv.authenticatedContext("alice").firestore();
const asBob = () => testEnv.authenticatedContext("bob").firestore();
const asAnon = () => testEnv.unauthenticatedContext().firestore();

describe("users", () => {
  it("denies all access to unauthenticated callers", async () => {
    await assertFails(asAnon().doc("users/alice").get());
  });

  it("lets a user edit their own display fields", async () => {
    await assertSucceeds(asAlice().doc("users/alice").update({ bio: "Updated bio" }));
  });

  it("stops a user editing somebody else's profile", async () => {
    await assertFails(asBob().doc("users/alice").update({ bio: "hacked" }));
  });

  it("stops a user forging their own follower count", async () => {
    await assertFails(asAlice().doc("users/alice").update({ followerCount: 999999 }));
  });

  it("stops a user granting themselves admin privileges", async () => {
    await assertFails(asAlice().doc("users/alice").update({ isAdmin: true }));
    await assertFails(asAlice().doc("users/alice").update({ role: "moderator" }));
  });
});

describe("posts", () => {
  it("allows reading a public author's post", async () => {
    await assertSucceeds(asBob().doc("posts/post-alice").get());
  });

  it("hides a private author's post from a non-follower", async () => {
    await assertFails(asBob().doc("posts/post-mallory").get());
  });

  it("stops a non-owner editing a post", async () => {
    await assertFails(asBob().doc("posts/post-alice").update({ caption: "defaced" }));
  });

  it("stops the owner forging the like count", async () => {
    await assertFails(asAlice().doc("posts/post-alice").update({ likeCount: 5000 }));
  });

  it("allows the owner to edit their caption", async () => {
    await assertSucceeds(asAlice().doc("posts/post-alice").update({ caption: "edited" }));
  });

  it("makes double-liking impossible by keying likes on the user id", async () => {
    await assertFails(
      asBob().doc("posts/post-alice/likes/alice").set({ userId: "bob" }),
    );
  });
});

describe("saved content", () => {
  it("is readable only by its owner", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().doc("users/alice/savedPosts/post-alice").set({ postId: "post-alice" });
    });
    await assertSucceeds(asAlice().doc("users/alice/savedPosts/post-alice").get());
    await assertFails(asBob().doc("users/alice/savedPosts/post-alice").get());
  });
});

describe("messages", () => {
  it("lets a member read conversation messages", async () => {
    await assertSucceeds(asAlice().doc("conversations/conv-1/messages/m1").get());
    await assertSucceeds(asBob().doc("conversations/conv-1/messages/m1").get());
  });

  it("blocks a non-member entirely", async () => {
    const asCarol = testEnv.authenticatedContext("carol").firestore();
    await assertFails(asCarol.doc("conversations/conv-1").get());
    await assertFails(asCarol.doc("conversations/conv-1/messages/m1").get());
    await assertFails(
      asCarol.collection("conversations/conv-1/messages").add({ senderId: "carol", text: "hi" }),
    );
  });

  it("stops a member rewriting another member's message text", async () => {
    await assertFails(asBob().doc("conversations/conv-1/messages/m1").update({ text: "forged" }));
  });

  it("lets a member add a reaction", async () => {
    await assertSucceeds(
      asBob().doc("conversations/conv-1/messages/m1").update({ reactions: { bob: "\u2764\ufe0f" } }),
    );
  });
});

describe("notifications", () => {
  it("cannot be created by a client for anybody", async () => {
    await assertFails(
      asBob().collection("notifications/alice/items").add({
        recipientId: "alice",
        actorId: "bob",
        type: "POST_LIKE",
      }),
    );
  });

  it("is readable only by the recipient", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().doc("notifications/alice/items/n1").set({
        recipientId: "alice",
        actorId: "bob",
        type: "POST_LIKE",
        isRead: false,
      });
    });
    await assertSucceeds(asAlice().doc("notifications/alice/items/n1").get());
    await assertFails(asBob().doc("notifications/alice/items/n1").get());
  });

  it("lets the recipient mark it read but not rewrite it", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().doc("notifications/alice/items/n1").set({
        recipientId: "alice",
        actorId: "bob",
        type: "POST_LIKE",
        isRead: false,
      });
    });
    await assertSucceeds(asAlice().doc("notifications/alice/items/n1").update({ isRead: true }));
    await assertFails(asAlice().doc("notifications/alice/items/n1").update({ type: "MENTION" }));
  });
});

describe("analytics and hashtags", () => {
  it("exposes analytics to the owner only", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().doc("analytics/alice").set({ postViews: 10 });
    });
    await assertSucceeds(asAlice().doc("analytics/alice").get());
    await assertFails(asBob().doc("analytics/alice").get());
  });

  it("forbids clients writing analytics or hashtag counters", async () => {
    await assertFails(asAlice().doc("analytics/alice").set({ postViews: 99999 }));
    await assertFails(asAlice().doc("hashtags/travel").set({ postCount: 99999 }));
  });
});

describe("reports", () => {
  it("lets a user file a report in the SUBMITTED state only", async () => {
    await assertFails(
      asAlice().collection("reports").add({
        reporterId: "alice",
        targetType: "POST",
        targetId: "post-mallory",
        reason: "spam",
        state: "ACTIONED",
      }),
    );
  });

  it("stops a non-moderator changing a report outcome", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().doc("reports/r1").set({
        reporterId: "alice",
        targetType: "POST",
        targetId: "post-mallory",
        reason: "spam",
        state: "SUBMITTED",
      });
    });
    await assertFails(asAlice().doc("reports/r1").update({ state: "ACTIONED" }));
  });
});

describe("rules hygiene", () => {
  it("contains no unconditional allow rule", () => {
    const firestoreRules = readFileSync("../firebase/firestore.rules", "utf8");
    const storageRules = readFileSync("../firebase/storage.rules", "utf8");
    const permissive = /^\s*allow[^;]*:\s*if\s+true\s*;/m;
    expect(permissive.test(firestoreRules)).to.equal(false);
    expect(permissive.test(storageRules)).to.equal(false);
  });
});
