package com.insangram.app.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.insangram.app.core.common.DispatcherProvider
import com.insangram.app.core.common.InsangramError
import com.insangram.app.core.common.InsangramResult
import com.insangram.app.domain.model.Conversation
import com.insangram.app.domain.model.DeliveryState
import com.insangram.app.domain.model.MediaType
import com.insangram.app.domain.model.Message
import com.insangram.app.domain.repository.MessageRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Real-time direct messages.
 *
 * `conversations/{id}` holds the membership and inbox preview; each message is
 * a document in `conversations/{id}/messages`. Both are read through snapshot
 * listeners, so chats update live on every device.
 */
@Singleton
class FirebaseMessageRepository @Inject constructor(
    private val local: DemoMessageRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val dispatchers: DispatcherProvider,
) : MessageRepository by local {

    private val conversations get() = firestore.collection(FirestorePaths.CONVERSATIONS)

    private fun uid(): String? = auth.currentUser?.uid

    private fun usersFlow() = firestore.collection(FirestorePaths.USERS).limit(200).snapshots()
        .map { docs -> docs.mapNotNull { it.toUserOrNull() }.associateBy { it.id } }

    override fun observeInbox(): Flow<List<Conversation>> {
        val viewerId = uid() ?: return flowOf(emptyList())
        val documents = conversations
            .whereArrayContains("memberIds", viewerId)
            .limit(100)
            .snapshots()

        return combine(documents, usersFlow()) { docs, usersById ->
            docs.mapNotNull { snapshot ->
                val memberIds = snapshot.stringList("memberIds")
                val isGroup = snapshot.getBoolean("isGroup") ?: false
                val other = memberIds.firstOrNull { it != viewerId }
                val unread = (snapshot.get("unread") as? Map<*, *>)
                    ?.get(viewerId) as? Number
                Conversation(
                    id = snapshot.id,
                    isGroup = isGroup,
                    title = if (isGroup) {
                        snapshot.getString("title").orEmpty()
                    } else {
                        usersById[other]?.username ?: snapshot.getString("title").orEmpty()
                    },
                    imageUrl = if (isGroup) null else usersById[other]?.photoUrl,
                    members = memberIds.mapNotNull { usersById[it] },
                    lastMessagePreview = snapshot.getString("lastMessagePreview").orEmpty(),
                    lastMessageAt = snapshot.getLong("lastMessageAt") ?: 0L,
                    unreadCount = unread?.toInt() ?: 0,
                    isMuted = snapshot.stringList("mutedBy").contains(viewerId),
                    typingUserIds = snapshot.stringList("typing").filter { it != viewerId },
                )
            }.sortedByDescending { it.lastMessageAt }
        }
    }

    override fun observeConversation(conversationId: String): Flow<Conversation?> =
        observeInbox().map { list -> list.firstOrNull { it.id == conversationId } }

    override fun observeTotalUnread(): Flow<Int> =
        observeInbox().map { list -> list.sumOf { it.unreadCount } }

    override fun observeMessages(conversationId: String): Flow<List<Message>> {
        val documents = conversations.document(conversationId)
            .collection(FirestorePaths.MESSAGES)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(300)
            .snapshots()

        return combine(documents, usersFlow()) { docs, usersById ->
            docs.mapNotNull { snapshot ->
                val senderId = snapshot.getString("senderId") ?: return@mapNotNull null
                val mediaType = snapshot.getString("mediaType")
                Message(
                    id = snapshot.id,
                    conversationId = conversationId,
                    sender = usersById[senderId] ?: unknownUser(senderId),
                    text = snapshot.getString("text").orEmpty(),
                    mediaUrl = snapshot.getString("mediaUrl"),
                    mediaType = when (mediaType) {
                        "VIDEO" -> MediaType.VIDEO
                        "IMAGE" -> MediaType.IMAGE
                        else -> null
                    },
                    replyToMessageId = snapshot.getString("replyToMessageId"),
                    replyToPreview = snapshot.getString("replyToPreview"),
                    reactions = (snapshot.get("reactions") as? Map<*, *>)
                        .orEmpty()
                        .mapNotNull { (key, value) ->
                            val k = key as? String ?: return@mapNotNull null
                            val v = value as? String ?: return@mapNotNull null
                            k to v
                        }
                        .toMap(),
                    readByIds = snapshot.stringList("readBy"),
                    deliveryState = DeliveryState.SENT,
                    isUnsent = snapshot.getBoolean("unsent") ?: false,
                    isPinned = snapshot.getBoolean("pinned") ?: false,
                    createdAt = snapshot.getLong("createdAt") ?: 0L,
                )
            }
        }
    }

    override fun observeSharedMedia(conversationId: String): Flow<List<Message>> =
        observeMessages(conversationId).map { list -> list.filter { it.mediaUrl != null } }

    override suspend fun startDirectConversation(userId: String): InsangramResult<String> =
        withContext(dispatchers.io) {
            val viewerId = uid()
                ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
            // Deterministic id keeps one thread per pair, whoever starts it.
            val id = listOf(viewerId, userId).sorted().joinToString("_")

            firebaseResult {
                val document = conversations.document(id)
                if (!document.get().await().exists()) {
                    document.set(
                        mapOf(
                            "memberIds" to listOf(viewerId, userId),
                            "isGroup" to false,
                            "title" to "",
                            "lastMessagePreview" to "",
                            "lastMessageAt" to System.currentTimeMillis(),
                            "unread" to mapOf(viewerId to 0L, userId to 0L),
                            "createdAt" to System.currentTimeMillis(),
                        ),
                    ).await()
                }
                id
            }
        }

    override suspend fun createGroup(
        userIds: List<String>,
        title: String,
    ): InsangramResult<String> = withContext(dispatchers.io) {
        val viewerId = uid()
            ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
        firebaseResult {
            val members = (userIds + viewerId).distinct()
            val document = conversations.document()
            document.set(
                mapOf(
                    "memberIds" to members,
                    "isGroup" to true,
                    "title" to title,
                    "adminIds" to listOf(viewerId),
                    "lastMessagePreview" to "",
                    "lastMessageAt" to System.currentTimeMillis(),
                    "unread" to members.associateWith { 0L },
                    "createdAt" to System.currentTimeMillis(),
                ),
            ).await()
            document.id
        }
    }

    override suspend fun sendText(
        conversationId: String,
        text: String,
        replyToMessageId: String?,
    ): InsangramResult<String> = withContext(dispatchers.io) {
        val viewerId = uid()
            ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
        if (text.isBlank()) {
            return@withContext InsangramResult.Failure(
                InsangramError.Validation("message", "Type a message first"),
            )
        }
        firebaseResult { writeMessage(conversationId, viewerId, text, null, null, replyToMessageId) }
    }

    override suspend fun sendMedia(
        conversationId: String,
        localUri: String,
        isVideo: Boolean,
    ): InsangramResult<String> = withContext(dispatchers.io) {
        val viewerId = uid()
            ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
        firebaseResult {
            val reference = storage.reference.child(
                "chats/$conversationId/${System.currentTimeMillis()}",
            )
            reference.putFile(Uri.parse(localUri)).await()
            val url = reference.downloadUrl.await().toString()
            writeMessage(
                conversationId = conversationId,
                senderId = viewerId,
                text = "",
                mediaUrl = url,
                mediaType = if (isVideo) "VIDEO" else "IMAGE",
                replyToMessageId = null,
            )
        }
    }

    private suspend fun writeMessage(
        conversationId: String,
        senderId: String,
        text: String,
        mediaUrl: String?,
        mediaType: String?,
        replyToMessageId: String?,
    ): String {
        val conversation = conversations.document(conversationId)
        val message = conversation.collection(FirestorePaths.MESSAGES).document()
        val now = System.currentTimeMillis()

        message.set(
            mapOf(
                "senderId" to senderId,
                "text" to text.trim(),
                "mediaUrl" to mediaUrl,
                "mediaType" to mediaType,
                "replyToMessageId" to replyToMessageId,
                "readBy" to listOf(senderId),
                "reactions" to emptyMap<String, String>(),
                "unsent" to false,
                "pinned" to false,
                "createdAt" to now,
            ),
        ).await()

        val memberIds = conversation.get().await().stringList("memberIds")
        val updates = mutableMapOf<String, Any?>(
            "lastMessagePreview" to (text.ifBlank { "Sent an attachment" }),
            "lastMessageAt" to now,
        )
        memberIds.filter { it != senderId }.forEach { memberId ->
            updates["unread.$memberId"] = FieldValue.increment(1)
        }
        conversation.update(updates).await()
        return message.id
    }

    override suspend fun markRead(conversationId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val viewerId = uid()
                ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
            firebaseResult {
                conversations.document(conversationId)
                    .update("unread.$viewerId", 0L)
                    .await()
                Unit
            }
        }

    override suspend fun react(messageId: String, emoji: String): InsangramResult<Unit> =
        InsangramResult.Failure(
            InsangramError.Unsupported("Reactions need the conversation id in online mode."),
        )

    override suspend fun setTyping(conversationId: String, typing: Boolean) {
        val viewerId = uid() ?: return
        runCatching {
            conversations.document(conversationId).update(
                "typing",
                if (typing) FieldValue.arrayUnion(viewerId) else FieldValue.arrayRemove(viewerId),
            ).await()
        }
    }

    override suspend fun setMuted(
        conversationId: String,
        muted: Boolean,
    ): InsangramResult<Unit> = withContext(dispatchers.io) {
        val viewerId = uid()
            ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
        firebaseResult {
            conversations.document(conversationId).update(
                "mutedBy",
                if (muted) FieldValue.arrayUnion(viewerId) else FieldValue.arrayRemove(viewerId),
            ).await()
            Unit
        }
    }

    override suspend fun leaveConversation(conversationId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val viewerId = uid()
                ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
            firebaseResult {
                conversations.document(conversationId)
                    .update("memberIds", FieldValue.arrayRemove(viewerId))
                    .await()
                Unit
            }
        }
}
