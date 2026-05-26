# Heracles 🏋️‍♂️

A lightweight, high-performance, local-first workout logger built natively for Android using Kotlin and Jetpack Compose. 

No tracking, no accounts, no bloated subscription models—just straight training data serialization.

> **Note:** This project is the mobile companion to the desktop ecosystem version (*Hercules*).

---

## 📸 Glimpse into the UI

| Logger Interface | Active Tracking View |
|---|---|
| <img src="screenshots/logger.png" width="300" alt="Heracles Logger Screen"/> | <img src="screenshots/session.png" width="300" alt="Heracles Active Session"/> |

*Custom theme engine currently in development to support system-wide dynamic Material You palettes alongside custom user presets.*

---

## ⚡ Features

- **Local-First Architecture:** Rapid atomic JSON file writes keep your workout logs stored securely on your own device (`~/.heracles/bodyweight/`).
- **Zero Friction Logging:** Fast input switching for Exercises, Sets, Reps, and Weights.
- **Built-in Session Tracker:** Features a precision scrubber tool for a sleek visual representation of training history and aggregate session volume calculations.
- **Background Autoclose Safety:** Multi-threaded architecture leverages a dedicated background coroutine context (`Dispatchers.IO`) to handle seamless autosaves without locking up the UI thread.
- **Highly Optimized Execution:** Stripped of heavy framework overhead to ensure immediate cold-starts and smooth scrolling even on legacy, low-spec hardware.

---

## 📦 Installation & Updates

Because this app bypasses the bureaucracy of the Google Play Store, updates are distributed directly through GitHub Releases. Overwriting an older installation with a newer APK version safely retains your local database logs.

1. Go to the [Releases](https://github.com/NyxUltor/Heracles/releases) tab.
2. Download the latest compiled `release.apk`.
3. Open the file on your Android device and enable "Install from Unknown Sources" if prompted.

---

## 🛠️ Tech Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Asynchrony:** Kotlin Coroutines & StateFlow
- **Data Persistence:** Local Serialization (Encrypted GPG Symmetric file options planned)
