# NfcReaderKMP AI Agent Guide

This document provides context, architecture guidelines, and coding standards for AI assistants working on the `NfcReaderKMP` repository.

## Project Context
`NfcReaderKMP` is a Kotlin Multiplatform (KMP) library for reading NFC tags on Android and iOS using Compose Multiplatform. It abstracts platform-specific NFC APIs into a unified Compose-friendly interface.

## Architecture Map
The project is divided into two main modules:
- `:nfcreader`: The core library module containing KMP logic.
- `:composeApp`: A sample application demonstrating the library's usage.

### Library Structure (`:nfcreader`)
- `commonMain`: Shared interfaces, data models, state management, and shared constants.
- `androidMain`: Android-specific implementation using `NfcAdapter` and Material3 `ModalBottomSheet`.
- `iosMain`: iOS-specific implementation using `CoreNFC`.

## Layer Responsibilities

### Contract Layer (`com.devtamuno.kmp.nfcreader.contract`)

**commonMain**
- **`NfcReadManager`**: Internal `expect`/`actual` class handling low-level platform NFC APIs.
- **`NfcReadManagerState`**: Public interface defining the state (`nfcReadResult`) and actions (`startScanning`, `stopScanning`).
- **`NfcReadManagerStateImpl`**: Internal implementation that coordinates between the `NfcReadManager` and the public state.
- **`NfcUriPrefixes`**: Shared `NFC_URI_PREFIXES` array (NFC Forum URI identifier codes). Used by both platform tag parsers — do not duplicate it in platform-specific files.

**androidMain**
- **`NfcReadManager.android.kt`**: Core class — session lifecycle, timeout, `onTagDiscovered`.
- **`NfcScanBottomSheet.kt`**: All Compose/Material3 UI for the bottom sheet shown during scanning.
- **`NfcTagParser.kt`**: Tag parsing — `getNfcTagType`, `getFriendlyName`, `NdefRecord.toReadableText`.

**iosMain**
- **`NfcReadManager.ios.kt`**: Core class — session lifecycle, delegate callbacks, NDEF coordination.
- **`NfcTagParser.kt`**: Tag parsing — `extractUid`, `getTechList`, `NFCNDEFPayload.readableText`, `NSData`/hex helpers.

### Data Layer (`com.devtamuno.kmp.nfcreader.data`)
- **`NfcConfig`**: Configuration for scanning behaviour. It uses nested `AndroidOptions` and `IosOptions` classes to separate platform-specific settings while allowing them to be configured from common code. Common properties (messages) are in the root class, while Android-specific UI/timeout settings and iOS-specific success messages are grouped in their respective option blocks.
- **`NfcReadResult`**: Sealed class representing the lifecycle of an NFC scan (`Initial`, `Scanning`, `Success`, `Error`, `OperationCancelled`).
- **`NfcTagData`**: Model for scanned tag information (Serial Number, Type, Payload, Tech List). `serialNumber` is available on both Android and iOS.
- **`NfcTagType`**: Enum of supported tag types — `NDEF`, `NON_NDEF`, `MIFARE`, `ISO15693`, `ISO7816`, `FELICA`.

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
fun rememberNfcReadManagerState(
    config: NfcConfig,
    nfcScanningAnimationSlot: @Composable ColumnScope.() -> Unit = {
        ScanningAnimationDefault.NfcScanningAnimation()
    },
): NfcReadManagerState =
    rememberMutableNfcReadManagerState(config, nfcScanningAnimationSlot).also {
        it.InitNfcManager()
    }

@Composable
private fun rememberMutableNfcReadManagerState(
    config: NfcConfig,
    nfcScanningAnimationSlot: @Composable ColumnScope.() -> Unit,
): NfcReadManagerState =
    remember { NfcReadManagerStateImpl(config, nfcScanningAnimationSlot) }
