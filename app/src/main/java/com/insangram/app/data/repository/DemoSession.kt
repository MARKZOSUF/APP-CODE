package com.insangram.app.data.repository

import com.insangram.app.core.datastore.InsangramPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Tracks who is signed in while running the demo flavor.
 *
 * Demo mode still has a real session: the signed-in id is persisted in
 * DataStore, and every repository resolves the viewer through this class
 * instead of hardcoding an id, so "log out and sign in as someone else"
 * genuinely changes what the whole app shows.
 */
@Singleton
class DemoSession @Inject constructor(
    private val preferences: InsangramPreferences,
) {
    suspend fun requireUserId(): String =
        preferences.demoUserId.first() ?: error("Demo session has no signed-in user")

    suspend fun userIdOrNull(): String? = preferences.demoUserId.first()

    fun observeUserId() = preferences.demoUserId
}
