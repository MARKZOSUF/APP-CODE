package com.insangram.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room cache of a user profile. `cachedAt` drives TTL invalidation; the cache
 * is authoritative only in demo mode.
 */
@Entity(
    tableName = "cached_user",
    indices = [Index("normalizedUsername", unique = true), Index("cachedAt")],
)
data class CachedUser(
    @PrimaryKey val userId: String,
    val username: String,
    val normalizedUsername: String,
    val fullName: String,
    val bio: String = "",
    val website: String = "",
    val pronouns: String = "",
    val photoUrl: String? = null,
    val isPrivate: Boolean = false,
    val isVerifiedBadge: Boolean = false,
    val postCount: Long = 0,
    val followerCount: Long = 0,
    val followingCount: Long = 0,
    val createdAt: Long = 0,
    val lastActiveAt: Long = 0,
    val showActivityStatus: Boolean = true,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "cached_post",
    indices = [Index("authorId"), Index("createdAt"), Index("feedScore"), Index("isArchived")],
)
data class CachedPost(
    @PrimaryKey val postId: String,
    val authorId: String,
    val caption: String = "",
    val locationName: String? = null,
    val hashtags: List<String> = emptyList(),
    val taggedUserIds: List<String> = emptyList(),
    val mentionedUsernames: List<String> = emptyList(),
    val likeCount: Long = 0,
    val commentCount: Long = 0,
    val saveCount: Long = 0,
    val shareCount: Long = 0,
    val viewCount: Long = 0,
    val commentsEnabled: Boolean = true,
    val hideLikeCount: Boolean = false,
    val isArchived: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = 0,
    val editedAt: Long? = null,
    val feedScore: Double = 0.0,
    val cachedAt: Long = System.currentTimeMillis(),
)

