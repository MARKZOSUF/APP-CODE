package com.insangram.app

import com.insangram.app.domain.usecase.FeedCandidate
import com.insangram.app.domain.usecase.FeedRankingUseCase
import com.insangram.app.domain.usecase.ViewerSignals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The feed score is deliberately transparent, so it can be asserted exactly.
 *
 * score = likes*1 + comments*3 + saves*4 + shares*4 + views*0.1
 *       + relationship + interest + watchTime + freshness - negative
 */
class FeedRankingUseCaseTest {

    private val ranking = FeedRankingUseCase()
    private val now = 1_757_000_000_000L

    @Test
    fun `engagement weights are applied exactly as documented`() {
        val breakdown = ranking.score(
            candidate(likes = 10, comments = 2, saves = 1, shares = 1, views = 100),
            ViewerSignals(),
            now,
        )
        // 10 + 6 + 4 + 4 + 10 = 34 of pure engagement.
        assertEquals(34.0, breakdown.engagementScore, 0.001)
    }

    @Test
    fun `following a creator ranks their post above an identical stranger post`() {
        val followed = ranking.score(
            candidate(authorId = "friend"),
            ViewerSignals(followedAuthorIds = setOf("friend")),
            now,
        )
        val stranger = ranking.score(candidate(authorId = "stranger"), ViewerSignals(), now)
        assertTrue(followed.score > stranger.score)
    }

    @Test
    fun `close friends outrank ordinary follows`() {
        val close = ranking.score(
            candidate(authorId = "friend"),
            ViewerSignals(followedAuthorIds = setOf("friend"), closeFriendIds = setOf("friend")),
            now,
        )
        val ordinary = ranking.score(
            candidate(authorId = "friend"),
            ViewerSignals(followedAuthorIds = setOf("friend")),
            now,
        )
        assertTrue(close.score > ordinary.score)
    }

    @Test
    fun `fresh content outranks identical old content`() {
        val fresh = ranking.score(candidate(createdAt = now - 60_000), ViewerSignals(), now)
        val old = ranking.score(
            candidate(createdAt = now - 14 * 24 * 60 * 60 * 1000L),
            ViewerSignals(),
            now,
        )
        assertTrue(fresh.score > old.score)
    }

    @Test
    fun `hidden and reported content is pushed to the bottom`() {
        val hidden = ranking.score(
            candidate(likes = 5_000),
            ViewerSignals(hiddenContentIds = setOf("post-1")),
            now,
        )
        val ordinary = ranking.score(candidate(likes = 1), ViewerSignals(), now)
        assertTrue(hidden.score < ordinary.score)
    }

    @Test
    fun `blocked authors score below everything else`() {
        val blocked = ranking.score(
            candidate(authorId = "blocked", likes = 10_000),
            ViewerSignals(blockedUserIds = setOf("blocked")),
            now,
        )
        assertTrue(blocked.score < 0.0)
    }

    @Test
    fun `hashtag interest lifts matching content`() {
        val interested = ranking.score(
            candidate(hashtags = listOf("travel")),
            ViewerSignals(hashtagInterest = mapOf("hashtag:travel" to 5.0)),
            now,
        )
        val neutral = ranking.score(candidate(hashtags = listOf("travel")), ViewerSignals(), now)
        assertTrue(interested.score > neutral.score)
    }

    private fun candidate(
        contentId: String = "post-1",
        authorId: String = "author-1",
        hashtags: List<String> = emptyList(),
        likes: Long = 0L,
        comments: Long = 0L,
        saves: Long = 0L,
        shares: Long = 0L,
        views: Long = 0L,
        createdAt: Long = now,
    ) = FeedCandidate(
        contentId = contentId,
        authorId = authorId,
        likes = likes,
        comments = comments,
        saves = saves,
        shares = shares,
        views = views,
        hashtags = hashtags,
        createdAt = createdAt,
    )
}
