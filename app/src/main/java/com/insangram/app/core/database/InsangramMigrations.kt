package com.insangram.app.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Explicit migrations. Destructive migration is never enabled, because losing
 * the cache would also lose queued offline actions and unsent drafts.
 */
object InsangramMigrations {

    /**
     * v1 -> v2: adds local ranking signals (`interest_signal`, `watch_time`)
     * and the `pronouns` profile field introduced with the profile editor.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS interest_signal (
                    userId TEXT NOT NULL,
                    token TEXT NOT NULL,
                    weight REAL NOT NULL DEFAULT 0.0,
                    updatedAt INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(userId, token)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS watch_time (
                    userId TEXT NOT NULL,
                    contentId TEXT NOT NULL,
                    watchedMs INTEGER NOT NULL DEFAULT 0,
                    completions INTEGER NOT NULL DEFAULT 0,
                    updatedAt INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(userId, contentId)
                )
                """.trimIndent(),
            )
            db.execSQL("ALTER TABLE cached_user ADD COLUMN pronouns TEXT NOT NULL DEFAULT ''")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
