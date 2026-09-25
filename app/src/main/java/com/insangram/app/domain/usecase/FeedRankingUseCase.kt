package com.insangram.app.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max

/**
 * Inputs for one ranking decision. Aggregate counts must come from the trusted
 * backend (Cloud Functions maintain them); the client never invents them.
 */
data class FeedCandidate(
    val contentId: String,
    val authorId: String,
    val likes: Long,
    val comments: Long,
    val saves: Long,
    val shares: Long,
    val views: Long,
    val hashtags: List<String>,
    val createdAt: Long,
)

/** Viewer-specific signals, all derived from local interaction history. */
data class ViewerSignals(
    val followedAuthorIds: Set<String> = emptySet(),
    val closeFriendIds: Set<String> = emptySet(),
    /** authorId -> affinity, roughly "interactions with this creator". */
    val creatorAffinity: Map<String, Double> = emptyMap(),
    /** normalized hashtag -> interest weight. */
    val hashtagInterest: Map<String, Double> = emptyMap(),
    /** contentId -> milliseconds watched. */
    val watchTimeMs: Map<String, Long> = emptyMap(),
    val hiddenContentIds: Set<String> = emptySet(),
    val reportedContentIds: Set<String> = emptySet(),
    val blockedUserIds: Set<String> = emptySet(),
    val mutedUserIds: Set<String> = emptySet(),
)

data class ScoredContent(val contentId: String, val score: Double, val breakdown: Map<String, Double>)

/**
 * Transparent, rule-based feed ranking. There is no machine-learned model here;
 * every term is documented and testable, which is exactly what the project
 * report needs to be able to explain.
 *
 * score = (likes x 1.0) + (comments x 3.0) + (saves x 4.0) + (shares x 4.0)
 *       + (views x 0.1) + relationshipScore + interestScore + watchTimeScore
 *       + freshnessScore - negativeFeedbackScore
 */
@Singleton
class FeedRankingUseCase @Inject constructor() {

    operator fun invoke(
        candidates: List<FeedCandidate>,
        signals: ViewerSignals,
        now: Long = System.currentTimeMillis(),
    ): List<ScoredContent> = candidates
        .filterNot { it.authorId in signals.blockedUserIds }
        .map { score(it, signals, now) }
        .sortedByDescending { it.score }

    fun score(
        candidate: FeedCandidate,
        signals: ViewerSignals,
        now: Long = System.currentTimeMillis(),
    ): ScoredContent {
        val engagement = (candidate.likes * WEIGHT_LIKE) +
            (candidate.comments * WEIGHT_COMMENT) +
            (candidate.saves * WEIGHT_SAVE) +
            (candidate.shares * WEIGHT_SHARE) +
            (candidate.views * WEIGHT_VIEW)

        val relationship = relationshipScore(candidate.authorId, signals)
        val interest = interestScore(candidate, signals)
        val watch = watchTimeScore(candidate.contentId, signals)
        val freshness = freshnessScore(candidate.createdAt, now)
        val negative = negativeFeedbackScore(candidate, signals)

        val total = engagement + relationship + interest + watch + freshness - negative

        return ScoredContent(
            contentId = candidate.contentId,
            score = total,
            breakdown = mapOf(
                "engagement" to engagement,
                "relationship" to relationship,
                "interest" to interest,
                "watchTime" to watch,
                "freshness" to freshness,
                "negative" to -negative,
            ),
        )
    }

    /** Followed authors and close friends are boosted so the feed stays social. */
    private fun relationshipScore(authorId: String, signals: ViewerSignals): Double {
        var score = 0.0
        if (authorId in signals.followedAuthorIds) score += FOLLOW_BOOST
        if (authorId in signals.closeFriendIds) score += CLOSE_FRIEND_BOOST
        val affinity = signals.creatorAffinity[authorId] ?: 0.0
        // Log-damped so one very active relationship cannot dominate the feed.
        score += ln(1.0 + max(0.0, affinity)) * AFFINITY_WEIGHT
        return score
    }

    /** Sum of the viewer's interest in each hashtag, damped by tag count. */
    private fun interestScore(candidate: FeedCandidate, signals: ViewerSignals): Double {
        if (candidate.hashtags.isEmpty() || signals.hashtagInterest.isEmpty()) return 0.0
        val matched = candidate.hashtags.sumOf { tag ->
            signals.hashtagInterest[tag.lowercase().removePrefix("#")] ?: 0.0
        }
        return ln(1.0 + max(0.0, matched)) * INTEREST_WEIGHT
    }

    /** Rewards content the viewer previously watched, capped at one minute. */
    private fun watchTimeScore(contentId: String, signals: ViewerSignals): Double {
        val watched = signals.watchTimeMs[contentId] ?: return 0.0
        val cappedSeconds = (watched / 1000.0).coerceAtMost(60.0)
        return cappedSeconds * WATCH_TIME_WEIGHT
    }

    /**
     * Exponential decay with a 24 hour half-life. A brand-new post gets the
     * full bonus; a two-day-old post keeps roughly a quarter of it.
     */
    fun freshnessScore(createdAt: Long, now: Long): Double {
        if (createdAt <= 0) return 0.0
        val ageHours = ((now - createdAt).coerceAtLeast(0)) / 3_600_000.0
        return FRESHNESS_MAX * exp(-ageHours / HALF_LIFE_HOURS)
    }

    private fun negativeFeedbackScore(candidate: FeedCandidate, signals: ViewerSignals): Double {
        var penalty = 0.0
        if (candidate.contentId in signals.hiddenContentIds) penalty += HIDDEN_PENALTY
        if (candidate.contentId in signals.reportedContentIds) penalty += REPORTED_PENALTY
        if (candidate.authorId in signals.mutedUserIds) penalty += MUTED_PENALTY
        return penalty
    }

    companion object {
        const val WEIGHT_LIKE = 1.0
        const val WEIGHT_COMMENT = 3.0
        const val WEIGHT_SAVE = 4.0
        const val WEIGHT_SHARE = 4.0
        const val WEIGHT_VIEW = 0.1

        const val FOLLOW_BOOST = 40.0
        const val CLOSE_FRIEND_BOOST = 25.0
        const val AFFINITY_WEIGHT = 12.0
        const val INTEREST_WEIGHT = 10.0
        const val WATCH_TIME_WEIGHT = 0.8
        const val FRESHNESS_MAX = 50.0
        const val HALF_LIFE_HOURS = 24.0

        const val HIDDEN_PENALTY = 1_000.0
        const val REPORTED_PENALTY = 1_000.0
        const val MUTED_PENALTY = 250.0
    }
}
