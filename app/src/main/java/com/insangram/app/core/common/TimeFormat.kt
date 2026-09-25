package com.insangram.app.core.common

import java.util.concurrent.TimeUnit

/** Relative timestamp formatting used by feed, comments, chat and stories. */
object TimeFormat {

    fun relative(epochMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val delta = (nowMillis - epochMillis).coerceAtLeast(0L)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
        val hours = TimeUnit.MILLISECONDS.toHours(delta)
        val days = TimeUnit.MILLISECONDS.toDays(delta)
        return when {
            minutes < 1 -> "now"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            days < 7 -> "${days}d"
            days < 365 -> "${days / 7}w"
            else -> "${days / 365}y"
        }
    }

    /** Long form used on post detail and analytics screens. */
    fun accessibleRelative(epochMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val delta = (nowMillis - epochMillis).coerceAtLeast(0L)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
        val hours = TimeUnit.MILLISECONDS.toHours(delta)
        val days = TimeUnit.MILLISECONDS.toDays(delta)
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "$minutes ${plural(minutes, "minute")} ago"
            hours < 24 -> "$hours ${plural(hours, "hour")} ago"
            days < 30 -> "$days ${plural(days, "day")} ago"
            else -> "${days / 30} ${plural(days / 30, "month")} ago"
        }
    }

    /** Remaining lifetime of a story, which expires 24h after creation. */
    fun storyRemaining(createdAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val expiry = createdAtMillis + TimeUnit.HOURS.toMillis(24)
        val remaining = expiry - nowMillis
        if (remaining <= 0) return "expired"
        val hours = TimeUnit.MILLISECONDS.toHours(remaining)
        return if (hours >= 1) "${hours}h left" else "${TimeUnit.MILLISECONDS.toMinutes(remaining)}m left"
    }

    fun duration(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    fun compactCount(value: Long): String = when {
        value < 1_000 -> value.toString()
        value < 1_000_000 -> trimZero(value / 1_000.0) + "K"
        else -> trimZero(value / 1_000_000.0) + "M"
    }

    private fun trimZero(value: Double): String {
        val rounded = (value * 10).toLong() / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    }

    private fun plural(value: Long, unit: String) = if (value == 1L) unit else unit + "s"
}
