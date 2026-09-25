package com.insangram.app.feature.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.insangram.app.domain.model.AppNotification
import com.insangram.app.domain.model.Conversation
import com.insangram.app.domain.model.Message
import com.insangram.app.domain.model.NotificationType
import com.insangram.app.domain.model.Post
import com.insangram.app.domain.model.StoryTrayItem
import com.insangram.app.domain.model.User
import com.insangram.app.domain.repository.AuthRepository
import com.insangram.app.domain.repository.MessageRepository
import com.insangram.app.domain.repository.NotificationRepository
import com.insangram.app.domain.repository.PostRepository
import com.insangram.app.domain.repository.StoryRepository
import com.insangram.app.domain.repository.UserRepository
import com.insangram.app.feature.ui.DemoChat
import com.insangram.app.feature.ui.DemoMessage
import com.insangram.app.feature.ui.DemoNotification
import com.insangram.app.feature.ui.DemoPost
import com.insangram.app.feature.ui.DemoUser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Real-data view models for the tab screens. Every screen in the app reads from
 * these instead of the sample content, so the shipped build shows whatever the
 * signed-in account actually has in Firestore / the local Room cache.
 *
 * The UI components still take the lightweight presentation models, so domain
 * objects are mapped here once and the composables stay dumb.
 */

// ---------------------------------------------------------------------------
// Mapping helpers
// ---------------------------------------------------------------------------

fun formatSocialCount(value: Long): String = when {
    value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
    value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
    else -> value.toString()
}

fun relativeTimeLabel(timestampMs: Long): String {
    if (timestampMs <= 0L) return ""
    val diff = System.currentTimeMillis() - timestampMs
    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> minutes.toString() + "m"
        hours < 24 -> hours.toString() + "h"
        days < 7 -> days.toString() + "d"
        else -> (days / 7).toString() + "w"
    }
}

fun User.toPresentation(seenStory: Boolean = false): DemoUser = DemoUser(
    username = username,
    name = fullName,
    avatar = photoUrl ?: "",
    bio = bio,
    seenStory = seenStory,
)

fun Post.toPresentation(): DemoPost = DemoPost(
    id = id,
    author = author.toPresentation(),
    location = locationName ?: "",
    image = media.firstOrNull()?.url ?: "",
    pageCount = media.size,
    likes = formatSocialCount(likeCount),
    caption = caption,
    hashtags = hashtags.joinToString(" ") { "#" + it.removePrefix("#") },
    liked = likedByViewer,
)

fun Conversation.toPresentation(): DemoChat {
    val other = members.firstOrNull()
    return DemoChat(
        user = DemoUser(
            username = if (isGroup || other == null) title else other.username,
            name = title,
            avatar = imageUrl ?: other?.photoUrl ?: "",
        ),
        preview = lastMessagePreview,
        time = relativeTimeLabel(lastMessageAt),
        unread = unreadCount > 0,
    )
}

fun Message.toPresentation(viewerId: String?): DemoMessage = DemoMessage(
    text = if (isUnsent) "Message unsent" else text,
    time = relativeTimeLabel(createdAt),
    mine = viewerId != null && sender.id == viewerId,
    image = mediaUrl ?: localMediaUri,
)

fun AppNotification.toPresentation(): DemoNotification = DemoNotification(
    user = actor.toPresentation(),
    text = when (type) {
        NotificationType.NEW_FOLLOWER -> "started following you"
        NotificationType.FOLLOW_REQUEST -> "requested to follow you"
        NotificationType.REQUEST_ACCEPTED -> "accepted your follow request"
        NotificationType.POST_LIKE -> "liked your post"
        NotificationType.REEL_LIKE -> "liked your reel"
        NotificationType.COMMENT -> "commented: " + previewText
        NotificationType.REPLY -> "replied: " + previewText
        NotificationType.MENTION -> "mentioned you"
        NotificationType.TAG -> "tagged you in a post"
        NotificationType.NEW_MESSAGE -> "sent you a message"
        NotificationType.STORY_REACTION -> "reacted to your story"
        NotificationType.STORY_REPLY -> "replied to your story"
    },
    time = relativeTimeLabel(createdAt),
    follow = type == NotificationType.NEW_FOLLOWER || type == NotificationType.FOLLOW_REQUEST,
    thumbnail = previewImageUrl,
)

