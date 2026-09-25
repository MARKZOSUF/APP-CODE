# Insangram R8 / ProGuard configuration

# Kotlin metadata & coroutines
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# kotlinx.serialization
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault
-keepclassmembers class com.insangram.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.insangram.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Firestore uses reflection to bind DTOs. Keep the remote DTO package intact.
-keep class com.insangram.app.data.remote.dto.** { *; }
-keepclassmembers class com.insangram.app.data.remote.dto.** {
    <init>();
    <fields>;
}
-keepnames class com.google.firebase.firestore.** { *; }
-dontwarn com.google.firebase.**

# Room generated implementations
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# Hilt / Dagger
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }

# Never keep debug logging in release; Timber-free project uses android.util.Log
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

# Enums are reflected over by Room TypeConverters and kotlinx.serialization.
# Without this, valueOf() throws at runtime only in minified release builds.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Room entities are bound field-by-field by generated code.
-keep class com.insangram.app.core.database.entity.** { *; }

# Domain models are used by serialization and reflection-free mapping; keeping
# their members avoids surprises when R8 is aggressive about data classes.
-keepclassmembers class com.insangram.app.domain.model.** { <fields>; }

# Hilt generates a _HiltModules class per @Module and workers per @HiltWorker.
-keep class * extends androidx.work.ListenableWorker { <init>(...); }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { <init>(...); }

# Compose Navigation reflects over composable destination arguments.
-dontwarn androidx.navigation.**

# ---------------------------------------------------------------------------
# Release-UI hardening. The screens are Compose-only and read their content
# through immutable data classes; R8 must not rewrite their constructors away.
# ---------------------------------------------------------------------------

# ViewModels are instantiated reflectively by the Lifecycle factory.
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keepclassmembers class * extends androidx.lifecycle.ViewModel { <init>(...); }

# Coil loads the network images used across the feed, reels, explore grid and
# avatars. Its OkHttp/Okio backend ships consumer rules, but the -dontwarn
# entries keep aggressive full-mode R8 from failing on optional platform APIs.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn coil.**
-keep class coil.** { *; }

# Demo/UI content holders are plain data classes read by the composables.
-keep class com.insangram.app.feature.ui.** { *; }

# CameraX and Media3 (reels playback + reels camera) resolve implementations by
# name at runtime.
-dontwarn androidx.camera.**
-keep class androidx.camera.core.impl.** { *; }
-dontwarn androidx.media3.**
-keep class androidx.media3.exoplayer.** { *; }

# DataStore preferences are serialized through generated protobuf classes.
-keep class androidx.datastore.*.** { *; }
-dontwarn com.google.protobuf.**

# WorkManager resolves workers by class name from the database.
-keep class androidx.work.impl.** { *; }

# Keep annotation-driven Compose runtime metadata used for stability inference.
-dontwarn androidx.compose.**
