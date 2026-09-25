# Build verification report

## Status: NOT VERIFIED

**The Gradle build was not executed. No APK was produced.** This file exists to
record that honestly, as the build instructions require, rather than to claim a
result that did not happen.

---

## Why

The environment this project was authored in is a Linux sandbox intended for
file generation and scripting. It is missing every tool required to compile an
Android application:

| Requirement | Present | Detail |
| --- | --- | --- |
| Android SDK | **No** | `ANDROID_HOME` unset; `/opt/android*` and `~/Android` do not exist |
| Gradle | **No** | not on `PATH` |
| Kotlin compiler | **No** | `kotlinc` not on `PATH` |
| Java compiler | **No** | JRE only — `java` present, `javac` absent |
| Java runtime | Yes | OpenJDK 25.0.4 (Amazon Corretto) — note this is *not* the JDK 17 the build needs |
| Network access | **No** | `curl https://services.gradle.org` → `Could not resolve host` |

Because there is no network, `gradle/wrapper/gradle-wrapper.jar` could not be
downloaded either, so even `./gradlew --version` cannot run from this archive
until the wrapper binary is restored.

---

## Commands that were NOT run

Each of the following was required by the build instructions and none of them
was executed. No output from them is reported anywhere in this project.

```bash
chmod +x gradlew
./gradlew --stop
./gradlew clean
./gradlew :app:assembleDemoDebug
./gradlew :app:testDemoDebugUnitTest
./gradlew :app:lintDemoDebug
./gradlew :app:assembleOnlineDebug
```

**Build result:** not run
**Unit test result:** not run
**Lint result:** not run
**APK output path:** none — no APK was produced

---

## What *was* verified

These checks were genuinely executed in the authoring environment and passed:

| Check | Tool | Result |
| --- | --- | --- |
| `firestore.indexes.json` is valid JSON | `jq` | pass |
| `firebase.json` is valid JSON | `jq` | pass |
| `demo_seed.json` is valid JSON | `jq` | pass |
| No `allow …: if true;` in either ruleset | `grep -E` | pass — zero matches |
| `scripts/seed_emulator.js` parses | `node --check` | pass |
| `scripts/verify_build.sh` parses | `bash -n` | pass |
| `gradlew` parses | `sh -n` | pass |
| Launcher icons render legibly at 48–192 px | visual inspection of a generated contact sheet | pass |

No Kotlin file has been compiled, and no XML resource has been processed by
AAPT. Treat every `.kt` and `.xml` file in this archive as **unverified**.

---

## Known issues you will hit on the first build

These were found by reading the source, not by compiling it, so the list is
almost certainly incomplete. `docs/STATUS.md` has the full account.

1. **The presentation layer does not exist yet.** There is no `MainActivity`,
   no `Application` class, no navigation host and no Compose screens. The build
   will fail at the manifest, which references components that are not present.
2. **Flavor DI modules are missing.** `DemoModeModule` and `OnlineModeModule`
   are referenced by the architecture but not written, so Hilt has no binding
   for the repository interfaces.
3. **DAO method mismatches in the sync workers.** `UploadWorker`,
   `PendingActionWorker` and `MaintenanceWorker` call several DAO methods that
   do not exist under those names, listed individually in `docs/STATUS.md`.
4. **`PostAssembler` and `DemoPostRepository` assume DAO methods** such as
   `likedContentIdsFor`, `savedPostIdsFor`, `previewFor` and `rankingCandidates`
   that need to be added to the DAOs or renamed at the call sites.
5. **Suspend calls inside non-suspend lambdas** in `DemoUserRepository` and
   `DemoAuthRepository` will not compile as written.
6. `MediaProcessor.buildColorMatrix` contains a nonsense literal
   (`1f.let { 0f },`) that should simply be `0f`.
7. Several version-catalog entries still need adding: `androidx.exifinterface`,
   `media3-database`, `media3-datasource` and `androidx.fragment`.

---

## How to produce a real report

On a machine with the Android SDK and JDK 17:

```bash
# 1. restore the wrapper binary
gradle wrapper --gradle-version 8.9      # or open the project in Android Studio

# 2. run the full sequence and overwrite this file with real results
./scripts/verify_build.sh
```

`scripts/verify_build.sh` records the exact commands, the environment, the
pass/fail state of each step and the APK path, appending real log excerpts for
any failure. It overwrites this file with genuine output.
