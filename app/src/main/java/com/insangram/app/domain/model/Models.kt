package com.insangram.app.domain.model

import com.insangram.app.core.designsystem.theme.ThemeMode

// ---------------------------------------------------------------------------
// Users and relationships
// ---------------------------------------------------------------------------

data class User(
    val id: String,
    val username: String,
    val fullName: String,
    val bio: String = "",
    val website: String = "",
    val pronouns: String = "",
    val photoUrl: String? = null,
    val isPrivate: Boolean = false,
    val postCount: Long = 0,
    val followerCount: Long = 0,
    val followingCount: Long = 0,
    val lastActiveAt: Long = 0,
    val showActivityStatus: Boolean = true,
) {
    val handle: String get() = "@" + username
    val initials: String
        get() = fullName.trim().split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { username.take(1).uppercase() }
}

/** The viewer's relationship to another account. Drives the follow button. */
enum class FollowState { NOT_FOLLOWING, REQUESTED, FOLLOWING, SELF, BLOCKED }

data class RelationshipSummary(
    val followState: FollowState = FollowState.NOT_FOLLOWING,
    val followsViewer: Boolean = false,
    val isCloseFriend: Boolean = false,
    val postsMuted: Boolean = false,
    val storiesMuted: Boolean = false,
    val isBlockedByViewer: Boolean = false,
    val isRestricted: Boolean = false,
)

// ---------------------------------------------------------------------------
// Content
// ---------------------------------------------------------------------------

enum class MediaType { IMAGE, VIDEO }

data class MediaItem(
    val url: String,
    val thumbnailUrl: String? = null,
    val type: MediaType = MediaType.IMAGE,
    val widthPx: Int = 0,
    val heightPx: Int = 0,
    val durationMs: Long = 0,
    val altText: String = "",
) {
    val aspectRatio: Float
        get() = if (widthPx > 0 && heightPx > 0) widthPx.toFloat() / heightPx else 1f
}

data class Post(
    val id: String,
    val author: User,
    val media: List<MediaItem>,
    val caption: String = "",
    val hashtags: List<String> = emptyList(),
    val mentionedUsernames: List<String> = emptyList(),
    val taggedUsers: List<User> = emptyList(),
    val locationName: String? = null,
    val likeCount: Long = 0,
    val commentCount: Long = 0,
    val saveCount: Long = 0,
    val shareCount: Long = 0,
    val viewCount: Long = 0,
    val likedByViewer: Boolean = false,
    val savedByViewer: Boolean = false,
    val commentsEnabled: Boolean = true,
    val hideLikeCount: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = 0,
    val editedAt: Long? = null,
    val commentPreview: List<Comment> = emptyList(),
    val relationship: RelationshipSummary = RelationshipSummary(),
) {
    val isCarousel: Boolean get() = media.size > 1
    val isEdited: Boolean get() = editedAt != null

    /** Single spoken description for TalkBack, per accessibility requirements. */
    fun accessibilityLabel(): String = buildString {
        append("Post by ").append(author.username).append(". ")
        val alt = media.firstOrNull()?.altText
        if (!alt.isNullOrBlank()) append(alt).append(". ")
        else append(if (media.size > 1) "${media.size} item carousel. " else "")
        if (caption.isNotBlank()) append("Caption: ").append(caption).append(". ")
        if (!hideLikeCount) append(likeCount).append(" likes. ")
        append(commentCount).append(" comments.")
    }
}

data class Reel(
    val id: String,
    val author: User,
    val videoUrl: String,
    val thumbnailUrl: String? = null,
    val caption: String = "",
    val hashtags: List<String> = emptyList(),
    val audioLabel: String = "Original audio",
    val durationMs: Long = 0,
    val likeCount: Long = 0,
    val commentCount: Long = 0,
    val saveCount: Long = 0,
    val shareCount: Long = 0,
    val viewCount: Long = 0,
    val likedByViewer: Boolean = false,
    val savedByViewer: Boolean = false,
    val commentsEnabled: Boolean = true,
    val createdAt: Long = 0,
    val relationship: RelationshipSummary = RelationshipSummary(),
)

enum class StoryType { PHOTO, VIDEO, TEXT }
enum class StoryAudience { EVERYONE, FOLLOWERS, CLOSE_FRIENDS }

