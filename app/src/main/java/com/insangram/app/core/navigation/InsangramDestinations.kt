package com.insangram.app.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.insangram.app.R

/**
 * Every navigable route in one place. Routes are plain strings so they work
 * with Navigation Compose deep links, and argument builders are provided so no
 * caller has to concatenate a route by hand.
 */
object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"

    const val LOGIN = "auth/login"
    const val REGISTER = "auth/register"
    const val FORGOT_PASSWORD = "auth/forgot"
    const val EMAIL_VERIFICATION = "auth/verify"
    const val ACCOUNT_RECOVERY = "auth/recovery"
    const val ACCOUNT_SELECTION = "auth/accounts"
    const val REAUTHENTICATE = "auth/reauthenticate"
    const val DELETE_ACCOUNT = "auth/delete"

    const val HOME = "home"
    const val EXPLORE = "explore"
    const val CREATE = "create"
    const val REELS = "reels"
    const val PROFILE_TAB = "profile_tab"

    const val SEARCH = "search"
    const val NOTIFICATIONS = "notifications"
    const val SAVED = "saved"
    const val ANALYTICS = "analytics"
    const val ARCHIVE = "archive"
    const val RECENTLY_DELETED = "recently_deleted"
    const val MESSAGES = "messages"
    const val NEW_CONVERSATION = "messages/new"
    const val NEW_GROUP = "messages/new_group"
    const val SETTINGS = "settings"
    const val SETTINGS_PRIVACY = "settings/privacy"
    const val SETTINGS_SECURITY = "settings/security"
    const val SETTINGS_NOTIFICATIONS = "settings/notifications"
    const val SETTINGS_APPEARANCE = "settings/appearance"
    const val SETTINGS_DATA = "settings/data"
    const val SETTINGS_BLOCKED = "settings/blocked"
    const val SETTINGS_ACCESSIBILITY = "settings/accessibility"
    const val SETTINGS_ABOUT = "settings/about"
    const val EDIT_PROFILE = "profile/edit"
    const val STORY_CREATOR = "stories/create"
    const val EDITOR = "editor"
    const val APP_LOCK = "app_lock"

    // --- parameterised routes ----------------------------------------------
    const val ARG_USERNAME = "username"
    const val ARG_POST_ID = "postId"
    const val ARG_REEL_ID = "reelId"
    const val ARG_STORY_USER_ID = "storyUserId"
    const val ARG_CONVERSATION_ID = "conversationId"
    const val ARG_COLLECTION_ID = "collectionId"
    const val ARG_HASHTAG = "hashtag"
    const val ARG_QUERY = "query"
    const val ARG_TAB = "tab"

    const val PROFILE = "profile/{$ARG_USERNAME}"
    const val POST_DETAIL = "post/{$ARG_POST_ID}"
    const val COMMENTS = "post/{$ARG_POST_ID}/comments"
    const val LIKED_BY = "post/{$ARG_POST_ID}/likes"
    const val STORY_VIEWER = "stories/{$ARG_STORY_USER_ID}"
    const val CHAT = "messages/{$ARG_CONVERSATION_ID}"
    const val CONVERSATION_INFO = "messages/{$ARG_CONVERSATION_ID}/info"
    const val COLLECTION_DETAIL = "saved/{$ARG_COLLECTION_ID}"
    const val HASHTAG_DETAIL = "hashtag/{$ARG_HASHTAG}"
    const val FOLLOWERS = "profile/{$ARG_USERNAME}/followers"
    const val FOLLOWING = "profile/{$ARG_USERNAME}/following"
    const val FOLLOW_REQUESTS = "profile/requests"

    fun profile(username: String) = "profile/$username"
    fun postDetail(postId: String) = "post/$postId"
    fun comments(postId: String) = "post/$postId/comments"
    fun likedBy(postId: String) = "post/$postId/likes"
    fun storyViewer(userId: String) = "stories/$userId"
    fun chat(conversationId: String) = "messages/$conversationId"
    fun conversationInfo(conversationId: String) = "messages/$conversationId/info"
    fun collection(collectionId: String) = "saved/$collectionId"
    fun hashtag(tag: String) = "hashtag/${tag.removePrefix("#")}"
    fun followers(username: String) = "profile/$username/followers"
    fun following(username: String) = "profile/$username/following"
}

/** The five primary bottom-navigation destinations. */
enum class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(Routes.HOME, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    EXPLORE(Routes.EXPLORE, R.string.nav_search, Icons.Filled.Search, Icons.Outlined.Search),
    CREATE(Routes.CREATE, R.string.nav_create, Icons.Filled.AddBox, Icons.Outlined.AddBox),
    // The design uses the squared "reel" glyph (a play button inside a frame),
    // not the round PlayCircle.
    REELS(Routes.REELS, R.string.nav_reels, Icons.Filled.Slideshow, Icons.Outlined.Slideshow),
    PROFILE(Routes.PROFILE_TAB, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person),
}
