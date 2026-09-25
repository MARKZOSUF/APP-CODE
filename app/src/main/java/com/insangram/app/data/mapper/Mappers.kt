package com.insangram.app.data.mapper

import com.insangram.app.core.database.entity.AnalyticsSnapshot
import com.insangram.app.core.database.entity.CachedComment
import com.insangram.app.core.database.entity.CachedConversation
import com.insangram.app.core.database.entity.CachedMessage
import com.insangram.app.core.database.entity.CachedNote
import com.insangram.app.core.database.entity.CachedNotification
import com.insangram.app.core.database.entity.CachedPost
import com.insangram.app.core.database.entity.CachedPostMedia
import com.insangram.app.core.database.entity.CachedReel
import com.insangram.app.core.database.entity.CachedStory
import com.insangram.app.core.database.entity.CachedUser
import com.insangram.app.core.database.entity.ContentReportEntity
import com.insangram.app.core.database.entity.HashtagEntity
import com.insangram.app.core.database.entity.LocalDraft
import com.insangram.app.core.database.entity.PendingUpload
import com.insangram.app.core.database.entity.SavedCollectionEntity
import com.insangram.app.core.database.entity.SearchHistoryEntity
import com.insangram.app.domain.model.AppNotification
import com.insangram.app.domain.model.AudienceScope
import com.insangram.app.domain.model.Comment
import com.insangram.app.domain.model.ContentReport
import com.insangram.app.domain.model.Conversation
import com.insangram.app.domain.model.CreatorAnalytics
import com.insangram.app.domain.model.DeliveryState
import com.insangram.app.domain.model.HashtagSummary
import com.insangram.app.domain.model.MediaItem
import com.insangram.app.domain.model.MediaType
import com.insangram.app.domain.model.Message
import com.insangram.app.domain.model.Note
import com.insangram.app.domain.model.NotificationType
import com.insangram.app.domain.model.Post
import com.insangram.app.domain.model.PostDraft
import com.insangram.app.domain.model.RecentSearch
import com.insangram.app.domain.model.Reel
import com.insangram.app.domain.model.RelationshipSummary
import com.insangram.app.domain.model.ReportState
import com.insangram.app.domain.model.ReportTargetType
import com.insangram.app.domain.model.SavedCollection
import com.insangram.app.domain.model.Story
import com.insangram.app.domain.model.StoryAudience
import com.insangram.app.domain.model.StoryType
import com.insangram.app.domain.model.UploadState
import com.insangram.app.domain.model.UploadTask
import com.insangram.app.domain.model.User

/**
 * Entity <-> domain mappers. Composables and ViewModels only ever see domain
 * models; Room entities never leave the data layer.
 */

fun CachedUser.toDomain(): User = User(
    id = userId,
    username = username,
    fullName = fullName,
    bio = bio,
    website = website,
    pronouns = pronouns,
    photoUrl = photoUrl,
    isPrivate = isPrivate,
    postCount = postCount,
    followerCount = followerCount,
    followingCount = followingCount,
    lastActiveAt = lastActiveAt,
    showActivityStatus = showActivityStatus,
)

fun User.toEntity(cachedAt: Long = System.currentTimeMillis()): CachedUser = CachedUser(
    userId = id,
    username = username,
    normalizedUsername = username.lowercase(),
    fullName = fullName,
    bio = bio,
    website = website,
    pronouns = pronouns,
    photoUrl = photoUrl,
    isPrivate = isPrivate,
    postCount = postCount,
    followerCount = followerCount,
    followingCount = followingCount,
    lastActiveAt = lastActiveAt,
    showActivityStatus = showActivityStatus,
    cachedAt = cachedAt,
)

fun CachedPostMedia.toDomain(): MediaItem = MediaItem(
    url = localUri ?: mediaUrl,
    thumbnailUrl = thumbnailUrl,
    type = if (mediaType == "VIDEO") MediaType.VIDEO else MediaType.IMAGE,
    widthPx = widthPx,
    heightPx = heightPx,
    durationMs = durationMs,
    altText = altText,
)

