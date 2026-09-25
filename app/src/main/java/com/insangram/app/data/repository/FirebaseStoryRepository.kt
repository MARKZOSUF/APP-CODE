package com.insangram.app.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.insangram.app.core.common.DispatcherProvider
import com.insangram.app.core.common.InsangramError
import com.insangram.app.core.common.InsangramResult
import com.insangram.app.domain.model.Story
import com.insangram.app.domain.model.StoryAudience
import com.insangram.app.domain.model.StoryTrayItem
import com.insangram.app.domain.model.StoryType
import com.insangram.app.domain.model.User
import com.insangram.app.domain.repository.StoryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Online stories. Every story carries an `expiresAt` 24 hours after creation
 * and expired documents are filtered out on read, so the tray is always live.
 */
@Singleton
class FirebaseStoryRepository @Inject constructor(
    private val local: DemoStoryRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val dispatchers: DispatcherProvider,
) : StoryRepository by local {

    private val stories get() = firestore.collection(FirestorePaths.STORIES)

    private fun uid(): String? = auth.currentUser?.uid

    private companion object {
        const val LIFETIME_MS = 24L * 60L * 60L * 1000L
    }

    override fun observeTray(): Flow<List<StoryTrayItem>> {
        val documents = stories.limit(200).snapshots()
        val authors = firestore.collection(FirestorePaths.USERS).limit(200).snapshots()
            .map { docs -> docs.mapNotNull { it.toUserOrNull() }.associateBy { it.id } }

        return combine(documents, authors) { docs, authorsById ->
            val now = System.currentTimeMillis()
            val viewerId = uid()
            docs
                .mapNotNull { toStory(it, authorsById, viewerId) }
                .filter { !it.isExpired(now) }
                .groupBy { it.author.id }
                .map { (_, authorStories) ->
                    StoryTrayItem(
                        author = authorStories.first().author,
                        stories = authorStories.sortedBy { it.createdAt },
                        allSeen = authorStories.all { it.seen },
                    )
                }
                .sortedByDescending { item -> item.stories.maxOfOrNull { it.createdAt } ?: 0L }
        }
    }

    private fun toStory(
        snapshot: com.google.firebase.firestore.DocumentSnapshot,
        authorsById: Map<String, User>,
        viewerId: String?,
    ): Story? {
        val authorId = snapshot.getString("authorId") ?: return null
        val seenBy = snapshot.stringList("seenBy")
        return Story(
            id = snapshot.id,
            author = authorsById[authorId] ?: unknownUser(authorId),
            type = when (snapshot.getString("type")) {
                "TEXT" -> StoryType.TEXT
                "VIDEO" -> StoryType.VIDEO
                else -> StoryType.PHOTO
            },
            mediaUrl = snapshot.getString("mediaUrl"),
            thumbnailUrl = snapshot.getString("mediaUrl"),
            text = snapshot.getString("text").orEmpty(),
            backgroundColor = snapshot.getLong("backgroundColor") ?: 0L,
            fontStyle = snapshot.getString("fontStyle") ?: "DEFAULT",
            audience = runCatching {
                StoryAudience.valueOf(snapshot.getString("audience") ?: "EVERYONE")
            }.getOrDefault(StoryAudience.EVERYONE),
            allowReplies = snapshot.getBoolean("allowReplies") ?: true,
            viewCount = snapshot.getLong("viewCount") ?: seenBy.size.toLong(),
            seen = viewerId != null && seenBy.contains(viewerId),
            createdAt = snapshot.getLong("createdAt") ?: 0L,
            expiresAt = snapshot.getLong("expiresAt") ?: 0L,
        )
    }

    override suspend fun storiesFor(userId: String): InsangramResult<List<Story>> =
        withContext(dispatchers.io) {
            val viewerId = uid()
            firebaseResult {
                val now = System.currentTimeMillis()
                stories.whereEqualTo("authorId", userId).get().await().documents
                    .mapNotNull { toStory(it, emptyMap(), viewerId) }
                    .filter { !it.isExpired(now) }
                    .sortedBy { it.createdAt }
            }
        }

    override suspend fun createPhotoStory(
        mediaUri: String,
        audience: StoryAudience,
        allowReplies: Boolean,
    ): InsangramResult<String> = withContext(dispatchers.io) {
        val viewerId = uid()
            ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
        firebaseResult {
            val document = stories.document()
            val reference = storage.reference.child("stories/$viewerId/${document.id}")
            reference.putFile(Uri.parse(mediaUri)).await()
            val url = reference.downloadUrl.await().toString()
            val now = System.currentTimeMillis()

            document.set(
                mapOf(
                    "authorId" to viewerId,
                    "type" to "PHOTO",
                    "mediaUrl" to url,
                    "audience" to audience.name,
                    "allowReplies" to allowReplies,
                    "seenBy" to emptyList<String>(),
                    "viewCount" to 0L,
                    "createdAt" to now,
                    "expiresAt" to now + LIFETIME_MS,
                ),
            ).await()
            document.id
        }
    }

    override suspend fun createTextStory(
        text: String,
        backgroundColor: Long,
        fontStyle: String,
        audience: StoryAudience,
    ): InsangramResult<String> = withContext(dispatchers.io) {
        val viewerId = uid()
            ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
        firebaseResult {
            val now = System.currentTimeMillis()
            val document = stories.document()
            document.set(
                mapOf(
                    "authorId" to viewerId,
                    "type" to "TEXT",
                    "text" to text,
                    "backgroundColor" to backgroundColor,
                    "fontStyle" to fontStyle,
                    "audience" to audience.name,
                    "allowReplies" to true,
                    "seenBy" to emptyList<String>(),
                    "viewCount" to 0L,
                    "createdAt" to now,
                    "expiresAt" to now + LIFETIME_MS,
                ),
            ).await()
            document.id
        }
    }

    override suspend fun markSeen(storyId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            val viewerId = uid()
                ?: return@withContext InsangramResult.Failure(InsangramError.NotAuthenticated)
            firebaseResult {
                stories.document(storyId).update(
                    mapOf(
                        "seenBy" to FieldValue.arrayUnion(viewerId),
                        "viewCount" to FieldValue.increment(1),
                    ),
                ).await()
                Unit
            }
        }

    override suspend fun deleteStory(storyId: String): InsangramResult<Unit> =
        withContext(dispatchers.io) {
            firebaseResult {
                stories.document(storyId).delete().await()
                Unit
            }
        }

    override suspend fun purgeExpired(): InsangramResult<Unit> = withContext(dispatchers.io) {
        val viewerId = uid() ?: return@withContext InsangramResult.Success(Unit)
        firebaseResult {
            val now = System.currentTimeMillis()
            stories.whereEqualTo("authorId", viewerId).get().await().documents
                .filter { (it.getLong("expiresAt") ?: 0L) in 1 until now }
                .forEach { it.reference.delete().await() }
            Unit
        }
    }
}
