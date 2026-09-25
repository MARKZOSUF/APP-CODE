package com.insangram.app.data.repository

import android.net.Uri
import androidx.paging.PagingData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.insangram.app.core.common.DispatcherProvider
import com.insangram.app.core.common.InsangramError
import com.insangram.app.core.common.InsangramResult
import com.insangram.app.core.common.Validators
import com.insangram.app.domain.model.Post
import com.insangram.app.domain.model.PostDraft
import com.insangram.app.domain.model.User
import com.insangram.app.domain.repository.PostRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Online posts: media is uploaded to Cloud Storage and the post document lives
 * in Firestore, so every device sees the same feed.
 *
 * Local-only concerns (drafts, upload queue, recently deleted cache) keep using
 * the on-device implementation through delegation, which also guarantees the
 * whole [PostRepository] contract stays satisfied.
 */
@Singleton
class FirebasePostRepository @Inject constructor(
    private val local: DemoPostRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val dispatchers: DispatcherProvider,
) : PostRepository by local {

    private val posts get() = firestore.collection(FirestorePaths.POSTS)

    private fun uid(): String? = auth.currentUser?.uid

    // --- reading ------------------------------------------------------------

    override fun homeFeed(chronological: Boolean): Flow<PagingData<Post>> =
        postsQuery(limit = 60).map { PagingData.from(it) }

    override fun explore(): Flow<PagingData<Post>> =
        postsQuery(limit = 90).map { list ->
            PagingData.from(list.sortedByDescending { it.likeCount + it.commentCount })
        }

    override fun profileGrid(userId: String): Flow<PagingData<Post>> =
        posts
            .whereEqualTo("authorId", userId)
            .whereEqualTo("archived", false)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(120)
            .snapshots()
            .map { docs -> PagingData.from(docs.mapNotNull { toPost(it, emptyMap()) }) }

    override fun observePost(postId: String): Flow<Post?> =
        posts.document(postId).snapshots().map { snapshot ->
            snapshot?.let { toPost(it, emptyMap()) }
        }

    /** Latest posts plus the author profiles they reference. */
    private fun postsQuery(limit: Long): Flow<List<Post>> {
        val documents = posts
            .whereEqualTo("archived", false)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .snapshots()

        val authors = firestore.collection(FirestorePaths.USERS)
            .limit(200)
            .snapshots()
            .map { docs -> docs.mapNotNull { it.toUserOrNull() }.associateBy { it.id } }

        return combine(documents, authors) { docs, authorsById ->
            docs.mapNotNull { toPost(it, authorsById) }
        }
    }

    private fun toPost(
        snapshot: com.google.firebase.firestore.DocumentSnapshot,
        authorsById: Map<String, User>,
    ): Post? {
        if (!snapshot.exists()) return null
        val authorId = snapshot.getString("authorId") ?: return null
        val viewerId = uid()
        val media = (snapshot.get("media") as? List<*>)
            .orEmpty()
            .mapNotNull { it as? Map<*, *> }
            .map { mediaFromMap(it) }
        val likedBy = snapshot.stringList("likedBy")
        val savedBy = snapshot.stringList("savedBy")

        return Post(
            id = snapshot.id,
            author = authorsById[authorId] ?: unknownUser(authorId),
            media = media,
            caption = snapshot.getString("caption").orEmpty(),
            hashtags = snapshot.stringList("hashtags"),
            mentionedUsernames = snapshot.stringList("mentions"),
            locationName = snapshot.getString("locationName"),
            likeCount = snapshot.getLong("likeCount") ?: likedBy.size.toLong(),
            commentCount = snapshot.getLong("commentCount") ?: 0L,
            saveCount = snapshot.getLong("saveCount") ?: 0L,
            shareCount = snapshot.getLong("shareCount") ?: 0L,
            viewCount = snapshot.getLong("viewCount") ?: 0L,
            likedByViewer = viewerId != null && likedBy.contains(viewerId),
            savedByViewer = viewerId != null && savedBy.contains(viewerId),
            commentsEnabled = snapshot.getBoolean("commentsEnabled") ?: true,
            isArchived = snapshot.getBoolean("archived") ?: false,
            createdAt = snapshot.getLong("createdAt") ?: 0L,
            editedAt = snapshot.getLong("editedAt"),
        )
    }

    override suspend fun refreshFeed(): InsangramResult<Unit> = InsangramResult.Success(Unit)

    // --- writing ------------------------------------------------------------

    override suspend fun createPost(draft: PostDraft): InsangramResult<String> =
        withContext(dispatchers.io) {
            val viewerId = uid()
                ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
            if (draft.mediaUris.isEmpty()) {
                return@withContext InsangramResult.Failure(
                    InsangramError.Validation("media", "Pick at least one photo or video"),
                )
            }

            firebaseResult {
                val document = posts.document()
                val uploaded = draft.mediaUris.mapIndexed { index, localUri ->
                    val isVideo = localUri.contains("video", ignoreCase = true) ||
                        localUri.endsWith(".mp4", ignoreCase = true)
                    val reference = storage.reference
                        .child("posts/$viewerId/${document.id}/$index")
                    reference.putFile(Uri.parse(localUri)).await()
                    val downloadUrl = reference.downloadUrl.await().toString()
                    mapOf(
                        "url" to downloadUrl,
                        "thumbnailUrl" to downloadUrl,
                        "type" to if (isVideo) "VIDEO" else "IMAGE",
                        "altText" to draft.altTexts.getOrElse(index) { "" },
                    )
                }

                val now = System.currentTimeMillis()
                document.set(
                    mapOf(
                        "authorId" to viewerId,
                        "caption" to draft.caption,
                        "hashtags" to Validators.extractHashtags(draft.caption),
                        "mentions" to Validators.extractMentions(draft.caption),
                        "locationName" to draft.locationName,
                        "media" to uploaded,
                        "commentsEnabled" to draft.commentsEnabled,
                        "archived" to false,
                        "likeCount" to 0L,
                        "commentCount" to 0L,
                        "saveCount" to 0L,
                        "shareCount" to 0L,
                        "viewCount" to 0L,
                        "likedBy" to emptyList<String>(),
                        "savedBy" to emptyList<String>(),
                        "createdAt" to now,
                    ),
                ).await()

                firestore.collection(FirestorePaths.USERS)
                    .document(viewerId)
                    .update("postCount", FieldValue.increment(1))
                    .await()

                document.id
            }
        }

    override suspend fun editPost(
        postId: String,
        caption: String,
        commentsEnabled: Boolean,
    ): InsangramResult<Unit> = withContext(dispatchers.io) {
        firebaseResult {
            posts.document(postId).update(
                mapOf(
                    "caption" to caption,
                    "hashtags" to Validators.extractHashtags(caption),
                    "mentions" to Validators.extractMentions(caption),
                    "commentsEnabled" to commentsEnabled,
                    "editedAt" to System.currentTimeMillis(),
                ),
            ).await()
            Unit
        }
    }

    override suspend fun deletePost(postId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val viewerId = uid()
                ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
            firebaseResult {
                posts.document(postId).delete().await()
                firestore.collection(FirestorePaths.USERS)
                    .document(viewerId)
                    .update("postCount", FieldValue.increment(-1))
                    .await()
                Unit
            }
        }

    override suspend fun setArchived(postId: String, archived: Boolean): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            firebaseResult {
                posts.document(postId).update("archived", archived).await()
                Unit
            }
        }

    override suspend fun toggleLike(postId: String): InsangramResult<Boolean> =
        withContext(dispatchers.io) {
            val viewerId = uid()
                ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
            firebaseResult {
                val document = posts.document(postId)
                val liked = document.get().await().stringList("likedBy").contains(viewerId)
                document.update(
                    mapOf(
                        "likedBy" to if (liked) {
                            FieldValue.arrayRemove(viewerId)
                        } else {
                            FieldValue.arrayUnion(viewerId)
                        },
                        "likeCount" to FieldValue.increment(if (liked) -1 else 1),
                    ),
                ).await()
                !liked
            }
        }

    override suspend fun toggleSave(
        postId: String,
        collectionId: String?,
    ): InsangramResult<Boolean> = withContext(dispatchers.io) {
        val viewerId = uid()
            ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
        firebaseResult {
            val document = posts.document(postId)
            val saved = document.get().await().stringList("savedBy").contains(viewerId)
            document.update(
                mapOf(
                    "savedBy" to if (saved) {
                        FieldValue.arrayRemove(viewerId)
                    } else {
                        FieldValue.arrayUnion(viewerId)
                    },
                    "saveCount" to FieldValue.increment(if (saved) -1 else 1),
                ),
            ).await()
            !saved
        }
    }

    override suspend fun registerShare(postId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            firebaseResult {
                posts.document(postId).update("shareCount", FieldValue.increment(1)).await()
                Unit
            }
        }

    override suspend fun registerView(postId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            firebaseResult {
                posts.document(postId).update("viewCount", FieldValue.increment(1)).await()
                Unit
            }
        }

    override suspend fun likedBy(postId: String, limit: Int): InsangramResult<List<User>> =
        withContext(dispatchers.io) {
            firebaseResult {
                val ids = posts.document(postId).get().await().stringList("likedBy").take(limit)
                ids.mapNotNull { id ->
                    firestore.collection(FirestorePaths.USERS)
                        .document(id)
                        .get()
                        .await()
                        .toUserOrNull()
                }
            }
        }

    override fun observeSavedPosts(collectionId: String?): Flow<List<Post>> {
        val viewerId = uid() ?: return flowOf(emptyList())
        return posts
            .whereArrayContains("savedBy", viewerId)
            .limit(120)
            .snapshots()
            .map { docs -> docs.mapNotNull { toPost(it, emptyMap()) } }
    }

    override fun observeArchived(): Flow<List<Post>> {
        val viewerId = uid() ?: return flowOf(emptyList())
        return posts
            .whereEqualTo("authorId", viewerId)
            .whereEqualTo("archived", true)
            .snapshots()
            .map { docs -> docs.mapNotNull { toPost(it, emptyMap()) } }
    }
}
