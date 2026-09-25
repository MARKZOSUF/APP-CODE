package com.insangram.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.insangram.app.core.database.entity.AnalyticsSnapshot
import com.insangram.app.core.database.entity.CachedNotification
import com.insangram.app.core.database.entity.ContentReportEntity
import com.insangram.app.core.database.entity.HashtagEntity
import com.insangram.app.core.database.entity.InterestSignalEntity
import com.insangram.app.core.database.entity.SearchHistoryEntity
import com.insangram.app.core.database.entity.SyncMetadata
import kotlinx.coroutines.flow.Flow

/** Notifications, search, hashtags, reports, analytics and sync bookkeeping. */
@Dao
interface ActivityDao {

    @Upsert suspend fun upsertNotifications(items: List<CachedNotification>)
    @Upsert suspend fun upsertNotification(item: CachedNotification)

    @Query("SELECT * FROM cached_notification WHERE recipientId = :userId ORDER BY createdAt DESC LIMIT :limit")
    fun observeNotifications(userId: String, limit: Int): Flow<List<CachedNotification>>

    @Query("SELECT COUNT(*) FROM cached_notification WHERE recipientId = :userId AND isRead = 0")
    fun observeUnreadCount(userId: String): Flow<Int>

    @Query("UPDATE cached_notification SET isRead = 1 WHERE notificationId = :notificationId")
    suspend fun markRead(notificationId: String)

    @Query("UPDATE cached_notification SET isRead = 1 WHERE recipientId = :userId")
    suspend fun markAllRead(userId: String)

    @Query("DELETE FROM cached_notification WHERE recipientId = :userId")
    suspend fun clearNotifications(userId: String)

    // --- search history -----------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(entry: SearchHistoryEntity)

    @Query("SELECT * FROM search_history WHERE ownerId = :userId ORDER BY searchedAt DESC LIMIT :limit")
    fun observeSearchHistory(userId: String, limit: Int): Flow<List<SearchHistoryEntity>>

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteSearchHistory(id: String)

    @Query("DELETE FROM search_history WHERE ownerId = :userId")
    suspend fun clearSearchHistory(userId: String)

    // --- hashtags -----------------------------------------------------------

    @Upsert suspend fun upsertHashtags(tags: List<HashtagEntity>)

    @Query("SELECT * FROM hashtag ORDER BY recentEngagement DESC, postCount DESC LIMIT :limit")
    fun observeTrendingHashtags(limit: Int): Flow<List<HashtagEntity>>

    @Query(
        "SELECT * FROM hashtag WHERE normalizedTag LIKE :prefix || '%' ORDER BY postCount DESC LIMIT :limit",
    )
    suspend fun searchHashtags(prefix: String, limit: Int): List<HashtagEntity>

    @Query("SELECT * FROM hashtag WHERE normalizedTag = :tag")
    suspend fun hashtag(tag: String): HashtagEntity?

    @Query(
        """
        UPDATE hashtag SET postCount = MAX(0, postCount + :delta), lastUsedAt = :now
        WHERE normalizedTag = :tag
        """,
    )
    suspend fun incrementHashtagCount(tag: String, delta: Long, now: Long)

    // --- interest signals (local ranking) -----------------------------------

    @Upsert suspend fun upsertInterestSignal(signal: InterestSignalEntity)

    @Query("SELECT * FROM interest_signal WHERE userId = :userId ORDER BY weight DESC LIMIT :limit")
    suspend fun topInterests(userId: String, limit: Int): List<InterestSignalEntity>

    @Query("SELECT * FROM interest_signal WHERE userId = :userId")
    suspend fun allInterests(userId: String): List<InterestSignalEntity>

    @Query("SELECT * FROM interest_signal WHERE userId = :userId AND token = :token")
    suspend fun interestSignal(userId: String, token: String): InterestSignalEntity?

    // --- reports ------------------------------------------------------------

    @Upsert suspend fun upsertReport(report: ContentReportEntity)

    @Query("SELECT * FROM content_report WHERE reporterId = :userId ORDER BY createdAt DESC")
    fun observeReports(userId: String): Flow<List<ContentReportEntity>>

    @Query("UPDATE content_report SET state = :state WHERE reportId = :reportId")
    suspend fun setReportState(reportId: String, state: String)

    // --- analytics ----------------------------------------------------------

    @Upsert suspend fun upsertAnalytics(snapshot: AnalyticsSnapshot)

    @Query("SELECT * FROM analytics_snapshot WHERE userId = :userId")
    fun observeAnalytics(userId: String): Flow<AnalyticsSnapshot?>

    @Query("SELECT * FROM analytics_snapshot WHERE userId = :userId")
    suspend fun analytics(userId: String): AnalyticsSnapshot?

    // --- sync metadata ------------------------------------------------------

    @Upsert suspend fun upsertSyncMetadata(metadata: SyncMetadata)

    @Query("SELECT * FROM sync_metadata WHERE key = :key")
    suspend fun syncMetadata(key: String): SyncMetadata?

    /** Keeps the notification cache bounded to the newest [limit] rows. */
    @Query(
        """
        DELETE FROM cached_notification
        WHERE notificationId NOT IN (
            SELECT notificationId FROM cached_notification
            ORDER BY createdAt DESC LIMIT :limit
        )
        """,
    )
    suspend fun trimNotifications(limit: Int)
}
