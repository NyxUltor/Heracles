# Heracles

Heracles is a lightweight, local-first workout logger for Android built with Kotlin and Jetpack Compose.

It keeps workout data on-device, uses local JSON storage, and avoids accounts, tracking, and subscriptions.

It is the mobile companion to the desktop ecosystem project, Hercules.

## App Summary

- Fast workout logging for exercises, sets, reps, weights, bodyweight, and duration
- Local session save, restore, export, and autosave behavior
- Custom theme modpacks with light/dark scheme support and style controls
- Pre-built session import flow with inbox-style review before loading into Logger
- Tracker screen for bodyweight and volume trends
- Offline-first storage with atomic JSON writes

## Screenshots

The next time the app is run on the connected phone or tablet, real device screenshots should be captured into the [screenshots](screenshots) folder and linked here.

Planned captures:

- [Logger screen](screenshots/logger-screen.png)
- [Active session screen](screenshots/active-session.png)
- [Theme editor](screenshots/theme-editor.png)
- [Theme presets](screenshots/theme-presets.png)

## Features

- Offline-first logging with session restore and autosave
- Theme modpacks with built-in presets and user-defined packs
- Pre-built routine import with ghost values in Logger
- Session tracker with radar and history views
- Local backup/export support for settings and sessions

## In Progress

- Richer pre-built text parsing and friendlier import formatting
- Time-related logging UI
- Notes support inside sessions
- New UI design polish
- Expanded roadmap cleanup

## Installation

Updates are distributed through GitHub Releases.

1. Open the latest release on GitHub.
2. Download the release APK.
3. Install it on your Android device.
4. Enable unknown-source installs if Android prompts for it.

## Tech Stack

- Kotlin
- Jetpack Compose Material 3
- Kotlin Coroutines and StateFlow
- Local JSON serialization

## Notes

- Data is stored locally on the device.
- The app is designed for fast on-device iteration and offline use.
- Some roadmap items are intentionally still rough and tracked in [PLANNED_CHANGES.md](PLANNED_CHANGES.md).

## Legacy Notes

Earlier versions of this README used a more marketing-style feature list and installation blurb. The current version keeps the same project intent but reflects the newer theme modpack, pre-built session, and logger work that is now in the codebase.
