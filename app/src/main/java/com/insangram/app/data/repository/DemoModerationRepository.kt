package com.insangram.app.data.repository

import com.insangram.app.core.common.InsangramResult
import com.insangram.app.domain.model.ContentReport
import com.insangram.app.domain.model.ReportTargetType
import com.insangram.app.domain.repository.ModerationRepository
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
class DemoModerationRepository @Inject constructor() : ModerationRepository {

    /**
     * Reports are accepted so the reporting flow is testable end to end, but
     * there is no review queue to submit them to without a backend.
     */
    override suspend fun report(
        targetType: ReportTargetType,
        targetId: String,
        reason: String,
        details: String,
    ): InsangramResult<Unit> = InsangramResult.Success(Unit)

    override fun observeMyReports(): Flow<List<ContentReport>> = flowOf(emptyList())
}
