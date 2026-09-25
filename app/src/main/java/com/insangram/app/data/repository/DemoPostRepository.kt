package com.insangram.app.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.insangram.app.core.common.DispatcherProvider
import com.insangram.app.core.common.InsangramConstants
import com.insangram.app.core.common.InsangramError
import com.insangram.app.core.common.InsangramResult
import com.insangram.app.core.database.InsangramDatabase
import com.insangram.app.core.database.entity.CachedNotification
import com.insangram.app.core.database.entity.CachedPost
import com.insangram.app.core.database.entity.CachedPostMedia
import com.insangram.app.core.database.entity.NegativeSignalEntity
import com.insangram.app.core.database.entity.SavedCollectionEntity
import com.insangram.app.core.database.entity.SavedPostEntity
import com.insangram.app.core.media.MediaProcessor
import com.insangram.app.data.mapper.toDomain
import com.insangram.app.data.mapper.toEntity
import com.insangram.app.domain.model.Post
import com.insangram.app.domain.model.PostDraft
import com.insangram.app.domain.model.SavedCollection
import com.insangram.app.domain.model.UploadTask
import com.insangram.app.domain.model.User
import com.insangram.app.domain.repository.PostRepository
import com.insangram.app.domain.usecase.FeedCandidate
import com.insangram.app.domain.usecase.FeedRankingUseCase
import com.insangram.app.domain.usecase.ViewerSignals
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Room-backed posts, likes, saves, collections, drafts and uploads.
 *
 * The feed is a Paging 3 stream over a Room PagingSource ordered by a
 * precomputed `feedScore` column. Scores are refreshed by [refreshFeed] using
 * the shared [FeedRankingUseCase] so demo mode and online mode rank content
 * with exactly the same transparent formula.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DemoPostRepository @Inject constructor(
    private val database: InsangramDatabase,
    private val assembler: PostAssembler,
    private val ranking: FeedRankingUseCase,
    private val mediaProcessor: MediaProcessor,
    private val session: DemoSession,
    private val dispatchers: DispatcherProvider,
) : PostRepository {

    private val postDao get() = database.postDao()
    private val userDao get() = database.userDao()
    private val activityDao get() = database.activityDao()
    private val draftDao get() = database.draftDao()

    private val pagingConfig = PagingConfig(
        pageSize = InsangramConstants.FEED_PAGE_SIZE,
        prefetchDistance = InsangramConstants.FEED_PAGE_SIZE / 2,
        enablePlaceholders = false,
    )

    override fun homeFeed(chronological: Boolean): Flow<PagingData<Post>> =
        session.observeUserId().flatMapLatest { viewerId ->
            if (viewerId == null) {
                flow { emit(PagingData.empty()) }
            } else {
                pagedPosts(viewerId) { feedViewerId, excludedIds ->
                    if (chronological) {
                        postDao.chronologicalFeedPagingSource(feedViewerId, excludedIds)
                    } else {
                        postDao.rankedFeedPagingSource(feedViewerId, excludedIds)
                    }
                }
            }
        }

    override fun explore(): Flow<PagingData<Post>> =
        session.observeUserId().flatMapLatest { viewerId ->
            if (viewerId == null) {
                flow { emit(PagingData.empty()) }
            } else {
                flow {
                    val excluded = hiddenAuthorIds(viewerId) + viewerId
                    emitAll(
                        Pager(pagingConfig) { postDao.explorePagingSource(excluded.toList()) }
                            .flow
                            .map { paging -> paging.mapToDomain(viewerId) },
                    )
                }
            }
        }

    override fun profileGrid(userId: String): Flow<PagingData<Post>> =
        session.observeUserId().flatMapLatest { viewerId ->
            if (viewerId == null) {
                flow { emit(PagingData.empty()) }
            } else {
                Pager(pagingConfig) { postDao.profileGridPagingSource(userId) }
                    .flow
                    .map { paging -> paging.mapToDomain(viewerId) }
            }
        }

    override fun observePost(postId: String): Flow<Post?> =
        session.observeUserId().flatMapLatest { viewerId ->
            postDao.observePost(postId).map { row ->
                if (row == null || viewerId == null) {
                    null
                } else {
                    assembler.posts(listOf(row), viewerId).firstOrNull()
                }
            }
        }

    /**
     * Recomputes feed scores for the candidate window.
     *
     * Demo mode has no server, so the counters in Room are the source of truth
     * here. In online mode the equivalent counters are maintained by Cloud
     * Functions and the client only ever reads them.
     */
    override suspend fun refreshFeed(): InsangramResult<Unit> = withContext(dispatchers.io) {
        val viewerId = session.userIdOrNull()
            ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
        val now = System.currentTimeMillis()

        val followingIds = userDao.followingIds(viewerId).toSet()
        val closeFriendIds = userDao.closeFriendIds(viewerId).toSet()
        val mutedIds = userDao.mutedPostAuthorIds(viewerId).toSet()
        val blockedIds = userDao.blockedIds(viewerId).toSet()
        val negativeIds = postDao.negativeSignalIds(viewerId).toSet()
        val interests = activityDao.allInterests(viewerId).associate { it.token to it.weight }

        val signals = ViewerSignals(
            followedAuthorIds = followingIds,
            closeFriendIds = closeFriendIds,
            mutedUserIds = mutedIds,
            blockedUserIds = blockedIds,
            hiddenContentIds = negativeIds,
            hashtagInterest = interests,
        )

        val candidates = postDao.rankingCandidates(InsangramConstants.RANKING_WINDOW)
        candidates.forEach { row ->
            val watched = database.reelDao().watchTime(viewerId, row.postId)?.watchedMs ?: 0L
            val score = ranking.score(
                FeedCandidate(
                    contentId = row.postId,
                    authorId = row.authorId,
                    likes = row.likeCount,
                    comments = row.commentCount,
                    saves = row.saveCount,
                    shares = row.shareCount,
                    views = row.viewCount,
                    hashtags = row.hashtags,
                    createdAt = row.createdAt,
                ),
                signals.copy(watchTimeMs = mapOf(row.postId to watched)),
                now,
            ).score
            postDao.setFeedScore(row.postId, score)
        }
        InsangramResult.Success(Unit)
    }

    override suspend fun createPost(draft: PostDraft): InsangramResult<String> =
        withContext(dispatchers.io) {
            val viewerId = session.requireUserId()
            if (draft.mediaUris.isEmpty()) {
                return@withContext InsangramResult.Failure(
                    InsangramError.Validation("media", "Add at least one photo or video"),
                )
            }

            val now = System.currentTimeMillis()
            val postId = "local-post-" + UUID.randomUUID().toString()
            val hashtags = com.insangram.app.core.common.Validators.extractHashtags(draft.caption)

            postDao.upsertPostWithMedia(
                CachedPost(
                    postId = postId,
                    authorId = viewerId,
                    caption = draft.caption,
                    locationName = draft.locationName,
                    hashtags = hashtags,
                    taggedUserIds = draft.taggedUserIds,
                    mentionedUsernames = com.insangram.app.core.common.Validators
                        .extractMentions(draft.caption),
                    commentsEnabled = draft.commentsEnabled,
                    createdAt = now,
                    feedScore = 0.0,
                    cachedAt = now,
                ),
                draft.mediaUris.mapIndexed { index, uri ->
                    CachedPostMedia(
                        postId = postId,
                        position = index,
                        mediaUrl = uri,
                        thumbnailUrl = uri,
                        mediaType = if (isVideoUri(uri)) "VIDEO" else "IMAGE",
                        altText = draft.altTexts.getOrElse(index) { "" },
                        localUri = uri,
                        mediaHash = mediaProcessor.mediaHash(android.net.Uri.parse(uri)),
                    )
                },
            )
            userDao.incrementPostCount(viewerId, 1)
            hashtags.forEach { activityDao.incrementHashtagCount(it, 1, now) }
            draft.id?.let { draftDao.deleteDraft(it) }

            // Mentioned users get a real notification row.
            com.insangram.app.core.common.Validators.extractMentions(draft.caption)
                .mapNotNull { userDao.getByUsername(it.lowercase()) }
                .forEach { mentioned ->
                    notify(mentioned.userId, viewerId, "MENTION", postId, draft.caption.take(60))
                }
            draft.taggedUserIds.forEach { tagged ->
                notify(tagged, viewerId, "TAG", postId, draft.caption.take(60))
            }

            refreshFeed()
            InsangramResult.Success(postId)
        }

    override suspend fun editPost(
        postId: String,
        caption: String,
        commentsEnabled: Boolean,
    ): InsangramResult<Unit> = withContext(dispatchers.io) {
        val viewerId = session.requireUserId()
        val post = postDao.getPost(postId)
            ?: return@withContext InsangramResult.Failure(InsangramError.NotFound("post"))
        if (post.authorId != viewerId) {
            return@withContext InsangramResult.Failure(InsangramError.PermissionDenied)
        }
        postDao.editPost(
            postId,
            caption,
            com.insangram.app.core.common.Validators.extractHashtags(caption),
            commentsEnabled,
            System.currentTimeMillis(),
        )
        InsangramResult.Success(Unit)
    }

    override suspend fun deletePost(postId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val viewerId = session.requireUserId()
            val post = postDao.getPost(postId)
                ?: return@withContext InsangramResult.Failure(InsangramError.NotFound("post"))
            if (post.authorId != viewerId) {
                return@withContext InsangramResult.Failure(InsangramError.PermissionDenied)
            }
            // Soft delete: the post moves to Recently deleted and is purged
            // after the retention window by MaintenanceWorker.
            postDao.setDeletedAt(postId, System.currentTimeMillis())
            userDao.incrementPostCount(viewerId, -1)
            InsangramResult.Success(Unit)
        }

    override suspend fun setArchived(postId: String, archived: Boolean): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val viewerId = session.requireUserId()
            val post = postDao.getPost(postId)
                ?: return@withContext InsangramResult.Failure(InsangramError.NotFound("post"))
            if (post.authorId != viewerId) {
                return@withContext InsangramResult.Failure(InsangramError.PermissionDenied)
            }
            postDao.setArchived(postId, archived)
            InsangramResult.Success(Unit)
        }

    override suspend fun restorePost(postId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val viewerId = session.requireUserId()
            val post = postDao.getPost(postId)
                ?: return@withContext InsangramResult.Failure(InsangramError.NotFound("post"))
            if (post.authorId != viewerId) {
                return@withContext InsangramResult.Failure(InsangramError.PermissionDenied)
            }
            postDao.setDeletedAt(postId, null)
            userDao.incrementPostCount(viewerId, 1)
            InsangramResult.Success(Unit)
        }

    override suspend fun permanentlyDelete(postId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val viewerId = session.requireUserId()
            val post = postDao.getPost(postId)
                ?: return@withContext InsangramResult.Success(Unit)
            if (post.authorId != viewerId) {
                return@withContext InsangramResult.Failure(InsangramError.PermissionDenied)
            }
            database.commentDao().deleteAllFor(postId)
            postDao.deleteMediaFor(postId)
            postDao.hardDelete(postId)
            InsangramResult.Success(Unit)
        }

    override fun observeArchived(): Flow<List<Post>> =
        session.observeUserId().flatMapLatest { viewerId ->
            if (viewerId == null) {
                flow { emit(emptyList()) }
            } else {
                postDao.observeArchived(viewerId).map { assembler.posts(it, viewerId) }
            }
        }

    override fun observeRecentlyDeleted(): Flow<List<Post>> =
        session.observeUserId().flatMapLatest { viewerId ->
            if (viewerId == null) {
                flow { emit(emptyList()) }
            } else {
                postDao.observeRecentlyDeleted(viewerId).map { assembler.posts(it, viewerId) }
            }
        }

    /** Returns the new like state so the caller can roll back an optimistic UI. */
    override suspend fun toggleLike(postId: String): InsangramResult<Boolean> =
        withContext(dispatchers.io) {
            val viewerId = session.requireUserId()
            val post = postDao.getPost(postId)
                ?: return@withContext InsangramResult.Failure(InsangramError.NotFound("post"))
            val nowLiked = postDao.toggleLike(viewerId, postId, System.currentTimeMillis())
            if (nowLiked && post.authorId != viewerId) {
                notify(post.authorId, viewerId, "POST_LIKE", postId, post.caption.take(60))
            }
            bumpInterest(viewerId, post.hashtags, post.authorId, if (nowLiked) 1.0 else -1.0)
            InsangramResult.Success(nowLiked)
        }

    override suspend fun toggleSave(
        postId: String,
        collectionId: String?,
    ): InsangramResult<Boolean> = withContext(dispatchers.io) {
        val viewerId = session.requireUserId()
        val alreadySaved = postDao.isSavedNow(viewerId, postId)
        if (alreadySaved) {
            postDao.deleteSaved(viewerId, postId)
            postDao.incrementSaveCount(postId, -1)
            InsangramResult.Success(false)
        } else {
            postDao.insertSaved(
                SavedPostEntity(
                    userId = viewerId,
                    postId = postId,
                    collectionId = collectionId,
                    savedAt = System.currentTimeMillis(),
                ),
            )
            postDao.incrementSaveCount(postId, 1)
            InsangramResult.Success(true)
        }
    }

    override suspend fun registerShare(postId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            postDao.incrementShareCount(postId)
            InsangramResult.Success(Unit)
        }

    override suspend fun registerView(postId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            postDao.incrementViewCount(postId)
            InsangramResult.Success(Unit)
        }

    override suspend fun likedBy(postId: String, limit: Int): InsangramResult<List<User>> =
        withContext(dispatchers.io) {
            val ids = postDao.likedByUserIds(postId, limit)
            InsangramResult.Success(userDao.getByIds(ids).map { it.toDomain() })
        }

    override suspend fun hidePost(postId: String): InsangramResult<Unit> =
        negativeSignal(postId, "HIDDEN")

    override suspend fun notInterested(postId: String): InsangramResult<Unit> =
        negativeSignal(postId, "NOT_INTERESTED")

    private suspend fun negativeSignal(postId: String, kind: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val viewerId = session.requireUserId()
            postDao.insertNegativeSignal(
                NegativeSignalEntity(
                    userId = viewerId,
                    contentId = postId,
                    kind = kind,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            postDao.getPost(postId)?.let { post ->
                bumpInterest(viewerId, post.hashtags, post.authorId, -1.5)
            }
            InsangramResult.Success(Unit)
        }

    override fun observeSavedPosts(collectionId: String?): Flow<List<Post>> =
        session.observeUserId().flatMapLatest { viewerId ->
            if (viewerId == null) {
                flow { emit(emptyList()) }
            } else {
                postDao.observeSavedPosts(viewerId, collectionId)
                    .map { assembler.posts(it, viewerId) }
            }
        }

    override fun observeCollections(): Flow<List<SavedCollection>> =
        session.observeUserId().flatMapLatest { viewerId ->
            if (viewerId == null) {
                flow { emit(emptyList()) }
            } else {
                postDao.observeCollections(viewerId).map { rows ->
                    rows.map { row ->
                        val cover = row.coverPostId?.let { postDao.mediaFor(it).firstOrNull()?.mediaUrl }
                        row.toDomain(
                            coverUrl = cover,
                            itemCount = postDao.collectionItemCount(viewerId, row.collectionId),
                        )
                    }
                }
            }
        }

    override suspend fun createCollection(name: String): InsangramResult<String> =
        withContext(dispatchers.io) {
            if (name.isBlank()) {
                return@withContext InsangramResult.Failure(
                    InsangramError.Validation("name", "Give the collection a name"),
                )
            }
            val viewerId = session.requireUserId()
            val id = "collection-" + UUID.randomUUID().toString()
            postDao.upsertCollection(
                SavedCollectionEntity(
                    collectionId = id,
                    ownerId = viewerId,
                    name = name.trim(),
                    createdAt = System.currentTimeMillis(),
                ),
            )
            InsangramResult.Success(id)
        }

    override suspend fun renameCollection(
        collectionId: String,
        name: String,
    ): InsangramResult<Unit> = withContext(dispatchers.io) {
        val viewerId = session.requireUserId()
        val existing = postDao.collection(collectionId)
            ?: return@withContext InsangramResult.Failure(InsangramError.NotFound("collection"))
        if (existing.ownerId != viewerId) {
            return@withContext InsangramResult.Failure(InsangramError.PermissionDenied)
        }
        postDao.upsertCollection(existing.copy(name = name.trim()))
        InsangramResult.Success(Unit)
    }

    override suspend fun deleteCollection(collectionId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val viewerId = session.requireUserId()
            val existing = postDao.collection(collectionId)
                ?: return@withContext InsangramResult.Success(Unit)
            if (existing.ownerId != viewerId) {
                return@withContext InsangramResult.Failure(InsangramError.PermissionDenied)
            }
            // Saved items survive; they simply return to "All saved".
            postDao.removeCollection(collectionId)
            InsangramResult.Success(Unit)
        }

    override suspend fun moveToCollection(
        postId: String,
        collectionId: String?,
    ): InsangramResult<Unit> = withContext(dispatchers.io) {
        val viewerId = session.requireUserId()
        postDao.moveToCollection(viewerId, postId, collectionId)
        InsangramResult.Success(Unit)
    }

    override fun observeDrafts(): Flow<List<PostDraft>> =
        session.observeUserId().flatMapLatest { viewerId ->
            if (viewerId == null) {
                flow { emit(emptyList()) }
            } else {
                draftDao.observeDrafts(viewerId).map { rows -> rows.map { it.toDomain() } }
            }
        }

    override suspend fun saveDraft(draft: PostDraft): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val viewerId = session.requireUserId()
            draftDao.upsertDraft(draft.toEntity(viewerId, System.currentTimeMillis()))
            InsangramResult.Success(Unit)
        }

    override suspend fun deleteDraft(draftId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            draftDao.deleteDraft(draftId)
            InsangramResult.Success(Unit)
        }

    override fun observeUploads(): Flow<List<UploadTask>> =
        session.observeUserId().flatMapLatest { viewerId ->
            if (viewerId == null) {
                flow { emit(emptyList()) }
            } else {
                draftDao.observeActiveUploads(viewerId).map { rows -> rows.map { it.toDomain() } }
            }
        }

    /**
     * Demo uploads copy local media into the app's own storage rather than
     * hitting the network, so the queued/running/succeeded state machine is
     * exercised for real without a backend.
     */
    override suspend fun retryUpload(uploadId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val upload = draftDao.upload(uploadId)
                ?: return@withContext InsangramResult.Failure(InsangramError.NotFound("upload"))
            if (upload.state == "SUCCEEDED") return@withContext InsangramResult.Success(Unit)

            val now = System.currentTimeMillis()
            draftDao.updateUploadState(uploadId, "RUNNING", 10, null, now)
            val copied = mediaProcessor.copyIntoAppStorage(upload.localUri)
                ?: run {
                    draftDao.updateUploadState(
                        uploadId,
                        "FAILED",
                        0,
                        "Source media could not be read",
                        now,
                    )
                    return@withContext InsangramResult.Failure(
                        InsangramError.MediaUnavailable,
                    )
                }
            draftDao.updateUploadState(uploadId, "SUCCEEDED", 100, null, now)
            draftDao.upsertUpload(upload.copy(remotePath = copied, updatedAt = now))
            InsangramResult.Success(Unit)
        }

    override suspend fun cancelUpload(uploadId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val now = System.currentTimeMillis()
            draftDao.updateUploadState(uploadId, "CANCELLED", 0, null, now)
            draftDao.deleteUpload(uploadId)
            InsangramResult.Success(Unit)
        }

    override suspend fun isDuplicateMedia(mediaHash: String): Boolean =
        withContext(dispatchers.io) {
            val viewerId = session.userIdOrNull() ?: return@withContext false
            draftDao.uploadByHash(viewerId, mediaHash) != null ||
                postDao.mediaHashExists(mediaHash)
        }

    // ---------------------------------------------------------------- helpers

    private fun pagedPosts(
        viewerId: String,
        source: suspend (String, List<String>) -> androidx.paging.PagingSource<Int, CachedPost>,
    ): Flow<PagingData<Post>> = flow {
        val excluded = (hiddenAuthorIds(viewerId) + hiddenContentIds(viewerId)).toList()
        emitAll(
            Pager(pagingConfig) {
                // Suspending lambda is resolved once per Pager, not per page.
                kotlinx.coroutines.runBlocking { source(viewerId, excluded) }
            }.flow.map { paging -> paging.mapToDomain(viewerId) },
        )
    }

    private suspend fun PagingData<CachedPost>.mapToDomain(viewerId: String): PagingData<Post> {
        // Assemble one row at a time here; batch assembly happens for the
        // non-paged surfaces where the full list is already in memory.
        return map { row -> assembler.posts(listOf(row), viewerId).first() }
    }

    /** Authors whose posts the viewer is allowed to see in the feed. */
    /** Demo mode has no server metadata, so the extension decides the type. */
    private fun isVideoUri(uri: String): Boolean {
        val path = uri.substringBefore('?').lowercase()
        return path.endsWith(".mp4") || path.endsWith(".mov") ||
            path.endsWith(".webm") || path.endsWith(".mkv") || path.endsWith(".3gp")
    }

    private suspend fun hiddenAuthorIds(viewerId: String): Set<String> =
        database.userDao().mutuallyBlockedIds(viewerId).toSet()

    private suspend fun hiddenContentIds(viewerId: String): List<String> =
        postDao.negativeSignalIds(viewerId)

    private suspend fun bumpInterest(
        viewerId: String,
        hashtags: List<String>,
        authorId: String,
        delta: Double,
    ) {
        val now = System.currentTimeMillis()
        (hashtags.map { "hashtag:$it" } + "creator:$authorId").forEach { token ->
            val existing = activityDao.interestSignal(viewerId, token)
            val weight = ((existing?.weight ?: 0.0) + delta).coerceIn(0.0, 25.0)
            activityDao.upsertInterestSignal(
                com.insangram.app.core.database.entity.InterestSignalEntity(
                    userId = viewerId,
                    token = token,
                    weight = weight,
                    updatedAt = now,
                ),
            )
        }
    }

    private suspend fun notify(
        recipientId: String,
        actorId: String,
        type: String,
        targetId: String?,
        preview: String,
    ) {
        if (recipientId == actorId) return
        activityDao.upsertNotification(
            CachedNotification(
                notificationId = UUID.randomUUID().toString(),
                recipientId = recipientId,
                actorId = actorId,
                type = type,
                targetId = targetId,
                previewText = preview,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }
}
