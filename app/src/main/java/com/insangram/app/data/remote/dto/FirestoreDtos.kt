package com.insangram.app.data.remote.dto

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Firestore DTOs. Every write that needs a trustworthy time uses
 * [ServerTimestamp] rather than a client clock, and aggregate counters are
 * read-only from the client's perspective (Cloud Functions own them).
 */

@IgnoreExtraProperties
data class UserDto(
    @DocumentId val id: String = "",
    val username: String = "",
    val normalizedUsername: String = "",
    /** Indexed prefixes so "prefix search" works without full-text search. */
    val usernamePrefixes: List<String> = emptyList(),
    val fullName: String = "",
    val searchableName: String = "",
    val bio: String = "",
    val website: String = "",
    val pronouns: String = "",
    val photoUrl: String? = null,
    val isPrivate: Boolean = false,
    val discoverable: Boolean = true,
    val showActivityStatus: Boolean = true,
    val postCount: Long = 0,
    val followerCount: Long = 0,
    val followingCount: Long = 0,
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val lastActiveAt: Date? = null,
)

/** usernames/{normalizedUsername} - uniqueness reservation document. */
@IgnoreExtraProperties
data class UsernameReservationDto(
    @DocumentId val normalizedUsername: String = "",
    val userId: String = "",
    @ServerTimestamp val reservedAt: Date? = null,
)

@IgnoreExtraProperties
data class PostMediaDto(
    val url: String = "",
    val thumbnailUrl: String? = null,
    val type: String = "IMAGE",
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0,
    val altText: String = "",
    val storagePath: String = "",
)

@IgnoreExtraProperties
data class PostDto(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val media: List<PostMediaDto> = emptyList(),
    val caption: String = "",
    /** Lowercased tokens supporting array-contains keyword search. */
    val captionTokens: List<String> = emptyList(),
    val hashtags: List<String> = emptyList(),
    val mentionedUsernames: List<String> = emptyList(),
    val taggedUserIds: List<String> = emptyList(),
    val locationName: String? = null,
    val commentsEnabled: Boolean = true,
    val hideLikeCount: Boolean = false,
    val isArchived: Boolean = false,
    val authorIsPrivate: Boolean = false,
    // Counter fields: maintained only by Cloud Functions.
    val likeCount: Long = 0,
    val commentCount: Long = 0,
    val saveCount: Long = 0,
    val shareCount: Long = 0,
    val viewCount: Long = 0,
    val deletedAt: Date? = null,
    @ServerTimestamp val createdAt: Date? = null,
    val editedAt: Date? = null,
)

@IgnoreExtraProperties
data class ReelDto(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String? = null,
    val storagePath: String = "",
    val caption: String = "",
    val captionTokens: List<String> = emptyList(),
    val hashtags: List<String> = emptyList(),
    val audioLabel: String = "Original audio",
    val durationMs: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val commentsEnabled: Boolean = true,
    val authorIsPrivate: Boolean = false,
    val likeCount: Long = 0,
    val commentCount: Long = 0,
    val saveCount: Long = 0,
    val shareCount: Long = 0,
    val viewCount: Long = 0,
    val totalWatchTimeMs: Long = 0,
    val deletedAt: Date? = null,
    @ServerTimestamp val createdAt: Date? = null,
)

@IgnoreExtraProperties
data class StoryDto(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val type: String = "PHOTO",
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val storagePath: String = "",
    val text: String = "",
    val backgroundColor: Long = 0,
    val fontStyle: String = "DEFAULT",
    val durationMs: Long = 5_000,
    val audience: String = "EVERYONE",
    val allowReplies: Boolean = true,
    val viewCount: Long = 0,
    val isArchived: Boolean = false,
    val highlightIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Date? = null,
    /** Set by the createStory Cloud Function to createdAt + 24h. */
    val expiresAt: Date? = null,
)

@IgnoreExtraProperties
data class CommentDto(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val text: String = "",
    val replyToCommentId: String? = null,
    val likeCount: Long = 0,
    val isPinned: Boolean = false,
    @ServerTimestamp val createdAt: Date? = null,
    val editedAt: Date? = null,
)

@IgnoreExtraProperties
data class LikeDto(
    @DocumentId val userId: String = "",
    @ServerTimestamp val createdAt: Date? = null,
)

