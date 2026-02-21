# NfcReaderKMP 📱

[![Kotlin](https://img.shields.io/badge/kotlin-2.1.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.10.1-blue?logo=jetbrains)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)

A powerful, easy-to-use Kotlin Multiplatform (KMP) library for reading NFC tags on Android and iOS using Compose Multiplatform.

[**📖 View Full API Documentation**](https://dalafiarisamuel.github.io/NfcReaderKMP/)

---

## ✨ Features

- **🚀 Unified API**: A single, clean API to handle NFC scanning on both platforms.
- **🎨 Compose Native**: Lifecycle-aware state management that fits perfectly into your Compose UI.
- **🛠️ Fully Customizable**:
    - **Android**: Custom Bottom Sheet with support for Lottie animations (via [Compottie](https://github.com/AlexZhirkevich/compottie)).
    - **iOS**: Seamless integration with the native system NFC scanning dialog.
- **⚙️ Flexible Configuration**: Control timeouts, dismissal behaviors, and UI strings with a type-safe DSL.
- **📊 Detailed Tag Info**: Extract Serial Numbers, NDEF payloads, and supported technology lists.

---

## 📦 Installation

Add the dependency to your `commonMain` source set in `build.gradle.kts`:

```kotlin
sourceSets {
    commonMain.dependencies {
        implementation("com.devtamuno.kmp:nfcreader:<version>")
    }
}
```

---

## 🛠️ Platform Setup

### Android 🤖

1. Add NFC permissions to your `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.NFC" />
<uses-feature android:name="android.hardware.nfc" android:required="false" />
```

### iOS 

1. Add `NFCReaderUsageDescription` to your `Info.plist`.
2. Enable the **Near Field Communication Tag Reading** capability in your Xcode project.
3. Add `NDEF` support to the `com.apple.developer.nfc.readersession.formats` entitlement.

---

## 🚀 Usage

### 1. Initialize the State Manager

You can use the declarative DSL to configure the reader:

```kotlin
val nfcManager = rememberNfcReadManagerState {
    titleMessage = "Ready to Scan"
    subtitleMessage = "Hold your tag near the device."
    buttonText = "Cancel"
    nfcReadTimeout = 30.seconds
    
    // Custom Lottie animation for Android (optional)
    nfcScanningAnimationSlot = {
      ScanningAnimationDefault.NfcScanningAnimation()
    }
}
```

### 2. Observe Results

Collect the `nfcReadResult` and react to different scanning states:

```kotlin
val result by nfcManager.nfcReadResult.collectAsState()

when (val state = result) {
    is NfcReadResult.Success -> {
        Text("Tag ID: ${state.data.serialNumber}")
        Text("Payload: ${state.data.payload}")
    }
    is NfcReadResult.Error -> {
        Text("Error: ${state.message}", color = Color.Red)
    }
    NfcReadResult.OperationCancelled -> {
        Text("Scanning cancelled by user")
    }
    NfcReadResult.Initial -> {
        Button(onClick = { nfcManager.startScanning() }) {
            Text("Start Scanning")
        }
    }
}
```

---

## ⚙️ Configuration Options (`NfcConfig`)

| Property | Type | Default | Platform |
| :--- | :--- | :--- | :--- |
| `titleMessage` | `String` | `"Ready to Scan"` | Android |
| `subtitleMessage` | `String` | `"Hold your tag near the device."` | Android & iOS |
| `buttonText` | `String` | `"Cancel"` | Android |
| `nfcReadTimeout` | `Duration` | `60.seconds` | Android |
| `sheetGesturesEnabled` | `Boolean` | `true` | Android |
| `shouldDismissBottomSheetOnBackPress` | `Boolean` | `false` | Android |
| `shouldDismissBottomSheetOnClickOutside` | `Boolean` | `false` | Android |
| `nfcScanningAnimationSlot` | `Composable` | Default Animation | Android |

---

## 📄 Data Models

### `NfcTagData`
- `serialNumber`: The tag's unique ID (Hex string). *Note: Android only.*
- `type`: Either `NDEF` or `NON_NDEF`.
- `payload`: The decoded string content of the tag.
- `techList`: A list of hardware technologies detected (e.g., `Mifare Classic`, `ISO 14443-3A`).

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
