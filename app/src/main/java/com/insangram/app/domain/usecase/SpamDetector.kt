package com.insangram.app.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton

data class SpamVerdict(
    val isSpam: Boolean,
    val confidence: Double,
    val reasons: List<String>,
) {
    val shouldWarn: Boolean get() = confidence >= 0.4
}

/**
 * Rule-based spam heuristics. This is deliberately simple, deterministic and
 * fully local - no generative model is involved. Each rule contributes a
 * weight, and text crossing the threshold is flagged.
 */
@Singleton
class SpamDetector @Inject constructor() {

    /**
     * @param recentMessages the author's own recent texts, used to detect the
     *   "same message over and over" pattern.
     */
    fun analyze(text: String, recentMessages: List<String> = emptyList()): SpamVerdict {
        val reasons = mutableListOf<String>()
        var score = 0.0
        val normalized = text.trim().lowercase()
        if (normalized.isEmpty()) return SpamVerdict(false, 0.0, emptyList())

        val linkCount = LINK_REGEX.findAll(text).count()
        if (linkCount >= 3) {
            score += 0.45
            reasons += "Contains $linkCount links"
        } else if (linkCount == 2) {
            score += 0.2
            reasons += "Contains multiple links"
        }

        val mentionCount = MENTION_REGEX.findAll(text).count()
        if (mentionCount >= 8) {
            score += 0.4
            reasons += "Mentions $mentionCount accounts"
        } else if (mentionCount >= 5) {
            score += 0.2
            reasons += "Mentions many accounts"
        }

        if (REPEATED_CHAR_REGEX.containsMatchIn(normalized)) {
            score += 0.15
            reasons += "Excessive repeated characters"
        }

        val letters = text.filter { it.isLetter() }
        if (letters.length >= 12 && letters.count { it.isUpperCase() }.toDouble() / letters.length > 0.7) {
            score += 0.15
            reasons += "Mostly capital letters"
        }

        val matchedTerms = BLOCKLIST.filter { normalized.contains(it) }
        if (matchedTerms.isNotEmpty()) {
            score += 0.25 * matchedTerms.size.coerceAtMost(3)
            reasons += "Matches promotional phrases: " + matchedTerms.joinToString(", ")
        }

        val duplicates = recentMessages.count { it.trim().lowercase() == normalized }
        if (duplicates >= 2) {
            score += 0.5
            reasons += "Repeated $duplicates times recently"
        } else if (duplicates == 1) {
            score += 0.2
            reasons += "Repeats a recent message"
        }

        val hashtagCount = HASHTAG_REGEX.findAll(text).count()
        if (hashtagCount >= 25) {
            score += 0.3
            reasons += "Uses $hashtagCount hashtags"
        }

        val confidence = score.coerceIn(0.0, 1.0)
        return SpamVerdict(isSpam = confidence >= SPAM_THRESHOLD, confidence = confidence, reasons = reasons)
    }

    companion object {
        const val SPAM_THRESHOLD = 0.6

        private val LINK_REGEX = Regex("""(https?://|www\.)\S+""", RegexOption.IGNORE_CASE)
        private val MENTION_REGEX = Regex("""@[A-Za-z0-9._]{2,30}""")
        private val HASHTAG_REGEX = Regex("""#[\p{L}0-9_]{1,50}""")
        private val REPEATED_CHAR_REGEX = Regex("""(.)\1{6,}""")

        /** Local, editable heuristics list - not a moderation authority. */
        private val BLOCKLIST = listOf(
            "free followers", "buy followers", "click this link", "crypto giveaway",
            "double your money", "dm me to earn", "work from home earn", "guaranteed profit",
            "limited time offer act now", "congratulations you won", "claim your prize",
            "investment opportunity", "forex signals", "telegram channel join now",
        )
    }
}
