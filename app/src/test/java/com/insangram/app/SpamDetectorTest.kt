package com.insangram.app

import com.insangram.app.domain.usecase.SpamDetector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rule-based spam heuristics. No model is involved - these are deterministic
 * checks, which is exactly why they are unit testable.
 */
class SpamDetectorTest {

    private val detector = SpamDetector()

    @Test
    fun `ordinary messages are not flagged`() {
        val result = detector.evaluate("Great shot! Where was this taken?")
        assertFalse(result.isSpam)
    }

    @Test
    fun `excessive links are flagged`() {
        val result = detector.evaluate(
            "Check http://a.example http://b.example http://c.example http://d.example",
        )
        assertTrue(result.isSpam)
        assertTrue(result.reasons.any { it.contains("link", ignoreCase = true) })
    }

    @Test
    fun `mention flooding is flagged`() {
        val mentions = (1..12).joinToString(" ") { "@user$it" }
        assertTrue(detector.evaluate("Giveaway $mentions").isSpam)
    }

    @Test
    fun `repeated characters are flagged`() {
        assertTrue(detector.evaluate("WOOOOOOOOOOOOOOOOW freeeeeeeeee").isSpam)
    }

    @Test
    fun `repeated identical messages are flagged`() {
        val text = "Follow me back please"
        val result = detector.evaluate(text, recentMessages = List(4) { text })
        assertTrue(result.isSpam)
    }

    @Test
    fun `promotional blocklist terms are flagged`() {
        assertTrue(detector.evaluate("FREE FOLLOWERS click this link now!!!").isSpam)
    }
}
