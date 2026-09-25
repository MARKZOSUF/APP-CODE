package com.insangram.app.core.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central place where every background job is enqueued. Named unique work is
 * used throughout so re-enqueuing after a process restart cannot create
 * duplicate uploads or duplicate action replays.
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager get() = WorkManager.getInstance(context)

    fun enqueueUpload(uploadId: String, unmeteredOnly: Boolean) {
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(workDataOf(UploadWorker.KEY_UPLOAD_ID to uploadId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(
                        if (unmeteredOnly) NetworkType.UNMETERED else NetworkType.CONNECTED,
                    )
                    .build(),
            )
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 20, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(
            UPLOAD_PREFIX + uploadId,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun cancelUpload(uploadId: String) {
        workManager.cancelUniqueWork(UPLOAD_PREFIX + uploadId)
    }

    /** Drains the offline action queue as soon as connectivity returns. */
    fun enqueuePendingActionFlush() {
        val request = OneTimeWorkRequestBuilder<PendingActionWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(WORK_PENDING_ACTIONS, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    fun schedulePeriodicMaintenance() {
        val request = PeriodicWorkRequestBuilder<MaintenanceWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().build())
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_MAINTENANCE,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        const val UPLOAD_PREFIX = "insangram_upload_"
        const val WORK_PENDING_ACTIONS = "insangram_pending_actions"
        const val WORK_MAINTENANCE = "insangram_maintenance"
    }
}
