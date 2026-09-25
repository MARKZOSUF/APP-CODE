package com.insangram.app.domain.usecase

import com.insangram.app.core.common.Validators
import com.insangram.app.domain.model.HashtagSummary
import com.insangram.app.domain.model.User
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln

/**
 * Client-side relevance ranking applied only to bounded result sets returned
 * by Firestore prefix queries or Room LIKE queries. Firestore has no full-text
 * search, so the strategy is: fetch a small candidate set using indexed
 * prefix/equality queries, then order it here.
 */
@Singleton
class SearchRanker @Inject constructor() {

    fun rankAccounts(
        query: String,
        candidates: List<User>,
        followedIds: Set<String> = emptySet(),
    ): List<User> {
        val q = Validators.normalizeUsername(query)
        if (q.isEmpty()) return candidates
        return candidates.sortedByDescending { user ->
            var score = 0.0
            val username = Validators.normalizeUsername(user.username)
            val name = user.fullName.lowercase()

            when {
                username == q -> score += 100.0
                username.startsWith(q) -> score += 60.0
                username.contains(q) -> score += 25.0
            }
            when {
                name.startsWith(q) -> score += 30.0
                name.split(" ").any { it.startsWith(q) } -> score += 20.0
                name.contains(q) -> score += 10.0
            }
            if (user.id in followedIds) score += 35.0
            // Popularity is a tiebreaker only, log-damped.
            score += ln(1.0 + user.followerCount.toDouble()) * 2.0
            score
        }
    }

    fun rankHashtags(query: String, candidates: List<HashtagSummary>): List<HashtagSummary> {
        val q = query.lowercase().removePrefix("#")
        return candidates.sortedByDescending { tag ->
            val normalized = tag.tag.lowercase().removePrefix("#")
            var score = 0.0
            when {
                normalized == q -> score += 100.0
                normalized.startsWith(q) -> score += 50.0
                normalized.contains(q) -> score += 20.0
            }
            score += ln(1.0 + tag.postCount.toDouble()) * 4.0
            score += ln(1.0 + tag.recentEngagement.toDouble()) * 2.0
            score
        }
    }

    /** Keyword-overlap scoring for captions, used for the Posts tab. */
    fun captionRelevance(query: String, caption: String, hashtags: List<String>): Double {
        val queryTokens = Validators.captionKeywordTokens(query)
        if (queryTokens.isEmpty()) return 0.0
        val captionTokens = Validators.captionKeywordTokens(caption).toSet()
        val tagTokens = hashtags.map { it.lowercase().removePrefix("#") }.toSet()

        var score = 0.0
        queryTokens.forEach { token ->
            if (token in tagTokens) score += 12.0
            if (token in captionTokens) score += 8.0
            else if (captionTokens.any { it.startsWith(token) }) score += 3.0
        }
        return score
    }

    /**
     * Generic account ranking over [SearchCandidate] rows. Candidates that do
     * not match the query at all are dropped rather than ordered last, so the
     * Accounts tab never shows unrelated profiles.
     */
    fun rank(query: String, candidates: List<SearchCandidate>): List<SearchCandidate> {
        val q = Validators.normalizeUsername(query)
        if (q.isEmpty()) return candidates
        return candidates
            .mapNotNull { candidate ->
                val score = matchScore(q, candidate)
                if (score <= 0.0) null else candidate to score
            }
            .sortedByDescending { (candidate, score) ->
                score + ln(1.0 + candidate.followerCount.toDouble()) * 2.0
            }
            .map { it.first }
    }

    private fun matchScore(normalizedQuery: String, candidate: SearchCandidate): Double {
        var score = 0.0
        val username = Validators.normalizeUsername(candidate.username)
        val name = candidate.fullName.lowercase()
        when {
            username == normalizedQuery -> score += 100.0
            username.startsWith(normalizedQuery) -> score += 60.0
            username.contains(normalizedQuery) -> score += 25.0
        }
        when {
            name.startsWith(normalizedQuery) -> score += 30.0
            name.split(" ").any { it.startsWith(normalizedQuery) } -> score += 20.0
            name.contains(normalizedQuery) -> score += 10.0
        }
        return score
    }
}

/**
 * Minimal account projection used by search ranking. Keeping ranking off the
 * full [User] model lets Room return a narrow query and lets the ranker be
 * unit tested without constructing whole profiles.
 */
data class SearchCandidate(
    val id: String,
    val username: String,
    val fullName: String,
    val followerCount: Long = 0,
)
