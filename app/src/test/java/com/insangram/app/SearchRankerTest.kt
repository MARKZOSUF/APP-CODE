package com.insangram.app

import com.insangram.app.domain.usecase.SearchCandidate
import com.insangram.app.domain.usecase.SearchRanker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Search ranking runs client side over a bounded result set, because Firestore
 * offers prefix matching and array-contains rather than full-text search.
 */
class SearchRankerTest {

    private val ranker = SearchRanker()

    @Test
    fun `an exact username match ranks first`() {
        val ranked = ranker.rank(
            query = "meera",
            candidates = listOf(
                SearchCandidate("1", "meerakapoor", "Meera Kapoor", followerCount = 900),
                SearchCandidate("2", "meera", "Meera K", followerCount = 10),
            ),
        )
        assertEquals("2", ranked.first().id)
    }

    @Test
    fun `prefix matches outrank mid-string matches`() {
        val ranked = ranker.rank(
            query = "kab",
            candidates = listOf(
                SearchCandidate("1", "itskabir", "Kabir", followerCount = 500),
                SearchCandidate("2", "kabir_r", "Kabir R", followerCount = 20),
            ),
        )
        assertEquals("2", ranked.first().id)
    }

    @Test
    fun `follower count breaks ties`() {
        val ranked = ranker.rank(
            query = "aarav",
            candidates = listOf(
                SearchCandidate("low", "aaravx", "Aarav X", followerCount = 5),
                SearchCandidate("high", "aaravy", "Aarav Y", followerCount = 5000),
            ),
        )
        assertEquals("high", ranked.first().id)
    }

    @Test
    fun `non matching candidates are dropped`() {
        val ranked = ranker.rank(
            query = "zzzz",
            candidates = listOf(SearchCandidate("1", "aarav", "Aarav", followerCount = 10)),
        )
        assertTrue(ranked.isEmpty())
    }
}
