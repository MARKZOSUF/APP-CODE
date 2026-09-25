package com.insangram.app.domain.repository

import androidx.paging.PagingData
import com.insangram.app.core.common.InsangramResult
import com.insangram.app.domain.model.AppNotification
import com.insangram.app.domain.model.AudienceScope
import com.insangram.app.domain.model.Comment
import com.insangram.app.domain.model.CommentSort
import com.insangram.app.domain.model.Conversation
import com.insangram.app.domain.model.ContentReport
import com.insangram.app.domain.model.CreatorAnalytics
import com.insangram.app.domain.model.HashtagSummary
import com.insangram.app.domain.model.Message
import com.insangram.app.domain.model.Note
import com.insangram.app.domain.model.Post
import com.insangram.app.domain.model.PostDraft
import com.insangram.app.domain.model.RecentSearch
import com.insangram.app.domain.model.Reel
import com.insangram.app.domain.model.RelationshipSummary
import com.insangram.app.domain.model.ReportTargetType
import com.insangram.app.domain.model.SavedCollection
import com.insangram.app.domain.model.SearchResults
import com.insangram.app.domain.model.SessionState
import com.insangram.app.domain.model.Story
import com.insangram.app.domain.model.StoryAudience
import com.insangram.app.domain.model.StoryTrayItem
import com.insangram.app.domain.model.StoryViewer
import com.insangram.app.domain.model.UploadTask
import com.insangram.app.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository contracts. Interfaces live in the domain layer; the demo and
 * online implementations live in the data layer and are bound per flavor.
 */

interface AuthRepository {
    val sessionState: Flow<SessionState>
    fun currentUserId(): String?

    suspend fun signIn(email: String, password: String): InsangramResult<User>
    suspend fun register(
        fullName: String,
        username: String,
        email: String,
        password: String,
        dateOfBirthMillis: Long,
        profilePictureUri: String?,
    ): InsangramResult<User>

    suspend fun sendPasswordReset(email: String): InsangramResult<Unit>
    suspend fun sendVerificationEmail(): InsangramResult<Unit>
    suspend fun refreshVerificationStatus(): InsangramResult<Boolean>
    suspend fun reauthenticate(password: String): InsangramResult<Unit>
    suspend fun signOut(): InsangramResult<Unit>
    suspend fun deleteAccount(password: String): InsangramResult<Unit>
    suspend fun isUsernameAvailable(username: String): InsangramResult<Boolean>
    /** Locally remembered accounts for the account-selection screen. */
    fun knownAccounts(): Flow<List<User>>
    suspend fun signInWithGoogle(idToken: String): InsangramResult<User>
    fun isGoogleSignInAvailable(): Boolean
}

interface UserRepository {
    fun observeUser(userId: String): Flow<User?>
    fun observeUserByUsername(username: String): Flow<User?>
    suspend fun refreshUser(userId: String): InsangramResult<User>
    suspend fun updateProfile(
        fullName: String,
        username: String,
        bio: String,
        website: String,
        pronouns: String,
        photoUri: String?,
    ): InsangramResult<User>

    fun observeRelationship(targetUserId: String): Flow<RelationshipSummary>
    suspend fun follow(targetUserId: String): InsangramResult<Unit>
    suspend fun unfollow(targetUserId: String): InsangramResult<Unit>
    suspend fun cancelFollowRequest(targetUserId: String): InsangramResult<Unit>
    suspend fun approveFollowRequest(requesterId: String): InsangramResult<Unit>
    suspend fun rejectFollowRequest(requesterId: String): InsangramResult<Unit>
    suspend fun removeFollower(followerId: String): InsangramResult<Unit>
    fun observeFollowRequests(): Flow<List<User>>
    fun observeFollowers(userId: String): Flow<List<User>>
    fun observeFollowing(userId: String): Flow<List<User>>
    fun observeCloseFriends(): Flow<List<User>>
    suspend fun setCloseFriend(userId: String, isCloseFriend: Boolean): InsangramResult<Unit>
    suspend fun setMuted(userId: String, posts: Boolean, stories: Boolean): InsangramResult<Unit>
    suspend fun block(userId: String): InsangramResult<Unit>
    suspend fun unblock(userId: String): InsangramResult<Unit>
    suspend fun restrict(userId: String, restricted: Boolean): InsangramResult<Unit>
    fun observeBlockedUsers(): Flow<List<User>>
    fun observeMutedUsers(): Flow<List<User>>
    suspend fun suggestedUsers(limit: Int): InsangramResult<List<User>>
}

