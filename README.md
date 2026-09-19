# SignalX⁵ᴳ — 5G Network Dashboard & Auto-Configurator

SignalX⁵ᴳ is an advanced Android cellular radio dashboard and automated 5G network manager designed for real-time cellular telemetry and effortless 5G (NR Only) lock on **non-rooted** Android devices.

---

## ⚡ Key Features

- **⚡ 1-Tap Auto-5G (No Root Required)**:
  - Automatically launches the hidden Android `RadioInfo` (`*#*#4636#*#*`) testing screen.
  - Selects **Phone 0** (primary SIM).
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
2. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
3. Install to your connected device:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📜 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