fun CachedPost.toDomain(
    author: User,
    media: List<CachedPostMedia>,
    likedByViewer: Boolean = false,
    savedByViewer: Boolean = false,
    taggedUsers: List<User> = emptyList(),
    commentPreview: List<Comment> = emptyList(),
    relationship: RelationshipSummary = RelationshipSummary(),
): Post = Post(
    id = postId,
    author = author,
    media = media.sortedBy { it.position }.map { it.toDomain() },
    caption = caption,
    hashtags = hashtags,
    mentionedUsernames = mentionedUsernames,
    taggedUsers = taggedUsers,
    locationName = locationName,
    likeCount = likeCount,
    commentCount = commentCount,
    saveCount = saveCount,
    shareCount = shareCount,
    viewCount = viewCount,
    likedByViewer = likedByViewer,
    savedByViewer = savedByViewer,
    commentsEnabled = commentsEnabled,
    hideLikeCount = hideLikeCount,
    isArchived = isArchived,
    createdAt = createdAt,
    editedAt = editedAt,
    commentPreview = commentPreview,
    relationship = relationship,
)

fun CachedReel.toDomain(
    author: User,
    likedByViewer: Boolean = false,
    savedByViewer: Boolean = false,
    relationship: RelationshipSummary = RelationshipSummary(),
): Reel = Reel(
    id = reelId,
    author = author,
    videoUrl = videoUrl,
    thumbnailUrl = thumbnailUrl,
    caption = caption,
    hashtags = hashtags,
    audioLabel = audioLabel,
    durationMs = durationMs,
    likeCount = likeCount,
    commentCount = commentCount,
    saveCount = saveCount,
    shareCount = shareCount,
    viewCount = viewCount,
    likedByViewer = likedByViewer,
    savedByViewer = savedByViewer,
    commentsEnabled = commentsEnabled,
    createdAt = createdAt,
    relationship = relationship,
)

fun CachedStory.toDomain(author: User): Story = Story(
    id = storyId,
    author = author,
    type = runCatching { StoryType.valueOf(storyType) }.getOrDefault(StoryType.PHOTO),
    mediaUrl = mediaUrl,
    thumbnailUrl = thumbnailUrl,
    text = textContent,
    backgroundColor = backgroundColor,
    fontStyle = fontStyle,
    durationMs = durationMs,
    audience = runCatching { StoryAudience.valueOf(audience) }.getOrDefault(StoryAudience.EVERYONE),
    allowReplies = allowReplies,
    viewCount = viewCount,
    seen = seenByMe,
    isArchived = isArchived,
    createdAt = createdAt,
    expiresAt = expiresAt,
)

fun CachedComment.toDomain(author: User, replies: List<Comment> = emptyList()): Comment = Comment(
    id = commentId,
    contentId = parentContentId,
    author = author,
    text = text,
    replyToCommentId = replyToCommentId,
    likeCount = likeCount,
    likedByViewer = likedByMe,
    isPinned = isPinned,
    createdAt = createdAt,
    editedAt = editedAt,
    replies = replies,
    replyCount = replies.size,
)

fun CachedConversation.toDomain(
    members: List<User>,
    viewerId: String,
    typingUserIds: List<String> = emptyList(),
    now: Long = System.currentTimeMillis(),
): Conversation {
    val resolvedTitle = when {
        isGroup && title.isNotBlank() -> title
        isGroup -> members.filter { it.id != viewerId }.joinToString(", ") { it.username }
        else -> members.firstOrNull { it.id != viewerId }?.fullName ?: title
    }
    val resolvedImage = if (isGroup) imageUrl else members.firstOrNull { it.id != viewerId }?.photoUrl
    return Conversation(
        id = conversationId,
        isGroup = isGroup,
        title = resolvedTitle,
        imageUrl = resolvedImage,
        members = members,
        adminIds = adminIds,
        lastMessagePreview = lastMessagePreview,
        lastMessageAt = lastMessageAt,
        unreadCount = unreadCount,
        isMuted = (mutedUntil ?: 0) > now,
        typingUserIds = typingUserIds,
    )
}

