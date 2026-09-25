<p align="center">
  <img src="assets/logo-light.svg" width="130" height="130" alt="SignalX Logo" />
</p>

<h1 align="center">SignalX⁵ᴳ</h1>

<p align="center">
  <b>Modern Android 5G Network Dashboard & 1-Tap Auto-5G (NR Only) Lock for Non-Rooted Devices</b>
</p>

<p align="center">
  <a href="#-key-features"><img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform" /></a>
  <a href="#-architecture--tech-stack"><img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" /></a>
  <a href="#-architecture--tech-stack"><img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose" /></a>
  <a href="#-how-it-works-without-root"><img src="https://img.shields.io/badge/5G_Lock-NR_Only-00F5D4?style=for-the-badge" alt="5G NR Only" /></a>
  <a href="#-how-it-works-without-root"><img src="https://img.shields.io/badge/Root_Required-NO-00C853?style=for-the-badge" alt="No Root Required" /></a>
</p>

---

## ⚡ Key Features

- **⚡ 1-Tap Auto-5G (No Root Required)**:
  - Automatically launches the hidden Android `RadioInfo` (`*#*#4636#*#*`) testing screen.
  - Automatically handles **Phone 0** selection.
  - Automatically selects **"NR only"** from the preferred network type dropdown to force pure 5G Standalone (SA).
  - Triggers SMSC update & refresh.
  - Smoothly scrolls all the way back to the top to display the configured settings.
  - Returns to the app and displays a verified confirmation dialog.
- **📊 Real-Time Cellular Telemetry**:
  - Monitors **RSRP**, **RSRQ**, **SINR**, and **CQI** signal indicators.
  - Displays Carrier Name, MCC/MNC, Cell ID (eNB/gNB), Tracking Area Code (TAC), and active 5G/4G frequency bands.
- **📱 Smart SIM Management**:
  - Live cellular status for active SIMs (supports Jio 5G, Airtel 5G, etc.).
- **🎨 Modern Dark Aesthetic**:
  - Built 100% with **Jetpack Compose** and **Material 3**, featuring sleek neon accents, glassmorphic cards, and dynamic visual indicators.

---

## 🛠️ Architecture & Tech Stack

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose + Material 3
- **Async & State**: Kotlin Coroutines & `StateFlow` / `SharedFlow`
- **Automation Service**: Android `AccessibilityService` (`SignalXAccessibilityService`) with gesture dispatch and node hierarchy navigation.
- **Minimum SDK**: Android 10 (API 29)
- **Target SDK**: Android 14 / 15 (API 35+)

---

## 🚀 How It Works (Without Root)

Android normally restricts third-party apps from programmatically altering the preferred network type without carrier privileges or system signatures (`android.permission.MODIFY_PHONE_STATE`).

SignalX⁵ᴳ solves this gracefully for standard consumer devices:
1. It navigates to Android's built-in `RadioInfo` activity via OEM fallback intent resolution.
2. The user enables the **SignalX Accessibility Service** once.
3. SignalX coordinates the accessibility tree to select Phone 0, choose `NR only`, refresh SMSC, scroll to the top, and return smoothly to the dashboard.

---

## 📥 Getting Started

### Prerequisites
- Android Studio Ladybug / Meerkat or later
- Android SDK 35+
- JDK 17 or JDK 21

### Building & Running
1. Clone the repository:
   ```bash
   git clone https://github.com/SanketYerwadkar/signalx-5g.git
   cd signalx-5g
   ```

2. Build the debug APK only:
   ```bash
   # Linux / macOS
   ./gradlew assembleDebug

   # Windows
   .\gradlew assembleDebug
   ```
   Output: `app/build/outputs/apk/debug/app-debug.apk`

3. Install to your connected device (USB debugging must be enabled):
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

4. **Build + Install in one step** (recommended during development):
   ```bash
   # Linux / macOS
   ./gradlew installDebug

   # Windows
   .\gradlew installDebug
   ```

5. **Build + Install + Launch** (fastest dev loop):
   ```bash
   # Linux / macOS
   ./gradlew installDebug && adb shell monkey -p com.signalx.app -c android.intent.category.LAUNCHER 1

   # Windows
   .\gradlew installDebug; adb shell monkey -p com.signalx.app -c android.intent.category.LAUNCHER 1
   ```

> **Tip:** Run `adb devices` first to confirm your device is detected before installing.

---

## 📜 License

This project is licensed under the MIT License.
