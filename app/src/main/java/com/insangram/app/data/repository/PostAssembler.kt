package com.insangram.app.data.repository

import com.insangram.app.core.database.InsangramDatabase
import com.insangram.app.core.database.entity.CachedPost
import com.insangram.app.core.database.entity.CachedReel
import com.insangram.app.data.mapper.toDomain
import com.insangram.app.domain.model.Comment
import com.insangram.app.domain.model.FollowState
import com.insangram.app.domain.model.Post
import com.insangram.app.domain.model.Reel
import com.insangram.app.domain.model.RelationshipSummary
import com.insangram.app.domain.model.User
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns cached rows into fully populated domain models.
 *
 * Everything is fetched in batches keyed by id, which is what keeps the feed
 * free of N+1 lookups: one query for authors, one for media, one for tagged
 * users, regardless of page size.
 */
@Singleton
class PostAssembler @Inject constructor(
    private val database: InsangramDatabase,
) {
    suspend fun posts(rows: List<CachedPost>, viewerId: String): List<Post> {
        if (rows.isEmpty()) return emptyList()

        val postIds = rows.map { it.postId }
        val authorIds = rows.map { it.authorId }.distinct()
        val taggedIds = rows.flatMap { it.taggedUserIds }.distinct()

        val users = database.userDao()
            .getByIds((authorIds + taggedIds).distinct())
            .associate { it.userId to it.toDomain() }
        val mediaByPost = database.postDao().mediaForAll(postIds).groupBy { it.postId }
        val likedIds = database.postDao().likedContentIdsFor(viewerId, postIds).toSet()
        val savedIds = database.postDao().savedPostIdsFor(viewerId, postIds).toSet()
        val relationships = relationships(viewerId, authorIds)

        return rows.mapNotNull { row ->
            val author = users[row.authorId] ?: return@mapNotNull null
            row.toDomain(
                author = author,
                media = mediaByPost[row.postId].orEmpty().sortedBy { it.position },
                likedByViewer = row.postId in likedIds,
                savedByViewer = row.postId in savedIds,
                taggedUsers = row.taggedUserIds.mapNotNull { users[it] },
                commentPreview = commentPreview(row.postId, users),
                relationship = relationships[row.authorId] ?: RelationshipSummary(),
            )
        }
    }

    suspend fun reels(rows: List<CachedReel>, viewerId: String): List<Reel> {
        if (rows.isEmpty()) return emptyList()
        val authorIds = rows.map { it.authorId }.distinct()
        val users = database.userDao().getByIds(authorIds).associate { it.userId to it.toDomain() }
        val reelIds = rows.map { it.reelId }
        val likedIds = database.postDao().likedContentIdsFor(viewerId, reelIds).toSet()
        val savedIds = database.postDao().savedPostIdsFor(viewerId, reelIds).toSet()
        val relationships = relationships(viewerId, authorIds)

        return rows.mapNotNull { row ->
            val author = users[row.authorId] ?: return@mapNotNull null
            row.toDomain(
                author = author,
                likedByViewer = row.reelId in likedIds,
                savedByViewer = row.reelId in savedIds,
                relationship = relationships[row.authorId] ?: RelationshipSummary(),
            )
        }
    }

    /** Two comments per post is enough for the feed preview line. */
    private suspend fun commentPreview(
        postId: String,
        users: Map<String, User>,
    ): List<Comment> {
        val rows = database.commentDao().previewFor(postId, PREVIEW_LIMIT)
        if (rows.isEmpty()) return emptyList()
        val missing = rows.map { it.authorId }.filter { it !in users }.distinct()
        val extra = if (missing.isEmpty()) {
            emptyMap()
        } else {
            database.userDao().getByIds(missing).associate { it.userId to it.toDomain() }
        }
        return rows.mapNotNull { row ->
            val author = users[row.authorId] ?: extra[row.authorId] ?: return@mapNotNull null
            row.toDomain(author)
        }
    }

    private suspend fun relationships(
        viewerId: String,
        authorIds: List<String>,
    ): Map<String, RelationshipSummary> {
        val followingIds = database.userDao().followingIds(viewerId).toSet()
        val closeFriendIds = database.userDao().closeFriendIds(viewerId).toSet()
        val blockedIds = database.userDao().blockedIds(viewerId).toSet()
        return authorIds.associateWith { authorId ->
            RelationshipSummary(
                followState = when {
                    authorId == viewerId -> FollowState.SELF
                    authorId in blockedIds -> FollowState.BLOCKED
                    authorId in followingIds -> FollowState.FOLLOWING
                    else -> FollowState.NOT_FOLLOWING
                },
                isCloseFriend = authorId in closeFriendIds,
                isBlockedByViewer = authorId in blockedIds,
            )
        }
    }

    private companion object {
        const val PREVIEW_LIMIT = 2
    }
}
