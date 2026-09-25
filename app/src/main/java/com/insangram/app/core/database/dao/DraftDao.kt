package com.insangram.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.insangram.app.core.database.entity.LocalDraft
import com.insangram.app.core.database.entity.MediaCacheEntry
import com.insangram.app.core.database.entity.PendingAction
import com.insangram.app.core.database.entity.PendingUpload
import kotlinx.coroutines.flow.Flow

/** Drafts, queued uploads and the offline action queue. */
@Dao
interface DraftDao {

    @Upsert suspend fun upsertDraft(draft: LocalDraft)

    @Query("SELECT * FROM local_draft WHERE ownerId = :ownerId ORDER BY updatedAt DESC")
    fun observeDrafts(ownerId: String): Flow<List<LocalDraft>>

    @Query("SELECT * FROM local_draft WHERE draftId = :draftId")
    suspend fun draft(draftId: String): LocalDraft?

    @Query("DELETE FROM local_draft WHERE draftId = :draftId")
    suspend fun deleteDraft(draftId: String)

    // --- uploads ------------------------------------------------------------

    @Upsert suspend fun upsertUpload(upload: PendingUpload)

    @Query("SELECT * FROM pending_upload WHERE ownerId = :ownerId AND state != 'SUCCEEDED' ORDER BY createdAt ASC")
    fun observeActiveUploads(ownerId: String): Flow<List<PendingUpload>>

    @Query("SELECT * FROM pending_upload WHERE uploadId = :uploadId")
    suspend fun upload(uploadId: String): PendingUpload?

    @Query("SELECT * FROM pending_upload WHERE state IN ('QUEUED', 'FAILED') ORDER BY createdAt ASC")
    suspend fun retryableUploads(): List<PendingUpload>

    /** Duplicate detection: the same media hash already uploaded by this user. */
    @Query("SELECT * FROM pending_upload WHERE ownerId = :ownerId AND mediaHash = :hash LIMIT 1")
    suspend fun uploadByHash(ownerId: String, hash: String): PendingUpload?

    @Query(
        """
        UPDATE pending_upload
        SET state = :state,
            progressPercent = :progress,
            lastError = :error,
            updatedAt = :now
        WHERE uploadId = :uploadId
        """,
    )
    suspend fun updateUploadState(
        uploadId: String,
        state: String,
        progress: Int,
        error: String?,
        now: Long,
    )

    @Query("UPDATE pending_upload SET attemptCount = attemptCount + 1 WHERE uploadId = :uploadId")
    suspend fun incrementUploadAttempt(uploadId: String)

    @Query("DELETE FROM pending_upload WHERE uploadId = :uploadId")
    suspend fun deleteUpload(uploadId: String)

    @Query("DELETE FROM pending_upload WHERE state = 'SUCCEEDED' AND updatedAt < :threshold")
    suspend fun pruneCompletedUploads(threshold: Long)

    // --- offline action queue ----------------------------------------------

    @Upsert suspend fun upsertAction(action: PendingAction)

    @Query("SELECT * FROM pending_action WHERE state = 'PENDING' ORDER BY createdAt ASC LIMIT :limit")
    suspend fun pendingActions(limit: Int): List<PendingAction>

    @Query("SELECT * FROM pending_action WHERE ownerId = :ownerId AND state != 'DONE' ORDER BY createdAt ASC")
    fun observePendingActions(ownerId: String): Flow<List<PendingAction>>

    @Query("SELECT * FROM pending_action WHERE idempotencyKey = :key LIMIT 1")
    suspend fun actionByIdempotencyKey(key: String): PendingAction?

    @Query(
        "UPDATE pending_action SET state = :state, attemptCount = attemptCount + 1, lastError = :error WHERE actionId = :actionId",
    )
    suspend fun updateActionState(actionId: String, state: String, error: String?)

    @Query("DELETE FROM pending_action WHERE actionId = :actionId")
    suspend fun deleteAction(actionId: String)

    @Query("DELETE FROM pending_action WHERE state = 'DONE'")
    suspend fun pruneCompletedActions()

    @Query("SELECT COUNT(*) FROM pending_action WHERE state = 'PENDING'")
    fun observePendingActionCount(): Flow<Int>

    // --- media cache --------------------------------------------------------

    @Upsert suspend fun upsertMediaCache(entry: MediaCacheEntry)

    @Query("SELECT SUM(sizeBytes) FROM media_cache_entry")
    suspend fun totalCacheBytes(): Long?

    @Query("SELECT * FROM media_cache_entry ORDER BY lastAccessedAt ASC LIMIT :limit")
    suspend fun leastRecentlyUsed(limit: Int): List<MediaCacheEntry>

    @Query("DELETE FROM media_cache_entry")
    suspend fun clearMediaCache()

    // --- worker-facing accessors -------------------------------------------
    // WorkManager workers address rows by id and record progress, so these
    // wrap the queries above with the names the workers use.

    @Query("SELECT * FROM pending_upload WHERE uploadId = :uploadId")
    suspend fun uploadById(uploadId: String): PendingUpload?

    @Query("UPDATE pending_upload SET progressPercent = :progressPercent WHERE uploadId = :uploadId")
    suspend fun updateUploadProgress(uploadId: String, progressPercent: Int)

    /** Oldest still-pending action; the replay queue is strictly ordered. */
    @Query(
        """
        SELECT * FROM pending_action
        WHERE state = 'PENDING'
        ORDER BY createdAt ASC
        LIMIT 1
        """,
    )
    suspend fun nextPendingAction(): PendingAction?

    @Query("DELETE FROM pending_action WHERE actionId = :actionId")
    suspend fun deletePendingAction(actionId: String)

    /**
     * Marks a replay attempt as failed and bumps the attempt counter, so a
     * poison row cannot spin forever.
     */
    @Query(
        """
        UPDATE pending_action
        SET state = 'FAILED', lastError = :error, attemptCount = attemptCount + 1
        WHERE actionId = :actionId
        """,
    )
    suspend fun markPendingActionFailed(actionId: String, error: String?)

    @Query("SELECT SUM(sizeBytes) FROM media_cache_entry")
    suspend fun totalMediaCacheBytes(): Long?

    @Query("SELECT * FROM media_cache_entry ORDER BY lastAccessedAt ASC LIMIT :limit")
    suspend fun leastRecentlyUsedMedia(limit: Int): List<MediaCacheEntry>

    @Query("DELETE FROM media_cache_entry WHERE cacheKey = :cacheKey")
    suspend fun deleteMediaCacheEntry(cacheKey: String)
}
