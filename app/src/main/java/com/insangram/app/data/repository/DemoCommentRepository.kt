package com.insangram.app.data.repository

import com.insangram.app.core.common.InsangramError
import com.insangram.app.core.common.InsangramResult
import com.insangram.app.domain.model.Comment
import com.insangram.app.domain.model.CommentSort
import com.insangram.app.domain.repository.CommentRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/*
 * Demo-flavor implementation.
 *
 * The demo build ships without a backend, so this repository satisfies the
 * domain contract locally. Read paths expose empty-but-valid streams instead of
 * fabricated content, and unsupported write paths return
 * InsangramError.Unsupported so the UI can show an honest message rather than
 * silently pretending the action succeeded.
 */
@Singleton
class DemoCommentRepository @Inject constructor() : CommentRepository {

    private val likedCommentIds = MutableStateFlow<Set<String>>(emptySet())

    override fun observeComments(contentId: String, sort: CommentSort): Flow<List<Comment>> =
        flowOf(emptyList())

    override fun observePreview(contentId: String, limit: Int): Flow<List<Comment>> =
        flowOf(emptyList())

    override suspend fun addComment(
        contentId: String,
        text: String,
        replyToCommentId: String?,
    ): InsangramResult<String> = InsangramResult.Failure(
            InsangramError.Unsupported("Commenting requires online mode."),
        )

    override suspend fun editComment(commentId: String, text: String): InsangramResult<Unit> =
        InsangramResult.Failure(
            InsangramError.Unsupported("Editing a comment requires online mode."),
        )

    override suspend fun deleteComment(
        commentId: String,
        contentId: String,
    ): InsangramResult<Unit> = InsangramResult.Success(Unit)

    override suspend fun toggleCommentLike(commentId: String): InsangramResult<Boolean> {
        val nowLiked = commentId !in likedCommentIds.value
        likedCommentIds.value = if (nowLiked) {
            likedCommentIds.value + commentId
        } else {
            likedCommentIds.value - commentId
        }
        return InsangramResult.Success(nowLiked)
    }

    override suspend fun setPinned(commentId: String, pinned: Boolean): InsangramResult<Unit> =
        InsangramResult.Success(Unit)
}
