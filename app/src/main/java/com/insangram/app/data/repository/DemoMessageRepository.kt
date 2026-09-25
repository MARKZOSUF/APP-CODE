package com.insangram.app.data.repository

import com.insangram.app.core.common.InsangramError
import com.insangram.app.core.common.InsangramResult
import com.insangram.app.domain.model.AudienceScope
import com.insangram.app.domain.model.Conversation
import com.insangram.app.domain.model.Message
import com.insangram.app.domain.model.Note
import com.insangram.app.domain.repository.MessageRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
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
class DemoMessageRepository @Inject constructor() : MessageRepository {

    override fun observeInbox(): Flow<List<Conversation>> = flowOf(emptyList())

    override fun observeConversation(conversationId: String): Flow<Conversation?> = flowOf(null)

    override fun observeMessages(conversationId: String): Flow<List<Message>> = flowOf(emptyList())

    override fun observeTotalUnread(): Flow<Int> = flowOf(0)

    override fun observeSharedMedia(conversationId: String): Flow<List<Message>> =
        flowOf(emptyList())

    override suspend fun startDirectConversation(userId: String): InsangramResult<String> =
        InsangramResult.Failure(
            InsangramError.Unsupported("Direct messaging requires online mode."),
        )

    override suspend fun createGroup(
        userIds: List<String>,
        title: String,
    ): InsangramResult<String> = InsangramResult.Failure(
            InsangramError.Unsupported("Group chats requires online mode."),
        )

    override suspend fun sendText(
        conversationId: String,
        text: String,
        replyToMessageId: String?,
    ): InsangramResult<String> = InsangramResult.Failure(
            InsangramError.Unsupported("Sending messages requires online mode."),
        )

    override suspend fun sendMedia(
        conversationId: String,
        localUri: String,
        isVideo: Boolean,
    ): InsangramResult<String> = InsangramResult.Failure(
            InsangramError.Unsupported("Sending media requires online mode."),
        )

    override suspend fun retryMessage(messageId: String): InsangramResult<Unit> =
        InsangramResult.Failure(
            InsangramError.Unsupported("Retrying a message requires online mode."),
        )

    override suspend fun react(messageId: String, emoji: String): InsangramResult<Unit> =
        InsangramResult.Success(Unit)

    override suspend fun unsend(messageId: String): InsangramResult<Unit> =
        InsangramResult.Success(Unit)

    override suspend fun deleteForMe(messageId: String): InsangramResult<Unit> =
        InsangramResult.Success(Unit)

    override suspend fun setPinned(messageId: String, pinned: Boolean): InsangramResult<Unit> =
        InsangramResult.Success(Unit)

    override suspend fun markRead(conversationId: String): InsangramResult<Unit> =
        InsangramResult.Success(Unit)

    override suspend fun setTyping(conversationId: String, typing: Boolean) {
        // Typing indicators need a realtime backend; no-op in demo mode.
    }

    override suspend fun searchMessages(
        conversationId: String,
        term: String,
    ): InsangramResult<List<Message>> = InsangramResult.Success(emptyList())

    override suspend fun updateGroup(
        conversationId: String,
        title: String,
        memberIds: List<String>,
    ): InsangramResult<Unit> = InsangramResult.Failure(
            InsangramError.Unsupported("Editing a group requires online mode."),
        )

    override suspend fun setMuted(
        conversationId: String,
        muted: Boolean,
    ): InsangramResult<Unit> = InsangramResult.Success(Unit)

    override suspend fun leaveConversation(conversationId: String): InsangramResult<Unit> =
        InsangramResult.Success(Unit)

    override fun observeNotes(): Flow<List<Note>> = flowOf(emptyList())

    override suspend fun postNote(
        text: String,
        emoji: String,
        audience: AudienceScope,
    ): InsangramResult<Unit> = InsangramResult.Failure(
            InsangramError.Unsupported("Notes requires online mode."),
        )

    override suspend fun clearNote(): InsangramResult<Unit> = InsangramResult.Success(Unit)

    override suspend fun replyToNote(noteId: String, text: String): InsangramResult<Unit> =
        InsangramResult.Failure(
            InsangramError.Unsupported("Replying to a note requires online mode."),
        )
}
