# Firestore schema

Server timestamps throughout. `R` = read, `C` = create, `U` = update,
`D` = delete. "owner" means `request.auth.uid` matches the document's owner.

---

## `users/{userId}`

| Field | Type | Notes |
| --- | --- | --- |
| `username` | string | display form |
| `normalizedUsername` | string | lowercase, unique via `usernames/` |
| `usernamePrefixes` | string[] | up to 20 prefixes, powers prefix search |
| `fullName` | string | |
| `bio`, `website`, `pronouns` | string | user supplied |
| `photoUrl` | string | Storage download URL |
| `isPrivate` | bool | gates all content reads |
| `followerCount`, `followingCount`, `postCount` | number | **functions only** |
| `showActivityStatus`, `showNotificationPreviews` | bool | |
| `privacy`, `notificationPreferences` | map | per-feature toggles |
| `createdAt`, `lastActiveAt` | timestamp | server |

**R** any signed-in user · **C** self at signup · **U** self, allow-listed
fields only · **D** self (cascades via `onUserDocumentDeleted`)
**Retention** until account deletion.

---

## `usernames/{normalizedUsername}`

`{ normalizedUsername, userId, reservedAt }`

**R** signed in · **C/U/D** the `reserveUsername` callable only, inside a
transaction that also updates the profile. Uniqueness cannot be expressed in
rules alone, which is why it is a function.

---

## `posts/{postId}`

| Field | Type | Notes |
| --- | --- | --- |
| `authorId` | string | immutable |
| `caption` | string | ≤ 2200 |
| `captionTokens` | string[] | derived server side, ≤ 40 |
| `hashtags`, `taggedUserIds`, `mentionedUsernames` | string[] | |
| `locationName` | string | |
| `media` | array of maps | `{ position, mediaUrl, thumbnailUrl, mediaType, widthPx, heightPx, altText }` |
| `commentsEnabled`, `hideLikeCount`, `isArchived` | bool | |
| `likeCount`, `commentCount`, `saveCount`, `shareCount`, `viewCount`, `feedScore` | number | **functions only** |
| `createdAt`, `editedAt`, `deletedAt` | timestamp | `deletedAt` = soft delete |

**R** author, or any signed-in user when the author is public, or an approved
follower · **C** author with `authorId == uid` and server `createdAt` ·
**U** author, protected counters excluded · **D** author
**Indexes** `authorId + createdAt`, `authorId + isArchived + createdAt`,
`hashtags + feedScore`, `captionTokens + createdAt`, `locationName + createdAt`,
`taggedUserIds + createdAt`
**Retention** 30 days after `deletedAt`, then purged with its media.

### `posts/{postId}/likes/{userId}`
`{ userId, createdAt }`. The document id **is** the user id, so double-liking is
structurally impossible. **C/D** self only, and only if the post is visible.

### `posts/{postId}/comments/{commentId}`
`{ authorId, text, replyToCommentId, likeCount, isPinned, createdAt, editedAt }`.
**C** any viewer when `commentsEnabled` · **U** comment author (text) or post
author (`isPinned`) · **D** comment author or post author.
`likeCount` is functions-only.

### `posts/{postId}/comments/{commentId}/likes/{userId}`
Same id-as-uid pattern.

---

## `reels/{reelId}` and `reels/{reelId}/likes/{userId}`

As posts, plus `videoUrl`, `thumbnailUrl`, `durationMs`, `audioLabel`,
`totalWatchTimeMs`. **Indexes** `feedScore desc`, `authorId + createdAt`.

---

## `stories/{storyId}`

`{ authorId, storyType, mediaUrl, textContent, backgroundColor, fontStyle,
audience, allowReplies, isArchived, viewCount, createdAt, expiresAt }`

**C** requires `expiresAt == request.time + duration.value(24, 'h')`.
**R** author always; others only while `expiresAt > request.time`, subject to
the audience and the private-account check. Archived stories are owner-only.
**Indexes** `authorId + expiresAt`, `isArchived + expiresAt`
**Retention** archived on expiry, or deleted with its media.

### `stories/{storyId}/views/{viewerId}`
`{ viewerId, reactionEmoji, viewedAt }`. **C** self · **R** story author only.

