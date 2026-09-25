package com.insangram.app.core.database

import android.content.Context
import androidx.room.Room
import com.insangram.app.core.database.dao.ActivityDao
import com.insangram.app.core.database.dao.CommentDao
import com.insangram.app.core.database.dao.DraftDao
import com.insangram.app.core.database.dao.MessageDao
import com.insangram.app.core.database.dao.PostDao
import com.insangram.app.core.database.dao.ReelDao
import com.insangram.app.core.database.dao.StoryDao
import com.insangram.app.core.database.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): InsangramDatabase =
        Room.databaseBuilder(context, InsangramDatabase::class.java, InsangramDatabase.NAME)
            .addMigrations(*InsangramMigrations.ALL)
            // Deliberately NOT fallbackToDestructiveMigration(): the cache also
            // holds queued offline writes that must survive an upgrade.
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()

    @Provides fun userDao(db: InsangramDatabase): UserDao = db.userDao()
    @Provides fun postDao(db: InsangramDatabase): PostDao = db.postDao()
    @Provides fun storyDao(db: InsangramDatabase): StoryDao = db.storyDao()
    @Provides fun reelDao(db: InsangramDatabase): ReelDao = db.reelDao()
    @Provides fun commentDao(db: InsangramDatabase): CommentDao = db.commentDao()
    @Provides fun messageDao(db: InsangramDatabase): MessageDao = db.messageDao()
    @Provides fun activityDao(db: InsangramDatabase): ActivityDao = db.activityDao()
    @Provides fun draftDao(db: InsangramDatabase): DraftDao = db.draftDao()
}
