# Setup guide

A condensed, copy-pasteable version of README sections 9–14.

## 1. Prerequisites

```bash
java -version     # must be 17
sdkmanager --list_installed | grep -E 'platforms;android-35|build-tools;35'
node --version    # 20.x, only for functions and the emulator
```

Android Studio Ladybug (2024.2.1) or newer.

## 2. Restore the Gradle wrapper binary

Not included in this archive. Do this once:

```bash
gradle wrapper --gradle-version 8.9
```

Or just open the project in Android Studio and let it sync.

## 3. Demo mode — zero configuration

```bash
chmod +x gradlew
./gradlew clean :app:assembleDemoDebug
./gradlew :app:installDemoDebug
```

Sign in with `aarav@insangram.example` / `Demo@12345`.

## 4. Online mode

```bash
# after creating the Firebase project and enabling Auth, Firestore,
# Storage, FCM and App Check:
cp .firebaserc.example .firebaserc     # set your project id
# place your downloaded config at app/google-services.json

firebase deploy --only firestore:rules,firestore:indexes,storage
cd functions && npm install && npm run build && firebase deploy --only functions && cd ..

./gradlew :app:assembleOnlineDebug
```

## 5. Emulators

```bash
cd functions && npm install && npm run build && cd ..
firebase emulators:start --only auth,firestore,storage,functions

# in a second shell
export FIRESTORE_EMULATOR_HOST=localhost:8080
export FIREBASE_AUTH_EMULATOR_HOST=localhost:9099
export FIREBASE_STORAGE_EMULATOR_HOST=localhost:9199
node scripts/seed_emulator.js
```

The seed script refuses to run against anything that is not a local emulator.

## 6. Create your first moderator

Moderator status is a custom claim and is intentionally not grantable from the
app. Set the bootstrap variable on the server, then call the callable once:

```bash
firebase functions:config:set   # or set MODERATOR_BOOTSTRAP_EMAIL in the runtime env
firebase deploy --only functions:grantModeratorClaim
```

Sign out and back in afterwards so the new claim lands in the ID token.

## 7. Verify the build

```bash
./scripts/verify_build.sh
```

This overwrites `BUILD_VERIFIED.md` with genuine results.