data class Story(
    val id: String,
    val author: User,
    val type: StoryType = StoryType.PHOTO,
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val text: String = "",
    val backgroundColor: Long = 0,
    val fontStyle: String = "DEFAULT",
    val durationMs: Long = 5_000,
    val audience: StoryAudience = StoryAudience.EVERYONE,
    val allowReplies: Boolean = true,
    val viewCount: Long = 0,
    val seen: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = 0,
    val expiresAt: Long = 0,
) {
    fun isExpired(now: Long): Boolean = expiresAt in 1 until now
}

/** One tray entry: an author plus their unexpired stories. */
data class StoryTrayItem(
    val author: User,
    val stories: List<Story>,
    val allSeen: Boolean,
    val isCloseFriends: Boolean = false,
    val uploadProgress: Int? = null,
    val uploadFailed: Boolean = false,
) {
    val isOwn: Boolean get() = uploadProgress != null || uploadFailed || stories.isEmpty()
}

data class StoryViewer(val user: User, val reactionEmoji: String?, val viewedAt: Long)

data class Comment(
    val id: String,
    val contentId: String,
    val author: User,
    val text: String,
    val replyToCommentId: String? = null,
    val likeCount: Long = 0,
    val likedByViewer: Boolean = false,
    val isPinned: Boolean = false,
    val createdAt: Long = 0,
    val editedAt: Long? = null,
    val replies: List<Comment> = emptyList(),
    val replyCount: Int = 0,
)

enum class CommentSort { NEWEST, TOP }

// ---------------------------------------------------------------------------
// Saves
// ---------------------------------------------------------------------------

data class SavedCollection(
    val id: String,
    val name: String,
    val coverUrl: String? = null,
    val itemCount: Int = 0,
)

// ---------------------------------------------------------------------------
// Messaging
// ---------------------------------------------------------------------------

enum class DeliveryState { SENDING, SENT, DELIVERED, READ, FAILED }

data class Message(
    val id: String,
    val conversationId: String,
    val sender: User,
    val text: String = "",
    val mediaUrl: String? = null,
    val localMediaUri: String? = null,
    val mediaType: MediaType? = null,
    val replyToMessageId: String? = null,
    val replyToPreview: String? = null,
    val reactions: Map<String, String> = emptyMap(),
    val readByIds: List<String> = emptyList(),
    val deliveryState: DeliveryState = DeliveryState.SENT,
    val isUnsent: Boolean = false,
    val isPinned: Boolean = false,
    val createdAt: Long = 0,
) {
    val isFailed: Boolean get() = deliveryState == DeliveryState.FAILED
}

data class Conversation(
    val id: String,
    val isGroup: Boolean,
    val title: String,
    val imageUrl: String? = null,
    val members: List<User> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val lastMessagePreview: String = "",
    val lastMessageAt: Long = 0,
    val unreadCount: Int = 0,
    val isMuted: Boolean = false,
    val typingUserIds: List<String> = emptyList(),
)

data class Note(
    val id: String,
    val author: User,
    val text: String,
    val emoji: String = "",
    val audience: AudienceScope = AudienceScope.FOLLOWERS,
    val createdAt: Long = 0,
    val expiresAt: Long = 0,
)

// ---------------------------------------------------------------------------
// Notifications, reports, analytics
// ---------------------------------------------------------------------------

enum class NotificationType {
    NEW_FOLLOWER, FOLLOW_REQUEST, REQUEST_ACCEPTED, POST_LIKE, REEL_LIKE,
    COMMENT, REPLY, MENTION, TAG, NEW_MESSAGE, STORY_REACTION, STORY_REPLY,
}

data class AppNotification(
    val id: String,
    val type: NotificationType,
    val actor: User,
    val targetId: String? = null,
    val previewText: String = "",
    val previewImageUrl: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = 0,
)

enum class ReportTargetType { USER, POST, REEL, STORY, COMMENT, MESSAGE }
enum class ReportState { SUBMITTED, UNDER_REVIEW, ACTIONED, REJECTED }

data class ContentReport(
    val id: String,
    val targetType: ReportTargetType,
    val targetId: String,
    val reason: String,
    val details: String = "",
    val state: ReportState = ReportState.SUBMITTED,
    val createdAt: Long = 0,
)

