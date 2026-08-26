# NfcReaderKMP

[![Kotlin](https://img.shields.io/badge/kotlin-2.1.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.10.1-blue?logo=jetbrains)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)

A powerful, easy-to-use Kotlin Multiplatform (KMP) library for reading NFC tags on Android and iOS using Compose Multiplatform.

[**View Full API Documentation**](https://dalafiarisamuel.github.io/NfcReaderKMP/)

---

## Work in Progress

> **This library is currently under active development and has not yet been published to Maven Central.**
>
> The core functionality is implemented and working on both Android and iOS, but we are still completing testing before the first stable release. The API may change before the official release.
>
> Watch or star this repository to be notified when it is released.

---

## Features

- **Unified API**: A single, clean API to handle NFC scanning on both platforms.
- **Compose Native**: Lifecycle-aware state management that fits perfectly into your Compose UI.
- **Fully Customizable**:
    - **Android**: Custom Bottom Sheet with support for Lottie animations (via [Compottie](https://github.com/AlexZhirkevich/compottie)).
    - **iOS**: Seamless integration with the native system NFC scanning dialog.
- **Flexible Configuration**: Control timeouts, dismissal behaviors, and UI strings with a type-safe DSL.
- **Detailed Tag Info**: Extract Serial Numbers, NDEF payloads, and supported technology lists.

---

## Installation

Add the dependency to your `commonMain` source set in `build.gradle.kts`:

```kotlin
sourceSets {
    commonMain.dependencies {
        implementation("com.devtamuno.kmp:nfcreader:<version>")
    }
}
```

---

## Platform Setup

### Android

1. Add NFC permissions to your `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.NFC" />
<uses-feature android:name="android.hardware.nfc" android:required="false" />
```

### iOS

1. Add `NFCReaderUsageDescription` to your `Info.plist`.
2. Enable the **Near Field Communication Tag Reading** capability in your Xcode project.
3. Add `NDEF` support to the `com.apple.developer.nfc.readersession.formats` entitlement.

---

## Usage

### 1. Initialize the State Manager

Create an `NfcConfig` and pass it to `rememberNfcReadManagerState`. You can optionally provide a custom animation slot:

```kotlin
val nfcManager = rememberNfcReadManagerState(
    config = NfcConfig(
        titleMessage = "Ready to Scan",
        subtitleMessage = "Hold your tag near the device.",
        buttonText = "Cancel"
    ),
    nfcScanningAnimationSlot = {
        // Custom Compose animation here
        Text("Scanning...")
    }
)
```

### 2. Observe Results

Collect the `nfcReadResult` and react to different scanning states:

```kotlin
val result by nfcManager.nfcReadResult.collectAsState()

when (val state = result) {
    is NfcReadResult.Success -> {
        Text("Tag ID: ${state.data.serialNumber}")
        Text("Type: ${state.data.type}")
        Text("Payload: ${state.data.payload}")
        Text("Technologies: ${state.data.techList.joinToString()}")
    }
    is NfcReadResult.Error -> {
        Text("Error: ${state.message}", color = Color.Red)
    }
    NfcReadResult.Scanning -> {
        Text("Scanning for NFC tag...")
    }
    NfcReadResult.OperationCancelled -> {
        Text("Scanning cancelled")
    }
    NfcReadResult.Initial -> {
        Button(onClick = { nfcManager.startScanning() }) {
            Text("Start Scanning")
        }
    }
}
```

---

## Configuration Options (`NfcConfig`)

`NfcConfig` uses the `expect`/`actual` pattern to provide a clean API. This ensures that platform-specific properties (like Android's BottomSheet behavior) are only visible and used on their respective platforms.

### Common Properties
These properties are available on both Android and iOS and are typically configured in your shared `commonMain` code.

| Property | Type | Default |
| :--- | :--- | :--- |
| `titleMessage` | `String` | Required |
| `subtitleMessage` | `String` | Required |
| `buttonText` | `String` | Required |
| `nfcUnsupportedMessage` | `String` | `"NFC is not supported on this device"` |
| `nfcDisabledMessage` | `String` | `"NFC is disabled on this device"` |

### Platform-Specific Properties
These properties are internal to each platform's implementation to keep the common API clean. They use the following defaults:

#### Android-Specific
- `nfcReadTimeout`: `60.seconds` (minimum 5s)
- `nfcScanTimeoutMessage`: `"NFC scan timed out"`
- `sheetGesturesEnabled`: `true`
- `shouldDismissBottomSheetOnBackPress`: `false`
- `shouldDismissBottomSheetOnClickOutside`: `false`

#### iOS-Specific
- `nfcSuccessMessage`: `"Tag scanned successfully"`

> **Note:** On iOS, only `subtitleMessage` is used — it maps directly to the native system NFC scanning dialog message. `titleMessage` and `buttonText` are required by the constructor but are not displayed on iOS as the system manages the dialog UI.

---

## Data Models

### `NfcTagData`
- `serialNumber`: The tag's unique ID as a hex-encoded string (available on both Android and iOS).
- `type`: The tag type as an `NfcTagType` enum — see values below.
- `payload`: The decoded string content of the tag. `null` if the tag is empty or non-NDEF.
- `techList`: A list of hardware technologies detected (e.g., `"Mifare Classic"`, `"ISO 14443-3A"`).

### `NfcTagType`

| Value | Description |
| :--- | :--- |
| `NDEF` | NFC Data Exchange Format tag |
| `NON_NDEF` | Tag that does not contain NDEF data |
| `MIFARE` | MIFARE-based tag (Classic, Ultralight, DESFire) |
| `ISO15693` | ISO 15693 vicinity tag |
| `ISO7816` | ISO 7816-4 based smart card or tag |
| `FELICA` | Sony FeliCa tag (transit/payments) |

### `NfcReadResult`

| State | Description |
| :--- | :--- |
| `Initial` | No scan has been initiated yet |
| `Scanning` | Actively scanning for a tag |
| `Success(data)` | Tag was read successfully |
| `Error(message)` | An error occurred during scanning |
| `OperationCancelled` | Scanning was cancelled by the user or system |

---

## Demo

|                   Android Implementation                    |                   iOS Implementation                    |
|:-----------------------------------------------------------:|:-------------------------------------------------------:|
| <img src="images/android_implementation.gif" width="300" /> | <img src="images/ios_implementation.gif" width="300" /> |

---

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.