@IgnoreExtraProperties
data class FollowDto(
    @DocumentId val id: String = "",
    val followerId: String = "",
    val followeeId: String = "",
    val isCloseFriend: Boolean = false,
    val mutedPosts: Boolean = false,
    val mutedStories: Boolean = false,
    @ServerTimestamp val createdAt: Date? = null,
)

@IgnoreExtraProperties
data class FollowRequestDto(
    @DocumentId val id: String = "",
    val requesterId: String = "",
    val targetId: String = "",
    val state: String = "PENDING",
    @ServerTimestamp val createdAt: Date? = null,
)

@IgnoreExtraProperties
data class ConversationDto(
    @DocumentId val id: String = "",
    val isGroup: Boolean = false,
    val title: String = "",
    val imageUrl: String? = null,
    val memberIds: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val createdBy: String = "",
    val lastMessagePreview: String = "",
    val lastMessageSenderId: String? = null,
    val typingUserIds: List<String> = emptyList(),
    @ServerTimestamp val lastMessageAt: Date? = null,
    @ServerTimestamp val createdAt: Date? = null,
)

@IgnoreExtraProperties
data class MessageDto(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val storagePath: String? = null,
    val replyToMessageId: String? = null,
    val replyToPreview: String? = null,
    val reactions: Map<String, String> = emptyMap(),
    val readByIds: List<String> = emptyList(),
    val isUnsent: Boolean = false,
    val isPinned: Boolean = false,
    val hiddenForIds: List<String> = emptyList(),
    /** Client-generated, enforced unique so retries cannot duplicate a send. */
    val idempotencyKey: String = "",
    @ServerTimestamp val createdAt: Date? = null,
)

@IgnoreExtraProperties
data class NotificationDto(
    @DocumentId val id: String = "",
    val type: String = "",
    val actorId: String = "",
    val targetId: String? = null,
    val previewText: String = "",
    val previewImageUrl: String? = null,
    val isRead: Boolean = false,
    /** Used by functions to suppress duplicate notifications. */
    val dedupeKey: String = "",
    @ServerTimestamp val createdAt: Date? = null,
)

@IgnoreExtraProperties
data class HashtagDto(
    @DocumentId val normalizedTag: String = "",
    val displayTag: String = "",
    val postCount: Long = 0,
    val recentEngagement: Long = 0,
    @ServerTimestamp val lastUsedAt: Date? = null,
)

@IgnoreExtraProperties
data class SavedPostDto(
    @DocumentId val postId: String = "",
    val collectionId: String? = null,
    @ServerTimestamp val savedAt: Date? = null,
)

@IgnoreExtraProperties
data class CollectionDto(
    @DocumentId val id: String = "",
    val name: String = "",
    val coverPostId: String? = null,
    @ServerTimestamp val createdAt: Date? = null,
)

@IgnoreExtraProperties
data class ReportDto(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val reason: String = "",
    val details: String = "",
    val state: String = "SUBMITTED",
    @ServerTimestamp val createdAt: Date? = null,
)

@IgnoreExtraProperties
data class AnalyticsDto(
    @DocumentId val userId: String = "",
    val postViews: Long = 0,
    val reelViews: Long = 0,
    val likes: Long = 0,
    val comments: Long = 0,
    val saves: Long = 0,
    val shares: Long = 0,
    val followers: Long = 0,
    val profileVisits: Long = 0,
    val watchTimeMs: Long = 0,
    val weeklyEngagement: List<Long> = emptyList(),
    val monthlyEngagement: List<Long> = emptyList(),
    val followerGrowth: List<Long> = emptyList(),
    @ServerTimestamp val updatedAt: Date? = null,
)

@IgnoreExtraProperties
data class DeviceTokenDto(
    @DocumentId val id: String = "",
    val token: String = "",
    val platform: String = "android",
    val appVersion: String = "",
    @ServerTimestamp val updatedAt: Date? = null,
)

@IgnoreExtraProperties
data class NoteDto(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val text: String = "",
    val emoji: String = "",
    val audience: String = "FOLLOWERS",
    @ServerTimestamp val createdAt: Date? = null,
    val expiresAt: Date? = null,
)

@IgnoreExtraProperties
data class StoryViewDto(
    @DocumentId val viewerId: String = "",
    val reactionEmoji: String? = null,
    @ServerTimestamp val viewedAt: Date? = null,
)

@IgnoreExtraProperties
data class FeedItemDto(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val score: Double = 0.0,
    @ServerTimestamp val createdAt: Date? = null,
)