data class CreatorAnalytics(
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
    val topPosts: List<Post> = emptyList(),
    val topReels: List<Reel> = emptyList(),
    val updatedAt: Long = 0,
) {
    /** Engagement rate as a percentage of reach; 0 when there is no reach yet. */
    val engagementRate: Double
        get() {
            val reach = postViews + reelViews
            if (reach <= 0) return 0.0
            return ((likes + comments + saves + shares).toDouble() / reach) * 100.0
        }
}

// ---------------------------------------------------------------------------
// Search
// ---------------------------------------------------------------------------

enum class SearchTab { TOP, ACCOUNTS, HASHTAGS, POSTS, REELS, PLACES }

data class HashtagSummary(val tag: String, val postCount: Long, val recentEngagement: Long = 0)

data class SearchResults(
    val accounts: List<User> = emptyList(),
    val hashtags: List<HashtagSummary> = emptyList(),
    val posts: List<Post> = emptyList(),
    val reels: List<Reel> = emptyList(),
    val places: List<String> = emptyList(),
) {
    val isEmpty: Boolean
        get() = accounts.isEmpty() && hashtags.isEmpty() && posts.isEmpty() &&
            reels.isEmpty() && places.isEmpty()
}

data class RecentSearch(val id: String, val term: String, val kind: String, val searchedAt: Long)

// ---------------------------------------------------------------------------
// Drafts and uploads
// ---------------------------------------------------------------------------

data class PostDraft(
    val id: String,
    val mediaUris: List<String> = emptyList(),
    val altTexts: List<String> = emptyList(),
    val caption: String = "",
    val locationName: String? = null,
    val taggedUserIds: List<String> = emptyList(),
    val commentsEnabled: Boolean = true,
    val filter: String = "ORIGINAL",
    val adjustments: Map<String, String> = emptyMap(),
    val updatedAt: Long = 0,
)

enum class UploadState { QUEUED, RUNNING, SUCCEEDED, FAILED, CANCELLED }

data class UploadTask(
    val id: String,
    val targetType: String,
    val localUri: String,
    val state: UploadState,
    val progressPercent: Int,
    val error: String? = null,
)

// ---------------------------------------------------------------------------
// Settings
// ---------------------------------------------------------------------------

enum class AudienceScope { EVERYONE, FOLLOWERS, CLOSE_FRIENDS, NOBODY }
enum class UploadQuality { DATA_SAVER, STANDARD, HIGH }
enum class AutoplayMode { ALWAYS, WIFI_ONLY, NEVER }
enum class AppLockMode { OFF, BIOMETRIC, PIN }

data class InsangramSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val reducedMotion: Boolean = false,
    val highContrast: Boolean = false,
    val languageTag: String = "",
    val uploadQuality: UploadQuality = UploadQuality.STANDARD,
    val dataSaver: Boolean = false,
    val autoplayMode: AutoplayMode = AutoplayMode.WIFI_ONLY,
    val privateAccount: Boolean = false,
    val commentAudience: AudienceScope = AudienceScope.EVERYONE,
    val messageAudience: AudienceScope = AudienceScope.FOLLOWERS,
    val storyAudience: AudienceScope = AudienceScope.FOLLOWERS,
    val mentionAudience: AudienceScope = AudienceScope.EVERYONE,
    val tagAudience: AudienceScope = AudienceScope.EVERYONE,
    val showActivityStatus: Boolean = true,
    val sendReadReceipts: Boolean = true,
    val discoverable: Boolean = true,
    val showNotificationPreviews: Boolean = true,
    val appLockMode: AppLockMode = AppLockMode.OFF,
    val appLockTimeoutMs: Long = 60_000,
    val pushLikes: Boolean = true,
    val pushComments: Boolean = true,
    val pushFollows: Boolean = true,
    val pushMessages: Boolean = true,
    val pushMentions: Boolean = true,
    val chronologicalFeed: Boolean = false,
)

/** Session state exposed by AuthRepository and consumed by the splash screen. */
sealed interface SessionState {
    data object Unknown : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val user: User, val emailVerified: Boolean) : SessionState
}
