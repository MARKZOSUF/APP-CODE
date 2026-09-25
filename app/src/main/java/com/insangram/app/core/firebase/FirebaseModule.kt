package com.insangram.app.core.firebase

import com.google.firebase.Firebase
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.messaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import com.insangram.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Firebase singletons.
 *
 * Every provider here is only reachable from the online flavor's dependency
 * graph. Demo builds never resolve these bindings, so a missing
 * google-services.json cannot crash the app.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun auth(): FirebaseAuth = Firebase.auth.also { auth ->
        if (BuildConfig.USE_FIREBASE_EMULATOR) {
            auth.useEmulator(BuildConfig.EMULATOR_HOST, 9099)
        }
    }

    @Provides
    @Singleton
    fun firestore(): FirebaseFirestore = Firebase.firestore.also { store ->
        if (BuildConfig.USE_FIREBASE_EMULATOR) {
            store.useEmulator(BuildConfig.EMULATOR_HOST, 8080)
        }
        store.firestoreSettings = firestoreSettings {
            // Firestore's own cache complements the Room cache: Room powers the
            // Paging sources, Firestore's cache serves single-document reads.
            setLocalCacheSettings(
                if (BuildConfig.USE_FIREBASE_EMULATOR) {
                    memoryCacheSettings { }
                } else {
                    persistentCacheSettings { }
                },
            )
        }
    }

    @Provides
    @Singleton
    fun storage(): FirebaseStorage = Firebase.storage.also { storage ->
        if (BuildConfig.USE_FIREBASE_EMULATOR) {
            storage.useEmulator(BuildConfig.EMULATOR_HOST, 9199)
        }
    }

    @Provides
    @Singleton
    fun functions(): FirebaseFunctions = Firebase.functions.also { functions ->
        if (BuildConfig.USE_FIREBASE_EMULATOR) {
            functions.useEmulator(BuildConfig.EMULATOR_HOST, 5001)
        }
    }

    @Provides
    @Singleton
    fun messaging(): FirebaseMessaging = Firebase.messaging

    @Provides
    @Singleton
    fun appCheck(): FirebaseAppCheck = FirebaseAppCheck.getInstance().apply {
        // Play Integrity is used in every variant. The debug provider lives in
        // the debug-only artifact, so it is resolved reflectively when present
        // instead of being referenced directly from this shared source set.
        val factory = if (BuildConfig.DEBUG) debugFactoryOrNull() else null
        installAppCheckProviderFactory(
            factory ?: PlayIntegrityAppCheckProviderFactory.getInstance(),
        )
    }

    private fun debugFactoryOrNull(): AppCheckProviderFactory? = runCatching {
        val type = Class.forName(
            "com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory",
        )
        type.getMethod("getInstance").invoke(null) as AppCheckProviderFactory
    }.getOrNull()
}
