# Implementation status

An honest, file-level account of what is in this archive and what is not.
Written because a partially finished project that says so is more useful than
one that claims completeness and fails on the first build.

---

## Summary

| Layer | State |
| --- | --- |
| Build configuration and version catalog | Written |
| Logo assets and launcher icons | Written and visually checked |
| Design system tokens (colour, type, shape, theme) | Written |
| Core utilities, error mapping, validators | Written |
| Room database — 31 entities, 8 DAOs, migrations | Written |
| DataStore preferences | Written |
| Domain models, 11 repository interfaces | Written |
| Use cases — feed ranking, spam, search, visibility, assistants | Written |
| Demo seed dataset — 134 media files, full JSON | Written and validated |
| Demo repositories — session, auth, user, post | Written |
| Media pipeline, ExoPlayer pool, security, sync workers | Written |
| Firestore rules, Storage rules, 21 composite indexes | Written |
| Cloud Functions — TypeScript, 24 exports | Written |
| Emulator seed script with production guard | Written |
| JVM unit tests, security-rules tests | Written |
| Documentation | Written |
| **Design-system components** | **Not written** |
| **Navigation host and bottom bar** | **Not written** |
| **Application class and MainActivity** | **Not written** |
| **Flavor DI modules** | **Not written** |
| **All Compose screens and ViewModels** | **Not written** |
| **Remaining demo repositories** | **Not written** |
| **Online Firestore repositories** | **Not written** |
| **Compose UI and Room migration tests** | **Not written** |

**The project will not compile in its current state.**

---

## Present (61 Kotlin files, 5 TypeScript files, 13 XML resources)

### Build and configuration
- `settings.gradle.kts`, root `build.gradle.kts`, `gradle.properties`
- `gradle/libs.versions.toml` — full version catalog
- `app/build.gradle.kts` — `demo`/`online` flavors, conditional google-services
  plugin, optional release signing, KSP Room schema export, JVM toolchain 17
- `app/proguard-rules.pro`, `app/google-services.json.example`, `.gitignore`
- `gradlew`, `gradlew.bat` (the wrapper **jar** is absent — see BUILD_VERIFIED.md)

### Resources
- `AndroidManifest.xml`, `strings`, `colors`, `themes`, `themes-night`, `dimens`
- `mipmap-anydpi-v26` adaptive icon XML, `ic_launcher_background.xml`
- 36 generated raster assets derived from the supplied logo across all densities:
  full logo, legacy launcher, round launcher, adaptive foreground, monochrome,
  notification silhouette, splash icon

### Kotlin — `core`
`InsangramResult`, `InsangramError`, `ErrorMapper`, `UiState`,
`DispatcherProvider`, `Validators`, `TimeFormat`, `InsangramConstants`,
`Color`, `Type`, `Shape`, `Theme`, `InsangramDestinations`,
`InsangramConverters`, `CacheEntities` (31 entities), `UserDao`, `PostDao`,
`StoryDao`, `ReelDao`, `CommentDao`, `MessageDao`, `ActivityDao`, `DraftDao`,
`InsangramDatabase`, `InsangramMigrations`, `DatabaseModule`,
`InsangramPreferences`, `FirebaseModule`, `FirestorePaths`,
`InsangramMessagingService`, `ConnectivityObserver`, `SyncScheduler`,
`UploadWorker`, `PendingActionWorker`, `MaintenanceWorker`, `PasswordHasher`,
`AppLockManager`, `MediaProcessor`, `ReelPlayerPool`, `InsangramTestRunner`

### Kotlin — `domain`
`Models`, `Repositories` (11 interfaces), `FeedRankingUseCase`, `SpamDetector`,
`ContentAssistants`, `SearchRanker`, `VisibilityPolicy`

### Kotlin — `data`
`Mappers`, `FirestoreDtos`, `DtoMappers`, `DemoSeedModels`, `DemoSeeder`,
`DemoSession`, `DemoAuthRepository`, `DemoUserRepository`, `PostAssembler`,
`DemoPostRepository`

### Backend
- `firebase/firestore.rules` — deny by default, per-collection rules
- `firebase/storage.rules` — auth, ownership, membership, size and type checks
- `firebase/firestore.indexes.json` — 21 composite indexes
- `firebase.json`, `.firebaserc.example`
- `functions/src/common.ts` — idempotency, notifications, FCM, rate limiting
- `functions/src/counters.ts` — trusted counters, search field derivation
- `functions/src/social.ts` — follow graph, username reservation, messages
- `functions/src/maintenance.ts` — scheduled cleanup, account deletion

### Demo data
`app/src/demo/assets/demo_seed.json` plus 134 procedurally generated images:
24 users, 56 posts, 22 reels, 18 stories, 191 comments, 180 follows,
8 conversations, 56 messages, 26 notifications, 31 hashtags, 3 collections,
14 saved posts, 6 notes, analytics snapshot.

### Tests
`ValidatorsTest`, `FeedRankingUseCaseTest`, `SpamDetectorTest`,
`PasswordHasherTest`, `SearchRankerTest`, `VisibilityPolicyTest`,
`functions/test/rules.test.ts`

---

## Absent

### Blocking the build
1. `InsangramApp.kt` — `@HiltAndroidApp`, WorkManager configuration,
   notification channel registration
2. `MainActivity.kt` — splash screen, edge-to-edge, theme collection; must
   extend a `FragmentActivity` subclass for `BiometricPrompt`
