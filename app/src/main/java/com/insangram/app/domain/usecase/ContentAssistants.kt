package com.insangram.app.domain.usecase

import com.insangram.app.core.common.Validators
import javax.inject.Inject
import javax.inject.Singleton

enum class CaptionStyle { CASUAL, PROFESSIONAL, FUNNY, TRAVEL, FOOD, STUDY, FITNESS }

/**
 * Template-driven caption suggestions. Entirely local and rule-based; nothing
 * is sent to an external model. The README states this explicitly so the
 * feature is never mistaken for generative AI.
 */
@Singleton
class CaptionAssistant @Inject constructor() {

    fun suggest(style: CaptionStyle, keywords: List<String>, limit: Int = 5): List<String> {
        val subject = keywords.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() } ?: "this moment"
        val secondary = keywords.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() } ?: subject
        return TEMPLATES.getValue(style)
            .map { it.replace("{subject}", subject).replace("{extra}", secondary) }
            .distinct()
            .take(limit)
    }

    fun stylesFor(keywords: List<String>): List<CaptionStyle> {
        val joined = keywords.joinToString(" ").lowercase()
        val inferred = buildList {
            if (TRAVEL_WORDS.any { joined.contains(it) }) add(CaptionStyle.TRAVEL)
            if (FOOD_WORDS.any { joined.contains(it) }) add(CaptionStyle.FOOD)
            if (STUDY_WORDS.any { joined.contains(it) }) add(CaptionStyle.STUDY)
            if (FITNESS_WORDS.any { joined.contains(it) }) add(CaptionStyle.FITNESS)
        }
        return (inferred + CaptionStyle.CASUAL + CaptionStyle.FUNNY + CaptionStyle.PROFESSIONAL).distinct()
    }

    private companion object {
        val TRAVEL_WORDS = listOf("beach", "trip", "travel", "mountain", "city", "trek", "flight")
        val FOOD_WORDS = listOf("food", "coffee", "lunch", "dinner", "cake", "recipe", "chai")
        val STUDY_WORDS = listOf("study", "exam", "college", "project", "library", "code", "semester")
        val FITNESS_WORDS = listOf("gym", "run", "workout", "yoga", "fitness", "training")

        val TEMPLATES: Map<CaptionStyle, List<String>> = mapOf(
            CaptionStyle.CASUAL to listOf(
                "Just {subject}, nothing else planned.",
                "A little bit of {subject} to reset the week.",
                "{subject} kind of day.",
                "Saving this one: {subject}.",
                "Ordinary day, good light, {subject}.",
            ),
            CaptionStyle.PROFESSIONAL to listOf(
                "Sharing a look at {subject}.",
                "Notes from {subject} - grateful for the process.",
                "Progress on {subject}, one step at a time.",
                "Documenting {subject} and what it taught me.",
                "Behind the scenes of {subject}.",
            ),
            CaptionStyle.FUNNY to listOf(
                "{subject}: attempted. Results: debatable.",
                "Told myself one photo of {subject}. Took forty.",
                "Me, {subject}, and zero regrets.",
                "If {subject} was a personality trait.",
                "Nobody asked, but here is {subject}.",
            ),
            CaptionStyle.TRAVEL to listOf(
                "Somewhere between {subject} and {extra}.",
                "Collecting places, starting with {subject}.",
                "{subject} looked better in person.",
                "Detours are the point. {subject}.",
                "Woke up early for {subject}. Worth it.",
            ),
            CaptionStyle.FOOD to listOf(
                "{subject} and absolutely no sharing.",
                "Made {subject}. Ate {subject}. Balanced.",
                "The {subject} was the whole plan.",
                "Simple {subject}, good company.",
                "Recipe request open for {subject}.",
            ),
            CaptionStyle.STUDY to listOf(
                "Slow progress on {subject} is still progress.",
                "{subject} today, revision tomorrow.",
                "Desk, coffee, {subject}.",
                "Two hours on {subject}, one idea worth keeping.",
                "Building {subject} one commit at a time.",
            ),
            CaptionStyle.FITNESS to listOf(
                "Showed up for {subject}. That is the win.",
                "{subject} done before the day started.",
                "Consistency over intensity: {subject}.",
                "Slightly sore, mostly pleased. {subject}.",
                "Week of {subject}, still going.",
            ),
        )
    }
}

