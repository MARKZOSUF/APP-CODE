package com.insangram.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.insangram.app.core.database.entity.BlockEdge
import com.insangram.app.core.database.entity.CachedUser
import com.insangram.app.core.database.entity.DemoAccount
import com.insangram.app.core.database.entity.FollowEdge
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Upsert
    suspend fun upsert(users: List<CachedUser>)

    @Upsert
    suspend fun upsert(user: CachedUser)

    @Query("SELECT * FROM cached_user WHERE userId = :userId")
    fun observeById(userId: String): Flow<CachedUser?>

    @Query("SELECT * FROM cached_user WHERE userId = :userId")
    suspend fun getById(userId: String): CachedUser?

    @Query("SELECT * FROM cached_user WHERE normalizedUsername = :normalizedUsername")
    suspend fun getByUsername(normalizedUsername: String): CachedUser?

    @Query("SELECT * FROM cached_user WHERE normalizedUsername = :normalizedUsername")
    fun observeByUsername(normalizedUsername: String): Flow<CachedUser?>

    @Query("SELECT * FROM cached_user WHERE userId IN (:userIds)")
    suspend fun getByIds(userIds: List<String>): List<CachedUser>

    @Query("SELECT * FROM cached_user WHERE userId IN (:userIds)")
    fun observeByIds(userIds: List<String>): Flow<List<CachedUser>>

    /**
     * Prefix + substring search over the normalized username and full name.
     * Bounded by [limit] so ranking always runs on a small result set.
     */
    @Query(
        """
        SELECT * FROM cached_user
        WHERE normalizedUsername LIKE :prefix || '%'
           OR normalizedUsername LIKE '%' || :prefix || '%'
           OR LOWER(fullName) LIKE '%' || :prefix || '%'
        ORDER BY
            CASE WHEN normalizedUsername = :prefix THEN 0
                 WHEN normalizedUsername LIKE :prefix || '%' THEN 1
                 ELSE 2 END,
            followerCount DESC
        LIMIT :limit
        """,
    )
    suspend fun search(prefix: String, limit: Int): List<CachedUser>

    @Query("SELECT * FROM cached_user ORDER BY followerCount DESC LIMIT :limit")
    suspend fun suggested(limit: Int): List<CachedUser>

    @Query("UPDATE cached_user SET lastActiveAt = :timestamp WHERE userId = :userId")
    suspend fun touchLastActive(userId: String, timestamp: Long)

    @Query("DELETE FROM cached_user WHERE cachedAt < :threshold AND userId != :keepUserId")
    suspend fun evictStale(threshold: Long, keepUserId: String)

    // --- follow graph -------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFollow(edge: FollowEdge)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFollows(edges: List<FollowEdge>)

    @Query("DELETE FROM follow_edge WHERE followerId = :followerId AND followeeId = :followeeId")
    suspend fun deleteFollow(followerId: String, followeeId: String)

    @Query(
        "SELECT * FROM follow_edge WHERE followerId = :followerId AND followeeId = :followeeId",
    )
    fun observeFollow(followerId: String, followeeId: String): Flow<FollowEdge?>

    @Query(
        "SELECT * FROM follow_edge WHERE followerId = :followerId AND followeeId = :followeeId",
    )
    suspend fun getFollow(followerId: String, followeeId: String): FollowEdge?

    @Query("SELECT followeeId FROM follow_edge WHERE followerId = :userId AND state = 'FOLLOWING'")
    suspend fun followingIds(userId: String): List<String>

    @Query("SELECT followeeId FROM follow_edge WHERE followerId = :userId AND state = 'FOLLOWING'")
    fun observeFollowingIds(userId: String): Flow<List<String>>

    @Query("SELECT followerId FROM follow_edge WHERE followeeId = :userId AND state = 'FOLLOWING'")
    suspend fun followerIds(userId: String): List<String>

    @Query(
        "SELECT followeeId FROM follow_edge WHERE followerId = :userId AND isCloseFriend = 1",
    )
    suspend fun closeFriendIds(userId: String): List<String>

    @Query("SELECT * FROM follow_edge WHERE followeeId = :userId AND state = 'REQUESTED'")
    fun observeIncomingRequests(userId: String): Flow<List<FollowEdge>>

    @Query(
        """
        UPDATE follow_edge SET isCloseFriend = :isCloseFriend
        WHERE followerId = :followerId AND followeeId = :followeeId
        """,
    )
    suspend fun setCloseFriend(followerId: String, followeeId: String, isCloseFriend: Boolean)

    @Query(
        """
        UPDATE follow_edge SET mutedPosts = :mutedPosts, mutedStories = :mutedStories
        WHERE followerId = :followerId AND followeeId = :followeeId
        """,
    )
    suspend fun setMuted(
        followerId: String,
        followeeId: String,
        mutedPosts: Boolean,
        mutedStories: Boolean,
    )

    @Query("SELECT followeeId FROM follow_edge WHERE followerId = :userId AND mutedPosts = 1")
    suspend fun mutedPostAuthorIds(userId: String): List<String>

    /** Approving a request flips state and bumps both denormalised counters. */
    @Transaction
    suspend fun approveRequest(followerId: String, followeeId: String, now: Long) {
        val existing = getFollow(followerId, followeeId) ?: return
        upsertFollow(existing.copy(state = "FOLLOWING", createdAt = now))
        incrementFollowerCount(followeeId, 1)
        incrementFollowingCount(followerId, 1)
    }

    @Query("UPDATE cached_user SET followerCount = MAX(0, followerCount + :delta) WHERE userId = :userId")
    suspend fun incrementFollowerCount(userId: String, delta: Long)

    @Query("UPDATE cached_user SET followingCount = MAX(0, followingCount + :delta) WHERE userId = :userId")
    suspend fun incrementFollowingCount(userId: String, delta: Long)

    @Query("UPDATE cached_user SET postCount = MAX(0, postCount + :delta) WHERE userId = :userId")
    suspend fun incrementPostCount(userId: String, delta: Long)

    // --- blocking -----------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlock(edge: BlockEdge)

    @Query("DELETE FROM block_edge WHERE blockerId = :blockerId AND blockedId = :blockedId")
    suspend fun deleteBlock(blockerId: String, blockedId: String)

    @Query("SELECT blockedId FROM block_edge WHERE blockerId = :userId AND restrictedOnly = 0")
    suspend fun blockedIds(userId: String): List<String>

    @Query("SELECT blockedId FROM block_edge WHERE blockerId = :userId AND restrictedOnly = 0")
    fun observeBlockedIds(userId: String): Flow<List<String>>

    @Query("SELECT blockedId FROM block_edge WHERE blockerId = :userId AND restrictedOnly = 1")
    suspend fun restrictedIds(userId: String): List<String>

    /** Ids that must never appear in the viewer's feed, either direction. */
    @Query(
        """
        SELECT blockedId FROM block_edge WHERE blockerId = :userId
        UNION
        SELECT blockerId FROM block_edge WHERE blockedId = :userId
        """,
    )
    suspend fun mutuallyBlockedIds(userId: String): List<String>

    // --- demo accounts ------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDemoAccount(account: DemoAccount)

    @Query("SELECT * FROM demo_account WHERE email = :email")
    suspend fun demoAccountByEmail(email: String): DemoAccount?

    @Query("SELECT * FROM demo_account")
    suspend fun allDemoAccounts(): List<DemoAccount>

    @Query("DELETE FROM demo_account WHERE userId = :userId")
    suspend fun deleteDemoAccount(userId: String)

    @Query("SELECT COUNT(*) FROM cached_user")
    suspend fun userCount(): Int
}