3. `core/navigation/InsangramNavHost.kt`, `InsangramBottomBar.kt`,
   `RootScaffold.kt`
4. `app/src/demo/java/.../DemoModeModule.kt` and
   `app/src/online/java/.../OnlineModeModule.kt` — the Hilt bindings that make
   flavor switching work

### Presentation layer
Every `feature/*` package: splash, onboarding, the nine auth screens, home feed,
story viewer and creator, create, media editor, explore, search, reels, profile,
comments, saved, the seven messaging screens, notes, notifications, analytics
and the twelve settings sections — screens and ViewModels alike.

### Design system components
Buttons, inputs, cards, dialogs, bottom sheets, snackbars, skeleton shimmer,
empty state, error state, avatar, gradient story ring, chips.

### Data layer
`DemoReelRepository`, `DemoStoryRepository`, `DemoCommentRepository`,
`DemoMessageRepository`, `DemoNotificationRepository`, `DemoSearchRepository`,
`DemoModerationRepository`, `DemoAnalyticsRepository`, and the entire set of
online Firestore-backed implementations.

### Tests
Compose navigation and UI tests, Room migration tests, repository integration
tests, ViewModel state tests.

---

## Known defects in the code that *is* present

Found by reading, not by compiling. Fix these first.

### Sync workers call DAO methods that do not exist

| Caller | Wrong call | Actual DAO API |
| --- | --- | --- |
| `UploadWorker` | `draftDao.uploadById(id)` | `upload(id)` |
| `UploadWorker` | `updateUploadState(id, state, error)` | `updateUploadState(uploadId, state, progress, error, now)` |
| `UploadWorker` | `updateUploadProgress(...)` | does not exist — fold into `updateUploadState` |
| `PendingActionWorker` | `nextPendingAction()` | `pendingActions(1).firstOrNull()` |
| `PendingActionWorker` | `deletePendingAction(id)` | `deleteAction(id)` |
| `PendingActionWorker` | `markPendingActionFailed(...)` | `updateActionState(actionId, state, error)` |
| `PendingActionWorker` | `action.actionType` | field is `type` |
| `MaintenanceWorker` | `storyDao.deleteExpired(now)` | `archiveExpired(now)` |
| `MaintenanceWorker` | `postDao.purgeRecentlyDeletedBefore(t)` | `purgeExpiredDeletions(t)` |
| `MaintenanceWorker` | `postDao.deleteStaleCache(t)` | `evictStale(threshold, keepAuthorId)` |
| `MaintenanceWorker` | `activityDao.trimNotifications(n)` | does not exist — add it |
| `MaintenanceWorker` | `draftDao.totalMediaCacheBytes()` | `totalCacheBytes()` |
| `MaintenanceWorker` | `leastRecentlyUsedMedia(200)` | `leastRecentlyUsed(200)` |
| `MaintenanceWorker` | `deleteMediaCacheEntry(key)` | does not exist — only `clearMediaCache()` |

### Repository assumptions that need DAO additions

`PostAssembler` calls `postDao.likedContentIdsFor`, `postDao.savedPostIdsFor`
and `commentDao.previewFor`; the DAOs currently expose the `observe…` Flow
variants instead. `DemoPostRepository` calls `postDao.rankingCandidates`,
`isSavedNow`, `mediaHashExists` and `collection`, none of which exist yet.

### Suspend-in-lambda errors

`DemoUserRepository.observeRelationship`, `observeFollowers`,
`observeCloseFriends` and `observeMutedUsers` call suspend DAO functions inside
non-suspend `combine`/`map` transforms. `DemoAuthRepository.knownAccounts` does
the same with `withContext`. Refactor each to a `flow { }` builder with
`emitAll`, or switch to `flatMapLatest`.

`DemoPostRepository` also uses `emitAll` without importing
`kotlinx.coroutines.flow.emitAll`, and `pagedPosts` wraps a `runBlocking` inside
the `Pager` factory, which blocks a thread and should be restructured.

### Other

- `MediaProcessor.buildColorMatrix` contains the literal `1f.let { 0f },` where
  `0f` was intended.
- `MessageDao.observeTotalUnread` embeds `'%"' || :viewerId || '"%'` inside a
  Kotlin raw string; verify the escaping survives.
- Missing catalog dependencies: `androidx.exifinterface:exifinterface`,
  `androidx.media3:media3-database`, `androidx.media3:media3-datasource`,
  `androidx.fragment:fragment-ktx`.
- Missing constants and members referenced by existing code, including several
  `InsangramConstants` entries, `InsangramError` variants and `Validators`
  helpers. The compiler will enumerate them precisely on the first run.
- `FirebaseModule` mixes KTX and non-KTX Firestore namespaces; confirm against
  Firebase BOM 33.7.0, which deprecates the KTX artifacts.

---

## Suggested order of work

1. Fix the DAO mismatches above — cheap, mechanical, and they block everything.
2. Add `InsangramApp`, `MainActivity`, the nav host and the two flavor modules.
   At that point `assembleDemoDebug` should reach the Compose layer.
3. Build the design-system components, then screens in dependency order:
   splash → auth → home feed → profile → comments → the rest.
4. Add the remaining demo repositories as each screen needs them.
5. Mirror them with Firestore implementations for the online flavor.
6. Run `./scripts/verify_build.sh` and let it overwrite `BUILD_VERIFIED.md`.
