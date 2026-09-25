package com.insangram.app.data.local

import com.insangram.app.core.common.DispatcherProvider
import com.insangram.app.core.database.InsangramDatabase
import com.insangram.app.core.database.entity.CachedUser
import com.insangram.app.core.database.entity.DemoAccount
import com.insangram.app.core.datastore.InsangramPreferences
import com.insangram.app.core.security.PasswordHasher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Populates the demo database on first launch.
 *
 * Seeding is idempotent and version-gated: [InsangramPreferences.demoSeededVersion]
 * records the schema version already written, so re-running this is a cheap
 * no-op rather than a duplicate insert. A [Mutex] guards against two callers
 * (splash screen and sign-in) seeding concurrently.
 *
 * Demo credentials are stored only as PBKDF2 hashes - the seed passwords below
 * are hashed on device and the plaintext is zeroed immediately after use.
 */
@Singleton
class DemoSeeder @Inject constructor(
    private val database: InsangramDatabase,
    private val preferences: InsangramPreferences,
    private val passwordHasher: PasswordHasher,
    private val dispatchers: DispatcherProvider,
) {

    private val mutex = Mutex()

    /** Seeds demo accounts if they are not already present. Safe to call often. */
    suspend fun ensureSeeded() {
        if (preferences.demoSeededVersion.first() >= SEED_VERSION) return
        withContext(dispatchers.io) {
            mutex.withLock {
                // Re-check inside the lock: another caller may have just seeded.
                if (preferences.demoSeededVersion.first() >= SEED_VERSION) return@withLock
                seedAccounts()
                preferences.setDemoSeededVersion(SEED_VERSION)
            }
        }
    }

    private suspend fun seedAccounts() {
        val userDao = database.userDao()
        val now = System.currentTimeMillis()
        val existing = userDao.allDemoAccounts().map { it.email }.toSet()

        DEMO_ACCOUNTS.forEachIndexed { index, seed ->
            if (seed.email in existing) return@forEachIndexed

            userDao.upsert(
                CachedUser(
                    userId = seed.userId,
                    username = seed.username,
                    normalizedUsername = seed.username,
                    fullName = seed.fullName,
                    bio = seed.bio,
                    photoUrl = "file:///android_asset/demo_media/avatar_%02d.png".format(index + 1),
                    createdAt = now,
                    lastActiveAt = now,
                    cachedAt = now,
                ),
            )

            val chars = seed.password.toCharArray()
            val hashed = passwordHasher.hash(chars)
            chars.fill('\u0000')

            userDao.upsertDemoAccount(
                DemoAccount(
                    userId = seed.userId,
                    email = seed.email,
                    passwordHash = hashed.hash,
                    passwordSalt = hashed.salt,
                    iterations = hashed.iterations,
                    emailVerified = true,
                    createdAt = now,
                ),
            )
        }
    }

    private data class SeedAccount(
        val userId: String,
        val username: String,
        val fullName: String,
        val email: String,
        val password: String,
        val bio: String,
    )

    private companion object {
        /** Bump this when the seed content changes so devices re-seed. */
        const val SEED_VERSION = 1

        val DEMO_ACCOUNTS = listOf(
            SeedAccount(
                userId = "demo-aarav",
                username = "aarav_dev",
                fullName = "Aarav Sharma",
                email = "aarav@insangram.example",
                password = "Demo@12345",
                bio = "Building things for Android.",
            ),
            SeedAccount(
                userId = "demo-meera",
                username = "meera.k",
                fullName = "Meera Kapoor",
                email = "meera@insangram.example",
                password = "Demo@12345",
                bio = "Photographer. Chasing light.",
            ),
            SeedAccount(
                userId = "demo-kabir",
                username = "kabir_r",
                fullName = "Kabir Rao",
                email = "kabir@insangram.example",
                password = "Demo@12345",
                bio = "Reels, food and travel.",
            ),
        )
    }
}