```

### Naming Conventions
- The `expect`/`actual` file for `NfcReadManager` uses `.android.kt` / `.ios.kt` suffixes.
- Supporting files within a platform source set (parsers, UI) use plain `.kt` without a platform suffix, since they live in a platform-specific source set already.
- Interfaces for state management should end in `State`.
- Implementations of those interfaces should end in `StateImpl`.

### Dependency Management
- All dependencies are managed in `gradle/libs.versions.toml`.
- Use **Type-safe project accessors** (e.g., `projects.nfcreader`).

## Platform Constraints

### Android
- Use `context.getSystemService(NfcManager::class.java)?.defaultAdapter` to obtain the adapter — `NfcAdapter.getDefaultAdapter(context)` is deprecated since API 33.
- Requires `NfcAdapter.ReaderCallback`. Reader mode flags: `FLAG_READER_NFC_A | NFC_B | NFC_F | NFC_V | NO_PLATFORM_SOUNDS`.
- Uses `NfcScanBottomSheet` (a separate file) to guide the user during scanning.
- Must handle `Activity` and `Context` via `LocalActivity.current` and `LocalContext.current`.
- The coroutine scope must have its children cancelled in `DisposableEffect(Unit) { onDispose { scope.coroutineContext.cancelChildren() } }` — a separate effect from the activity-bound one — so it only stops active jobs when the composable fully leaves composition, not on configuration change.
- `timeoutJob` is marked `@Volatile` because it is written on the Main thread and read from the NFC callback thread in `onTagDiscovered`.

### iOS
- Uses `NFCTagReaderSessionDelegateProtocol`.
- The system handles the scanning UI. `RegisterManager()` is a no-op.
- Poll for all supported tag families: `NFCPollingISO14443 or NFCPollingISO15693 or NFCPollingISO18092`. Omitting `NFCPollingISO18092` silently disables FeliCa detection.
- Supported tag types: `NFCTagTypeMiFare`, `NFCTagTypeISO15693`, `NFCTagTypeISO7816Compatible`, `NFCTagTypeFeliCa`.
- **`pendingResult` pattern**: capture the result in `finishSession` before calling `invalidateSession`, then emit it inside `tagReaderSession(_:didInvalidateWithError:)` — after the native dialog fully dismisses. This prevents UI flicker.
- Session timeout (error code `201`) and user cancellation (error code `200`) both map to `NfcReadResult.OperationCancelled`. Use the named constants `NFC_USER_CANCELLED_ERROR_CODE` / `NFC_SESSION_TIMEOUT_ERROR_CODE` — never bare literals.

## Testing Expectations
- Unit tests should be placed in `commonTest` where possible.
- Android-specific instrumentation tests go in `androidDeviceTest`.

## Anti-patterns to Avoid
- **Do not** expose platform-specific types (like `android.nfc.Tag` or `NFCTagProtocol`) in `commonMain`.
- **Do not** trigger NFC scanning without user interaction or a clear lifecycle-bound event.
- **Do not** forget to call `stopScanning()` or handle `onDispose` to release hardware resources.
- **Do not** duplicate `NFC_URI_PREFIXES` in platform files — it lives in `commonMain` and is accessible directly.
- **Do not** hardcode error or status strings — always use the corresponding `NfcConfig` property (`nfcUnsupportedMessage`, `nfcDisabledMessage`, `nfcScanTimeoutMessage`, `nfcSuccessMessage`).
- **Do not** map bare `NfcA` or `NfcB` tech entries to `NfcTagType.ISO7816` — only `IsoDep` indicates an ISO 7816 smart card.

## Code Examples

### Defining a Result (Sealed Class)
```kotlin
sealed class NfcReadResult {
    data object Initial : NfcReadResult()
    data object Scanning : NfcReadResult()
    data class Success(val data: NfcTagData) : NfcReadResult()
    data class Error(val message: String) : NfcReadResult()
    data object OperationCancelled : NfcReadResult()
}
```

### Implementing Platform Logic (iOS snippet)
```kotlin
internal actual class NfcReadManager
actual constructor(
    private val config: NfcConfig,
    nfcScanningAnimationSlot: @Composable ColumnScope.() -> Unit,
) : NSObject(), NFCTagReaderSessionDelegateProtocol {

    private var pendingResult: NfcReadResult? = null
    private var session: NFCTagReaderSession? = null

    actual fun startScanning() {
        session = NFCTagReaderSession(
            pollingOption = NFCPollingISO14443 or NFCPollingISO15693 or NFCPollingISO18092,
            delegate = this,
            queue = null,
        ).apply {
            alertMessage = config.subtitleMessage
            beginSession()
        }
    }

    // Result is captured before invalidation and emitted after dialog dismisses
    private fun finishSession(session: NFCTagReaderSession, result: NfcReadResult) {
        pendingResult = result
        session.invalidateSession()
    }

    override fun tagReaderSession(session: NFCTagReaderSession, didInvalidateWithError: NSError) {
        updateState(pendingResult ?: NfcReadResult.OperationCancelled)
        pendingResult = null
    }
}
```