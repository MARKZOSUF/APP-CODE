package com.insangram.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.insangram.app.core.common.DispatcherProvider
import com.insangram.app.core.common.InsangramError
import com.insangram.app.core.common.InsangramResult
import com.insangram.app.domain.model.Comment
import com.insangram.app.domain.model.CommentSort
import com.insangram.app.domain.model.User
import com.insangram.app.domain.repository.CommentRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Online comments: one flat `comments` collection keyed by `contentId`. */
@Singleton
class FirebaseCommentRepository @Inject constructor(
    private val local: DemoCommentRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val dispatchers: DispatcherProvider,
) : CommentRepository by local {

    private val comments get() = firestore.collection(FirestorePaths.COMMENTS)

    private fun uid(): String? = auth.currentUser?.uid

    override fun observeComments(contentId: String, sort: CommentSort): Flow<List<Comment>> =
        commentsFor(contentId, limit = 200).map { list ->
            when (sort) {
                CommentSort.NEWEST -> list.sortedByDescending { it.createdAt }
                CommentSort.TOP -> list.sortedByDescending { it.likeCount }
            }
        }

    override fun observePreview(contentId: String, limit: Int): Flow<List<Comment>> =
        commentsFor(contentId, limit.toLong())

    private fun commentsFor(contentId: String, limit: Long): Flow<List<Comment>> {
        val documents = comments
            .whereEqualTo("contentId", contentId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .snapshots()

        val authors = firestore.collection(FirestorePaths.USERS).limit(200).snapshots()
            .map { docs -> docs.mapNotNull { it.toUserOrNull() }.associateBy { it.id } }

        return combine(documents, authors) { docs, authorsById ->
            val viewerId = uid()
            docs.mapNotNull { snapshot ->
                val authorId = snapshot.getString("authorId") ?: return@mapNotNull null
                val likedBy = snapshot.stringList("likedBy")
                Comment(
                    id = snapshot.id,
                    contentId = contentId,
                    author = authorsById[authorId] ?: unknownUser(authorId),
                    text = snapshot.getString("text").orEmpty(),
                    replyToCommentId = snapshot.getString("replyToCommentId"),
                    likeCount = snapshot.getLong("likeCount") ?: likedBy.size.toLong(),
                    likedByViewer = viewerId != null && likedBy.contains(viewerId),
                    isPinned = snapshot.getBoolean("pinned") ?: false,
                    createdAt = snapshot.getLong("createdAt") ?: 0L,
                    editedAt = snapshot.getLong("editedAt"),
                )
            }
        }
    }

    override suspend fun addComment(
        contentId: String,
        text: String,
        replyToCommentId: String?,
    ): InsangramResult<String> = withContext(dispatchers.io) {
        val viewerId = uid()
            ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
        if (text.isBlank()) {
            return@withContext InsangramResult.Failure(
                InsangramError.Validation("comment", "Write something first"),
            )
        }

        firebaseResult {
            val document = comments.document()
            document.set(
                mapOf(
                    "contentId" to contentId,
                    "authorId" to viewerId,
                    "text" to text.trim(),
                    "replyToCommentId" to replyToCommentId,
                    "likeCount" to 0L,
                    "likedBy" to emptyList<String>(),
                    "pinned" to false,
                    "createdAt" to System.currentTimeMillis(),
                ),
            ).await()

            runCatching {
                firestore.collection(FirestorePaths.POSTS)
                    .document(contentId)
                    .update("commentCount", FieldValue.increment(1))
                    .await()
            }
            document.id
        }
    }

    override suspend fun editComment(commentId: String, text: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            firebaseResult {
                comments.document(commentId).update(
                    mapOf("text" to text.trim(), "editedAt" to System.currentTimeMillis()),
                ).await()
                Unit
            }
        }

    override suspend fun deleteComment(
        commentId: String,
        contentId: String,
    ): InsangramResult<Unit> = withContext(dispatchers.io) {
        firebaseResult {
            comments.document(commentId).delete().await()
            runCatching {
                firestore.collection(FirestorePaths.POSTS)
                    .document(contentId)
                    .update("commentCount", FieldValue.increment(-1))
                    .await()
            }
            Unit
        }
    }

    override suspend fun toggleCommentLike(commentId: String): InsangramResult<Boolean> =
        withContext(dispatchers.io) {
            val viewerId = uid()
                ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
            firebaseResult {
                val document = comments.document(commentId)
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

    override suspend fun setPinned(commentId: String, pinned: Boolean): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            firebaseResult {
                comments.document(commentId).update("pinned", pinned).await()
                Unit
            }
        }

    @Suppress("unused")
    private fun placeholder(user: User) = user
}
