package com.insangram.app.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.insangram.app.core.common.InsangramConstants
import com.insangram.app.core.database.dao.ActivityDao
import com.insangram.app.core.database.dao.DraftDao
import com.insangram.app.core.database.dao.PostDao
import com.insangram.app.core.database.dao.StoryDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic local housekeeping: expire stories, trim the media cache and drop
 * cache rows past their freshness window. This keeps the Room database bounded
 * without ever deleting the user's own drafts or queued actions.
 */
@HiltWorker
class MaintenanceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val storyDao: StoryDao,
    private val postDao: PostDao,
    private val draftDao: DraftDao,
    private val activityDao: ActivityDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()

        storyDao.deleteExpired(now)
        postDao.purgeRecentlyDeletedBefore(now - InsangramConstants.RECENTLY_DELETED_RETENTION_MS)
        postDao.deleteStaleCache(now - InsangramConstants.FEED_CACHE_TTL_MS)
        activityDao.trimNotifications(InsangramConstants.MAX_CACHED_NOTIFICATIONS)

        // Evict least-recently-used media cache rows above the size ceiling.
        var total = draftDao.totalMediaCacheBytes() ?: 0L
        if (total > InsangramConstants.MEDIA_CACHE_BYTES) {
            val candidates = draftDao.leastRecentlyUsedMedia(200)
            for (entry in candidates) {
                if (total <= InsangramConstants.MEDIA_CACHE_BYTES) break
                runCatching { java.io.File(entry.localPath).delete() }
                draftDao.deleteMediaCacheEntry(entry.cacheKey)
                total -= entry.sizeBytes
            }
        }
        return Result.success()
    }
}
