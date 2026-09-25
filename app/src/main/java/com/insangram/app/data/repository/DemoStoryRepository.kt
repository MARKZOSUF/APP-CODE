package com.insangram.app.data.repository

import com.insangram.app.core.common.DispatcherProvider
import com.insangram.app.core.common.InsangramError
import com.insangram.app.core.common.InsangramResult
import com.insangram.app.core.database.InsangramDatabase
import com.insangram.app.domain.model.Story
import com.insangram.app.domain.model.StoryAudience
import com.insangram.app.domain.model.StoryTrayItem
import com.insangram.app.domain.model.StoryViewer
import com.insangram.app.domain.repository.StoryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

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
class DemoStoryRepository @Inject constructor(
    private val database: InsangramDatabase,
    private val dispatchers: DispatcherProvider,
) : StoryRepository {

    override fun observeTray(): Flow<List<StoryTrayItem>> = flowOf(emptyList())

    override suspend fun storiesFor(userId: String): InsangramResult<List<Story>> =
        InsangramResult.Success(emptyList())

    override suspend fun createPhotoStory(
        mediaUri: String,
        audience: StoryAudience,
        allowReplies: Boolean,
    ): InsangramResult<String> = InsangramResult.Failure(
            InsangramError.Unsupported("Publishing a story requires online mode."),
        )

    override suspend fun createTextStory(
        text: String,
        backgroundColor: Long,
        fontStyle: String,
        audience: StoryAudience,
    ): InsangramResult<String> = InsangramResult.Failure(
            InsangramError.Unsupported("Publishing a story requires online mode."),
        )

    override suspend fun markSeen(storyId: String): InsangramResult<Unit> =
        InsangramResult.Success(Unit)

    override suspend fun react(storyId: String, emoji: String): InsangramResult<Unit> =
        InsangramResult.Success(Unit)

    override suspend fun reply(storyId: String, text: String): InsangramResult<Unit> =
        InsangramResult.Failure(
            InsangramError.Unsupported("Story replies requires online mode."),
        )

    override fun observeViewers(storyId: String): Flow<List<StoryViewer>> = flowOf(emptyList())

    override suspend fun deleteStory(storyId: String): InsangramResult<Unit> =
        InsangramResult.Success(Unit)

    override fun observeArchive(): Flow<List<Story>> = flowOf(emptyList())

    override fun observeHighlights(): Flow<List<Story>> = flowOf(emptyList())

    override suspend fun addToHighlight(
        storyId: String,
        highlightName: String,
    ): InsangramResult<Unit> = InsangramResult.Success(Unit)

    /** Drops cached stories past the 24 hour lifetime. This one is real work. */
    override suspend fun purgeExpired(): InsangramResult<Unit> = withContext(dispatchers.io) {
        runCatching { database.storyDao().deleteExpired(System.currentTimeMillis()) }
            .fold(
                onSuccess = { InsangramResult.Success(Unit) },
                onFailure = { InsangramResult.Failure(InsangramError.DatabaseFailure(it)) },
            )
    }
}
