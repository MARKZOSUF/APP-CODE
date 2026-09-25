package com.insangram.app.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.insangram.app.core.common.InsangramResult
import com.insangram.app.core.database.dao.DraftDao
import com.insangram.app.domain.repository.CommentRepository
import com.insangram.app.domain.repository.MessageRepository
import com.insangram.app.domain.repository.NotificationRepository
import com.insangram.app.domain.repository.PostRepository
import com.insangram.app.domain.repository.StoryRepository
import com.insangram.app.domain.repository.UserRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Replays actions the user took while offline, in submission order.
 *
 * Each queued row carries an idempotency key; repositories treat a repeated key
 * as a no-op, which is what makes a WorkManager retry safe.
 */
@HiltWorker
class PendingActionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val draftDao: DraftDao,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val commentRepository: CommentRepository,
    private val messageRepository: MessageRepository,
    private val storyRepository: StoryRepository,
    private val notificationRepository: NotificationRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        var sawFailure = false

        while (true) {
            val action = draftDao.nextPendingAction() ?: break
            val payload = action.payload

            val result: InsangramResult<*> = when (action.type) {
                "LIKE", "UNLIKE" -> postRepository.toggleLike(payload["postId"].orEmpty())
                "SAVE", "UNSAVE" -> postRepository.toggleSave(
                    payload["postId"].orEmpty(),
                    payload["collectionId"],
                )

                "FOLLOW" -> userRepository.follow(payload["userId"].orEmpty())
                "UNFOLLOW" -> userRepository.unfollow(payload["userId"].orEmpty())
                "COMMENT" -> commentRepository.addComment(
                    payload["contentId"].orEmpty(),
                    payload["text"].orEmpty(),
                    payload["replyTo"],
                )

                "DELETE_COMMENT" -> commentRepository.deleteComment(
                    payload["commentId"].orEmpty(),
                    payload["contentId"].orEmpty(),
                )

                "SEND_MESSAGE" -> messageRepository.sendText(
                    payload["conversationId"].orEmpty(),
                    payload["text"].orEmpty(),
                    payload["replyTo"],
                )

                "VIEW_STORY" -> storyRepository.markSeen(payload["storyId"].orEmpty())
                "MARK_READ" -> notificationRepository.markRead(payload["notificationId"].orEmpty())
                else -> InsangramResult.Success(Unit)
            }

            when (result) {
                is InsangramResult.Success -> draftDao.deletePendingAction(action.actionId)
                is InsangramResult.Failure -> {
                    draftDao.markPendingActionFailed(
                        action.actionId,
                        result.error.technicalMessage,
                    )
                    sawFailure = true
                    // Give up on rows that have failed too many times so one bad
                    // action cannot block the whole queue forever.
                    if (action.attemptCount + 1 >= MAX_ATTEMPTS) {
                        draftDao.deletePendingAction(action.actionId)
                    }
                    break
                }
            }
        }

        return if (sawFailure && runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
    }
}
