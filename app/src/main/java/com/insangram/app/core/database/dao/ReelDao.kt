package com.insangram.app.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.insangram.app.core.database.entity.CachedReel
import com.insangram.app.core.database.entity.ReelRemoteKey
import com.insangram.app.core.database.entity.WatchTimeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReelDao {

    @Upsert suspend fun upsert(reels: List<CachedReel>)
    @Upsert suspend fun upsert(reel: CachedReel)

    @Query(
        """
        SELECT r.* FROM cached_reel r
        INNER JOIN cached_user u ON u.userId = r.authorId
        WHERE r.deletedAt IS NULL
          AND r.authorId NOT IN (:excludedAuthorIds)
          AND r.reelId NOT IN (SELECT contentId FROM negative_signal WHERE userId = :viewerId)
          AND (
              u.isPrivate = 0 OR r.authorId = :viewerId
              OR EXISTS (
                  SELECT 1 FROM follow_edge f
                  WHERE f.followerId = :viewerId AND f.followeeId = r.authorId AND f.state = 'FOLLOWING'
              )
          )
        ORDER BY r.feedScore DESC, r.createdAt DESC
        """,
    )
    fun recommendedPagingSource(
        viewerId: String,
        excludedAuthorIds: List<String>,
    ): PagingSource<Int, CachedReel>

    @Query("SELECT * FROM cached_reel WHERE reelId = :reelId")
    suspend fun getReel(reelId: String): CachedReel?

    @Query("SELECT * FROM cached_reel WHERE reelId = :reelId")
    fun observeReel(reelId: String): Flow<CachedReel?>

    @Query("SELECT * FROM cached_reel WHERE authorId = :authorId AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeAuthorReels(authorId: String): Flow<List<CachedReel>>

    @Query(
        "SELECT * FROM cached_reel WHERE deletedAt IS NULL ORDER BY viewCount DESC, createdAt DESC LIMIT :limit",
    )
    suspend fun trending(limit: Int): List<CachedReel>

    @Query("UPDATE cached_reel SET viewCount = viewCount + 1 WHERE reelId = :reelId")
    suspend fun incrementViewCount(reelId: String)

    @Query("UPDATE cached_reel SET likeCount = MAX(0, likeCount + :delta) WHERE reelId = :reelId")
    suspend fun incrementLikeCount(reelId: String, delta: Long)

    @Query("UPDATE cached_reel SET commentCount = MAX(0, commentCount + :delta) WHERE reelId = :reelId")
    suspend fun incrementCommentCount(reelId: String, delta: Long)

    @Query("UPDATE cached_reel SET saveCount = MAX(0, saveCount + :delta) WHERE reelId = :reelId")
    suspend fun incrementSaveCount(reelId: String, delta: Long)

    @Query("UPDATE cached_reel SET shareCount = shareCount + 1 WHERE reelId = :reelId")
    suspend fun incrementShareCount(reelId: String)

    @Query(
        "UPDATE cached_reel SET totalWatchTimeMs = totalWatchTimeMs + :deltaMs WHERE reelId = :reelId",
    )
    suspend fun addWatchTime(reelId: String, deltaMs: Long)

    @Query("UPDATE cached_reel SET feedScore = :score WHERE reelId = :reelId")
    suspend fun setFeedScore(reelId: String, score: Double)

    @Query("UPDATE cached_reel SET deletedAt = :deletedAt WHERE reelId = :reelId")
    suspend fun setDeletedAt(reelId: String, deletedAt: Long?)

    @Query("SELECT * FROM cached_reel WHERE authorId = :authorId AND deletedAt IS NOT NULL")
    fun observeRecentlyDeleted(authorId: String): Flow<List<CachedReel>>

    @Query("DELETE FROM cached_reel WHERE deletedAt IS NOT NULL AND deletedAt < :threshold")
    suspend fun purgeExpiredDeletions(threshold: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchTime(entity: WatchTimeEntity)

    @Query("SELECT * FROM watch_time WHERE userId = :userId AND contentId = :contentId")
    suspend fun watchTime(userId: String, contentId: String): WatchTimeEntity?

    @Query("SELECT SUM(watchedMs) FROM watch_time WHERE userId = :userId")
    suspend fun totalWatchTime(userId: String): Long?

    @Upsert suspend fun upsertKeys(keys: List<ReelRemoteKey>)

    @Query("SELECT * FROM reel_remote_key ORDER BY position DESC LIMIT 1")
    suspend fun lastKey(): ReelRemoteKey?

    @Query("DELETE FROM reel_remote_key")
    suspend fun clearKeys()

    @Query("SELECT COUNT(*) FROM cached_reel")
    suspend fun reelCount(): Int
}
