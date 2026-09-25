package com.insangram.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.insangram.app.core.designsystem.theme.ThemeMode
import com.insangram.app.domain.model.AppLockMode
import com.insangram.app.domain.model.AudienceScope
import com.insangram.app.domain.model.AutoplayMode
import com.insangram.app.domain.model.InsangramSettings
import com.insangram.app.domain.model.UploadQuality
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "insangram_settings",
)

/**
 * All persisted user preferences. Session tokens are never stored here -
 * Firebase owns the session, and the demo flavor stores only a user id.
 */
@Singleton
class InsangramPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.settingsDataStore

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")

        val UPLOAD_QUALITY = stringPreferencesKey("upload_quality")
        val DATA_SAVER = booleanPreferencesKey("data_saver")
        val AUTOPLAY = stringPreferencesKey("autoplay")

        val PRIVATE_ACCOUNT = booleanPreferencesKey("private_account")
        val COMMENT_AUDIENCE = stringPreferencesKey("comment_audience")
        val MESSAGE_AUDIENCE = stringPreferencesKey("message_audience")
        val STORY_AUDIENCE = stringPreferencesKey("story_audience")
        val MENTION_AUDIENCE = stringPreferencesKey("mention_audience")
        val TAG_AUDIENCE = stringPreferencesKey("tag_audience")
        val ACTIVITY_STATUS = booleanPreferencesKey("activity_status")
        val READ_RECEIPTS = booleanPreferencesKey("read_receipts")
        val DISCOVERABLE = booleanPreferencesKey("discoverable")
        val NOTIFICATION_PREVIEWS = booleanPreferencesKey("notification_previews")

        val APP_LOCK_MODE = stringPreferencesKey("app_lock_mode")
        val APP_LOCK_TIMEOUT_MS = longPreferencesKey("app_lock_timeout_ms")

        val PUSH_LIKES = booleanPreferencesKey("push_likes")
        val PUSH_COMMENTS = booleanPreferencesKey("push_comments")
        val PUSH_FOLLOWS = booleanPreferencesKey("push_follows")
        val PUSH_MESSAGES = booleanPreferencesKey("push_messages")
        val PUSH_MENTIONS = booleanPreferencesKey("push_mentions")

        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val TERMS_ACCEPTED_AT = longPreferencesKey("terms_accepted_at")
        val DEMO_USER_ID = stringPreferencesKey("demo_user_id")
        val DEMO_SEEDED_VERSION = intPreferencesKey("demo_seeded_version")
        val LAST_FEED_SYNC = longPreferencesKey("last_feed_sync")
        val CHRONOLOGICAL_FEED = booleanPreferencesKey("chronological_feed")
    }

    /** Corrupt-preference recovery: fall back to defaults instead of crashing. */
    private val preferences: Flow<Preferences> = dataStore.data.catch { throwable ->
        if (throwable is IOException) emit(emptyPreferences()) else throw throwable
    }

    val settings: Flow<InsangramSettings> = preferences.map { prefs ->
        InsangramSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            highContrast = prefs[Keys.HIGH_CONTRAST] ?: false,
            languageTag = prefs[Keys.LANGUAGE_TAG] ?: "",
            uploadQuality = prefs[Keys.UPLOAD_QUALITY]?.let {
                runCatching { UploadQuality.valueOf(it) }.getOrNull()
            } ?: UploadQuality.STANDARD,
            dataSaver = prefs[Keys.DATA_SAVER] ?: false,
            autoplayMode = prefs[Keys.AUTOPLAY]?.let {
                runCatching { AutoplayMode.valueOf(it) }.getOrNull()
            } ?: AutoplayMode.WIFI_ONLY,
            privateAccount = prefs[Keys.PRIVATE_ACCOUNT] ?: false,
            commentAudience = audience(prefs[Keys.COMMENT_AUDIENCE], AudienceScope.EVERYONE),
            messageAudience = audience(prefs[Keys.MESSAGE_AUDIENCE], AudienceScope.FOLLOWERS),
            storyAudience = audience(prefs[Keys.STORY_AUDIENCE], AudienceScope.FOLLOWERS),
            mentionAudience = audience(prefs[Keys.MENTION_AUDIENCE], AudienceScope.EVERYONE),
            tagAudience = audience(prefs[Keys.TAG_AUDIENCE], AudienceScope.EVERYONE),
            showActivityStatus = prefs[Keys.ACTIVITY_STATUS] ?: true,
            sendReadReceipts = prefs[Keys.READ_RECEIPTS] ?: true,
            discoverable = prefs[Keys.DISCOVERABLE] ?: true,
            showNotificationPreviews = prefs[Keys.NOTIFICATION_PREVIEWS] ?: true,
            appLockMode = prefs[Keys.APP_LOCK_MODE]?.let {
                runCatching { AppLockMode.valueOf(it) }.getOrNull()
            } ?: AppLockMode.OFF,
            appLockTimeoutMs = prefs[Keys.APP_LOCK_TIMEOUT_MS] ?: 60_000L,
            pushLikes = prefs[Keys.PUSH_LIKES] ?: true,
            pushComments = prefs[Keys.PUSH_COMMENTS] ?: true,
            pushFollows = prefs[Keys.PUSH_FOLLOWS] ?: true,
            pushMessages = prefs[Keys.PUSH_MESSAGES] ?: true,
            pushMentions = prefs[Keys.PUSH_MENTIONS] ?: true,
            chronologicalFeed = prefs[Keys.CHRONOLOGICAL_FEED] ?: false,
        )
    }

    val onboardingComplete: Flow<Boolean> = preferences.map { it[Keys.ONBOARDING_COMPLETE] ?: false }
    val demoUserId: Flow<String?> = preferences.map { it[Keys.DEMO_USER_ID] }
    val demoSeededVersion: Flow<Int> = preferences.map { it[Keys.DEMO_SEEDED_VERSION] ?: 0 }
    val lastFeedSync: Flow<Long> = preferences.map { it[Keys.LAST_FEED_SYNC] ?: 0L }

    suspend fun setThemeMode(mode: ThemeMode) = put(Keys.THEME_MODE, mode.name)
    suspend fun setDynamicColor(enabled: Boolean) = put(Keys.DYNAMIC_COLOR, enabled)
    suspend fun setReducedMotion(enabled: Boolean) = put(Keys.REDUCED_MOTION, enabled)
    suspend fun setHighContrast(enabled: Boolean) = put(Keys.HIGH_CONTRAST, enabled)
    suspend fun setLanguageTag(tag: String) = put(Keys.LANGUAGE_TAG, tag)
    suspend fun setUploadQuality(quality: UploadQuality) = put(Keys.UPLOAD_QUALITY, quality.name)
    suspend fun setDataSaver(enabled: Boolean) = put(Keys.DATA_SAVER, enabled)
    suspend fun setAutoplayMode(mode: AutoplayMode) = put(Keys.AUTOPLAY, mode.name)
    suspend fun setPrivateAccount(enabled: Boolean) = put(Keys.PRIVATE_ACCOUNT, enabled)
    suspend fun setCommentAudience(scope: AudienceScope) = put(Keys.COMMENT_AUDIENCE, scope.name)
    suspend fun setMessageAudience(scope: AudienceScope) = put(Keys.MESSAGE_AUDIENCE, scope.name)
    suspend fun setStoryAudience(scope: AudienceScope) = put(Keys.STORY_AUDIENCE, scope.name)
    suspend fun setMentionAudience(scope: AudienceScope) = put(Keys.MENTION_AUDIENCE, scope.name)
    suspend fun setTagAudience(scope: AudienceScope) = put(Keys.TAG_AUDIENCE, scope.name)
    suspend fun setActivityStatus(enabled: Boolean) = put(Keys.ACTIVITY_STATUS, enabled)
    suspend fun setReadReceipts(enabled: Boolean) = put(Keys.READ_RECEIPTS, enabled)
    suspend fun setDiscoverable(enabled: Boolean) = put(Keys.DISCOVERABLE, enabled)
    suspend fun setNotificationPreviews(enabled: Boolean) = put(Keys.NOTIFICATION_PREVIEWS, enabled)
    suspend fun setAppLockMode(mode: AppLockMode) = put(Keys.APP_LOCK_MODE, mode.name)
    suspend fun setAppLockTimeout(ms: Long) = put(Keys.APP_LOCK_TIMEOUT_MS, ms)
    suspend fun setPushLikes(enabled: Boolean) = put(Keys.PUSH_LIKES, enabled)
    suspend fun setPushComments(enabled: Boolean) = put(Keys.PUSH_COMMENTS, enabled)
    suspend fun setPushFollows(enabled: Boolean) = put(Keys.PUSH_FOLLOWS, enabled)
    suspend fun setPushMessages(enabled: Boolean) = put(Keys.PUSH_MESSAGES, enabled)
    suspend fun setPushMentions(enabled: Boolean) = put(Keys.PUSH_MENTIONS, enabled)
    suspend fun setChronologicalFeed(enabled: Boolean) = put(Keys.CHRONOLOGICAL_FEED, enabled)
    suspend fun setOnboardingComplete(complete: Boolean) = put(Keys.ONBOARDING_COMPLETE, complete)
    suspend fun setTermsAcceptedAt(timestamp: Long) = put(Keys.TERMS_ACCEPTED_AT, timestamp)
    suspend fun setDemoSeededVersion(version: Int) = put(Keys.DEMO_SEEDED_VERSION, version)
    suspend fun setLastFeedSync(timestamp: Long) = put(Keys.LAST_FEED_SYNC, timestamp)

    suspend fun setDemoUserId(userId: String?) {
        dataStore.edit { prefs ->
            if (userId == null) prefs.remove(Keys.DEMO_USER_ID) else prefs[Keys.DEMO_USER_ID] = userId
        }
    }

    /** Used by Settings > Delete account and by logout in demo mode. */
    suspend fun clearSessionScopedPreferences() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.DEMO_USER_ID)
            prefs.remove(Keys.LAST_FEED_SYNC)
        }
    }

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    private fun audience(raw: String?, fallback: AudienceScope) =
        raw?.let { runCatching { AudienceScope.valueOf(it) }.getOrNull() } ?: fallback
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun preferences(@ApplicationContext context: Context) = InsangramPreferences(context)
}
