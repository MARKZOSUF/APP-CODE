package com.insangram.app.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.insangram.app.core.database.entity.CachedPost
import com.insangram.app.core.database.entity.CachedPostMedia
import com.insangram.app.core.database.entity.FeedRemoteKey
import com.insangram.app.core.database.entity.LikeEntity
import com.insangram.app.core.database.entity.NegativeSignalEntity
import com.insangram.app.core.database.entity.SavedCollectionEntity
import com.insangram.app.core.database.entity.SavedPostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {

    @Upsert
    suspend fun upsertPosts(posts: List<CachedPost>)

    @Upsert
    suspend fun upsertPost(post: CachedPost)

    @Upsert
    suspend fun upsertMedia(media: List<CachedPostMedia>)

    @Query("DELETE FROM cached_post_media WHERE postId = :postId")
    suspend fun deleteMediaFor(postId: String)

    @Transaction
    suspend fun upsertPostWithMedia(post: CachedPost, media: List<CachedPostMedia>) {
        upsertPost(post)
        deleteMediaFor(post.postId)
        upsertMedia(media)
    }

    @Query("SELECT * FROM cached_post WHERE postId = :postId")
    suspend fun getPost(postId: String): CachedPost?

    @Query("SELECT * FROM cached_post WHERE postId = :postId")
    fun observePost(postId: String): Flow<CachedPost?>

    @Query("SELECT * FROM cached_post_media WHERE postId = :postId ORDER BY position ASC")
    suspend fun mediaFor(postId: String): List<CachedPostMedia>

    @Query("SELECT * FROM cached_post_media WHERE postId IN (:postIds) ORDER BY position ASC")
    suspend fun mediaForAll(postIds: List<String>): List<CachedPostMedia>

    @Query("SELECT * FROM cached_post_media WHERE postId IN (:postIds) ORDER BY position ASC")
    fun observeMediaForAll(postIds: List<String>): Flow<List<CachedPostMedia>>

    /**
     * Ranked feed page. Excludes blocked authors, muted authors, suppressed
     * content, archived and soft-deleted posts, and private accounts the viewer
     * does not follow.
     */
    @Query(
        """
        SELECT p.* FROM cached_post p
        INNER JOIN cached_user u ON u.userId = p.authorId
        WHERE p.isArchived = 0
          AND p.deletedAt IS NULL
          AND p.authorId NOT IN (:excludedAuthorIds)
          AND p.postId NOT IN (
              SELECT contentId FROM negative_signal WHERE userId = :viewerId
          )
          AND (
              u.isPrivate = 0
              OR p.authorId = :viewerId
              OR EXISTS (
                  SELECT 1 FROM follow_edge f
                  WHERE f.followerId = :viewerId AND f.followeeId = p.authorId
                    AND f.state = 'FOLLOWING'
              )
          )
        ORDER BY p.feedScore DESC, p.createdAt DESC
        """,
    )
    fun rankedFeedPagingSource(
        viewerId: String,
        excludedAuthorIds: List<String>,
    ): PagingSource<Int, CachedPost>

    /** Chronological fallback used when ranking is disabled or scores are cold. */
    @Query(
        """
        SELECT p.* FROM cached_post p
        WHERE p.isArchived = 0 AND p.deletedAt IS NULL
          AND p.authorId NOT IN (:excludedAuthorIds)
          AND (
              p.authorId = :viewerId
              OR EXISTS (
                  SELECT 1 FROM follow_edge f
                  WHERE f.followerId = :viewerId AND f.followeeId = p.authorId
                    AND f.state = 'FOLLOWING' AND f.mutedPosts = 0
              )
          )
        ORDER BY p.createdAt DESC
        """,
    )
    fun chronologicalFeedPagingSource(
        viewerId: String,
        excludedAuthorIds: List<String>,
    ): PagingSource<Int, CachedPost>

    @Query(
        """
        SELECT * FROM cached_post
        WHERE authorId = :authorId AND isArchived = 0 AND deletedAt IS NULL
        ORDER BY createdAt DESC
        """,
    )
    fun profileGridPagingSource(authorId: String): PagingSource<Int, CachedPost>

    @Query(
        """
        SELECT * FROM cached_post
        WHERE authorId = :authorId AND isArchived = 0 AND deletedAt IS NULL
        ORDER BY createdAt DESC LIMIT :limit
        """,
    )
    fun observeAuthorPosts(authorId: String, limit: Int): Flow<List<CachedPost>>

    @Query("SELECT * FROM cached_post WHERE authorId = :authorId AND isArchived = 1 ORDER BY createdAt DESC")
    fun observeArchived(authorId: String): Flow<List<CachedPost>>

    @Query(
        """
        SELECT * FROM cached_post
        WHERE authorId = :authorId AND deletedAt IS NOT NULL
        ORDER BY deletedAt DESC
        """,
    )
    fun observeRecentlyDeleted(authorId: String): Flow<List<CachedPost>>

    /** Explore mosaic: recent public posts from accounts the viewer can see. */
    @Query(
        """
        SELECT p.* FROM cached_post p
        INNER JOIN cached_user u ON u.userId = p.authorId
        WHERE u.isPrivate = 0 AND p.isArchived = 0 AND p.deletedAt IS NULL
          AND p.authorId NOT IN (:excludedAuthorIds)
        ORDER BY (p.likeCount + p.commentCount * 3 + p.saveCount * 4) DESC, p.createdAt DESC
        """,
    )
    fun explorePagingSource(excludedAuthorIds: List<String>): PagingSource<Int, CachedPost>

    @Query(
        """
        SELECT p.* FROM cached_post p
        INNER JOIN cached_user u ON u.userId = p.authorId
        WHERE u.isPrivate = 0 AND p.isArchived = 0 AND p.deletedAt IS NULL
          AND (LOWER(p.caption) LIKE '%' || :term || '%' OR p.hashtags LIKE '%"' || :term || '"%')
        ORDER BY p.likeCount DESC
        LIMIT :limit
        """,
    )
    suspend fun searchPosts(term: String, limit: Int): List<CachedPost>

    @Query(
        """
        SELECT p.* FROM cached_post p
        WHERE p.hashtags LIKE '%"' || :tag || '"%' AND p.isArchived = 0 AND p.deletedAt IS NULL
        ORDER BY p.createdAt DESC LIMIT :limit
        """,
    )
    suspend fun postsForHashtag(tag: String, limit: Int): List<CachedPost>

    @Query("SELECT * FROM cached_post WHERE locationName = :place AND deletedAt IS NULL LIMIT :limit")
    suspend fun postsForPlace(place: String, limit: Int): List<CachedPost>

    @Query("UPDATE cached_post SET isArchived = :archived WHERE postId = :postId")
    suspend fun setArchived(postId: String, archived: Boolean)

    @Query("UPDATE cached_post SET deletedAt = :deletedAt WHERE postId = :postId")
    suspend fun setDeletedAt(postId: String, deletedAt: Long?)

    @Query("DELETE FROM cached_post WHERE postId = :postId")
    suspend fun hardDelete(postId: String)

    @Query("DELETE FROM cached_post WHERE deletedAt IS NOT NULL AND deletedAt < :threshold")
    suspend fun purgeExpiredDeletions(threshold: Long)

    @Query(
        """
        UPDATE cached_post SET caption = :caption, hashtags = :hashtags,
            commentsEnabled = :commentsEnabled, editedAt = :editedAt
        WHERE postId = :postId
        """,
    )
    suspend fun editPost(
        postId: String,
        caption: String,
        hashtags: List<String>,
        commentsEnabled: Boolean,
        editedAt: Long,
    )

    @Query("UPDATE cached_post SET feedScore = :score WHERE postId = :postId")
    suspend fun setFeedScore(postId: String, score: Double)

    @Query("UPDATE cached_post SET viewCount = viewCount + 1 WHERE postId = :postId")
    suspend fun incrementViewCount(postId: String)

    @Query("UPDATE cached_post SET shareCount = shareCount + 1 WHERE postId = :postId")
    suspend fun incrementShareCount(postId: String)

    @Query(
        """
        UPDATE cached_post SET likeCount = MAX(0, likeCount + :delta) WHERE postId = :postId
        """,
    )
    suspend fun incrementLikeCount(postId: String, delta: Long)

    @Query("UPDATE cached_post SET commentCount = MAX(0, commentCount + :delta) WHERE postId = :postId")
    suspend fun incrementCommentCount(postId: String, delta: Long)

    @Query("UPDATE cached_post SET saveCount = MAX(0, saveCount + :delta) WHERE postId = :postId")
    suspend fun incrementSaveCount(postId: String, delta: Long)

    // --- likes --------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLike(like: LikeEntity): Long

    @Query("DELETE FROM post_like WHERE userId = :userId AND contentId = :contentId")
    suspend fun deleteLike(userId: String, contentId: String)

    @Query(
        "SELECT EXISTS(SELECT 1 FROM post_like WHERE userId = :userId AND contentId = :contentId)",
    )
    fun observeIsLiked(userId: String, contentId: String): Flow<Boolean>

    @Query("SELECT contentId FROM post_like WHERE userId = :userId")
    fun observeLikedContentIds(userId: String): Flow<List<String>>

    @Query("SELECT userId FROM post_like WHERE contentId = :contentId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun likedByUserIds(contentId: String, limit: Int): List<String>

    /**
     * Toggling a like updates the edge and the denormalised counter in one
     * transaction, so an optimistic UI update can never leave them disagreeing.
     * In online mode the trusted count is recomputed by a Cloud Function.
     */
    @Transaction
    suspend fun toggleLike(userId: String, contentId: String, now: Long): Boolean {
        val inserted = insertLike(LikeEntity(userId, contentId, "POST", now))
        return if (inserted == -1L) {
            deleteLike(userId, contentId)
            incrementLikeCount(contentId, -1)
            false
        } else {
            incrementLikeCount(contentId, 1)
            true
        }
    }

    // --- saves & collections ------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaved(entity: SavedPostEntity)

    @Query("DELETE FROM saved_post WHERE userId = :userId AND postId = :postId")
    suspend fun deleteSaved(userId: String, postId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_post WHERE userId = :userId AND postId = :postId)")
    fun observeIsSaved(userId: String, postId: String): Flow<Boolean>

    @Query("SELECT postId FROM saved_post WHERE userId = :userId")
    fun observeSavedPostIds(userId: String): Flow<List<String>>

    @Query(
        """
        SELECT p.* FROM cached_post p
        INNER JOIN saved_post s ON s.postId = p.postId
        WHERE s.userId = :userId AND (:collectionId IS NULL OR s.collectionId = :collectionId)
        ORDER BY s.savedAt DESC
        """,
    )
    fun observeSavedPosts(userId: String, collectionId: String?): Flow<List<CachedPost>>

    @Query("UPDATE saved_post SET collectionId = :collectionId WHERE userId = :userId AND postId = :postId")
    suspend fun moveToCollection(userId: String, postId: String, collectionId: String?)

    @Upsert
    suspend fun upsertCollection(collection: SavedCollectionEntity)

    @Query("DELETE FROM saved_collection WHERE collectionId = :collectionId")
    suspend fun deleteCollection(collectionId: String)

    @Query("UPDATE saved_post SET collectionId = NULL WHERE collectionId = :collectionId")
    suspend fun detachCollection(collectionId: String)

    @Transaction
    suspend fun removeCollection(collectionId: String) {
        detachCollection(collectionId)
        deleteCollection(collectionId)
    }

    @Query("SELECT * FROM saved_collection WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    fun observeCollections(ownerId: String): Flow<List<SavedCollectionEntity>>

    @Query("SELECT COUNT(*) FROM saved_post WHERE userId = :userId AND collectionId = :collectionId")
    suspend fun collectionItemCount(userId: String, collectionId: String): Int

    // --- negative signals ---------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNegativeSignal(signal: NegativeSignalEntity)

    @Query("SELECT contentId FROM negative_signal WHERE userId = :userId")
    suspend fun negativeSignalIds(userId: String): List<String>

    // --- paging remote keys -------------------------------------------------

    @Upsert
    suspend fun upsertFeedKeys(keys: List<FeedRemoteKey>)

    @Query("SELECT * FROM feed_remote_key WHERE postId = :postId")
    suspend fun feedKey(postId: String): FeedRemoteKey?

    @Query("SELECT * FROM feed_remote_key WHERE ownerFeedId = :feedId ORDER BY position DESC LIMIT 1")
    suspend fun lastFeedKey(feedId: String): FeedRemoteKey?

    @Query("DELETE FROM feed_remote_key WHERE ownerFeedId = :feedId")
    suspend fun clearFeedKeys(feedId: String)

    @Query("SELECT COUNT(*) FROM cached_post")
    suspend fun postCount(): Int

    @Query("DELETE FROM cached_post WHERE cachedAt < :threshold AND authorId != :keepAuthorId")
    suspend fun evictStale(threshold: Long, keepAuthorId: String)

    // --- batched viewer state ----------------------------------------------
    // The assembler hydrates a whole page at once, so viewer state is fetched
    // in one IN(...) query per concern rather than per row.

    @Query("SELECT contentId FROM post_like WHERE userId = :viewerId AND contentId IN (:contentIds)")
    suspend fun likedContentIdsFor(viewerId: String, contentIds: List<String>): List<String>

    @Query("SELECT postId FROM saved_post WHERE userId = :viewerId AND postId IN (:postIds)")
    suspend fun savedPostIdsFor(viewerId: String, postIds: List<String>): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_post WHERE userId = :viewerId AND postId = :postId)")
    suspend fun isSavedNow(viewerId: String, postId: String): Boolean

    @Query("SELECT * FROM saved_collection WHERE collectionId = :collectionId")
    suspend fun collection(collectionId: String): SavedCollectionEntity?

    /** Duplicate-upload guard: the same media hash is never published twice. */
    @Query("SELECT EXISTS(SELECT 1 FROM cached_post_media WHERE mediaHash = :mediaHash)")
    suspend fun mediaHashExists(mediaHash: String): Boolean

    /**
     * Bounded candidate window handed to the local feed ranker. Ranking runs
     * over a recent slice rather than the whole cache so scoring stays cheap.
     */
    @Query(
        """
        SELECT * FROM cached_post
        WHERE isArchived = 0 AND deletedAt IS NULL
        ORDER BY createdAt DESC
        LIMIT :limit
        """,
    )
    suspend fun rankingCandidates(limit: Int): List<CachedPost>

    // --- maintenance --------------------------------------------------------

    @Query("DELETE FROM cached_post WHERE deletedAt IS NOT NULL AND deletedAt < :threshold")
    suspend fun purgeRecentlyDeletedBefore(threshold: Long)

    /** Drops stale cached rows, but never the viewer's own or pending content. */
    @Query(
        """
        DELETE FROM cached_post
        WHERE cachedAt < :threshold AND deletedAt IS NULL AND isArchived = 0
        """,
    )
    suspend fun deleteStaleCache(threshold: Long)
}
