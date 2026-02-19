# NfcReaderKMP

A Kotlin Multiplatform (KMP) library for reading NFC tags on Android and iOS using Compose Multiplatform.

## Features

- **Cross-Platform**: Unified API for NFC scanning on Android and iOS.
- **Compose Integrated**: Lifecycle-aware state management with `rememberNfcReadManagerState`.
- **Customizable UI**:
    - **Android**: Customizable bottom sheet with titles, messages, and a slot for custom scanning animations (Lottie supported via Compottie).
    - **iOS**: Uses the native system NFC scanning dialog.
- **Configurable**: Control timeouts, dismissal behavior, and UI strings via `NfcConfig`.
- **Comprehensive Data**: Extracts Serial Number (Android), Tag Type, Payload (NDEF), and supported technologies.

## Installation

Add the dependency to your `commonMain` source set:

```kotlin
// build.gradle.kts
sourceSets {
    commonMain.dependencies {
        implementation("com.devtamuno.kmp:nfcreader:<version>")
    }
}
```

### Platform Setup

#### Android
Ensure your `AndroidManifest.xml` includes the NFC permission:
```xml
<uses-permission android:name="android.permission.NFC" />
<uses-feature android:name="android.hardware.nfc" android:required="false" />
```

#### iOS
1. Add `NFCReaderUsageDescription` to your `Info.plist`.
2. Enable the **Near Field Communication Tag Reading** capability in Xcode.
3. Add `com.apple.developer.nfc.readersession.formats` to your entitlements with `NDEF` support.

## Usage

### 1. Define Configuration
```kotlin
val config = NfcConfig(
    titleMessage = "Ready to Scan",
    subtitleMessage = "Hold your tag near the device.",
    buttonText = "Cancel",
    nfcReadTimeout = 30.seconds,
    // Optional: Custom animation slot for Android
    nfcScanningAnimationSlot = {
      ScanningAnimationDefault.NfcScanningAnimation()
    }
)
```
*Note: On iOS, only `subtitleMessage` is displayed in the native UI.*

### 2. Remember State in Composable
```kotlin
val nfcManager = rememberNfcReadManagerState(config)
val result by nfcManager.nfcReadResult.collectAsState()
```

### 3. Handle Results
```kotlin
when (val state = result) {
    is NfcReadResult.Success -> {
        val data = state.data
        println("Scanned: ${data.serialNumber}, Payload: ${data.payload}")
    }
    is NfcReadResult.Error -> {
        println("Error: ${state.message}")
    }
    NfcReadResult.OperationCancelled -> {
        println("User cancelled")
    }
    NfcReadResult.Initial -> {
        // Idle state
    }
}
```

### 4. Trigger Scanning
```kotlin
Button(onClick = { nfcManager.startScanning() }) {
    Text("Scan NFC Tag")
}
```

## Data Models

### NfcTagData
- `serialNumber`: Hex-encoded UID (Available on Android; empty string on iOS NDEF sessions).
- `type`: `NDEF` or `NON_NDEF`.
- `payload`: String content of the tag (UTF-8).
- `techList`: List of supported technologies (e.g., "ISO 14443-3A").
