package com.insangram.app.data.repository

import com.insangram.app.core.common.DispatcherProvider
import com.insangram.app.core.common.InsangramError
import com.insangram.app.core.common.InsangramResult
import com.insangram.app.core.common.Validators
import com.insangram.app.core.database.InsangramDatabase
import com.insangram.app.core.database.entity.CachedUser
import com.insangram.app.core.database.entity.DemoAccount
import com.insangram.app.core.datastore.InsangramPreferences
import com.insangram.app.core.security.HashedPassword
import com.insangram.app.core.security.PasswordHasher
import com.insangram.app.data.local.DemoSeeder
import com.insangram.app.data.mapper.toDomain
import com.insangram.app.domain.model.SessionState
import com.insangram.app.domain.model.User
import com.insangram.app.domain.repository.AuthRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Local authentication for the demo flavor.
 *
 * Passwords are verified against PBKDF2 hashes stored in the `demo_account`
 * table. The plaintext a user types is hashed, compared, then discarded - it is
 * never persisted and never logged.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DemoAuthRepository @Inject constructor(
    private val database: InsangramDatabase,
    private val preferences: InsangramPreferences,
    private val passwordHasher: PasswordHasher,
    private val seeder: DemoSeeder,
    private val session: DemoSession,
    private val dispatchers: DispatcherProvider,
) : AuthRepository {

    private val userDao get() = database.userDao()

    override val sessionState: Flow<SessionState> =
        preferences.demoUserId.flatMapLatest { userId ->
            if (userId == null) {
                flowOf(SessionState.SignedOut)
            } else {
                userDao.observeById(userId).map { cached ->
                    if (cached == null) {
                        SessionState.SignedOut
                    } else {
                        SessionState.SignedIn(cached.toDomain(), emailVerified = true)
                    }
                }
            }
        }

    /**
     * Demo mode reads the persisted id synchronously-ish; callers that need a
     * guaranteed value use the suspending session helper instead.
     */
    override fun currentUserId(): String? = cachedUserId

    @Volatile
    private var cachedUserId: String? = null

    /** Called by the splash screen before any session decision is made. */
    suspend fun prepare(): String = withContext(dispatchers.io) {
        seeder.ensureSeeded()
        cachedUserId = session.userIdOrNull()
        cachedUserId.orEmpty()
    }

    override suspend fun signIn(email: String, password: String): InsangramResult<User> =
        withContext(dispatchers.io) {
            seeder.ensureSeeded()
            val normalizedEmail = email.trim().lowercase()
            if (!Validators.isValidEmail(normalizedEmail)) {
                return@withContext InsangramResult.Failure(InsangramError.Validation("email", "Enter a valid email address"))
            }
            val account = userDao.demoAccountByEmail(normalizedEmail)
                ?: return@withContext InsangramResult.Failure(InsangramError.InvalidCredentials)

            val chars = password.toCharArray()
            val matches = passwordHasher.verify(
                password = chars,
                stored = HashedPassword(
                    hash = account.passwordHash,
                    salt = account.passwordSalt,
                    iterations = account.iterations,
                ),
            )
            chars.fill('\u0000')
            if (!matches) {
                return@withContext InsangramResult.Failure(InsangramError.InvalidCredentials)
            }

            val user = userDao.getById(account.userId)
                ?: return@withContext InsangramResult.Failure(InsangramError.NotFound("account"))
            preferences.setDemoUserId(user.userId)
            cachedUserId = user.userId
            userDao.touchLastActive(user.userId, System.currentTimeMillis())
            InsangramResult.Success(user.toDomain())
        }

    override suspend fun register(
        fullName: String,
        username: String,
        email: String,
        password: String,
        dateOfBirthMillis: Long,
        profilePictureUri: String?,
    ): InsangramResult<User> = withContext(dispatchers.io) {
        seeder.ensureSeeded()

        Validators.validateRegistration(
            fullName = fullName,
            username = username,
            email = email,
            password = password,
            dateOfBirthMillis = dateOfBirthMillis,
        )?.let { return@withContext InsangramResult.Failure(it) }

        val normalizedUsername = Validators.normalizeUsername(username)
        if (userDao.getByUsername(normalizedUsername) != null) {
            return@withContext InsangramResult.Failure(
                InsangramError.Validation("username", "That username is already taken"),
            )
        }
        val normalizedEmail = email.trim().lowercase()
        if (userDao.demoAccountByEmail(normalizedEmail) != null) {
            return@withContext InsangramResult.Failure(
                InsangramError.Validation("email", "An account already uses that email"),
            )
        }

        val now = System.currentTimeMillis()
        val userId = "local-" + UUID.randomUUID().toString()
        userDao.upsert(
            CachedUser(
                userId = userId,
                username = username.trim(),
                normalizedUsername = normalizedUsername,
                fullName = fullName.trim(),
                photoUrl = profilePictureUri,
                createdAt = now,
                lastActiveAt = now,
                cachedAt = now,
            ),
        )

        val chars = password.toCharArray()
        val hashed = passwordHasher.hash(chars)
        chars.fill('\u0000')
        userDao.upsertDemoAccount(
            DemoAccount(
                userId = userId,
                email = normalizedEmail,
                passwordHash = hashed.hash,
                passwordSalt = hashed.salt,
                iterations = hashed.iterations,
                emailVerified = true,
                createdAt = now,
            ),
        )

        preferences.setDemoUserId(userId)
        cachedUserId = userId
        InsangramResult.Success(userDao.getById(userId)!!.toDomain())
    }

    /**
     * Demo mode has no mail server. Rather than pretending an email was sent,
     * this reports the feature as unavailable offline so the UI can say so.
     */
    override suspend fun sendPasswordReset(email: String): InsangramResult<Unit> =
        InsangramResult.Failure(
            InsangramError.Unsupported(
                "Password reset emails require online mode with Firebase Authentication.",
            ),
        )

    override suspend fun sendVerificationEmail(): InsangramResult<Unit> =
        InsangramResult.Failure(
            InsangramError.Unsupported(
                "Email verification requires online mode with Firebase Authentication.",
            ),
        )

    /** Demo accounts are pre-verified, so this is always true. */
    override suspend fun refreshVerificationStatus(): InsangramResult<Boolean> =
        InsangramResult.Success(true)

    override suspend fun reauthenticate(password: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val userId = session.userIdOrNull()
                ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
            val account = userDao.allDemoAccounts().firstOrNull { it.userId == userId }
                ?: return@withContext InsangramResult.Failure(InsangramError.NotFound("account"))
            val chars = password.toCharArray()
            val matches = passwordHasher.verify(
                password = chars,
                stored = HashedPassword(
                    hash = account.passwordHash,
                    salt = account.passwordSalt,
                    iterations = account.iterations,
                ),
            )
            chars.fill('\u0000')
            if (matches) {
                InsangramResult.Success(Unit)
            } else {
                InsangramResult.Failure(InsangramError.InvalidCredentials)
            }
        }

    override suspend fun signOut(): InsangramResult<Unit> = withContext(dispatchers.io) {
        preferences.setDemoUserId(null)
        cachedUserId = null
        InsangramResult.Success(Unit)
    }

    override suspend fun deleteAccount(password: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            when (val reauth = reauthenticate(password)) {
                is InsangramResult.Failure -> return@withContext reauth
                is InsangramResult.Success -> Unit
            }
            val userId = session.requireUserId()
            // Only locally created accounts can be deleted; the bundled demo
            // personas are restored by the seeder and are shared fixtures.
            if (!userId.startsWith("local-")) {
                return@withContext InsangramResult.Failure(
                    InsangramError.Unsupported(
                        "Bundled demo personas cannot be deleted. Register a new account to try deletion.",
                    ),
                )
            }
            userDao.deleteDemoAccount(userId)
            preferences.setDemoUserId(null)
            cachedUserId = null
            InsangramResult.Success(Unit)
        }

    override suspend fun isUsernameAvailable(username: String): InsangramResult<Boolean> =
        withContext(dispatchers.io) {
            val normalized = Validators.normalizeUsername(username)
            if (normalized.isBlank()) {
                InsangramResult.Failure(
                    InsangramError.Validation("username", "Enter a username"),
                )
            } else {
                InsangramResult.Success(userDao.getByUsername(normalized) == null)
            }
        }

    /** Powers the account-selection screen: every seeded demo login. */
    override fun knownAccounts(): Flow<List<User>> =
        preferences.demoUserId.map {
            withContext(dispatchers.io) {
                val accounts = userDao.allDemoAccounts()
                userDao.getByIds(accounts.map { account -> account.userId })
                    .map { cached -> cached.toDomain() }
            }
        }

    override suspend fun signInWithGoogle(idToken: String): InsangramResult<User> =
        InsangramResult.Failure(
            InsangramError.Unsupported("Google Sign-In requires online mode."),
        )

    override fun isGoogleSignInAvailable(): Boolean = false
}
