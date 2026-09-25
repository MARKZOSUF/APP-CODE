package com.insangram.app.data.repository

import androidx.paging.PagingData
import com.insangram.app.core.common.InsangramError
import com.insangram.app.core.common.InsangramResult
import com.insangram.app.domain.model.Reel
import com.insangram.app.domain.repository.ReelRepository
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
class DemoReelRepository @Inject constructor() : ReelRepository {

    private val likedReelIds = MutableStateFlow<Set<String>>(emptySet())
    private val savedReelIds = MutableStateFlow<Set<String>>(emptySet())

    override fun reelFeed(): Flow<PagingData<Reel>> = flowOf(PagingData.empty())

    override fun observeReel(reelId: String): Flow<Reel?> = flowOf(null)

    override fun observeAuthorReels(userId: String): Flow<List<Reel>> = flowOf(emptyList())

    override suspend fun trending(limit: Int): InsangramResult<List<Reel>> =
        InsangramResult.Success(emptyList())

    override suspend fun uploadReel(
        videoUri: String,
        caption: String,
        audioLabel: String,
    ): InsangramResult<String> = InsangramResult.Failure(
            InsangramError.Unsupported("Reel upload requires online mode."),
        )

    /** Like state is kept in memory so the UI toggles correctly within a session. */
    override suspend fun toggleLike(reelId: String): InsangramResult<Boolean> {
        val nowLiked = reelId !in likedReelIds.value
        likedReelIds.value = if (nowLiked) {
            likedReelIds.value + reelId
        } else {
            likedReelIds.value - reelId
        }
        return InsangramResult.Success(nowLiked)
    }

    override suspend fun toggleSave(reelId: String): InsangramResult<Boolean> {
        val nowSaved = reelId !in savedReelIds.value
        savedReelIds.value = if (nowSaved) {
            savedReelIds.value + reelId
        } else {
            savedReelIds.value - reelId
        }
        return InsangramResult.Success(nowSaved)
    }

    override suspend fun registerView(reelId: String): InsangramResult<Unit> =
        InsangramResult.Success(Unit)

    override suspend fun recordWatchTime(reelId: String, watchedMs: Long, completed: Boolean) {
        // Watch-time analytics are an online-only feature; nothing to persist here.
    }

    override suspend fun registerShare(reelId: String): InsangramResult<Unit> =
        InsangramResult.Success(Unit)

    override suspend fun notInterested(reelId: String): InsangramResult<Unit> =
        InsangramResult.Success(Unit)

    override suspend fun deleteReel(reelId: String): InsangramResult<Unit> =
        InsangramResult.Success(Unit)
}
