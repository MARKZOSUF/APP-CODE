package com.insangram.app.di

import com.insangram.app.BuildConfig
import com.insangram.app.data.repository.DemoCommentRepository
import com.insangram.app.data.repository.DemoMessageRepository
import com.insangram.app.data.repository.DemoNotificationRepository
import com.insangram.app.data.repository.DemoPostRepository
import com.insangram.app.data.repository.DemoStoryRepository
import com.insangram.app.data.repository.DemoUserRepository
import com.insangram.app.data.repository.FirebaseCommentRepository
import com.insangram.app.data.repository.FirebaseMessageRepository
import com.insangram.app.data.repository.FirebaseNotificationRepository
import com.insangram.app.data.repository.FirebasePostRepository
import com.insangram.app.data.repository.FirebaseStoryRepository
import com.insangram.app.data.repository.FirebaseUserRepository
import com.insangram.app.domain.repository.CommentRepository
import com.insangram.app.domain.repository.MessageRepository
import com.insangram.app.domain.repository.NotificationRepository
import com.insangram.app.domain.repository.PostRepository
import com.insangram.app.domain.repository.StoryRepository
import com.insangram.app.domain.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Picks the online (Firebase) or on-device (demo) implementation for every
 * repository that has both.
 *
 * The demo flavour never touches Firebase, and the online flavour falls back
 * to the demo data automatically when `google-services.json` is missing, so a
 * build always runs either way.
 *
 * Both sides are injected as [Provider]s so only the selected implementation
 * is ever constructed.
 */
@Module
@InstallIn(SingletonComponent::class)
object OnlineRepositoryModule {

    private val useFirebase: Boolean
        get() = BuildConfig.ONLINE_MODE && BuildConfig.FIREBASE_CONFIGURED

    @Provides
    @Singleton
    fun provideUserRepository(
        demo: Provider<DemoUserRepository>,
        online: Provider<FirebaseUserRepository>,
    ): UserRepository = if (useFirebase) online.get() else demo.get()

    @Provides
    @Singleton
    fun providePostRepository(
        demo: Provider<DemoPostRepository>,
        online: Provider<FirebasePostRepository>,
    ): PostRepository = if (useFirebase) online.get() else demo.get()

    @Provides
    @Singleton
    fun provideStoryRepository(
        demo: Provider<DemoStoryRepository>,
        online: Provider<FirebaseStoryRepository>,
    ): StoryRepository = if (useFirebase) online.get() else demo.get()

    @Provides
    @Singleton
    fun provideCommentRepository(
        demo: Provider<DemoCommentRepository>,
        online: Provider<FirebaseCommentRepository>,
    ): CommentRepository = if (useFirebase) online.get() else demo.get()

    @Provides
    @Singleton
    fun provideMessageRepository(
        demo: Provider<DemoMessageRepository>,
        online: Provider<FirebaseMessageRepository>,
    ): MessageRepository = if (useFirebase) online.get() else demo.get()

    @Provides
    @Singleton
    fun provideNotificationRepository(
        demo: Provider<DemoNotificationRepository>,
        online: Provider<FirebaseNotificationRepository>,
    ): NotificationRepository = if (useFirebase) online.get() else demo.get()
}
