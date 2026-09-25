package com.insangram.app.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.insangram.app.core.common.DispatcherProvider
import com.insangram.app.core.common.InsangramError
import com.insangram.app.core.common.InsangramResult
import com.insangram.app.core.common.Validators
import com.insangram.app.domain.model.SessionState
import com.insangram.app.domain.model.User
import com.insangram.app.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Real authentication for the online flavor.
 *
 * Accounts live in Firebase Auth; the public profile document lives in
 * Firestore under `users/{uid}`, and `usernames/{username}` is a uniqueness
 * index so two people can never claim the same handle.
 *
 * Nothing here throws: every Firebase failure is mapped to an
 * [InsangramError] exactly like the demo implementation does, so the UI keeps
 * one single error surface.
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val dispatchers: DispatcherProvider,
) : AuthRepository {

    private companion object {
        const val USERS = "users"
        const val USERNAMES = "usernames"

        const val FIELD_USERNAME = "username"
        const val FIELD_FULL_NAME = "fullName"
        const val FIELD_BIO = "bio"
        const val FIELD_WEBSITE = "website"
        const val FIELD_PRONOUNS = "pronouns"
        const val FIELD_PHOTO_URL = "photoUrl"
        const val FIELD_IS_PRIVATE = "isPrivate"
        const val FIELD_POST_COUNT = "postCount"
        const val FIELD_FOLLOWER_COUNT = "followerCount"
        const val FIELD_FOLLOWING_COUNT = "followingCount"
        const val FIELD_LAST_ACTIVE_AT = "lastActiveAt"
        const val FIELD_SHOW_ACTIVITY = "showActivityStatus"
        const val FIELD_EMAIL = "email"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_DOB = "dateOfBirthMillis"
        const val FIELD_UID = "uid"
    }

    /**
     * Emits on every Firebase Auth state change, then keeps emitting whenever
     * the viewer's own profile document changes (avatar, counts, bio).
     */
    override val sessionState: Flow<SessionState> = callbackFlow {
        var profileRegistration: com.google.firebase.firestore.ListenerRegistration? = null

        val authListener = FirebaseAuth.AuthStateListener { instance ->
            profileRegistration?.remove()
            profileRegistration = null

            val firebaseUser = instance.currentUser
            if (firebaseUser == null) {
                trySend(SessionState.SignedOut)
                return@AuthStateListener
            }

            trySend(SessionState.Unknown)
            profileRegistration = firestore.collection(USERS)
                .document(firebaseUser.uid)
                .addSnapshotListener { snapshot, _ ->
                    val user = snapshot?.toUser(firebaseUser.uid)
                        ?: firebaseUser.toFallbackUser()
                    trySend(
                        SessionState.SignedIn(
                            user = user,
                            emailVerified = firebaseUser.isEmailVerified,
                        ),
                    )
                }
        }

        auth.addAuthStateListener(authListener)
        awaitClose {
            profileRegistration?.remove()
            auth.removeAuthStateListener(authListener)
        }
    }

    override fun currentUserId(): String? = auth.currentUser?.uid

    override suspend fun signIn(email: String, password: String): InsangramResult<User> =
        withContext(dispatchers.io) {
            val normalizedEmail = email.trim().lowercase()
            if (!Validators.isValidEmail(normalizedEmail)) {
                return@withContext InsangramResult.Failure(
                    InsangramError.Validation("email", "Enter a valid email address"),
                )
            }
            if (password.isEmpty()) {
                return@withContext InsangramResult.Failure(
                    InsangramError.Validation("password", "Enter your password"),
                )
            }

            runFirebase {
                val credential = auth
                    .signInWithEmailAndPassword(normalizedEmail, password)
                    .awaitResult()
                val firebaseUser = credential.user
                    ?: throw IllegalStateException("Sign-in returned no user")
                // Heals accounts whose profile document was never written
                // (for example when the rules were not deployed yet).
                runCatching { ensureProfileExists(firebaseUser) }
                touchLastActive(firebaseUser.uid)
                loadProfile(firebaseUser)
            }
        }

    override suspend fun register(
        fullName: String,
        username: String,
        email: String,
        password: String,
        dateOfBirthMillis: Long,
        profilePictureUri: String?,
    ): InsangramResult<User> = withContext(dispatchers.io) {
        val normalizedEmail = email.trim().lowercase()
        val normalizedUsername = Validators.normalizeUsername(username)

        if (!Validators.isValidEmail(normalizedEmail)) {
            return@withContext InsangramResult.Failure(
                InsangramError.Validation("email", "Enter a valid email address"),
            )
        }
        if (normalizedUsername.length < 3) {
            return@withContext InsangramResult.Failure(
                InsangramError.Validation("username", "Usernames need at least 3 characters"),
            )
        }
        if (fullName.isBlank()) {
            return@withContext InsangramResult.Failure(
                InsangramError.Validation("fullName", "Enter your name"),
            )
        }
        if (password.length < 8) {
            return@withContext InsangramResult.Failure(InsangramError.WeakPassword)
        }

        // Best-effort handle check before creating the account. This read is
        // done while still signed out, so if the security rules (or the
        // network) refuse it we simply continue: registration must never be
        // blocked by an optional pre-check.
        val usernameDoc = firestore.collection(USERNAMES).document(normalizedUsername)
        val alreadyTaken = runCatching { usernameDoc.get().awaitResult().exists() }
            .getOrDefault(false)
        if (alreadyTaken) {
            return@withContext InsangramResult.Failure(InsangramError.UsernameTaken)
        }

        runFirebase {
            val credential = auth
                .createUserWithEmailAndPassword(normalizedEmail, password)
                .awaitResult()
            val firebaseUser = credential.user
                ?: throw IllegalStateException("Registration returned no user")
            val uid = firebaseUser.uid
            val now = System.currentTimeMillis()

            val profile = mapOf(
                FIELD_UID to uid,
                FIELD_USERNAME to normalizedUsername,
                FIELD_FULL_NAME to fullName.trim(),
                FIELD_EMAIL to normalizedEmail,
                FIELD_BIO to "",
                FIELD_WEBSITE to "",
                FIELD_PRONOUNS to "",
                FIELD_PHOTO_URL to profilePictureUri,
                FIELD_IS_PRIVATE to false,
                FIELD_POST_COUNT to 0L,
                FIELD_FOLLOWER_COUNT to 0L,
                FIELD_FOLLOWING_COUNT to 0L,
                FIELD_SHOW_ACTIVITY to true,
                FIELD_LAST_ACTIVE_AT to now,
                FIELD_CREATED_AT to now,
                FIELD_DOB to dateOfBirthMillis,
            )

            // The Auth account already exists at this point, so the profile
            // write and the verification mail are best-effort: a missing
            // Firestore rule must not make sign-up look like it failed. The
            // profile is re-created lazily on the next sign-in if needed.
            runCatching {
                firestore.runBatch { batch ->
                    batch.set(firestore.collection(USERS).document(uid), profile)
                    batch.set(usernameDoc, mapOf(FIELD_UID to uid, FIELD_CREATED_AT to now))
                }.awaitResult()
            }

            runCatching { firebaseUser.sendEmailVerification().awaitResult() }

            User(
                id = uid,
                username = normalizedUsername,
                fullName = fullName.trim(),
                photoUrl = profilePictureUri,
                lastActiveAt = now,
            )
        }
    }

    override suspend fun sendPasswordReset(email: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val normalizedEmail = email.trim().lowercase()
            if (!Validators.isValidEmail(normalizedEmail)) {
                return@withContext InsangramResult.Failure(
                    InsangramError.Validation("email", "Enter a valid email address"),
                )
            }
            runFirebase {
                auth.sendPasswordResetEmail(normalizedEmail).awaitResult()
                Unit
            }
        }

    override suspend fun sendVerificationEmail(): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val firebaseUser = auth.currentUser
                ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
            runFirebase {
                firebaseUser.sendEmailVerification().awaitResult()
                Unit
            }
        }

    override suspend fun refreshVerificationStatus(): InsangramResult<Boolean> =
        withContext(dispatchers.io) {
            val firebaseUser = auth.currentUser
                ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
            runFirebase {
                firebaseUser.reload().awaitResult()
                auth.currentUser?.isEmailVerified ?: false
            }
        }

    override suspend fun reauthenticate(password: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val firebaseUser = auth.currentUser
                ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
            val email = firebaseUser.email
                ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
            runFirebase {
                firebaseUser
                    .reauthenticate(EmailAuthProvider.getCredential(email, password))
                    .awaitResult()
                Unit
            }
        }

    override suspend fun signOut(): InsangramResult<Unit> = withContext(dispatchers.io) {
        runFirebase {
            auth.signOut()
            Unit
        }
    }

    override suspend fun deleteAccount(password: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val firebaseUser = auth.currentUser
                ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
            val email = firebaseUser.email
                ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
            val uid = firebaseUser.uid

            runFirebase {
                firebaseUser
                    .reauthenticate(EmailAuthProvider.getCredential(email, password))
                    .awaitResult()

                val snapshot = firestore.collection(USERS).document(uid).get().awaitResult()
                val username = snapshot.getString(FIELD_USERNAME)

                firestore.runBatch { batch ->
                    batch.delete(firestore.collection(USERS).document(uid))
                    if (!username.isNullOrBlank()) {
                        batch.delete(firestore.collection(USERNAMES).document(username))
                    }
                }.awaitResult()

                firebaseUser.delete().awaitResult()
                Unit
            }
        }

    override suspend fun isUsernameAvailable(username: String): InsangramResult<Boolean> =
        withContext(dispatchers.io) {
            val normalized = Validators.normalizeUsername(username)
            if (normalized.length < 3) {
                return@withContext InsangramResult.Success(false)
            }
            runFirebase {
                !firestore.collection(USERNAMES)
                    .document(normalized)
                    .get()
                    .awaitResult()
                    .exists()
            }
        }

    /**
     * Firebase Auth keeps no local list of previously used accounts, so the
     * account switcher shows the signed-in account only.
     */
    override fun knownAccounts(): Flow<List<User>> =
        flowOf(listOfNotNull(auth.currentUser?.toFallbackUser()))

    override suspend fun signInWithGoogle(idToken: String): InsangramResult<User> =
        withContext(dispatchers.io) {
            runFirebase {
                val credential = auth
                    .signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                    .awaitResult()
                val firebaseUser = credential.user
                    ?: throw IllegalStateException("Google sign-in returned no user")
                ensureProfileExists(firebaseUser)
                loadProfile(firebaseUser)
            }
        }

    override fun isGoogleSignInAvailable(): Boolean = true

    // --- helpers -----------------------------------------------------------

    private suspend fun loadProfile(firebaseUser: FirebaseUser): User {
        val snapshot = firestore.collection(USERS)
            .document(firebaseUser.uid)
            .get()
            .awaitResult()
        return snapshot.toUser(firebaseUser.uid) ?: firebaseUser.toFallbackUser()
    }

    /** Google sign-in can create a brand new account; give it a profile. */
    private suspend fun ensureProfileExists(firebaseUser: FirebaseUser) {
        val document = firestore.collection(USERS).document(firebaseUser.uid)
        if (document.get().awaitResult().exists()) return

        val now = System.currentTimeMillis()
        val fallbackUsername = buildString {
            append(
                Validators.normalizeUsername(
                    firebaseUser.email?.substringBefore('@').orEmpty().ifBlank { "insangram" },
                ),
            )
            append(firebaseUser.uid.take(4))
        }

        document.set(
            mapOf(
                FIELD_UID to firebaseUser.uid,
                FIELD_USERNAME to fallbackUsername,
                FIELD_FULL_NAME to (firebaseUser.displayName ?: fallbackUsername),
                FIELD_EMAIL to firebaseUser.email.orEmpty(),
                FIELD_PHOTO_URL to firebaseUser.photoUrl?.toString(),
                FIELD_BIO to "",
                FIELD_WEBSITE to "",
                FIELD_PRONOUNS to "",
                FIELD_IS_PRIVATE to false,
                FIELD_POST_COUNT to 0L,
                FIELD_FOLLOWER_COUNT to 0L,
                FIELD_FOLLOWING_COUNT to 0L,
                FIELD_SHOW_ACTIVITY to true,
                FIELD_LAST_ACTIVE_AT to now,
                FIELD_CREATED_AT to now,
            ),
        ).awaitResult()

        firestore.collection(USERNAMES).document(fallbackUsername).set(
            mapOf(FIELD_UID to firebaseUser.uid, FIELD_CREATED_AT to now),
        ).awaitResult()
    }

    private suspend fun touchLastActive(uid: String) {
        runCatching {
            firestore.collection(USERS)
                .document(uid)
                .update(FIELD_LAST_ACTIVE_AT, System.currentTimeMillis())
                .awaitResult()
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toUser(uid: String): User? {
        if (!exists()) return null
        val username = getString(FIELD_USERNAME) ?: return null
        return User(
            id = uid,
            username = username,
            fullName = getString(FIELD_FULL_NAME) ?: username,
            bio = getString(FIELD_BIO).orEmpty(),
            website = getString(FIELD_WEBSITE).orEmpty(),
            pronouns = getString(FIELD_PRONOUNS).orEmpty(),
            photoUrl = getString(FIELD_PHOTO_URL),
            isPrivate = getBoolean(FIELD_IS_PRIVATE) ?: false,
            postCount = getLong(FIELD_POST_COUNT) ?: 0L,
            followerCount = getLong(FIELD_FOLLOWER_COUNT) ?: 0L,
            followingCount = getLong(FIELD_FOLLOWING_COUNT) ?: 0L,
            lastActiveAt = getLong(FIELD_LAST_ACTIVE_AT) ?: 0L,
            showActivityStatus = getBoolean(FIELD_SHOW_ACTIVITY) ?: true,
        )
    }

    /** Used while the profile document is still being fetched or created. */
    private fun FirebaseUser.toFallbackUser(): User {
        val fallbackName = displayName
            ?: email?.substringBefore('@')
            ?: "insangram"
        return User(
            id = uid,
            username = Validators.normalizeUsername(fallbackName),
            fullName = fallbackName,
            photoUrl = photoUrl?.toString(),
        )
    }

    /**
     * Bridges a Play Services [Task] to a coroutine without pulling in
     * kotlinx-coroutines-play-services.
     */
    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            val error = task.exception
            when {
                // Resume with the exception instead of cancelling the
                // coroutine. Cancelling used to kill the caller, so the sign-up
                // form stayed on the spinner and never showed the real reason.
                error != null -> continuation.resumeWithException(error)
                task.isCanceled -> continuation.resumeWithException(
                    IllegalStateException("Firebase task was cancelled"),
                )
                else ->
                    @Suppress("UNCHECKED_CAST")
                    continuation.resume(task.result as T)
            }
        }
    }

    /** Maps Firebase exceptions onto the shared error taxonomy. */
    private inline fun <T> runFirebase(block: () -> T): InsangramResult<T> = try {
        InsangramResult.Success(block())
    } catch (cancellation: kotlinx.coroutines.CancellationException) {
        val cause = cancellation.cause ?: cancellation
        InsangramResult.Failure(mapFirebaseError(cause))
    } catch (throwable: Throwable) {
        InsangramResult.Failure(mapFirebaseError(throwable))
    }

    private fun mapFirebaseError(throwable: Throwable): InsangramError = when (throwable) {
        is FirebaseAuthWeakPasswordException -> InsangramError.WeakPassword
        is FirebaseAuthUserCollisionException -> InsangramError.EmailAlreadyInUse
        is FirebaseAuthInvalidCredentialsException -> InsangramError.InvalidCredentials
        is FirebaseAuthInvalidUserException -> InsangramError.InvalidCredentials
        is java.io.IOException -> InsangramError.Offline(throwable)
        else -> when {
            throwable.message?.contains("network", ignoreCase = true) == true ->
                InsangramError.Offline(throwable)
            throwable.message?.contains("too many", ignoreCase = true) == true ->
                InsangramError.TooManyAttempts
            throwable.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ->
                InsangramError.PermissionDenied
            else -> InsangramError.RemoteFailure(throwable)
        }
    }
}
