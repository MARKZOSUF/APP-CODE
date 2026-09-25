package com.insangram.app.di

import com.insangram.app.BuildConfig
import com.insangram.app.data.repository.DemoAuthRepository
import com.insangram.app.data.repository.FirebaseAuthRepository
import com.insangram.app.domain.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Chooses the authentication backend at runtime.
 *
 * The online flavor signs people in through Firebase Auth; the demo flavor
 * keeps using the local PBKDF2 accounts so it still builds and runs with no
 * google-services.json present.
 *
 * Both implementations are injected as [Provider]s on purpose: only the one
 * that is actually selected gets constructed, so a demo build never touches
 * the Firebase singletons.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthRepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        demo: Provider<DemoAuthRepository>,
        firebase: Provider<FirebaseAuthRepository>,
    ): AuthRepository =
        if (BuildConfig.ONLINE_MODE && BuildConfig.FIREBASE_CONFIGURED) {
            firebase.get()
        } else {
            demo.get()
        }
}
