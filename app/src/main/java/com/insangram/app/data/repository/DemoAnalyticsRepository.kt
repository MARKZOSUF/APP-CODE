package com.insangram.app.data.repository

import com.insangram.app.core.common.InsangramResult
import com.insangram.app.domain.model.CreatorAnalytics
import com.insangram.app.domain.repository.AnalyticsRepository
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
class DemoAnalyticsRepository @Inject constructor() : AnalyticsRepository {

    /** Null means "no analytics yet", which the UI renders as an empty state. */
    override fun observeAnalytics(): Flow<CreatorAnalytics?> = flowOf(null)

    override suspend fun refresh(): InsangramResult<Unit> = InsangramResult.Success(Unit)

    override suspend fun recordProfileVisit(userId: String) {
        // Profile-visit tracking is an online-only creator feature.
    }
}
