package com.insangram.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.insangram.app.core.database.entity.CachedComment
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {

    @Upsert suspend fun upsert(comments: List<CachedComment>)
    @Upsert suspend fun upsert(comment: CachedComment)

    /** Top-level comments, pinned first. Replies are loaded per parent. */
    @Query(
        """
        SELECT * FROM cached_comment
        WHERE parentContentId = :contentId AND replyToCommentId IS NULL
          AND authorId NOT IN (:excludedAuthorIds)
        ORDER BY isPinned DESC,
            CASE WHEN :newestFirst THEN createdAt END DESC,
            CASE WHEN NOT :newestFirst THEN likeCount END DESC,
            createdAt DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun topLevelComments(
        contentId: String,
        excludedAuthorIds: List<String>,
        newestFirst: Boolean,
        limit: Int,
        offset: Int,
    ): List<CachedComment>

    @Query(
        """
        SELECT * FROM cached_comment
        WHERE parentContentId = :contentId AND replyToCommentId IS NULL
          AND authorId NOT IN (:excludedAuthorIds)
        ORDER BY isPinned DESC, createdAt DESC
        """,
    )
    fun observeTopLevelComments(
        contentId: String,
        excludedAuthorIds: List<String>,
    ): Flow<List<CachedComment>>

    @Query("SELECT * FROM cached_comment WHERE replyToCommentId = :commentId ORDER BY createdAt ASC")
    fun observeReplies(commentId: String): Flow<List<CachedComment>>

    @Query("SELECT * FROM cached_comment WHERE parentContentId = :contentId ORDER BY createdAt ASC LIMIT :limit")
    fun observePreview(contentId: String, limit: Int): Flow<List<CachedComment>>

    @Query("SELECT * FROM cached_comment WHERE commentId = :commentId")
    suspend fun getComment(commentId: String): CachedComment?

    @Query("UPDATE cached_comment SET text = :text, editedAt = :editedAt WHERE commentId = :commentId")
    suspend fun edit(commentId: String, text: String, editedAt: Long)

    @Query("UPDATE cached_comment SET isPinned = :pinned WHERE commentId = :commentId")
    suspend fun setPinned(commentId: String, pinned: Boolean)

    @Query(
        """
        UPDATE cached_comment
        SET likedByMe = :liked, likeCount = MAX(0, likeCount + :delta)
        WHERE commentId = :commentId
        """,
    )
    suspend fun setLiked(commentId: String, liked: Boolean, delta: Long)

    @Query("DELETE FROM cached_comment WHERE commentId = :commentId")
    suspend fun delete(commentId: String)

    @Query("DELETE FROM cached_comment WHERE replyToCommentId = :commentId")
    suspend fun deleteReplies(commentId: String)

    /** Deleting a comment removes its replies so no orphans remain. */
    @Transaction
    suspend fun deleteWithReplies(commentId: String): Int {
        val replyCount = replyCount(commentId)
        deleteReplies(commentId)
        delete(commentId)
        return replyCount + 1
    }

    @Query("SELECT COUNT(*) FROM cached_comment WHERE replyToCommentId = :commentId")
    suspend fun replyCount(commentId: String): Int

    @Query("SELECT COUNT(*) FROM cached_comment WHERE parentContentId = :contentId")
    suspend fun commentCount(contentId: String): Int

    @Query("DELETE FROM cached_comment WHERE parentContentId = :contentId")
    suspend fun deleteAllFor(contentId: String)

    /**
     * Suspend form of [observePreview] used by the feed assembler, which needs
     * a one-shot read rather than a stream.
     */
    @Query(
        """
        SELECT * FROM cached_comment
        WHERE parentContentId = :contentId AND replyToCommentId IS NULL
        ORDER BY isPinned DESC, likeCount DESC, createdAt ASC
        LIMIT :limit
        """,
    )
    suspend fun previewFor(contentId: String, limit: Int): List<CachedComment>
}
