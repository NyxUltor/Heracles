Heracles (Android)
===================

Current release: v0.2

This repository is an Android-first rewrite of the Heracles workout app. The legacy desktop/web Python prototype has been archived to `legacy_desktop/`.

Quick start (Android Studio)

- Open this folder in Android Studio.
- Let Gradle sync and accept any SDK/component prompts.
- Run the app on a device or emulator via Run > Run 'app'.

Build from command line (requires Android SDK + JDK)

```bash
cd /path/to/Heracles
./gradlew assembleDebug
# APK will be under app/build/outputs/apk/debug/
```

Where to look

- Android app module: `app/`
- Archive of previous desktop/web prototype: `legacy_desktop/`

If you want me to also produce a debug APK here (requires Android SDK), tell me to attempt a local Gradle build.
# Heracles

Heracles is the native offline Android version of the app.

## Open in Android Studio
1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Run on an Android device or emulator.

## Build an APK
Use Android Studio: Build > Build Bundle(s) / APK(s) > Build APK(s)

## What it does
- Works offline on the phone.
- Saves workouts locally on-device.
- Uses a hamburger menu for navigation.
- Restores the latest session and autosaves as you work.

## Notes
- The separate `HeraclesAndroid` folder was only a staging copy while the native Android project was being assembled.
- This repo root is the Android project now.