interface PostRepository {
    fun homeFeed(chronological: Boolean): Flow<PagingData<Post>>
    fun explore(): Flow<PagingData<Post>>
    fun profileGrid(userId: String): Flow<PagingData<Post>>
    fun observePost(postId: String): Flow<Post?>
    suspend fun refreshFeed(): InsangramResult<Unit>

    suspend fun createPost(draft: PostDraft): InsangramResult<String>
    suspend fun editPost(
        postId: String,
        caption: String,
        commentsEnabled: Boolean,
    ): InsangramResult<Unit>

    suspend fun deletePost(postId: String): InsangramResult<Unit>
    suspend fun setArchived(postId: String, archived: Boolean): InsangramResult<Unit>
    suspend fun restorePost(postId: String): InsangramResult<Unit>
    suspend fun permanentlyDelete(postId: String): InsangramResult<Unit>
    fun observeArchived(): Flow<List<Post>>
    fun observeRecentlyDeleted(): Flow<List<Post>>

    suspend fun toggleLike(postId: String): InsangramResult<Boolean>
    suspend fun toggleSave(postId: String, collectionId: String?): InsangramResult<Boolean>
    suspend fun registerShare(postId: String): InsangramResult<Unit>
    suspend fun registerView(postId: String): InsangramResult<Unit>
    suspend fun likedBy(postId: String, limit: Int): InsangramResult<List<User>>
    suspend fun hidePost(postId: String): InsangramResult<Unit>
    suspend fun notInterested(postId: String): InsangramResult<Unit>

    fun observeSavedPosts(collectionId: String?): Flow<List<Post>>
    fun observeCollections(): Flow<List<SavedCollection>>
    suspend fun createCollection(name: String): InsangramResult<String>
    suspend fun renameCollection(collectionId: String, name: String): InsangramResult<Unit>
    suspend fun deleteCollection(collectionId: String): InsangramResult<Unit>
    suspend fun moveToCollection(postId: String, collectionId: String?): InsangramResult<Unit>

    fun observeDrafts(): Flow<List<PostDraft>>
    suspend fun saveDraft(draft: PostDraft): InsangramResult<Unit>
    suspend fun deleteDraft(draftId: String): InsangramResult<Unit>
    fun observeUploads(): Flow<List<UploadTask>>
    suspend fun retryUpload(uploadId: String): InsangramResult<Unit>
    suspend fun cancelUpload(uploadId: String): InsangramResult<Unit>
    /** Returns true when this media hash was already uploaded by this user. */
    suspend fun isDuplicateMedia(mediaHash: String): Boolean
}

interface ReelRepository {
    fun reelFeed(): Flow<PagingData<Reel>>
    fun observeReel(reelId: String): Flow<Reel?>
    fun observeAuthorReels(userId: String): Flow<List<Reel>>
    suspend fun trending(limit: Int): InsangramResult<List<Reel>>
    suspend fun uploadReel(
        videoUri: String,
        caption: String,
        audioLabel: String,
    ): InsangramResult<String>

    suspend fun toggleLike(reelId: String): InsangramResult<Boolean>
    suspend fun toggleSave(reelId: String): InsangramResult<Boolean>
    suspend fun registerView(reelId: String): InsangramResult<Unit>
    suspend fun recordWatchTime(reelId: String, watchedMs: Long, completed: Boolean)
    suspend fun registerShare(reelId: String): InsangramResult<Unit>
    suspend fun notInterested(reelId: String): InsangramResult<Unit>
    suspend fun deleteReel(reelId: String): InsangramResult<Unit>
}

interface StoryRepository {
    fun observeTray(): Flow<List<StoryTrayItem>>
    suspend fun storiesFor(userId: String): InsangramResult<List<Story>>
    suspend fun createPhotoStory(
        mediaUri: String,
        audience: StoryAudience,
        allowReplies: Boolean,
    ): InsangramResult<String>

    suspend fun createTextStory(
        text: String,
        backgroundColor: Long,
        fontStyle: String,
        audience: StoryAudience,
    ): InsangramResult<String>

    suspend fun markSeen(storyId: String): InsangramResult<Unit>
    suspend fun react(storyId: String, emoji: String): InsangramResult<Unit>
    suspend fun reply(storyId: String, text: String): InsangramResult<Unit>
    fun observeViewers(storyId: String): Flow<List<StoryViewer>>
    suspend fun deleteStory(storyId: String): InsangramResult<Unit>
    fun observeArchive(): Flow<List<Story>>
    fun observeHighlights(): Flow<List<Story>>
    suspend fun addToHighlight(storyId: String, highlightName: String): InsangramResult<Unit>
    /** Drops locally cached stories past their 24 hour lifetime. */
    suspend fun purgeExpired(): InsangramResult<Unit>
}

