package com.insangram.app.core.firebase

/**
 * Single source of truth for Firestore collection paths and Storage prefixes.
 * The security rules in firebase/firestore.rules mirror these exactly.
 */
object FirestorePaths {
    const val USERS = "users"
    const val USERNAMES = "usernames"
    const val POSTS = "posts"
    const val REELS = "reels"
    const val STORIES = "stories"
    const val FOLLOWS = "follows"
    const val FOLLOW_REQUESTS = "followRequests"
    const val CONVERSATIONS = "conversations"
    const val NOTIFICATIONS = "notifications"
    const val REPORTS = "reports"
    const val HASHTAGS = "hashtags"
    const val ANALYTICS = "analytics"
    const val FEED_ITEMS = "feedItems"
    const val DEVICE_TOKENS = "deviceTokens"
    const val NOTES = "notes"

    const val SUB_LIKES = "likes"
    const val SUB_COMMENTS = "comments"
    const val SUB_VIEWS = "views"
    const val SUB_MEMBERS = "members"
    const val SUB_MESSAGES = "messages"
    const val SUB_ITEMS = "items"
    const val SUB_TOKENS = "tokens"
    const val SUB_SAVED_POSTS = "savedPosts"
    const val SUB_COLLECTIONS = "collections"
    const val SUB_SEARCH_HISTORY = "searchHistory"
    const val SUB_BLOCKS = "blocks"

    /** follows/{followerId}_{followeeId} makes duplicate follows impossible. */
    fun followId(followerId: String, followeeId: String) = followerId + "_" + followeeId

    /** followRequests/{requesterId}_{targetId} - same idea for requests. */
    fun followRequestId(requesterId: String, targetId: String) = requesterId + "_" + targetId

    /** Deterministic id for one-to-one chats so two people share one thread. */
    fun directConversationId(a: String, b: String) =
        listOf(a, b).sorted().joinToString("_")

    object Storage {
        fun profile(userId: String) = "users/$userId/profile"
        fun post(userId: String, postId: String) = "users/$userId/posts/$postId"
        fun reel(userId: String, reelId: String) = "users/$userId/reels/$reelId"
        fun story(userId: String, storyId: String) = "users/$userId/stories/$storyId"
        fun message(conversationId: String, messageId: String) =
            "conversations/$conversationId/messages/$messageId"
    }
}
