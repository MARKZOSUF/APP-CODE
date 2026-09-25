package com.insangram.app.di

import com.insangram.app.data.repository.DemoAnalyticsRepository
import com.insangram.app.data.repository.DemoModerationRepository
import com.insangram.app.data.repository.DemoReelRepository
import com.insangram.app.data.repository.DemoSearchRepository
import com.insangram.app.domain.repository.AnalyticsRepository
import com.insangram.app.domain.repository.ModerationRepository
import com.insangram.app.domain.repository.ReelRepository
import com.insangram.app.domain.repository.SearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds every repository interface to its implementation.
 *
 * Without these bindings Hilt fails the build with "cannot be provided" errors
 * wherever a repository is injected (workers, view models, services).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // User, Post, Story, Comment, Message and Notification are provided by
    // OnlineRepositoryModule, which switches between the Firebase and demo
    // implementations at runtime.

    @Binds
    @Singleton
    abstract fun bindReelRepository(impl: DemoReelRepository): ReelRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: DemoSearchRepository): SearchRepository

    @Binds
    @Singleton
    abstract fun bindModerationRepository(impl: DemoModerationRepository): ModerationRepository

    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(impl: DemoAnalyticsRepository): AnalyticsRepository
}
