package com.insangram.app.data.repository

import com.insangram.app.core.common.DispatcherProvider
import com.insangram.app.core.common.InsangramError
import com.insangram.app.core.common.InsangramResult
import com.insangram.app.core.common.Validators
import com.insangram.app.core.database.InsangramDatabase
import com.insangram.app.core.database.entity.BlockEdge
import com.insangram.app.core.database.entity.CachedNotification
import com.insangram.app.core.database.entity.FollowEdge
import com.insangram.app.data.mapper.toDomain
import com.insangram.app.domain.model.FollowState
import com.insangram.app.domain.model.RelationshipSummary
import com.insangram.app.domain.model.User
import com.insangram.app.domain.repository.UserRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Room-backed profile and follow graph for the demo flavor.
 *
 * The follow graph is a single `follow_edge` table keyed by
 * (followerId, followeeId), which is what structurally prevents duplicate
 * follows and duplicate requests. Private accounts create a REQUESTED edge
 * instead of a FOLLOWING edge.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DemoUserRepository @Inject constructor(
    private val database: InsangramDatabase,
    private val session: DemoSession,
    private val dispatchers: DispatcherProvider,
) : UserRepository {

    private val userDao get() = database.userDao()
    private val activityDao get() = database.activityDao()

    override fun observeUser(userId: String): Flow<User?> =
        userDao.observeById(userId).map { it?.toDomain() }

    override fun observeUserByUsername(username: String): Flow<User?> =
        userDao.observeByUsername(Validators.normalizeUsername(username))
            .map { it?.toDomain() }

    override suspend fun refreshUser(userId: String): InsangramResult<User> =
        withContext(dispatchers.io) {
            // Demo mode is the source of truth locally; "refresh" simply re-reads.
            userDao.getById(userId)?.let { InsangramResult.Success(it.toDomain()) }
                ?: InsangramResult.Failure(InsangramError.NotFound("user"))
        }

    override suspend fun updateProfile(
        fullName: String,
        username: String,
        bio: String,
        website: String,
        pronouns: String,
        photoUri: String?,
    ): InsangramResult<User> = withContext(dispatchers.io) {
        val viewerId = session.requireUserId()
        val current = userDao.getById(viewerId)
            ?: return@withContext InsangramResult.Failure(InsangramError.NotFound("user"))

        Validators.validateProfile(fullName = fullName, username = username, website = website)
            ?.let { return@withContext InsangramResult.Failure(it) }

        val normalized = Validators.normalizeUsername(username)
        val clash = userDao.getByUsername(normalized)
        if (clash != null && clash.userId != viewerId) {
            return@withContext InsangramResult.Failure(
                InsangramError.Validation("username", "That username is already taken"),
            )
        }

        userDao.upsert(
            current.copy(
                username = username.trim(),
                normalizedUsername = normalized,
                fullName = fullName.trim(),
                bio = bio.trim(),
                website = website.trim(),
                pronouns = pronouns.trim(),
                photoUrl = photoUri ?: current.photoUrl,
                cachedAt = System.currentTimeMillis(),
            ),
        )
        InsangramResult.Success(userDao.getById(viewerId)!!.toDomain())
    }

    override fun observeRelationship(targetUserId: String): Flow<RelationshipSummary> =
        session.observeUserId().flatMapLatest { viewerId ->
            if (viewerId == null) {
                flow { emit(RelationshipSummary()) }
            } else if (viewerId == targetUserId) {
                flow { emit(RelationshipSummary(followState = FollowState.SELF)) }
            } else {
                combine(
                    userDao.observeFollow(viewerId, targetUserId),
                    userDao.observeFollow(targetUserId, viewerId),
                    userDao.observeBlockedIds(viewerId),
                ) { outgoing, incoming, blockedIds ->
                    val blocked = targetUserId in blockedIds
                    RelationshipSummary(
                        followState = when {
                            blocked -> FollowState.BLOCKED
                            outgoing?.state == "FOLLOWING" -> FollowState.FOLLOWING
                            outgoing?.state == "REQUESTED" -> FollowState.REQUESTED
                            else -> FollowState.NOT_FOLLOWING
                        },
                        followsViewer = incoming?.state == "FOLLOWING",
                        isCloseFriend = outgoing?.isCloseFriend == true,
                        postsMuted = outgoing?.mutedPosts == true,
                        storiesMuted = outgoing?.mutedStories == true,
                        isBlockedByViewer = blocked,
                        isRestricted = targetUserId in userDao.restrictedIds(viewerId),
                    )
                }
            }
        }

    override suspend fun follow(targetUserId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val viewerId = session.requireUserId()
            if (viewerId == targetUserId) {
                return@withContext InsangramResult.Failure(
                    InsangramError.Unsupported("You cannot follow your own account."),
                )
            }
            val target = userDao.getById(targetUserId)
                ?: return@withContext InsangramResult.Failure(InsangramError.NotFound("user"))
            if (targetUserId in userDao.mutuallyBlockedIds(viewerId)) {
                return@withContext InsangramResult.Failure(InsangramError.PermissionDenied)
            }

            val existing = userDao.getFollow(viewerId, targetUserId)
            if (existing != null) {
                // Already following or already requested: idempotent no-op.
                return@withContext InsangramResult.Success(Unit)
            }

            val now = System.currentTimeMillis()
            val requested = target.isPrivate
            userDao.upsertFollow(
                FollowEdge(
                    followerId = viewerId,
                    followeeId = targetUserId,
                    state = if (requested) "REQUESTED" else "FOLLOWING",
                    createdAt = now,
                ),
            )
            if (!requested) {
                userDao.incrementFollowerCount(targetUserId, 1)
                userDao.incrementFollowingCount(viewerId, 1)
            }
            notify(
                recipientId = targetUserId,
                actorId = viewerId,
                type = if (requested) "FOLLOW_REQUEST" else "NEW_FOLLOWER",
            )
            InsangramResult.Success(Unit)
        }

    override suspend fun unfollow(targetUserId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val viewerId = session.requireUserId()
            val existing = userDao.getFollow(viewerId, targetUserId)
                ?: return@withContext InsangramResult.Success(Unit)
            userDao.deleteFollow(viewerId, targetUserId)
            if (existing.state == "FOLLOWING") {
                userDao.incrementFollowerCount(targetUserId, -1)
                userDao.incrementFollowingCount(viewerId, -1)
            }
            InsangramResult.Success(Unit)
        }

    override suspend fun cancelFollowRequest(targetUserId: String): InsangramResult<Unit> =
        unfollow(targetUserId)

    override suspend fun approveFollowRequest(requesterId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val viewerId = session.requireUserId()
            val request = userDao.getFollow(requesterId, viewerId)
            if (request == null || request.state != "REQUESTED") {
                return@withContext InsangramResult.Failure(InsangramError.NotFound("request"))
            }
            userDao.approveRequest(requesterId, viewerId, System.currentTimeMillis())
            notify(recipientId = requesterId, actorId = viewerId, type = "REQUEST_ACCEPTED")
            InsangramResult.Success(Unit)
        }

    override suspend fun rejectFollowRequest(requesterId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val viewerId = session.requireUserId()
            userDao.deleteFollow(requesterId, viewerId)
            InsangramResult.Success(Unit)
        }

    override suspend fun removeFollower(followerId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val viewerId = session.requireUserId()
            val edge = userDao.getFollow(followerId, viewerId)
                ?: return@withContext InsangramResult.Success(Unit)
            userDao.deleteFollow(followerId, viewerId)
            if (edge.state == "FOLLOWING") {
                userDao.incrementFollowerCount(viewerId, -1)
                userDao.incrementFollowingCount(followerId, -1)
            }
            InsangramResult.Success(Unit)
        }

    override fun observeFollowRequests(): Flow<List<User>> =
        session.observeUserId().flatMapLatest { viewerId ->
            if (viewerId == null) {
                flow { emit(emptyList()) }
            } else {
                userDao.observeIncomingRequests(viewerId).map { edges ->
                    userDao.getByIds(edges.map { it.followerId }).map { it.toDomain() }
                }
            }
        }

    override fun observeFollowers(userId: String): Flow<List<User>> =
        userDao.observeById(userId).map {
            userDao.getByIds(userDao.followerIds(userId)).map { user -> user.toDomain() }
        }

    override fun observeFollowing(userId: String): Flow<List<User>> =
        userDao.observeFollowingIds(userId).map { ids ->
            userDao.getByIds(ids).map { it.toDomain() }
        }

    override fun observeCloseFriends(): Flow<List<User>> =
        session.observeUserId().flatMapLatest { viewerId ->
            if (viewerId == null) {
                flow { emit(emptyList()) }
            } else {
                userDao.observeFollowingIds(viewerId).map {
                    userDao.getByIds(userDao.closeFriendIds(viewerId)).map { it.toDomain() }
                }
            }
        }

    override suspend fun setCloseFriend(
        userId: String,
        isCloseFriend: Boolean,
    ): InsangramResult<Unit> = withContext(dispatchers.io) {
        val viewerId = session.requireUserId()
        userDao.setCloseFriend(viewerId, userId, isCloseFriend)
        InsangramResult.Success(Unit)
    }

    override suspend fun setMuted(
        userId: String,
        posts: Boolean,
        stories: Boolean,
    ): InsangramResult<Unit> = withContext(dispatchers.io) {
        val viewerId = session.requireUserId()
        userDao.setMuted(viewerId, userId, posts, stories)
        InsangramResult.Success(Unit)
    }

    override suspend fun block(userId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val viewerId = session.requireUserId()
            if (viewerId == userId) {
                return@withContext InsangramResult.Failure(
                    InsangramError.Unsupported("You cannot block your own account."),
                )
            }
            userDao.upsertBlock(
                BlockEdge(
                    blockerId = viewerId,
                    blockedId = userId,
                    restrictedOnly = false,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            // Blocking severs the follow relationship in both directions.
            unfollow(userId)
            removeFollower(userId)
            InsangramResult.Success(Unit)
        }

    override suspend fun unblock(userId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val viewerId = session.requireUserId()
            userDao.deleteBlock(viewerId, userId)
            InsangramResult.Success(Unit)
        }

    override suspend fun restrict(userId: String, restricted: Boolean): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val viewerId = session.requireUserId()
            if (restricted) {
                userDao.upsertBlock(
                    BlockEdge(
                        blockerId = viewerId,
                        blockedId = userId,
                        restrictedOnly = true,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            } else {
                userDao.deleteBlock(viewerId, userId)
            }
            InsangramResult.Success(Unit)
        }

    override fun observeBlockedUsers(): Flow<List<User>> =
        session.observeUserId().flatMapLatest { viewerId ->
            if (viewerId == null) {
                flow { emit(emptyList()) }
            } else {
                userDao.observeBlockedIds(viewerId).map { ids ->
                    userDao.getByIds(ids).map { it.toDomain() }
                }
            }
        }

    override fun observeMutedUsers(): Flow<List<User>> =
        session.observeUserId().flatMapLatest { viewerId ->
            if (viewerId == null) {
                flow { emit(emptyList()) }
            } else {
                userDao.observeFollowingIds(viewerId).map {
                    userDao.getByIds(userDao.mutedPostAuthorIds(viewerId)).map { u -> u.toDomain() }
                }
            }
        }

    override suspend fun suggestedUsers(limit: Int): InsangramResult<List<User>> =
        withContext(dispatchers.io) {
            val viewerId = session.requireUserId()
            val exclude = userDao.followingIds(viewerId).toSet() +
                userDao.blockedIds(viewerId).toSet() + viewerId
            val suggestions = userDao.suggested(limit + exclude.size)
                .filter { it.userId !in exclude }
                .take(limit)
                .map { it.toDomain() }
            InsangramResult.Success(suggestions)
        }

    private suspend fun notify(recipientId: String, actorId: String, type: String) {
        activityDao.upsertNotification(
            CachedNotification(
                notificationId = UUID.randomUUID().toString(),
                recipientId = recipientId,
                actorId = actorId,
                type = type,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }
}
