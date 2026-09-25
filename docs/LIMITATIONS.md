# Known limitations

Every item here is a deliberate, documented trade-off or a genuine gap. Nothing
is hidden behind a "coming soon" screen.

## Build and delivery

1. **The Gradle build has never run.** The authoring environment had no Android
   SDK, no Gradle, no Kotlin compiler and no network. `BUILD_VERIFIED.md`
   records this in full.
2. **No APK was produced**, for the same reason.
3. **`gradle/wrapper/gradle-wrapper.jar` is missing.** It is a binary that must
   be downloaded. Android Studio regenerates it on first sync, or run
   `gradle wrapper --gradle-version 8.9`.
4. **The presentation layer is incomplete.** See `docs/STATUS.md` for a
   file-by-file account.

## Single Gradle module

The project is one Gradle module with strictly separated packages rather than
the ~30 modules the ideal structure implies. Layering, dependency direction and
package boundaries are identical; only the enforcement mechanism differs. For a
student-scale project the build-speed win outweighs compile-time enforcement.
`docs/ARCHITECTURE.md` describes the split path.

## Demo mode

5. **Reels have thumbnails, not video.** Generating 22 real video files would
   add tens of megabytes to the archive. The ExoPlayer integration, player pool,
   lifecycle handling and watch-time tracking are all real and work against any
   URI; only the bundled sample bytes are still images.
6. **No real-time updates.** Demo mode is a local Room database, so there are no
   push updates from other users. Interactions are simulated deterministically.
7. **Demo passwords are documented.** `Demo@12345` is printed in the README on
   purpose. It unlocks nothing beyond the local fictional dataset, and it is
   stored as a PBKDF2-SHA256 hash, never as plaintext.

## Search

8. **No full-text search.** Firestore does not offer it, and this project does
   not pretend otherwise. The implemented strategy is normalized fields,
   username prefix arrays, per-hashtag documents, caption keyword tokens,
   composite indexes, and client-side ranking over a bounded result set.
   Production would add Algolia, Typesense or Elasticsearch.
9. **Prefix arrays are capped at 20 characters**, so very long usernames are
   matchable only on their first 20 characters.
10. **Caption tokens are capped at 40 per post**, and words shorter than three
    characters and common stop words are dropped.

## Smart features

11. **Nothing here is machine learning.** The Caption, Hashtag and Comment
    Assistants are template and keyword engines. The Spam Detector is a scored
    rule set. No OpenAI, Gemini, Claude or other model is called, bundled or
    running on device.
12. **Duplicate detection is a local perceptual-ish hash** over downscaled
    bitmap bytes. It catches re-uploads of the same file reliably and edited
    variants unreliably.

## Recommendation

13. **The feed score is a hand-tuned linear formula**, not a learned model. That
    is a feature for a project that must be explainable and testable, but it
    will not match a production recommender.
14. **Ranking runs over a bounded candidate window** (the most recent N cached
    posts) rather than the entire corpus.

## Real-time and cost

15. **Typing indicators and activity status use Firestore documents.** Each
    keystroke burst is a write. Firebase Realtime Database presence would be
    materially cheaper at scale.
16. **Read receipts are per-message array writes**, which is simple but grows
    with group size.
17. **Cloud Functions requires the Blaze plan.** The project cannot run its
    trusted counters, notification fan-out or cleanup jobs on the free tier.

## Moderation

18. **Reports are recorded, not adjudicated.** There is no automated takedown.
    A moderator with the custom claim moves a report between states.
19. **No content classification.** Nothing scans images or text for policy
    violations.
20. **Moderator bootstrap is deliberately awkward.** `grantModeratorClaim`
    refuses to do anything unless `MODERATOR_BOOTSTRAP_EMAIL` is set in the
    server environment. This is correct — privilege must never be grantable from
    a client — but it means you must set that variable once to create your first
    moderator.

## Media

21. **Filters are ColorMatrix based**, applied on a background dispatcher. They
    are fast and dependency free, but cannot express curve or LUT effects.
22. **Video editing is not implemented.** Videos can be captured, selected,
    uploaded and played, but not trimmed or filtered.
23. **No image caching CDN.** Storage serves media directly; a production
    deployment would front it with a CDN.

## Accessibility

24. Content descriptions, semantic roles, 48dp targets, dynamic type and
    reduced-motion support are designed into the component API, but **have not
    been validated with TalkBack or Accessibility Scanner**, because the app has
    not been run.
