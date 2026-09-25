# Building Insangram

## TL;DR - original (online) release APK

1. Open the `Insangram` folder in Android Studio (Ladybug or newer).
2. Settings > Build, Execution, Deployment > Build Tools > Gradle > **Gradle JDK = 17**.
3. Let Gradle sync.
4. Build > Generate Signed App Bundle / APK > **APK** > create a keystore.
5. Build Variant: **`onlineRelease`** > Create.

Output: `app/build/outputs/apk/online/release/app-online-release.apk`

| | demo | online |
|---|---|---|
| applicationId | `com.insangram.app.demo` | `com.insangram.app` |
| app name | Insangram Demo | Insangram |
| versionName | `1.0.0-demo` | `1.0.0` |

## Gradle wrapper

The original archive shipped without `gradle/wrapper/gradle-wrapper.jar`, which
makes `./gradlew` unusable. Rather than ship a broken wrapper, the wrapper files
were removed. Open the project in Android Studio and use its bundled Gradle; if
it offers to regenerate the wrapper, accept. Targets Gradle 8.9+ / AGP 8.7.3.

To restore it manually with a system Gradle:

```bash
gradle wrapper --gradle-version 8.9
```

## Signing

`app/build.gradle.kts` only creates a `release` signing config when a
`keystore.properties` file exists in the project root. Copy
`keystore.properties.example` to `keystore.properties` and fill it in, or just
use Android Studio's Generate Signed APK wizard (which bypasses the file
entirely).

Without either, the release APK builds **unsigned** and cannot be installed.

## Does the online flavor need google-services.json?

**No - it builds and runs without it.** The google-services plugin is applied
conditionally:

```kotlin
val hasFirebaseConfig = file("google-services.json").exists()
if (hasFirebaseConfig) { apply(plugin = ...) }
```

With no config, `onlineRelease` compiles normally and `FIREBASE_CONFIGURED`
becomes `false`. Nothing injects a Firebase type at runtime except
`FirebaseModule` itself, so the app does not crash.

If you DO add `app/google-services.json`, note that the plugin then applies to
**every** variant, including demo. Register **both** package names as Android
apps in the same Firebase project, otherwise the demo variant fails with
`No matching client found for package name 'com.insangram.app.demo'`:

- `com.insangram.app`
- `com.insangram.app.demo`

## Honest scope note

There is no `app/src/online/` source set. The `online` flavor differs from
`demo` only in applicationId, app name, versionName and three BuildConfig
flags (`ONLINE_MODE`, `FIREBASE_CONFIGURED`, `SEED_DEMO_DATA`) - and those
three flags are not read anywhere in Kotlin code.

So `onlineRelease` and `demoRelease` are **functionally identical**. Firestore
DTOs (`data/remote/dto`), `FirestorePaths`, security rules and Cloud Functions
all exist, but the layer binding them to the repository interfaces was never
written. All 11 repositories are local/Room-backed implementations.

Building `onlineRelease` gives you clean branding, not extra functionality.

## Demo credentials

Seeded on first launch by `data/local/DemoSeeder.kt` (hardcoded, present in
both flavors - it does not read the `demo_seed.json` asset):

- `aarav@insangram.example` / `Demo@12345`
- `meera@insangram.example` / `Demo@12345`
- `kabir@insangram.example` / `Demo@12345`
