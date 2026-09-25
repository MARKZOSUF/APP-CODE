package com.insangram.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.insangram.app.core.common.DispatcherProvider
import com.insangram.app.core.common.InsangramError
import com.insangram.app.core.common.InsangramResult
import com.insangram.app.domain.model.AppNotification
import com.insangram.app.domain.model.NotificationType
import com.insangram.app.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Online activity feed plus FCM device-token registration.
 *
 * Notification documents are written by the app itself for follows, likes and
 * comments, and by Cloud Functions (or any server) for push delivery.
 */
@Singleton
class FirebaseNotificationRepository @Inject constructor(
    private val local: DemoNotificationRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val dispatchers: DispatcherProvider,
) : NotificationRepository by local {

    private val notifications get() = firestore.collection(FirestorePaths.NOTIFICATIONS)

    private fun uid(): String? = auth.currentUser?.uid

    override fun observeNotifications(): Flow<List<AppNotification>> {
        val viewerId = uid() ?: return flowOf(emptyList())
        val documents = notifications
            .whereEqualTo("recipientId", viewerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(120)
            .snapshots()

        val actors = firestore.collection(FirestorePaths.USERS).limit(200).snapshots()
            .map { docs -> docs.mapNotNull { it.toUserOrNull() }.associateBy { it.id } }

        return combine(documents, actors) { docs, actorsById ->
            docs.mapNotNull { snapshot ->
                val actorId = snapshot.getString("actorId") ?: return@mapNotNull null
                AppNotification(
                    id = snapshot.id,
                    type = runCatching {
                        NotificationType.valueOf(snapshot.getString("type") ?: "NEW_FOLLOWER")
                    }.getOrDefault(NotificationType.NEW_FOLLOWER),
                    actor = actorsById[actorId] ?: unknownUser(actorId),
                    targetId = snapshot.getString("targetId"),
                    previewText = snapshot.getString("previewText").orEmpty(),
                    previewImageUrl = snapshot.getString("previewImageUrl"),
                    isRead = snapshot.getBoolean("read") ?: false,
                    createdAt = snapshot.getLong("createdAt") ?: 0L,
                )
            }
        }
    }

    override fun observeUnreadCount(): Flow<Int> =
        observeNotifications().map { list -> list.count { !it.isRead } }

    override suspend fun markRead(notificationId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            firebaseResult {
                notifications.document(notificationId).update("read", true).await()
                Unit
            }
        }

    override suspend fun markAllRead(): InsangramResult<Unit> = withContext(dispatchers.io) {
        val viewerId = uid()
            ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
        firebaseResult {
            notifications
                .whereEqualTo("recipientId", viewerId)
                .whereEqualTo("read", false)
                .get()
                .await()
                .documents
                .forEach { it.reference.update("read", true).await() }
            Unit
        }
    }

    override suspend fun registerDeviceToken(token: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val viewerId = uid()
                ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
            firebaseResult {
                firestore.collection(FirestorePaths.USERS)
                    .document(viewerId)
                    .collection(FirestorePaths.DEVICE_TOKENS)
                    .document(token)
                    .set(mapOf("createdAt" to System.currentTimeMillis()))
                    .await()
                Unit
            }
        }

    override suspend fun unregisterDeviceToken(): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val viewerId = uid() ?: return@withContext InsangramResult.Success(Unit)
            firebaseResult {
                firestore.collection(FirestorePaths.USERS)
                    .document(viewerId)
                    .collection(FirestorePaths.DEVICE_TOKENS)
                    .get()
                    .await()
                    .documents
                    .forEach { it.reference.delete().await() }
                Unit
            }
        }
}
