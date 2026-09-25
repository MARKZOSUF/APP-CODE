package com.insangram.app.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.insangram.app.core.database.dao.DraftDao
import com.insangram.app.domain.repository.PostRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Performs a queued media upload in the background so leaving the create
 * screen never cancels a publish. State transitions are persisted in Room, so
 * the UI can always show an accurate queued/running/failed state.
 */
@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val draftDao: DraftDao,
    private val postRepository: PostRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uploadId = inputData.getString(KEY_UPLOAD_ID) ?: return Result.failure()
        val upload = draftDao.uploadById(uploadId) ?: return Result.success()

        // Idempotency: a completed upload must never be replayed.
        if (upload.state == "SUCCEEDED") return Result.success()

        val startedAt = System.currentTimeMillis()
        draftDao.updateUploadState(uploadId, "RUNNING", 10, null, startedAt)
        return when (val result = postRepository.retryUpload(uploadId)) {
            is com.insangram.app.core.common.InsangramResult.Success -> {
                draftDao.updateUploadState(
                    uploadId,
                    "SUCCEEDED",
                    100,
                    null,
                    System.currentTimeMillis(),
                )
                draftDao.updateUploadProgress(uploadId, 100)
                Result.success()
            }

            is com.insangram.app.core.common.InsangramResult.Failure -> {
                val message = result.error.technicalMessage
                draftDao.updateUploadState(
                    uploadId,
                    "FAILED",
                    0,
                    message,
                    System.currentTimeMillis(),
                )
                draftDao.incrementUploadAttempt(uploadId)
                if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
            }
        }
    }

    companion object {
        const val KEY_UPLOAD_ID = "upload_id"
        const val MAX_ATTEMPTS = 4
    }
}
