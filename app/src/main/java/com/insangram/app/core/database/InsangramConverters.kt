package com.insangram.app.core.database

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room type converters. Lists are stored as JSON text rather than as separate
 * tables because they are always read and written as a whole (hashtags, media
 * ids, member ids) and are bounded in size by validation.
 */
class InsangramConverters {

    @TypeConverter
    fun fromStringList(values: List<String>?): String = json.encodeToString(values ?: emptyList())

    @TypeConverter
    fun toStringList(raw: String?): List<String> =
        if (raw.isNullOrBlank()) emptyList() else runCatching {
            json.decodeFromString<List<String>>(raw)
        }.getOrDefault(emptyList())

    @TypeConverter
    fun fromStringMap(values: Map<String, String>?): String =
        json.encodeToString(values ?: emptyMap())

    @TypeConverter
    fun toStringMap(raw: String?): Map<String, String> =
        if (raw.isNullOrBlank()) emptyMap() else runCatching {
            json.decodeFromString<Map<String, String>>(raw)
        }.getOrDefault(emptyMap())

    @TypeConverter
    fun fromLongList(values: List<Long>?): String = json.encodeToString(values ?: emptyList())

    @TypeConverter
    fun toLongList(raw: String?): List<Long> =
        if (raw.isNullOrBlank()) emptyList() else runCatching {
            json.decodeFromString<List<Long>>(raw)
        }.getOrDefault(emptyList())

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