fun CachedMessage.toDomain(sender: User): Message = Message(
    id = messageId,
    conversationId = conversationId,
    sender = sender,
    text = text,
    mediaUrl = mediaUrl,
    localMediaUri = localMediaUri,
    mediaType = mediaType?.let { runCatching { MediaType.valueOf(it) }.getOrNull() },
    replyToMessageId = replyToMessageId,
    replyToPreview = replyToPreview,
    reactions = reactions,
    readByIds = readByIds,
    deliveryState = runCatching { DeliveryState.valueOf(deliveryState) }
        .getOrDefault(DeliveryState.SENT),
    isUnsent = isUnsent,
    isPinned = isPinned,
    createdAt = createdAt,
)

fun CachedNotification.toDomain(actor: User): AppNotification = AppNotification(
    id = notificationId,
    type = runCatching { NotificationType.valueOf(type) }
        .getOrDefault(NotificationType.POST_LIKE),
    actor = actor,
    targetId = targetId,
    previewText = previewText,
    previewImageUrl = previewImageUrl,
    isRead = isRead,
    createdAt = createdAt,
)

fun CachedNote.toDomain(author: User): Note = Note(
    id = noteId,
    author = author,
    text = text,
    emoji = emoji,
    audience = runCatching { AudienceScope.valueOf(audience) }
        .getOrDefault(AudienceScope.FOLLOWERS),
    createdAt = createdAt,
    expiresAt = expiresAt,
)

fun HashtagEntity.toDomain(): HashtagSummary =
    HashtagSummary(tag = displayTag, postCount = postCount, recentEngagement = recentEngagement)

fun SearchHistoryEntity.toDomain(): RecentSearch =
    RecentSearch(id = id, term = term, kind = kind, searchedAt = searchedAt)

fun SavedCollectionEntity.toDomain(coverUrl: String?, itemCount: Int): SavedCollection =
    SavedCollection(id = collectionId, name = name, coverUrl = coverUrl, itemCount = itemCount)

fun ContentReportEntity.toDomain(): ContentReport = ContentReport(
    id = reportId,
    targetType = runCatching { ReportTargetType.valueOf(targetType) }
        .getOrDefault(ReportTargetType.POST),
    targetId = targetId,
    reason = reason,
    details = details,
    state = runCatching { ReportState.valueOf(state) }.getOrDefault(ReportState.SUBMITTED),
    createdAt = createdAt,
)

fun AnalyticsSnapshot.toDomain(topPosts: List<Post>, topReels: List<Reel>): CreatorAnalytics =
    CreatorAnalytics(
        postViews = postViews,
        reelViews = reelViews,
        likes = likes,
        comments = comments,
        saves = saves,
        shares = shares,
        followers = followers,
        profileVisits = profileVisits,
        watchTimeMs = watchTimeMs,
        weeklyEngagement = weeklyEngagement,
        monthlyEngagement = monthlyEngagement,
        followerGrowth = followerGrowth,
        topPosts = topPosts,
        topReels = topReels,
        updatedAt = updatedAt,
    )

fun LocalDraft.toDomain(): PostDraft = PostDraft(
    id = draftId,
    mediaUris = mediaUris,
    altTexts = altTexts,
    caption = caption,
    locationName = locationName,
    taggedUserIds = taggedUserIds,
    commentsEnabled = commentsEnabled,
    filter = appliedFilter,
    adjustments = adjustments,
    updatedAt = updatedAt,
)

fun PostDraft.toEntity(ownerId: String, now: Long): LocalDraft = LocalDraft(
    draftId = id,
    ownerId = ownerId,
    caption = caption,
    locationName = locationName,
    mediaUris = mediaUris,
    altTexts = altTexts,
    taggedUserIds = taggedUserIds,
    commentsEnabled = commentsEnabled,
    appliedFilter = filter,
    adjustments = adjustments,
    createdAt = if (updatedAt == 0L) now else updatedAt,
    updatedAt = now,
)

fun PendingUpload.toDomain(): UploadTask = UploadTask(
    id = uploadId,
    targetType = targetType,
    localUri = localUri,
    state = runCatching { UploadState.valueOf(state) }.getOrDefault(UploadState.QUEUED),
    progressPercent = progressPercent,
    error = lastError,
)
