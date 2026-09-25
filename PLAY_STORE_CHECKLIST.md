# Insangram — Play Store release checklist

## 0. Build order (local)

```powershell
cd C:\Users\monuk\Desktop\Insangram
.\gradlew.bat --stop
Remove-Item -Recurse -Force .\app\build, .\build, .\.kotlin -ErrorAction SilentlyContinue
# stale folder from older zips — must be gone
Remove-Item -Recurse -Force .\app\src\main\java\com\insangram\app\core\testing -ErrorAction SilentlyContinue

.\gradlew.bat :app:assembleDemoDebug           # offline demo, no Firebase needed
.\gradlew.bat :app:assembleOnlineRelease       # signed APK
.\gradlew.bat :app:bundleOnlineRelease         # AAB for Play Store
```

Outputs:
- APK: `app\build\outputs\apk\online\release\app-online-release.apk`
- AAB: `app\build\outputs\bundle\onlineRelease\app-online-release.aab`

## 1. Firebase console (one time)

1. Project → Add Android app → package name `com.insangram.app`.
2. Download `google-services.json` → put it in `app/`.
3. **Authentication** → Sign-in method → enable **Email/Password**.
4. **Firestore Database** → Create database (production mode).
5. **Storage** → Get started.
6. **Cloud Messaging** is enabled automatically.
7. Add signing fingerprints: `.\gradlew.bat signingReport` → copy SHA-1 and
   SHA-256 into Firebase project settings.
8. Deploy rules:
   ```bash
   firebase deploy --only firestore:rules,storage:rules,firestore:indexes
   ```
   Use `firebase/firestore.client.rules` + `firebase/storage.client.rules`
   if you are running without Cloud Functions.

## 2. Signing

`keystore.properties` (project root, never commit it):

```
storeFile=../insangram-release.jks
storePassword=********
keyAlias=insangram
keyPassword=********
```

Back up `insangram-release.jks` — losing it means you can never update the app.

## 3. Versioning

In `app/build.gradle.kts`, bump before every upload:

```kotlin
versionCode = 1     // +1 for every Play upload
versionName = "1.0.0"
```

## 4. Play Console → App content answers

| Section | Answer |
| --- | --- |
| Privacy policy | Public URL of `PRIVACY_POLICY.md` |
| Ads | No ads |
| App access | Provide a test account (email + password) — reviewers must be able to sign in |
| Content rating | Social → user-generated content = Yes |
| Target audience | 13+ |
| Data safety | Email, name, photos/videos, messages, user IDs — collected, encrypted in transit, deletable |
| Account deletion | Required for UGC apps: in-app path Settings → Account → Delete account, plus a web deletion request URL |
| Government / financial / health | No |

## 5. UGC policy requirements (mandatory for social apps)

Already present in the app — keep them working:
- Report content / report user
- Block user
- Mute / restrict
- In-app account deletion
- Terms of use acceptance at sign-up

## 6. Store listing assets

- App icon 512×512 PNG (export the gradient launcher icon)
- Feature graphic 1024×500
- At least 2 phone screenshots (use the Home, Reels, Profile, Chat screens)
- Short description ≤ 80 chars, full description ≤ 4000 chars

## 7. Pre-upload smoke test

Install the release APK on a real device and confirm:
- Sign up → email/password account is created in Firebase Auth
- Profile edit + avatar upload appears in Storage
- Create post → shows in Home feed on a second device
- Like / comment / save / follow update live
- DM send + unread badge
- Story create + auto-expiry after 24h
- Push notification arrives (send a test from Firebase console)
