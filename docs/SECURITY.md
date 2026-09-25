# Security model

## Principles

1. **Deny by default.** Both rulesets start closed. There is no
   `allow read, write: if true;` anywhere, and `functions/test/rules.test.ts`
   asserts that with a regular expression.
2. **The client is untrusted.** Anything that must be true is enforced in rules
   or in a Cloud Function, never only in Kotlin.
3. **Least privilege.** Every rule grants the narrowest access that makes the
   feature work.

---

## Authentication

- Online mode uses Firebase Auth email/password, with optional Google Sign-In
  shown only when the flavor is configured for it.
- Passwords never reach Firestore, Room or the logs.
- Re-authentication is required before changing an email or deleting an account.
- Demo mode stores PBKDF2-SHA256 hashes: 120,000 iterations, a 16-byte random
  salt per account and a 256-bit derived key. The plaintext is discarded during
  seeding and is never written to Room.
- App Check with Play Integrity is enabled so requests must come from a genuine
  build of this app.

---

## Profile field allow-list

A user may write only:

```
username, normalizedUsername, fullName, bio, website, pronouns, photoUrl,
isPrivate, lastActiveAt, showActivityStatus, privacy,
notificationPreferences, usernamePrefixes, updatedAt
```

Any update touching a field outside that list is rejected. That single rule
simultaneously blocks counter forgery and privilege escalation — there is no
`isAdmin`, `role` or `claims` field a client could reach even if one existed.

---

## Privilege

Moderator status is a **Firebase Auth custom claim**, checked as
`request.auth.token.moderator == true`. Custom claims can only be set with the
Admin SDK. The Android client has no path to them at all.

`grantModeratorClaim` exists for development bootstrap and is inert unless
`MODERATOR_BOOTSTRAP_EMAIL` is configured in the functions environment. No
administrator credential appears anywhere in this repository.

---

## Private accounts

Readability of a post is evaluated in the rules, not the UI:

```
author is me
  OR author is not private
  OR follows/{me}_{author} exists with state == 'FOLLOWING'
```

A pending `REQUESTED` relationship does not grant access. A block in either
direction revokes it regardless of everything else.

---

## Trusted counters

`likeCount`, `commentCount`, `saveCount`, `shareCount`, `viewCount`,
`followerCount`, `followingCount`, `postCount`, `totalWatchTimeMs`, `feedScore`
and `createdAt` are in the protected set. Rules reject any client write to them.
The only writer is a Cloud Functions trigger, each guarded by an idempotency
key document so an at-least-once retry cannot double-count.

---

## Notifications

`notifications/{userId}/items/{id}` has `allow create: if false`. No client can
fabricate activity for anyone, including themselves. The recipient may read
their own items and may flip `isRead`, nothing more.

---

## Conversations

Membership is an array on the conversation document. Reading the conversation,
reading or writing its messages, and uploading to its Storage prefix all require
appearing in that array. A non-member cannot even learn a conversation exists.

A member may edit only their own message, and may add reactions and read
receipts to others' messages — not their text.

---

## Owner-only data

Saved posts, collections, search history and analytics live under
`users/{userId}/…` or `analytics/{userId}` and are readable only by that user.
Analytics is additionally write-denied to clients.

---

## Stories

Creation must set `expiresAt == request.time + duration.value(24, 'h')`, so a
client cannot mint an immortal story. Reads require `expiresAt > request.time`,
so an expired story becomes unreadable the moment it lapses — before any
scheduled cleanup runs. Archived stories are readable only by their owner.

---

## Storage

| Path | Who may write |
| --- | --- |
| `users/{uid}/profile/` | that user |
| `users/{uid}/posts/{postId}/` | that user |
| `users/{uid}/reels/{reelId}/` | that user |
| `users/{uid}/stories/{storyId}/` | that user |
| `conversations/{cid}/messages/{mid}/` | authenticated members of `cid` |

Every write also validates content type and size: images must be
`image/(jpeg|png|webp|heic|heif)` and at most 8 MB; videos must be
`video/(mp4|webm|quicktime)` and at most 120 MB. Unmatched paths are denied.

Cleanup jobs remove files from cancelled uploads, deleted posts and messages,
expired stories and deleted accounts.

---

## Rate limiting

`enforceRateLimit` uses a fixed window in a single `rateLimits/{uid}` document,
updated transactionally. Bounded document size, one read-write per call, and
fixed limits per action. Clients cannot write that collection.

---

## Secrets

Not present in this repository, by design:

- `app/google-services.json` — git-ignored; `.example` shows the shape
- Service-account keys — never committed
- `keystore.properties` and any `.jks` — git-ignored; signing is opt-in
- `.firebaserc` — git-ignored; `.example` shows the shape

Structured logs record ids and counts only. No token, password or request body
is ever logged.
