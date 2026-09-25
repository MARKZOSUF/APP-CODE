package com.insangram.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.insangram.app.core.database.dao.ActivityDao
import com.insangram.app.core.database.dao.CommentDao
import com.insangram.app.core.database.dao.DraftDao
import com.insangram.app.core.database.dao.MessageDao
import com.insangram.app.core.database.dao.PostDao
import com.insangram.app.core.database.dao.ReelDao
import com.insangram.app.core.database.dao.StoryDao
import com.insangram.app.core.database.dao.UserDao
import com.insangram.app.core.database.entity.AnalyticsSnapshot
import com.insangram.app.core.database.entity.BlockEdge
import com.insangram.app.core.database.entity.CachedComment
import com.insangram.app.core.database.entity.CachedConversation
import com.insangram.app.core.database.entity.CachedMessage
import com.insangram.app.core.database.entity.CachedNote
import com.insangram.app.core.database.entity.CachedNotification
import com.insangram.app.core.database.entity.CachedPost
import com.insangram.app.core.database.entity.CachedPostMedia
import com.insangram.app.core.database.entity.CachedReel
import com.insangram.app.core.database.entity.CachedStory
import com.insangram.app.core.database.entity.CachedUser
import com.insangram.app.core.database.entity.ContentReportEntity
import com.insangram.app.core.database.entity.DemoAccount
import com.insangram.app.core.database.entity.FeedRemoteKey
import com.insangram.app.core.database.entity.FollowEdge
import com.insangram.app.core.database.entity.HashtagEntity
import com.insangram.app.core.database.entity.InterestSignalEntity
import com.insangram.app.core.database.entity.LikeEntity
import com.insangram.app.core.database.entity.LocalDraft
import com.insangram.app.core.database.entity.MediaCacheEntry
import com.insangram.app.core.database.entity.NegativeSignalEntity
import com.insangram.app.core.database.entity.PendingAction
import com.insangram.app.core.database.entity.PendingUpload
import com.insangram.app.core.database.entity.ReelRemoteKey
import com.insangram.app.core.database.entity.SavedCollectionEntity
import com.insangram.app.core.database.entity.SavedPostEntity
import com.insangram.app.core.database.entity.SearchHistoryEntity
import com.insangram.app.core.database.entity.StoryViewEntity
import com.insangram.app.core.database.entity.SyncMetadata
import com.insangram.app.core.database.entity.WatchTimeEntity

/**
 * The single Room database. Schemas are exported to app/schemas so migrations
 * can be tested with MigrationTestHelper (see InsangramMigrationTest).
 *
 * No authentication token, Firebase password, or reversible credential is ever
 * stored here. The only credential-adjacent table is [DemoAccount], which
 * holds a PBKDF2 hash plus salt for offline demo sign-in.
 */
@Database(
    entities = [
        CachedUser::class,
        CachedPost::class,
        CachedPostMedia::class,
        CachedStory::class,
        CachedReel::class,
        CachedComment::class,
        CachedConversation::class,
        CachedMessage::class,
        CachedNotification::class,
        CachedNote::class,
        FollowEdge::class,
        BlockEdge::class,
        SavedPostEntity::class,
        SavedCollectionEntity::class,
        LikeEntity::class,
        StoryViewEntity::class,
        HashtagEntity::class,
        SearchHistoryEntity::class,
        FeedRemoteKey::class,
        ReelRemoteKey::class,
        LocalDraft::class,
        PendingUpload::class,
        PendingAction::class,
        MediaCacheEntry::class,
        SyncMetadata::class,
        AnalyticsSnapshot::class,
        DemoAccount::class,
        ContentReportEntity::class,
        NegativeSignalEntity::class,
        InterestSignalEntity::class,
        WatchTimeEntity::class,
    ],
    version = InsangramDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(InsangramConverters::class)
abstract class InsangramDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao
    abstract fun storyDao(): StoryDao
    abstract fun reelDao(): ReelDao
    abstract fun commentDao(): CommentDao
    abstract fun messageDao(): MessageDao
    abstract fun activityDao(): ActivityDao
    abstract fun draftDao(): DraftDao

    companion object {
        const val VERSION = 2
        const val NAME = "insangram.db"
    }
}
