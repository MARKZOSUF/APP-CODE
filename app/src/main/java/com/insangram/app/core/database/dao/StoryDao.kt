package com.insangram.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.insangram.app.core.database.entity.CachedNote
import com.insangram.app.core.database.entity.CachedStory
import com.insangram.app.core.database.entity.StoryViewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {

    @Upsert suspend fun upsert(stories: List<CachedStory>)
    @Upsert suspend fun upsert(story: CachedStory)

    /**
     * Active stories only. Expiry is evaluated against [now] rather than a
     * stored flag so an expired story can never leak into the tray, even if
     * the server-side cleanup function has not run yet.
     */
    @Query(
        """
        SELECT s.* FROM cached_story s
        WHERE s.expiresAt > :now AND s.isArchived = 0
          AND s.authorId NOT IN (:excludedAuthorIds)
          AND (
              s.authorId = :viewerId
              OR EXISTS (
                  SELECT 1 FROM follow_edge f
                  WHERE f.followerId = :viewerId AND f.followeeId = s.authorId
                    AND f.state = 'FOLLOWING' AND f.mutedStories = 0
              )
          )
          AND (
              s.audience = 'EVERYONE'
              OR s.authorId = :viewerId
              OR (
                  s.audience = 'CLOSE_FRIENDS' AND EXISTS (
                      SELECT 1 FROM follow_edge f2
                      WHERE f2.followerId = s.authorId AND f2.followeeId = :viewerId
                        AND f2.isCloseFriend = 1
                  )
              )
          )
        ORDER BY s.seenByMe ASC, s.createdAt DESC
        """,
    )
    fun observeActiveStories(
        viewerId: String,
        now: Long,
        excludedAuthorIds: List<String>,
    ): Flow<List<CachedStory>>

    @Query(
        "SELECT * FROM cached_story WHERE authorId = :authorId AND expiresAt > :now AND isArchived = 0 ORDER BY createdAt ASC",
    )
    suspend fun activeStoriesFor(authorId: String, now: Long): List<CachedStory>

    @Query("SELECT * FROM cached_story WHERE storyId = :storyId")
    suspend fun getStory(storyId: String): CachedStory?

    /** Archive is owner-only; callers must pass the signed-in user id. */
    @Query("SELECT * FROM cached_story WHERE authorId = :ownerId AND isArchived = 1 ORDER BY createdAt DESC")
    fun observeArchive(ownerId: String): Flow<List<CachedStory>>

    @Query("SELECT * FROM cached_story WHERE authorId = :ownerId AND highlightIds != '[]' ORDER BY createdAt DESC")
    fun observeHighlights(ownerId: String): Flow<List<CachedStory>>

    @Query("UPDATE cached_story SET seenByMe = 1 WHERE storyId = :storyId")
    suspend fun markSeen(storyId: String)

    @Query("UPDATE cached_story SET isArchived = 1 WHERE expiresAt <= :now AND isArchived = 0")
    suspend fun archiveExpired(now: Long)

    @Query("UPDATE cached_story SET highlightIds = :highlightIds WHERE storyId = :storyId")
    suspend fun setHighlights(storyId: String, highlightIds: List<String>)

    @Query("DELETE FROM cached_story WHERE storyId = :storyId")
    suspend fun delete(storyId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertView(view: StoryViewEntity)

    @Query("UPDATE cached_story SET viewCount = viewCount + 1 WHERE storyId = :storyId")
    suspend fun incrementViewCount(storyId: String)

    @Query("SELECT * FROM story_view WHERE storyId = :storyId ORDER BY viewedAt DESC")
    fun observeViewers(storyId: String): Flow<List<StoryViewEntity>>

    // --- notes --------------------------------------------------------------

    @Upsert suspend fun upsertNote(note: CachedNote)

    @Query(
        """
        SELECT n.* FROM cached_note n
        WHERE n.expiresAt > :now
          AND (
              n.authorId = :viewerId
              OR EXISTS (
                  SELECT 1 FROM follow_edge f
                  WHERE f.followerId = :viewerId AND f.followeeId = n.authorId AND f.state = 'FOLLOWING'
              )
          )
        ORDER BY n.createdAt DESC
        """,
    )
    fun observeNotes(viewerId: String, now: Long): Flow<List<CachedNote>>

    @Query("DELETE FROM cached_note WHERE authorId = :authorId")
    suspend fun deleteNotesFor(authorId: String)

    @Query("DELETE FROM cached_note WHERE expiresAt <= :now")
    suspend fun purgeExpiredNotes(now: Long)

    /**
     * Hard-deletes expired stories that were never added to a highlight.
     * Highlighted stories are archived instead so the profile keeps them.
     */
    @Query(
        """
        DELETE FROM cached_story
        WHERE expiresAt < :now AND isArchived = 0 AND highlightIds = '[]'
        """,
    )
    suspend fun deleteExpired(now: Long)
}
