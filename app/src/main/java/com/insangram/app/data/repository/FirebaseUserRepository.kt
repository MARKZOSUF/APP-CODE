package com.insangram.app.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.insangram.app.core.common.DispatcherProvider
import com.insangram.app.core.common.InsangramError
import com.insangram.app.core.common.InsangramResult
import com.insangram.app.domain.model.FollowState
import com.insangram.app.domain.model.RelationshipSummary
import com.insangram.app.domain.model.User
import com.insangram.app.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Online profiles and the follow graph.
 *
 * A follow is one document in `follows` keyed `{follower}_{followee}` so both
 * directions can be queried with a single index, and the denormalised counters
 * on each profile keep the profile header cheap to render.
 */
@Singleton
class FirebaseUserRepository @Inject constructor(
    private val local: DemoUserRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val dispatchers: DispatcherProvider,
) : UserRepository by local {

    private val users get() = firestore.collection(FirestorePaths.USERS)
    private val follows get() = firestore.collection(FirestorePaths.FOLLOWS)

    private fun uid(): String? = auth.currentUser?.uid

    private fun edgeId(follower: String, followee: String) = follower + "_" + followee

    override fun observeUser(userId: String): Flow<User?> =
        users.document(userId).snapshots().map { it?.toUserOrNull() }

    override fun observeUserByUsername(username: String): Flow<User?> =
        users.whereEqualTo("username", username.lowercase()).limit(1).snapshots()
            .map { docs -> docs.firstOrNull()?.toUserOrNull() }

    override suspend fun refreshUser(userId: String): InsangramResult<User> =
        withContext(dispatchers.io) {
            firebaseResult {
                users.document(userId).get().await().toUserOrNull()
                    ?: throw IllegalStateException("Profile not found")
            }
        }

    override suspend fun updateProfile(
        fullName: String,
        username: String,
        bio: String,
        website: String,
        pronouns: String,
        photoUri: String?,
    ): InsangramResult<User> = withContext(dispatchers.io) {
        val viewerId = uid()
            ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
        firebaseResult {
            val photoUrl = photoUri?.takeIf { it.startsWith("content://") || it.startsWith("file://") }
                ?.let { local ->
                    val reference = storage.reference.child("avatars/$viewerId/profile.jpg")
                    reference.putFile(Uri.parse(local)).await()
                    reference.downloadUrl.await().toString()
                } ?: photoUri

            val updates = mutableMapOf<String, Any?>(
                "fullName" to fullName.trim(),
                "bio" to bio,
                "website" to website,
                "pronouns" to pronouns,
            )
            if (photoUrl != null) updates["photoUrl"] = photoUrl
            users.document(viewerId).update(updates).await()

            users.document(viewerId).get().await().toUserOrNull()
                ?: throw IllegalStateException("Profile not found")
        }
    }

    override fun observeRelationship(targetUserId: String): Flow<RelationshipSummary> {
        val viewerId = uid() ?: return flowOf(RelationshipSummary())
        if (viewerId == targetUserId) {
            return flowOf(RelationshipSummary(followState = FollowState.SELF))
        }
        return follows.document(edgeId(viewerId, targetUserId)).snapshots().map { snapshot ->
            val following = snapshot?.exists() == true
            RelationshipSummary(
                followState = if (following) FollowState.FOLLOWING else FollowState.NOT_FOLLOWING,
            )
        }
    }

    override suspend fun follow(targetUserId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val viewerId = uid()
                ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
            if (viewerId == targetUserId) return@withContext InsangramResult.Success(Unit)

            firebaseResult {
                firestore.runBatch { batch ->
                    batch.set(
                        follows.document(edgeId(viewerId, targetUserId)),
                        mapOf(
                            "followerId" to viewerId,
                            "followeeId" to targetUserId,
                            "createdAt" to System.currentTimeMillis(),
                        ),
                    )
                    batch.update(
                        users.document(targetUserId),
                        "followerCount",
                        FieldValue.increment(1),
                    )
                    batch.update(
                        users.document(viewerId),
                        "followingCount",
                        FieldValue.increment(1),
                    )
                }.await()

                // Let the other account know, so the activity tab is real.
                firestore.collection(FirestorePaths.NOTIFICATIONS).add(
                    mapOf(
                        "recipientId" to targetUserId,
                        "actorId" to viewerId,
                        "type" to "NEW_FOLLOWER",
                        "previewText" to "started following you",
                        "read" to false,
                        "createdAt" to System.currentTimeMillis(),
                    ),
                ).await()
                Unit
            }
        }

    override suspend fun unfollow(targetUserId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val viewerId = uid()
                ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
            firebaseResult {
                firestore.runBatch { batch ->
                    batch.delete(follows.document(edgeId(viewerId, targetUserId)))
                    batch.update(
                        users.document(targetUserId),
                        "followerCount",
                        FieldValue.increment(-1),
                    )
                    batch.update(
                        users.document(viewerId),
                        "followingCount",
                        FieldValue.increment(-1),
                    )
                }.await()
                Unit
            }
        }

    override fun observeFollowers(userId: String): Flow<List<User>> =
        follows.whereEqualTo("followeeId", userId).limit(200).snapshots()
            .map { docs -> docs.mapNotNull { it.getString("followerId") } }
            .map { ids -> loadUsers(ids) }

    override fun observeFollowing(userId: String): Flow<List<User>> =
        follows.whereEqualTo("followerId", userId).limit(200).snapshots()
            .map { docs -> docs.mapNotNull { it.getString("followeeId") } }
            .map { ids -> loadUsers(ids) }

    override suspend fun suggestedUsers(limit: Int): InsangramResult<List<User>> =
        withContext(dispatchers.io) {
            val viewerId = uid()
            firebaseResult {
                users.limit(limit.toLong() + 1).get().await().documents
                    .mapNotNull { it.toUserOrNull() }
                    .filter { it.id != viewerId }
                    .take(limit)
            }
        }

    private suspend fun loadUsers(ids: List<String>): List<User> = ids.mapNotNull { id ->
        runCatching { users.document(id).get().await().toUserOrNull() }.getOrNull()
    }
}
