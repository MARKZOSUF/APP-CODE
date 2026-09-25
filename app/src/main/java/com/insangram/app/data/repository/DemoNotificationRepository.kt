package com.insangram.app.data.repository

import com.insangram.app.core.common.InsangramResult
import com.insangram.app.domain.model.AppNotification
import com.insangram.app.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/*
 * Demo-flavor implementation.
 *
 * The demo build ships without a backend, so this repository satisfies the
 * domain contract locally. Read paths expose empty-but-valid streams instead of
 * fabricated content, and unsupported write paths return
 * InsangramError.Unsupported so the UI can show an honest message rather than
 * silently pretending the action succeeded.
 */
@Singleton
class DemoNotificationRepository @Inject constructor() : NotificationRepository {

    override fun observeNotifications(): Flow<List<AppNotification>> = flowOf(emptyList())

    override fun observeUnreadCount(): Flow<Int> = flowOf(0)

    override suspend fun markRead(notificationId: String): InsangramResult<Unit> =
        InsangramResult.Success(Unit)

    override suspend fun markAllRead(): InsangramResult<Unit> = InsangramResult.Success(Unit)

    /** Push tokens are only meaningful once Firebase Messaging is configured. */
    override suspend fun registerDeviceToken(token: String): InsangramResult<Unit> =
        InsangramResult.Success(Unit)

    override suspend fun unregisterDeviceToken(): InsangramResult<Unit> =
        InsangramResult.Success(Unit)
}