// ---------------------------------------------------------------------------
// Home feed
// ---------------------------------------------------------------------------

@HiltViewModel
class HomeFeedViewModel @Inject constructor(
    private val postRepository: PostRepository,
    storyRepository: StoryRepository,
    userRepository: UserRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val viewerId: String? = authRepository.currentUserId()

    val feed: Flow<PagingData<Post>> =
        postRepository.homeFeed(chronological = false).cachedIn(viewModelScope)

    val stories: StateFlow<List<StoryTrayItem>> = storyRepository.observeTray()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val viewer: StateFlow<User?> =
        (if (viewerId != null) userRepository.observeUser(viewerId) else flowOf(null))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun refresh() {
        viewModelScope.launch { postRepository.refreshFeed() }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch { postRepository.toggleLike(postId) }
    }

    fun toggleSave(postId: String) {
        viewModelScope.launch { postRepository.toggleSave(postId, null) }
    }

    fun share(postId: String) {
        viewModelScope.launch { postRepository.registerShare(postId) }
    }
}

// ---------------------------------------------------------------------------
// Profile tab
// ---------------------------------------------------------------------------

@HiltViewModel
class ProfileTabViewModel @Inject constructor(
    postRepository: PostRepository,
    private val userRepository: UserRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val viewerId: String? = authRepository.currentUserId()

    val viewer: StateFlow<User?> =
        (if (viewerId != null) userRepository.observeUser(viewerId) else flowOf(null))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val grid: Flow<PagingData<Post>> =
        if (viewerId != null) {
            postRepository.profileGrid(viewerId).cachedIn(viewModelScope)
        } else {
            flowOf(PagingData.empty())
        }

    val saved: StateFlow<List<Post>> = postRepository.observeSavedPosts(null)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Persists the edit-profile form for the signed-in account. */
    fun saveProfile(
        fullName: String,
        username: String,
        bio: String,
        website: String,
        pronouns: String,
    ) {
        viewModelScope.launch {
            userRepository.updateProfile(
                fullName = fullName,
                username = username,
                bio = bio,
                website = website,
                pronouns = pronouns,
                photoUri = null,
            )
        }
    }
}

/** Any profile, looked up by username, for both the tab and other accounts. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
) : ViewModel() {

    private val username = MutableStateFlow("")

    val user: StateFlow<User?> = username
        .flatMapLatest { name ->
            if (name.isBlank()) flowOf(null) else userRepository.observeUserByUsername(name)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val posts: Flow<PagingData<Post>> = user
        .flatMapLatest { loaded ->
            if (loaded == null) flowOf(PagingData.empty()) else postRepository.profileGrid(loaded.id)
        }
        .cachedIn(viewModelScope)

    fun open(name: String) {
        if (username.value != name) username.value = name
    }

    fun follow(userId: String) {
        viewModelScope.launch { userRepository.follow(userId) }
    }

    fun unfollow(userId: String) {
        viewModelScope.launch { userRepository.unfollow(userId) }
    }
}

// ---------------------------------------------------------------------------
// Direct messages
// ---------------------------------------------------------------------------

@HiltViewModel
class InboxViewModel @Inject constructor(
    messageRepository: MessageRepository,
) : ViewModel() {

    val conversations: StateFlow<List<Conversation>> = messageRepository.observeInbox()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unread: StateFlow<Int> = messageRepository.observeTotalUnread()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    val viewerId: String? = authRepository.currentUserId()

    private val conversationId = MutableStateFlow<String?>(null)

    val messages: StateFlow<List<Message>> = conversationId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(emptyList()) else messageRepository.observeMessages(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val title: StateFlow<String> = conversationId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) {
                flowOf(null)
            } else {
                messageRepository.observeConversation(id)
            }
        }
        .map { conversation -> conversation?.title ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun open(id: String) {
        conversationId.value = id
    }

    fun send(text: String) {
        val id = conversationId.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch { messageRepository.sendText(id, text, null) }
    }
}

// ---------------------------------------------------------------------------
// Notifications
// ---------------------------------------------------------------------------

@HiltViewModel
class NotificationsFeedViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    val notifications: StateFlow<List<AppNotification>> =
        notificationRepository.observeNotifications()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unread: StateFlow<Int> = notificationRepository.observeUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun markAllRead() {
        viewModelScope.launch { notificationRepository.markAllRead() }
    }
}