---

## `follows/{followerId}_{followeeId}`

`{ followerId, followeeId, state, isCloseFriend, mutedPosts, mutedStories,
createdAt }` where `state` is `FOLLOWING` or `REQUESTED`.

The composite id makes duplicate relationships impossible. **C** the follower,
with `REQUESTED` forced when the target is private · **U** the follower for mute
and close-friend flags; the **followee only** may promote `REQUESTED` →
`FOLLOWING`, which is how approval works · **D** either party.
**Indexes** `followerId + state + createdAt`, `followeeId + state + createdAt`

---

## `conversations/{conversationId}`

`{ isGroup, title, imageUrl, memberIds[], adminIds[], lastMessagePreview,
lastMessageSenderId, lastMessageAt, typingUserIds[], createdAt }`

**R/U** members · **C** creator, who must include themselves in `memberIds` ·
group metadata edits restricted to `adminIds`.
**Index** `memberIds array-contains + lastMessageAt desc`

### `conversations/{id}/members/{userId}`
`{ joinedAt, unreadCount, mutedUntil, lastReadAt }`. `unreadCount` is
functions-maintained; a member may clear their own.

### `conversations/{id}/messages/{messageId}`
`{ senderId, text, mediaUrl, mediaType, replyToMessageId, reactions,
readByIds[], deliveryState, isUnsent, isPinned, idempotencyKey, createdAt }`

**R** members · **C** members with `senderId == uid` and a server timestamp ·
**U** sender for unsend, any member for reactions and read receipts ·
**D** sender. **Index** `createdAt desc`

---

## `notifications/{userId}/items/{notificationId}`

`{ recipientId, actorId, type, targetId, previewText, previewImageUrl, isRead,
createdAt }`

**R** recipient only · **C `if false`** — Cloud Functions only ·
**U** recipient, `isRead` only · **D** recipient
**Index** `isRead + createdAt desc`

---

## `users/{userId}/savedPosts/{postId}` · `collections/{id}` · `searchHistory/{id}`

Owner-only for every operation. Saved posts carry `{ postId, collectionId,
savedAt }`; collections carry `{ name, coverPostId, createdAt }`; history
carries `{ term, kind, searchedAt }`.

---

## `reports/{reportId}`

`{ reporterId, targetType, targetId, reason, details, state, createdAt }` with
`targetType` in `USER | POST | REEL | STORY | COMMENT | MESSAGE` and `state` in
`SUBMITTED | UNDER_REVIEW | ACTIONED | REJECTED`.

**C** any signed-in user, `state` forced to `SUBMITTED` · **R/U** the reporter
(own reports) or a moderator · state transitions are moderator-only.
**Index** `state + createdAt`

---

## `hashtags/{normalizedHashtag}`

`{ normalizedHashtag, displayTag, postCount, recentEngagement, lastUsedAt }`

**R** signed in · **write `if false`** — functions only.
**Index** `recentEngagement + postCount`

---

## `analytics/{userId}`

One bounded aggregate document per creator: `{ postViews, reelViews, likes,
comments, saves, shares, followers, profileVisits, watchTimeMs,
weeklyEngagement, monthlyEngagement, followerGrowth, updatedAt }`

**R** owner only · **write `if false`** — functions only. Bounded by design so
the dashboard costs one read rather than a collection scan.

---

## `feedItems/{userId}/items/{itemId}` · `deviceTokens/{userId}/tokens/{tokenId}` · `notes/{noteId}`

- **feedItems** — precomputed fan-out slots, owner-read, functions-write.
- **deviceTokens** — `{ token, platform, refreshedAt }`, owner-only; stale
  entries pruned after 60 days and invalid ones removed on send failure.
- **notes** — `{ authorId, text, emoji, audience, createdAt, expiresAt }`,
  readable by the chosen audience while unexpired, deleted on a schedule.
  **Index** `authorId + expiresAt`

---

## `rateLimits/{userId}` and `idempotency/{key}`

Internal. Both are **read and write denied to all clients** and exist purely so
functions can throttle abuse and make at-least-once triggers safe.