/**
 * Suggests hashtags from a bundled dataset by matching caption keywords and
 * category, then blending in currently trending tags supplied by the caller.
 */
@Singleton
class HashtagAssistant @Inject constructor() {

    fun suggest(
        caption: String,
        trending: List<String> = emptyList(),
        limit: Int = 12,
    ): List<String> {
        val keywords = Validators.captionKeywordTokens(caption)
        val alreadyUsed = Validators.extractHashtags(caption).map { it.lowercase() }.toSet()

        val scored = mutableMapOf<String, Int>()
        keywords.forEach { keyword ->
            DATASET.forEach { (category, tags) ->
                if (category.contains(keyword) || keyword.contains(category)) {
                    tags.forEach { scored[it] = (scored[it] ?: 0) + 3 }
                }
                tags.forEach { tag ->
                    if (tag.contains(keyword)) scored[tag] = (scored[tag] ?: 0) + 2
                }
            }
        }
        trending.forEach { tag ->
            val normalized = tag.lowercase().removePrefix("#")
            scored[normalized] = (scored[normalized] ?: 0) + 1
        }
        if (scored.isEmpty()) DATASET.getValue("general").forEach { scored[it] = 1 }

        return scored.entries
            .filterNot { it.key in alreadyUsed }
            .sortedByDescending { it.value }
            .take(limit)
            .map { "#" + it.key }
    }

    fun categories(): List<String> = DATASET.keys.sorted()

    fun tagsForCategory(category: String): List<String> =
        DATASET[category].orEmpty().map { "#" + it }

    private companion object {
        val DATASET: Map<String, List<String>> = mapOf(
            "general" to listOf("insangram", "photooftheday", "dailypost", "moments", "capture"),
            "travel" to listOf("travel", "wanderlust", "roadtrip", "mountains", "beachday", "cityscape"),
            "food" to listOf("foodie", "homecooked", "coffeetime", "streetfood", "baking", "dinnertime"),
            "study" to listOf("studygram", "studentlife", "campus", "exams", "notes", "productivity"),
            "fitness" to listOf("fitness", "gymlife", "running", "yoga", "training", "healthyhabits"),
            "tech" to listOf("android", "kotlin", "coding", "developer", "opensource", "buildinpublic"),
            "art" to listOf("art", "sketchbook", "illustration", "design", "handmade", "creative"),
            "music" to listOf("music", "guitar", "livemusic", "playlist", "songwriting"),
            "nature" to listOf("nature", "sunsetlover", "greenery", "wildlife", "monsoon"),
            "pets" to listOf("petsofinsangram", "doglife", "catsofinsangram", "rescuepet"),
            "fashion" to listOf("ootd", "styleinspo", "thrifted", "minimalstyle"),
            "reels" to listOf("reels", "shortvideo", "trending", "behindthescenes"),
        )
    }
}

/** Short, respectful canned replies offered under the comment composer. */
@Singleton
class CommentAssistant @Inject constructor() {

    fun suggestions(commentText: String, isAuthorReplying: Boolean): List<String> {
        val normalized = commentText.lowercase()
        return when {
            QUESTION_WORDS.any { normalized.contains(it) } || normalized.endsWith("?") ->
                listOf(
                    "Good question - I will share the details soon.",
                    "Thanks for asking! Sending you a message.",
                    "Adding that to the caption now.",
                )

            isAuthorReplying -> listOf(
                "Thank you, that means a lot.",
                "Appreciate you stopping by.",
                "Glad you liked it!",
                "Thanks for the kind words.",
            )

            else -> listOf(
                "This looks great.",
                "Love the colours here.",
                "Well done!",
                "Really nice shot.",
                "Congratulations!",
            )
        }
    }

    private companion object {
        val QUESTION_WORDS = listOf("how", "where", "what", "when", "which", "why", "can you")
    }
}