interface CommentRepository {
    fun observeComments(contentId: String, sort: CommentSort): Flow<List<Comment>>
    fun observePreview(contentId: String, limit: Int): Flow<List<Comment>>
    suspend fun addComment(
        contentId: String,
        text: String,
        replyToCommentId: String?,
    ): InsangramResult<String>

    suspend fun editComment(commentId: String, text: String): InsangramResult<Unit>
    suspend fun deleteComment(commentId: String, contentId: String): InsangramResult<Unit>
    suspend fun toggleCommentLike(commentId: String): InsangramResult<Boolean>
    suspend fun setPinned(commentId: String, pinned: Boolean): InsangramResult<Unit>
}

interface MessageRepository {
    fun observeInbox(): Flow<List<Conversation>>
    fun observeConversation(conversationId: String): Flow<Conversation?>
    fun observeMessages(conversationId: String): Flow<List<Message>>
    fun observeTotalUnread(): Flow<Int>
    fun observeSharedMedia(conversationId: String): Flow<List<Message>>

    suspend fun startDirectConversation(userId: String): InsangramResult<String>
    suspend fun createGroup(userIds: List<String>, title: String): InsangramResult<String>
    suspend fun sendText(
        conversationId: String,
        text: String,
        replyToMessageId: String?,
    ): InsangramResult<String>

    suspend fun sendMedia(
        conversationId: String,
        localUri: String,
        isVideo: Boolean,
    ): InsangramResult<String>

    suspend fun retryMessage(messageId: String): InsangramResult<Unit>
    suspend fun react(messageId: String, emoji: String): InsangramResult<Unit>
    suspend fun unsend(messageId: String): InsangramResult<Unit>
    suspend fun deleteForMe(messageId: String): InsangramResult<Unit>
    suspend fun setPinned(messageId: String, pinned: Boolean): InsangramResult<Unit>
    suspend fun markRead(conversationId: String): InsangramResult<Unit>
    suspend fun setTyping(conversationId: String, typing: Boolean)
    suspend fun searchMessages(conversationId: String, term: String): InsangramResult<List<Message>>
    suspend fun updateGroup(
        conversationId: String,
        title: String,
        memberIds: List<String>,
    ): InsangramResult<Unit>

    suspend fun setMuted(conversationId: String, muted: Boolean): InsangramResult<Unit>
    suspend fun leaveConversation(conversationId: String): InsangramResult<Unit>

    fun observeNotes(): Flow<List<Note>>
    suspend fun postNote(text: String, emoji: String, audience: AudienceScope): InsangramResult<Unit>
    suspend fun clearNote(): InsangramResult<Unit>
    suspend fun replyToNote(noteId: String, text: String): InsangramResult<Unit>
}

interface NotificationRepository {
    fun observeNotifications(): Flow<List<AppNotification>>
    fun observeUnreadCount(): Flow<Int>
    suspend fun markRead(notificationId: String): InsangramResult<Unit>
    suspend fun markAllRead(): InsangramResult<Unit>
    suspend fun registerDeviceToken(token: String): InsangramResult<Unit>
    suspend fun unregisterDeviceToken(): InsangramResult<Unit>
}

interface SearchRepository {
    suspend fun search(query: String): InsangramResult<SearchResults>
    fun observeRecentSearches(): Flow<List<RecentSearch>>
    suspend fun recordSearch(term: String, kind: String): InsangramResult<Unit>
    suspend fun deleteRecentSearch(id: String): InsangramResult<Unit>
    suspend fun clearSearchHistory(): InsangramResult<Unit>
    suspend fun trendingHashtags(limit: Int): InsangramResult<List<HashtagSummary>>
    suspend fun postsForHashtag(tag: String, limit: Int): InsangramResult<List<Post>>
}

interface ModerationRepository {
    suspend fun report(
        targetType: ReportTargetType,
        targetId: String,
        reason: String,
        details: String,
    ): InsangramResult<Unit>

    fun observeMyReports(): Flow<List<ContentReport>>
}

interface AnalyticsRepository {
    fun observeAnalytics(): Flow<CreatorAnalytics?>
    suspend fun refresh(): InsangramResult<Unit>
    suspend fun recordProfileVisit(userId: String)
}
