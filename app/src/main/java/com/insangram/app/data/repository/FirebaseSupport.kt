package com.insangram.app.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.insangram.app.core.common.InsangramError
import com.insangram.app.core.common.InsangramResult
import com.insangram.app.domain.model.MediaItem
import com.insangram.app.domain.model.MediaType
import com.insangram.app.domain.model.User
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Shared plumbing for the Firebase-backed repositories: coroutine bridging for
 * Play Services tasks, snapshot flows, document mapping and error mapping.
 *
 * Everything here is internal to the data layer - the UI only ever sees domain
 * models and [InsangramError].
 */

/** Firestore collection names, in one place so the security rules can match. */
internal object FirestorePaths {
    const val USERS = "users"
    const val USERNAMES = "usernames"
    const val POSTS = "posts"
    const val COMMENTS = "comments"
    const val LIKES = "likes"
    const val SAVES = "saves"
    const val FOLLOWS = "follows"
    const val STORIES = "stories"
    const val STORY_VIEWS = "views"
    const val CONVERSATIONS = "conversations"
    const val MESSAGES = "messages"
    const val NOTIFICATIONS = "notifications"
    const val DEVICE_TOKENS = "deviceTokens"
    const val REPORTS = "reports"
}

/** Bridges a Play Services [Task] to a coroutine without extra dependencies. */
internal suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        val error = task.exception
        when {
            // Resume with the failure instead of cancelling: cancelling would
            // kill the caller's coroutine, so the UI would stay on the loading
            // spinner forever instead of showing the real error.
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

/** Emits the query results again every time the server data changes. */
internal fun Query.snapshots(): Flow<List<DocumentSnapshot>> = callbackFlow {
    val registration = addSnapshotListener { snapshot, _ ->
        trySend(snapshot?.documents.orEmpty())
    }
    awaitClose { registration.remove() }
}

/** Emits the document again every time it changes. */
internal fun com.google.firebase.firestore.DocumentReference.snapshots(): Flow<DocumentSnapshot?> =
    callbackFlow {
        val registration = addSnapshotListener { snapshot, _ -> trySend(snapshot) }
        awaitClose { registration.remove() }
    }

/** Maps a Firebase/Firestore throwable onto the shared error taxonomy. */
internal fun mapThrowable(throwable: Throwable): InsangramError = when {
    throwable is java.io.IOException -> InsangramError.Offline(throwable)
    throwable.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ->
        InsangramError.PermissionDenied
    throwable.message?.contains("UNAVAILABLE", ignoreCase = true) == true ->
        InsangramError.Offline(throwable)
    throwable.message?.contains("network", ignoreCase = true) == true ->
        InsangramError.Offline(throwable)
    else -> InsangramError.RemoteFailure(throwable)
}

/** Runs [block], returning a [InsangramResult] instead of throwing. */
internal inline fun <T> firebaseResult(block: () -> T): InsangramResult<T> = try {
    InsangramResult.Success(block())
} catch (cancellation: kotlinx.coroutines.CancellationException) {
    InsangramResult.Failure(mapThrowable(cancellation.cause ?: cancellation))
} catch (throwable: Throwable) {
    InsangramResult.Failure(mapThrowable(throwable))
}

/** Reads a `users/{uid}` document into the domain [User]. */
internal fun DocumentSnapshot.toUserOrNull(): User? {
    if (!exists()) return null
    val username = getString("username") ?: return null
    return User(
        id = id,
        username = username,
        fullName = getString("fullName") ?: username,
        bio = getString("bio").orEmpty(),
        website = getString("website").orEmpty(),
        pronouns = getString("pronouns").orEmpty(),
        photoUrl = getString("photoUrl"),
        isPrivate = getBoolean("isPrivate") ?: false,
        postCount = getLong("postCount") ?: 0L,
        followerCount = getLong("followerCount") ?: 0L,
        followingCount = getLong("followingCount") ?: 0L,
        lastActiveAt = getLong("lastActiveAt") ?: 0L,
        showActivityStatus = getBoolean("showActivityStatus") ?: true,
    )
}

/** Placeholder used while an author document is still loading. */
internal fun unknownUser(id: String): User =
    User(id = id, username = "insangram", fullName = "Insangram user")

/** Reads a stored media map written by the upload path. */
internal fun mediaFromMap(raw: Map<*, *>): MediaItem = MediaItem(
    url = raw["url"] as? String ?: "",
    thumbnailUrl = raw["thumbnailUrl"] as? String,
    type = if ((raw["type"] as? String) == "VIDEO") MediaType.VIDEO else MediaType.IMAGE,
    widthPx = (raw["widthPx"] as? Number)?.toInt() ?: 0,
    heightPx = (raw["heightPx"] as? Number)?.toInt() ?: 0,
    durationMs = (raw["durationMs"] as? Number)?.toLong() ?: 0L,
    altText = raw["altText"] as? String ?: "",
)

internal fun MediaItem.toMap(): Map<String, Any?> = mapOf(
    "url" to url,
    "thumbnailUrl" to thumbnailUrl,
    "type" to type.name,
    "widthPx" to widthPx,
    "heightPx" to heightPx,
    "durationMs" to durationMs,
    "altText" to altText,
)

/** Firestore stores lists as `List<*>`; this narrows them safely. */
internal fun DocumentSnapshot.stringList(field: String): List<String> =
    (get(field) as? List<*>)?.mapNotNull { it as? String }.orEmpty()
