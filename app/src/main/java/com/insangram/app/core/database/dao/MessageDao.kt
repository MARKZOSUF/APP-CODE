package com.insangram.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.insangram.app.core.database.entity.CachedConversation
import com.insangram.app.core.database.entity.CachedMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Upsert
    suspend fun upsertConversations(
        conversations: List<CachedConversation>,
    )

    @Upsert
    suspend fun upsertConversation(
        conversation: CachedConversation,
    )

    /**
     * Inbox for the signed-in user.
     *
     * Membership is filtered in SQL so a conversation the viewer does not
     * belong to cannot be rendered, even if it reached the local cache.
     */
    @Query(
        """
        SELECT *
        FROM cached_conversation
        WHERE memberIds LIKE '%"' || :viewerId || '"%'
        ORDER BY lastMessageAt DESC
        """,
    )
    fun observeInbox(
        viewerId: String,
    ): Flow<List<CachedConversation>>

    @Query(
        """
        SELECT *
        FROM cached_conversation
        WHERE conversationId = :conversationId
        """,
    )
    fun observeConversation(
        conversationId: String,
    ): Flow<CachedConversation?>

    @Query(
        """
        SELECT *
        FROM cached_conversation
        WHERE conversationId = :conversationId
        """,
    )
    suspend fun getConversation(
        conversationId: String,
    ): CachedConversation?

    /**
     * Finds an existing one-to-one conversation so duplicates are not created.
     */
    @Query(
        """
        SELECT *
        FROM cached_conversation
        WHERE isGroup = 0
          AND memberIds LIKE '%"' || :userA || '"%'
          AND memberIds LIKE '%"' || :userB || '"%'
        LIMIT 1
        """,
    )
    suspend fun findDirectConversation(
        userA: String,
        userB: String,
    ): CachedConversation?

    /*
     * Triple-quoted SQL is required here because the LIKE pattern contains
     * double-quote characters used to match an exact ID in the stored JSON.
     *
     * COALESCE returns 0 instead of NULL when no conversations are present.
     */
    @Query(
        """
        SELECT COALESCE(SUM(unreadCount), 0)
        FROM cached_conversation
        WHERE memberIds LIKE '%"' || :viewerId || '"%'
        """,
    )
    fun observeTotalUnread(
        viewerId: String,
    ): Flow<Int>

    @Query(
        """
        UPDATE cached_conversation
        SET unreadCount = 0
        WHERE conversationId = :conversationId
        """,
    )
    suspend fun clearUnread(
        conversationId: String,
    )

    @Query(
        """
        UPDATE cached_conversation
        SET unreadCount = unreadCount + 1
        WHERE conversationId = :conversationId
        """,
    )
    suspend fun incrementUnread(
        conversationId: String,
    )

    @Query(
        """
        UPDATE cached_conversation
        SET memberIds = :memberIds,
            adminIds = :adminIds,
            title = :title,
            imageUrl = :imageUrl
        WHERE conversationId = :conversationId
        """,
    )
    suspend fun updateGroup(
        conversationId: String,
        memberIds: List<String>,
        adminIds: List<String>,
        title: String,
        imageUrl: String?,
    )

    @Query(
        """
        UPDATE cached_conversation
        SET mutedUntil = :until
        WHERE conversationId = :conversationId
        """,
    )
    suspend fun setMuted(
        conversationId: String,
        until: Long?,
    )

    @Query(
        """
        DELETE FROM cached_conversation
        WHERE conversationId = :conversationId
        """,
    )
    suspend fun deleteConversation(
        conversationId: String,
    )

    // ------------------------------------------------------------------------
    // Messages
    // ------------------------------------------------------------------------

    @Upsert
    suspend fun upsertMessages(
        messages: List<CachedMessage>,
    )

    @Upsert
    suspend fun upsertMessage(
        message: CachedMessage,
    )

    @Query(
        """
        SELECT *
        FROM cached_message
        WHERE conversationId = :conversationId
          AND hiddenForMe = 0
        ORDER BY createdAt DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun messagePage(
        conversationId: String,
        limit: Int,
        offset: Int,
    ): List<CachedMessage>

    @Query(
        """
        SELECT *
        FROM cached_message
        WHERE conversationId = :conversationId
          AND hiddenForMe = 0
        ORDER BY createdAt ASC
        """,
    )
    fun observeMessages(
        conversationId: String,
    ): Flow<List<CachedMessage>>

    @Query(
        """
        SELECT *
        FROM cached_message
        WHERE messageId = :messageId
        """,
    )
    suspend fun getMessage(
        messageId: String,
    ): CachedMessage?

    @Query(
        """
        SELECT *
        FROM cached_message
        WHERE idempotencyKey = :key
        LIMIT 1
        """,
    )
    suspend fun messageByIdempotencyKey(
        key: String,
    ): CachedMessage?

    @Query(
        """
        SELECT *
        FROM cached_message
        WHERE conversationId = :conversationId
          AND mediaUrl IS NOT NULL
        ORDER BY createdAt DESC
        """,
    )
    fun observeSharedMedia(
        conversationId: String,
    ): Flow<List<CachedMessage>>

    @Query(
        """
        SELECT *
        FROM cached_message
        WHERE conversationId = :conversationId
          AND LOWER(text) LIKE '%' || LOWER(:term) || '%'
        ORDER BY createdAt DESC
        LIMIT :limit
        """,
    )
    suspend fun searchMessages(
        conversationId: String,
        term: String,
        limit: Int,
    ): List<CachedMessage>

    @Query(
        """
        UPDATE cached_message
        SET deliveryState = :state
        WHERE messageId = :messageId
        """,
    )
    suspend fun setDeliveryState(
        messageId: String,
        state: String,
    )

    @Query(
        """
        UPDATE cached_message
        SET readByIds = :readByIds,
            deliveryState = 'READ'
        WHERE messageId = :messageId
        """,
    )
    suspend fun setReadBy(
        messageId: String,
        readByIds: List<String>,
    )

    @Query(
        """
        UPDATE cached_message
        SET reactions = :reactions
        WHERE messageId = :messageId
        """,
    )
    suspend fun setReactions(
        messageId: String,
        reactions: Map<String, String>,
    )

    @Query(
        """
        UPDATE cached_message
        SET isPinned = :pinned
        WHERE messageId = :messageId
        """,
    )
    suspend fun setPinned(
        messageId: String,
        pinned: Boolean,
    )

    /**
     * Unsend removes message content for everyone but keeps a tombstone row.
     */
    @Query(
        """
        UPDATE cached_message
        SET isUnsent = 1,
            text = '',
            mediaUrl = NULL,
            localMediaUri = NULL
        WHERE messageId = :messageId
        """,
    )
    suspend fun unsend(
        messageId: String,
    )

    @Query(
        """
        UPDATE cached_message
        SET hiddenForMe = 1
        WHERE messageId = :messageId
        """,
    )
    suspend fun hideForMe(
        messageId: String,
    )

    @Query(
        """
        SELECT *
        FROM cached_message
        WHERE deliveryState = 'FAILED'
        """,
    )
    suspend fun failedMessages(): List<CachedMessage>

    /**
     * Appends a message and updates the conversation preview atomically.
     */
    @Transaction
    suspend fun appendMessage(
        message: CachedMessage,
        previewText: String,
    ) {
        upsertMessage(message)

        val conversation =
            getConversation(message.conversationId) ?: return

        upsertConversation(
            conversation.copy(
                lastMessagePreview = previewText,
                lastMessageSenderId = message.senderId,
                lastMessageAt = message.createdAt,
            ),
        )
    }

    @Query(
        """
        DELETE FROM cached_message
        WHERE conversationId = :conversationId
        """,
    )
    suspend fun deleteMessagesFor(
        conversationId: String,
    )
}