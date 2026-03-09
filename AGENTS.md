# NfcReaderKMP AI Agent Guide

This document provides context, architecture guidelines, and coding standards for AI assistants working on the `NfcReaderKMP` repository.

## Project Context
`NfcReaderKMP` is a Kotlin Multiplatform (KMP) library for reading NFC tags on Android and iOS using Compose Multiplatform. It abstracts platform-specific NFC APIs into a unified Compose-friendly interface.

## Architecture Map
The project is divided into two main modules:
- `:nfcreader`: The core library module containing KMP logic.
- `:composeApp`: A sample application demonstrating the library's usage.

### Library Structure (`:nfcreader`)
- `commonMain`: Shared interfaces, data models, and the state management logic.
- `androidMain`: Android-specific implementation using `NfcAdapter` and Material3 `ModalBottomSheet`.
- `iosMain`: iOS-specific implementation using `CoreNFC`.

## Layer Responsibilities

### Contract Layer (`com.devtamuno.kmp.nfcreader.contract`)
- **`NfcReadManager`**: Internal `expect`/`actual` class handling low-level platform NFC APIs.
- **`NfcReadManagerState`**: Public interface defining the state (`nfcReadResult`) and actions (`startScanning`, `stopScanning`).
- **`NfcReadManagerStateImpl`**: Internal implementation that coordinates between the `NfcReadManager` and the public state.

### Data Layer (`com.devtamuno.kmp.nfcreader.data`)
- **`NfcConfig`**: Configuration for scanning behavior (timeouts, messages, animations).
- **`NfcReadResult`**: Sealed class representing the lifecycle of an NFC scan (Initial, Scanning, Success, Error, Cancelled).
- **`NfcTagData`**: Model for scanned tag information (Serial Number, Type, Payload, Tech List).

### UI Layer (`com.devtamuno.kmp.nfcreader.ui`)
- Contains default UI components like `ScanningAnimationDefault`.

## Coding Standards

### State Management
- Use the **State Holder** pattern. State should be created and remembered using a `rememberX` composable.
- Implementation details should be `internal`.
- Expose state via `StateFlow`.

Example:
```kotlin
@Composable
fun rememberNfcReadManagerState(config: NfcConfig): NfcReadManagerState =
    remember { NfcReadManagerStateImpl(config) }.also { it.InitNfcManager() }
```

### Naming Conventions
- Platform-specific implementations of `expect` classes must use the `.android.kt` and `.ios.kt` suffixes if file-per-platform is used.
- Interfaces for state management should end in `State`.
- Implementations of those interfaces should end in `StateImpl`.

### Dependency Management
- All dependencies are managed in `gradle/libs.versions.toml`.
- Use **Type-safe project accessors** (e.g., `projects.nfcreader`).

## Platform Constraints

### Android
- Requires `NfcAdapter.ReaderCallback`.
- Uses a `ModalBottomSheet` to guide the user during scanning.
- Must handle `Activity` and `Context` via `LocalActivity.current` and `LocalContext.current`.

### iOS
- Uses `NFCTagReaderSessionDelegateProtocol`.
- The system handles the scanning UI. `RegisterManager()` is a no-op.
- Results are only emitted after the system dialog is dismissed to ensure UI consistency.

## Testing Expectations
- Unit tests should be placed in `commonTest` where possible.
- Android-specific instrumentation tests go in `androidDeviceTest`.

## Anti-patterns to Avoid
- **Do not** expose platform-specific types (like `android.nfc.Tag` or `NFCTagProtocol`) in `commonMain`.
- **Do not** trigger NFC scanning without user interaction or a clear lifecycle-bound event.
- **Do not** forget to call `stopScanning()` or handle `onDispose` to release hardware resources.

## Code Examples

### Defining a Result (Sealed Class)
```kotlin
sealed interface NfcReadResult {
    data object Initial : NfcReadResult
    data object Scanning : NfcReadResult
    data class Success(val data: NfcTagData) : NfcReadResult
    data class Error(val message: String) : NfcReadResult
    data object OperationCancelled : NfcReadResult
}
```

### Implementing Platform Logic (iOS snippet)
```kotlin
internal actual class NfcReadManager actual constructor(private val config: NfcConfig) :
    NSObject(), NFCTagReaderSessionDelegateProtocol {
    
    actual fun startScanning() {
        session = NFCTagReaderSession(pollingOption = ..., delegate = this, queue = null)
        session?.beginSession()
    }
}
```
