package com.insangram.app.core.common

import java.util.concurrent.TimeUnit

object InsangramConstants {
    const val DEEP_LINK_SCHEME = "insangram"
    const val SHARE_BASE_URL = "https://insangram.example/p"

    const val FEED_PAGE_SIZE = 8
    const val GRID_PAGE_SIZE = 21
    const val REEL_PAGE_SIZE = 5
    const val COMMENT_PAGE_SIZE = 20
    const val MESSAGE_PAGE_SIZE = 30
    const val NOTIFICATION_PAGE_SIZE = 25
    const val SEARCH_RESULT_LIMIT = 30
    const val SEARCH_DEBOUNCE_MS = 300L

    /** Feed/profile cache staleness window before a network refresh is forced. */
    val FEED_CACHE_TTL_MS: Long = TimeUnit.MINUTES.toMillis(15)
    val PROFILE_CACHE_TTL_MS: Long = TimeUnit.HOURS.toMillis(6)
    val STORY_LIFETIME_MS: Long = TimeUnit.HOURS.toMillis(24)
    val RECENTLY_DELETED_RETENTION_MS: Long = TimeUnit.DAYS.toMillis(30)
    val NOTE_LIFETIME_MS: Long = TimeUnit.HOURS.toMillis(24)

    const val MAX_CAROUSEL_ITEMS = 10
    const val MAX_IMAGE_UPLOAD_BYTES = 8L * 1024 * 1024
    const val MAX_VIDEO_UPLOAD_BYTES = 100L * 1024 * 1024
    const val MAX_UPLOAD_EDGE_PX = 1440
    const val REEL_MAX_DURATION_MS = 90_000L

    /** Simultaneous ExoPlayer instances allowed in the reels pager. */
    const val MAX_CONCURRENT_PLAYERS = 3

    const val NOTIFICATION_CHANNEL_SOCIAL = "insangram_social"
    const val NOTIFICATION_CHANNEL_MESSAGES = "insangram_messages"
    const val NOTIFICATION_CHANNEL_FOLLOWS = "insangram_follows"
    const val NOTIFICATION_CHANNEL_UPLOADS = "insangram_uploads"

    // --- notification channel aliases --------------------------------------
    // Short aliases kept because the messaging service and the notification
    // builders refer to channels by these names.
    const val CHANNEL_SOCIAL = NOTIFICATION_CHANNEL_SOCIAL
    const val CHANNEL_MESSAGES = NOTIFICATION_CHANNEL_MESSAGES
    const val CHANNEL_FOLLOWS = NOTIFICATION_CHANNEL_FOLLOWS
    const val CHANNEL_UPLOADS = NOTIFICATION_CHANNEL_UPLOADS

    // --- cache + ranking budgets -------------------------------------------
    /** Rows the maintenance worker keeps in the notification cache. */
    const val MAX_CACHED_NOTIFICATIONS = 200

    /** Disk budget for the image/media cache before LRU eviction runs. */
    const val MEDIA_CACHE_BYTES = 256L * 1024L * 1024L

    /** Disk budget for the Media3 video cache used by the reel player pool. */
    const val VIDEO_CACHE_BYTES = 128L * 1024L * 1024L

    /** Candidate rows pulled from Room before the local feed ranker scores them. */
    const val RANKING_WINDOW = 120
}
