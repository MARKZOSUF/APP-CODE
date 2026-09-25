package com.insangram.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.insangram.app.core.common.InsangramConstants
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point.
 *
 * The manifest removes androidx.work.WorkManagerInitializer, so WorkManager is
 * initialised on demand here through [Configuration.Provider]. That is what
 * lets @HiltWorker workers receive constructor injection.
 */
@HiltAndroidApp
class InsangramApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    /**
     * Channels must exist before the first notification is posted, and creating
     * an existing channel is a no-op, so this runs unconditionally on start.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return

        val channels = listOf(
            Triple(
                InsangramConstants.CHANNEL_SOCIAL,
                R.string.notif_channel_social_name,
                R.string.notif_channel_social_desc,
            ),
            Triple(
                InsangramConstants.CHANNEL_MESSAGES,
                R.string.notif_channel_messages_name,
                R.string.notif_channel_messages_desc,
            ),
            Triple(
                InsangramConstants.CHANNEL_FOLLOWS,
                R.string.notif_channel_follows_name,
                R.string.notif_channel_follows_desc,
            ),
            Triple(
                InsangramConstants.CHANNEL_UPLOADS,
                R.string.notif_channel_uploads_name,
                R.string.notif_channel_uploads_desc,
            ),
        )

        channels.forEach { (id, nameRes, descRes) ->
            val importance = if (id == InsangramConstants.CHANNEL_UPLOADS) {
                NotificationManager.IMPORTANCE_LOW
            } else {
                NotificationManager.IMPORTANCE_DEFAULT
            }
            manager.createNotificationChannel(
                NotificationChannel(id, getString(nameRes), importance).apply {
                    description = getString(descRes)
                },
            )
        }
    }
}
