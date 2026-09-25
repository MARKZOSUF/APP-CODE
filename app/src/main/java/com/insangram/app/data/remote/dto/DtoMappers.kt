package com.insangram.app.data.remote.dto

import com.insangram.app.core.database.entity.CachedComment
import com.insangram.app.core.database.entity.CachedConversation
import com.insangram.app.core.database.entity.CachedMessage
import com.insangram.app.core.database.entity.CachedNotification
import com.insangram.app.core.database.entity.CachedPost
import com.insangram.app.core.database.entity.CachedPostMedia
import com.insangram.app.core.database.entity.CachedReel
import com.insangram.app.core.database.entity.CachedStory
import com.insangram.app.core.database.entity.CachedUser
import java.util.Date

/** DTO -> Room entity. Null server timestamps mean "pending", mapped to 0. */
private fun Date?.millis(): Long = this?.time ?: 0L

fun UserDto.toEntity(now: Long = System.currentTimeMillis()) = CachedUser(
    userId = id,
    username = username,
    normalizedUsername = normalizedUsername.ifBlank { username.lowercase() },
    fullName = fullName,
    bio = bio,
    website = website,
    pronouns = pronouns,
    photoUrl = photoUrl,
    isPrivate = isPrivate,
    postCount = postCount,
    followerCount = followerCount,
    followingCount = followingCount,
    createdAt = createdAt.millis(),
    lastActiveAt = lastActiveAt.millis(),
    showActivityStatus = showActivityStatus,
    cachedAt = now,
)

fun PostDto.toEntity(now: Long = System.currentTimeMillis()) = CachedPost(
    postId = id,
    authorId = authorId,
    caption = caption,
    locationName = locationName,
    hashtags = hashtags,
    taggedUserIds = taggedUserIds,
    mentionedUsernames = mentionedUsernames,
    likeCount = likeCount,
    commentCount = commentCount,
    saveCount = saveCount,
    shareCount = shareCount,
    viewCount = viewCount,
    commentsEnabled = commentsEnabled,
    hideLikeCount = hideLikeCount,
    isArchived = isArchived,
    deletedAt = deletedAt?.time,
    createdAt = createdAt.millis(),
    editedAt = editedAt?.time,
    cachedAt = now,
)

fun PostDto.toMediaEntities(): List<CachedPostMedia> = media.mapIndexed { index, item ->
    CachedPostMedia(
        postId = id,
        position = index,
        mediaUrl = item.url,
        thumbnailUrl = item.thumbnailUrl,
        mediaType = item.type,
        widthPx = item.width,
        heightPx = item.height,
        durationMs = item.durationMs,
        altText = item.altText,
    )
}

fun ReelDto.toEntity(now: Long = System.currentTimeMillis()) = CachedReel(
    reelId = id,
    authorId = authorId,
    videoUrl = videoUrl,
    thumbnailUrl = thumbnailUrl,
    caption = caption,
    audioLabel = audioLabel,
    durationMs = durationMs,
    widthPx = width,
    heightPx = height,
    hashtags = hashtags,
    likeCount = likeCount,
    commentCount = commentCount,
    saveCount = saveCount,
    shareCount = shareCount,
    viewCount = viewCount,
    totalWatchTimeMs = totalWatchTimeMs,
    commentsEnabled = commentsEnabled,
    deletedAt = deletedAt?.time,
    createdAt = createdAt.millis(),
    cachedAt = now,
)

fun StoryDto.toEntity(seen: Boolean, now: Long = System.currentTimeMillis()) = CachedStory(
    storyId = id,
    authorId = authorId,
    mediaUrl = mediaUrl,
    thumbnailUrl = thumbnailUrl,
    storyType = type,
    textContent = text,
    backgroundColor = backgroundColor,
    fontStyle = fontStyle,
    durationMs = durationMs,
    audience = audience,
    allowReplies = allowReplies,
    viewCount = viewCount,
    seenByMe = seen,
    isArchived = isArchived,
    highlightIds = highlightIds,
    createdAt = createdAt.millis(),
    expiresAt = expiresAt.millis(),
    cachedAt = now,
)

fun CommentDto.toEntity(
    contentId: String,
    contentType: String,
    likedByMe: Boolean,
    now: Long = System.currentTimeMillis(),
) = CachedComment(
    commentId = id,
    parentContentId = contentId,
    parentContentType = contentType,
    authorId = authorId,
    text = text,
    replyToCommentId = replyToCommentId,
    likeCount = likeCount,
    likedByMe = likedByMe,
    isPinned = isPinned,
    editedAt = editedAt?.time,
    createdAt = createdAt.millis(),
    cachedAt = now,
)

fun ConversationDto.toEntity(unreadCount: Int, now: Long = System.currentTimeMillis()) =
    CachedConversation(
        conversationId = id,
        isGroup = isGroup,
        title = title,
        imageUrl = imageUrl,
        memberIds = memberIds,
        adminIds = adminIds,
        lastMessagePreview = lastMessagePreview,
        lastMessageSenderId = lastMessageSenderId,
        lastMessageAt = lastMessageAt.millis(),
        unreadCount = unreadCount,
        cachedAt = now,
    )

fun MessageDto.toEntity(conversationId: String, viewerId: String) = CachedMessage(
    messageId = id,
    conversationId = conversationId,
    senderId = senderId,
    text = text,
    mediaUrl = mediaUrl,
    mediaType = mediaType,
    replyToMessageId = replyToMessageId,
    replyToPreview = replyToPreview,
    reactions = reactions,
    readByIds = readByIds,
    deliveryState = if (readByIds.any { it != senderId }) "READ" else "DELIVERED",
    isUnsent = isUnsent,
    hiddenForMe = viewerId in hiddenForIds,
    isPinned = isPinned,
    idempotencyKey = idempotencyKey.ifBlank { null },
    createdAt = createdAt.millis(),
)

fun NotificationDto.toEntity(recipientId: String) = CachedNotification(
    notificationId = id,
    recipientId = recipientId,
    actorId = actorId,
    type = type,
    targetId = targetId,
    previewText = previewText,
    previewImageUrl = previewImageUrl,
    isRead = isRead,
    createdAt = createdAt.millis(),
)