/** Carousel items for a post, ordered by [position]. */
@Entity(
    tableName = "cached_post_media",
    primaryKeys = ["postId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = CachedPost::class,
            parentColumns = ["postId"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("postId")],
)
data class CachedPostMedia(
    val postId: String,
    val position: Int,
    val mediaUrl: String,
    val thumbnailUrl: String? = null,
    val mediaType: String = "IMAGE",
    val widthPx: Int = 0,
    val heightPx: Int = 0,
    val durationMs: Long = 0,
    val altText: String = "",
    val localUri: String? = null,
    val mediaHash: String? = null,
)

@Entity(
    tableName = "cached_story",
    indices = [Index("authorId"), Index("expiresAt"), Index("createdAt")],
)
data class CachedStory(
    @PrimaryKey val storyId: String,
    val authorId: String,
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val storyType: String = "PHOTO",
    val textContent: String = "",
    val backgroundColor: Long = 0,
    val fontStyle: String = "DEFAULT",
    val durationMs: Long = 5_000,
    val audience: String = "EVERYONE",
    val allowReplies: Boolean = true,
    val viewCount: Long = 0,
    val seenByMe: Boolean = false,
    val isArchived: Boolean = false,
    val highlightIds: List<String> = emptyList(),
    val createdAt: Long = 0,
    val expiresAt: Long = 0,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "cached_reel",
    indices = [Index("authorId"), Index("createdAt"), Index("feedScore")],
)
data class CachedReel(
    @PrimaryKey val reelId: String,
    val authorId: String,
    val videoUrl: String,
    val thumbnailUrl: String? = null,
    val caption: String = "",
    val audioLabel: String = "Original audio",
    val durationMs: Long = 0,
    val widthPx: Int = 0,
    val heightPx: Int = 0,
    val hashtags: List<String> = emptyList(),
    val likeCount: Long = 0,
    val commentCount: Long = 0,
    val saveCount: Long = 0,
    val shareCount: Long = 0,
    val viewCount: Long = 0,
    val totalWatchTimeMs: Long = 0,
    val commentsEnabled: Boolean = true,
    val deletedAt: Long? = null,
    val createdAt: Long = 0,
    val feedScore: Double = 0.0,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "cached_comment",
    indices = [Index("parentContentId"), Index("authorId"), Index("replyToCommentId"), Index("createdAt")],
)
data class CachedComment(
    @PrimaryKey val commentId: String,
    /** Post id or reel id the comment belongs to. */
    val parentContentId: String,
    val parentContentType: String = "POST",
    val authorId: String,
    val text: String,
    val replyToCommentId: String? = null,
    val likeCount: Long = 0,
    val likedByMe: Boolean = false,
    val isPinned: Boolean = false,
    val editedAt: Long? = null,
    val createdAt: Long = 0,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "cached_conversation",
    indices = [Index("lastMessageAt"), Index("isGroup")],
)
data class CachedConversation(
    @PrimaryKey val conversationId: String,
    val isGroup: Boolean = false,
    val title: String = "",
    val imageUrl: String? = null,
    val memberIds: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val lastMessagePreview: String = "",
    val lastMessageSenderId: String? = null,
    val lastMessageAt: Long = 0,
    val unreadCount: Int = 0,
    val mutedUntil: Long? = null,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "cached_message",
    foreignKeys = [
        ForeignKey(
            entity = CachedConversation::class,
            parentColumns = ["conversationId"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("conversationId"), Index("createdAt"), Index("deliveryState")],
)
data class CachedMessage(
    @PrimaryKey val messageId: String,
    val conversationId: String,
    val senderId: String,
    val text: String = "",
    val mediaUrl: String? = null,
    val localMediaUri: String? = null,
    val mediaType: String? = null,
    val replyToMessageId: String? = null,
    val replyToPreview: String? = null,
    /** userId -> emoji */
    val reactions: Map<String, String> = emptyMap(),
    val readByIds: List<String> = emptyList(),
    /** SENDING, SENT, DELIVERED, READ, FAILED */
    val deliveryState: String = "SENT",
    val isUnsent: Boolean = false,
    val hiddenForMe: Boolean = false,
    val isPinned: Boolean = false,
    val idempotencyKey: String? = null,
    val createdAt: Long = 0,
)

@Entity(tableName = "cached_notification", indices = [Index("createdAt"), Index("isRead")])
data class CachedNotification(
    @PrimaryKey val notificationId: String,
    val recipientId: String,
    val actorId: String,
    val type: String,
    val targetId: String? = null,
    val previewText: String = "",
    val previewImageUrl: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = 0,
)

@Entity(tableName = "cached_note", indices = [Index("authorId"), Index("expiresAt")])
data class CachedNote(
    @PrimaryKey val noteId: String,
    val authorId: String,
    val text: String,
    val emoji: String = "",
    val audience: String = "FOLLOWERS",
    val createdAt: Long = 0,
    val expiresAt: Long = 0,
)

/** Denormalised follow edge. Composite key prevents duplicate relationships. */
@Entity(
    tableName = "follow_edge",
    primaryKeys = ["followerId", "followeeId"],
    indices = [Index("followeeId"), Index("state")],
)
data class FollowEdge(
    val followerId: String,
    val followeeId: String,
    /** FOLLOWING, REQUESTED */
    val state: String = "FOLLOWING",
    val isCloseFriend: Boolean = false,
    val mutedPosts: Boolean = false,
    val mutedStories: Boolean = false,
    val createdAt: Long = 0,
)

@Entity(tableName = "block_edge", primaryKeys = ["blockerId", "blockedId"])
data class BlockEdge(
    val blockerId: String,
    val blockedId: String,
    val restrictedOnly: Boolean = false,
    val createdAt: Long = 0,
)

@Entity(tableName = "saved_post", primaryKeys = ["userId", "postId"], indices = [Index("collectionId")])
data class SavedPostEntity(
    val userId: String,
    val postId: String,
    val collectionId: String? = null,
    val savedAt: Long = 0,
)

@Entity(tableName = "saved_collection", indices = [Index("ownerId")])
data class SavedCollectionEntity(
    @PrimaryKey val collectionId: String,
    val ownerId: String,
    val name: String,
    val coverPostId: String? = null,
    val createdAt: Long = 0,
)

@Entity(tableName = "post_like", primaryKeys = ["userId", "contentId"])
data class LikeEntity(
    val userId: String,
    val contentId: String,
    val contentType: String = "POST",
    val createdAt: Long = 0,
)

@Entity(tableName = "story_view", primaryKeys = ["storyId", "viewerId"])
data class StoryViewEntity(
    val storyId: String,
    val viewerId: String,
    val reactionEmoji: String? = null,
    val viewedAt: Long = 0,
)

@Entity(tableName = "hashtag", indices = [Index("postCount")])
data class HashtagEntity(
    @PrimaryKey val normalizedTag: String,
    val displayTag: String,
    val postCount: Long = 0,
    val recentEngagement: Long = 0,
    val lastUsedAt: Long = 0,
)

@Entity(tableName = "search_history", indices = [Index("searchedAt")])
data class SearchHistoryEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val term: String,
    /** USER, HASHTAG, PLACE, TEXT */
    val kind: String = "TEXT",
    val searchedAt: Long = 0,
)

/** Paging 3 remote keys for the home feed. */
@Entity(tableName = "feed_remote_key")
data class FeedRemoteKey(
    @PrimaryKey val postId: String,
    val ownerFeedId: String,
    val prevCursor: String? = null,
    val nextCursor: String? = null,
    val position: Int = 0,
    val fetchedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "reel_remote_key")
data class ReelRemoteKey(
    @PrimaryKey val reelId: String,
    val nextCursor: String? = null,
    val position: Int = 0,
    val fetchedAt: Long = System.currentTimeMillis(),
)

/** A composition still being edited; never uploaded until published. */
@Entity(tableName = "local_draft", indices = [Index("updatedAt")])
data class LocalDraft(
    @PrimaryKey val draftId: String,
    val ownerId: String,
    val kind: String = "POST",
    val caption: String = "",
    val locationName: String? = null,
    val mediaUris: List<String> = emptyList(),
    val altTexts: List<String> = emptyList(),
    val taggedUserIds: List<String> = emptyList(),
    val commentsEnabled: Boolean = true,
    val appliedFilter: String = "ORIGINAL",
    val adjustments: Map<String, String> = emptyMap(),
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

/** A queued media upload owned by WorkManager. */
@Entity(tableName = "pending_upload", indices = [Index("state"), Index("mediaHash")])
data class PendingUpload(
    @PrimaryKey val uploadId: String,
    val ownerId: String,
    val draftId: String? = null,
    val targetType: String = "POST",
    val localUri: String,
    val remotePath: String,
    val mediaHash: String? = null,
    val sizeBytes: Long = 0,
    /** QUEUED, RUNNING, SUCCEEDED, FAILED, CANCELLED */
    val state: String = "QUEUED",
    val progressPercent: Int = 0,
    val attemptCount: Int = 0,
    val lastError: String? = null,
    val idempotencyKey: String,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

/**
 * A write the user performed while offline. Replayed in order by
 * PendingActionWorker; [idempotencyKey] makes replay safe.
 */
@Entity(tableName = "pending_action", indices = [Index("state"), Index("createdAt")])
data class PendingAction(
    @PrimaryKey val actionId: String,
    val ownerId: String,
    /** LIKE, UNLIKE, SAVE, UNSAVE, FOLLOW, UNFOLLOW, COMMENT, DELETE_COMMENT, SEND_MESSAGE, VIEW_STORY, MARK_READ */
    val type: String,
    val targetId: String,
    val payload: Map<String, String> = emptyMap(),
    val state: String = "PENDING",
    val attemptCount: Int = 0,
    val lastError: String? = null,
    val idempotencyKey: String,
    val createdAt: Long = 0,
)

@Entity(tableName = "media_cache_entry", indices = [Index("lastAccessedAt")])
data class MediaCacheEntry(
    @PrimaryKey val cacheKey: String,
    val remoteUrl: String,
    val localPath: String,
    val sizeBytes: Long = 0,
    val lastAccessedAt: Long = 0,
)

@Entity(tableName = "sync_metadata")
data class SyncMetadata(
    @PrimaryKey val key: String,
    val lastSyncedAt: Long = 0,
    val cursor: String? = null,
    val itemCount: Int = 0,
)

/** Bounded per-user analytics aggregate, mirroring `analytics/{userId}`. */
@Entity(tableName = "analytics_snapshot")
data class AnalyticsSnapshot(
    @PrimaryKey val userId: String,
    val postViews: Long = 0,
    val reelViews: Long = 0,
    val likes: Long = 0,
    val comments: Long = 0,
    val saves: Long = 0,
    val shares: Long = 0,
    val followers: Long = 0,
    val profileVisits: Long = 0,
    val watchTimeMs: Long = 0,
    /** 7 most recent daily buckets, oldest first. */
    val weeklyEngagement: List<Long> = emptyList(),
    val monthlyEngagement: List<Long> = emptyList(),
    val followerGrowth: List<Long> = emptyList(),
    val updatedAt: Long = 0,
)

/**
 * Demo-mode credential store. Only a salted PBKDF2 hash is ever written -
 * never a plaintext or reversible password. See core/security/PasswordHasher.
 */
@Entity(tableName = "demo_account", indices = [Index("email", unique = true)])
data class DemoAccount(
    @PrimaryKey val userId: String,
    val email: String,
    val passwordHash: String,
    val passwordSalt: String,
    val iterations: Int,
    val emailVerified: Boolean = true,
    val createdAt: Long = 0,
)

@Entity(tableName = "content_report", indices = [Index("reporterId"), Index("state")])
data class ContentReportEntity(
    @PrimaryKey val reportId: String,
    val reporterId: String,
    val targetType: String,
    val targetId: String,
    val reason: String,
    val details: String = "",
    /** SUBMITTED, UNDER_REVIEW, ACTIONED, REJECTED */
    val state: String = "SUBMITTED",
    val createdAt: Long = 0,
)

/** Content the viewer explicitly suppressed; feeds subtract these. */
@Entity(tableName = "negative_signal", primaryKeys = ["userId", "contentId"])
data class NegativeSignalEntity(
    val userId: String,
    val contentId: String,
    /** HIDDEN, NOT_INTERESTED, REPORTED */
    val kind: String,
    val createdAt: Long = 0,
)

/** Rolling interaction signals used by the local feed ranker. */
@Entity(tableName = "interest_signal", primaryKeys = ["userId", "token"])
data class InterestSignalEntity(
    val userId: String,
    /** hashtag:travel or creator:{userId} */
    val token: String,
    val weight: Double = 0.0,
    val updatedAt: Long = 0,
)

@Entity(tableName = "watch_time", primaryKeys = ["userId", "contentId"])
data class WatchTimeEntity(
    val userId: String,
    val contentId: String,
    val watchedMs: Long = 0,
    val completions: Int = 0,
    val updatedAt: Long = 0,
)
