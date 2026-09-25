# Architecture

## The shape

```
┌─────────────────────────────────────────────────────────────┐
│  feature/*        Compose screens + ViewModels               │
│                   immutable StateFlow UI state               │
└───────────────────────────┬─────────────────────────────────┘
                            │ depends on
┌───────────────────────────▼─────────────────────────────────┐
│  domain/          models · repository interfaces · use cases │
│                   pure Kotlin, no Android imports            │
└───────────────────────────▲─────────────────────────────────┘
                            │ implements
┌───────────────────────────┴─────────────────────────────────┐
│  data/            repositories · mappers · DTOs · seeding    │
└───────────────────────────┬─────────────────────────────────┘
                            │ uses
┌───────────────────────────▼─────────────────────────────────┐
│  core/            database · datastore · firebase · media    │
│                   security · sync · designsystem · navigation│
└─────────────────────────────────────────────────────────────┘
```

Dependencies point inward. `domain` is the stable centre and knows nothing
about Android, Room or Firebase, which is precisely why the ranking, spam,
search and visibility logic can be unit tested on the JVM in milliseconds.

## Why one Gradle module

The target structure lists roughly thirty Gradle modules. This project uses one
module with the same package boundaries. The trade-off:

- **Gained:** dramatically faster clean builds and configuration time, no
  cross-module API plumbing, far simpler Hilt setup for a student project.
- **Lost:** the compiler cannot mechanically prevent a Composable from importing
  a DAO. That rule is enforced by review and by the package layout instead.

To split later, each `core/*`, `data`, `domain` and `feature/*` package moves to
`:core:x`, `:data`, `:domain`, `:feature:x` unchanged. No code needs rewriting
because nothing imports across a layer boundary today; only `settings.gradle.kts`
and the `build.gradle.kts` files change.

## State management

Every ViewModel exposes exactly one immutable state object:

```kotlin
private val _state = MutableStateFlow(HomeUiState())
val state: StateFlow<HomeUiState> = _state.asStateFlow()
```

Screens collect with `collectAsStateWithLifecycle()`, so collection stops when
the screen is not started. That matters most for Firestore snapshot listeners:
the listener is bound to the flow, so a backgrounded screen stops billing reads.

Every UI state models four cases explicitly — loading, success, empty, error —
rather than inferring emptiness from a null.

## Optimistic updates

Likes, saves and follows apply locally first, then reconcile:

1. Write the new state to Room immediately; the UI updates on the next emission.
2. Perform the remote write.
3. On failure, restore the previous value and surface a snackbar with retry.

The repository returns the resulting boolean state so the caller can roll back
precisely rather than guessing.

## Offline-first

Room is the single source of truth for the UI even in online mode. Firestore
writes into Room; the UI only ever reads Room. That gives offline reads for
free and removes the flicker of a network round trip.

Writes that cannot complete offline become `PendingAction` rows, drained by
`PendingActionWorker` under a WorkManager network constraint. Each row carries
an idempotency key, so a retried action cannot double-apply. Uploads work the
same way through `PendingUpload` and `UploadWorker`, which survives process
death and reports progress back into Room.

Actions that require the network are shown as *pending*, never as complete.

## Error handling

`ErrorMapper` is the single translation point from exception to typed
`InsangramError`. Screens render errors from the type, so a Firestore
`PERMISSION_DENIED`, a Room constraint violation and an IO timeout each produce
an appropriate message and retry affordance without any screen knowing which
backend produced it.

## Trust boundary

The client is never trusted with anything that matters:

| Value | Written by |
| --- | --- |
| `likeCount`, `commentCount`, `saveCount`, `shareCount`, `viewCount` | Cloud Functions only |
| `followerCount`, `followingCount`, `postCount` | Cloud Functions only |
| Notification documents | Cloud Functions only |
| Moderator privilege | Firebase custom claim, server only |
| `createdAt`, `expiresAt` | server timestamps, validated in rules |

Security rules reject those writes from a device, so the numbers the app renders
can be believed.
