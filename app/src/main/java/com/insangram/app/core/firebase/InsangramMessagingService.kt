package com.insangram.app.core.firebase

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.insangram.app.MainActivity
import com.insangram.app.R
import com.insangram.app.core.common.InsangramConstants
import com.insangram.app.core.datastore.InsangramPreferences
import com.insangram.app.domain.repository.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receives FCM pushes. Payloads are always data-only so this service controls
 * the channel, the deep link, and - importantly - whether the message body is
 * shown at all, honouring the "notification previews" privacy setting.
 */
@AndroidEntryPoint
class InsangramMessagingService : FirebaseMessagingService() {

    @Inject lateinit var notificationRepository: NotificationRepository
    @Inject lateinit var preferences: InsangramPreferences

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { notificationRepository.registerDeviceToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val type = data["type"] ?: return
        val title = data["title"].orEmpty().ifBlank { getString(R.string.app_name) }
        val body = data["body"].orEmpty()
        val deepLink = data["deepLink"]
        val dedupeKey = data["dedupeKey"] ?: type + (data["targetId"] ?: "")

        scope.launch {
            val settings = preferences.settings.first()
            val allowed = when {
                type.contains("MESSAGE") -> settings.pushMessages
                type.contains("LIKE") -> settings.pushLikes
                type.contains("COMMENT") || type.contains("REPLY") -> settings.pushComments
                type.contains("FOLLOW") || type.contains("REQUEST") -> settings.pushFollows
                type.contains("MENTION") || type.contains("TAG") -> settings.pushMentions
                else -> true
            }
            if (!allowed) return@launch

            // Previews disabled: show that something arrived, never the text.
            val shownBody = if (settings.showNotificationPreviews) {
                body
            } else {
                getString(R.string.notification_hidden_preview)
            }
            show(type, title, shownBody, deepLink, dedupeKey)
        }
    }

    private fun show(
        type: String,
        title: String,
        body: String,
        deepLink: String?,
        dedupeKey: String,
    ) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val channelId = when {
            type.contains("MESSAGE") -> InsangramConstants.CHANNEL_MESSAGES
            type.contains("FOLLOW") || type.contains("REQUEST") -> InsangramConstants.CHANNEL_FOLLOWS
            else -> InsangramConstants.CHANNEL_SOCIAL
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            deepLink?.let { data = android.net.Uri.parse(it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            dedupeKey.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_insangram)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Stable id from the dedupe key: a retried push replaces, never stacks.
        NotificationManagerCompat.from(this).notify(dedupeKey.hashCode(), notification)
    }
}
