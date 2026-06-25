package com.devtamuno.kmp.nfcreader.data

import androidx.compose.runtime.Immutable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for NFC scanning.
 *
 * **Note:** On iOS, only [subtitleMessage] is used in the native NFC scanning UI.
 *
 * @property titleMessage The title message to be displayed during NFC scanning.
 * @property subtitleMessage The subtitle message to be displayed during NFC scanning.
 * @property buttonText The text for the button in the scanning UI.
 * @property sheetGesturesEnabled Whether sheet gestures are enabled.
 * @property shouldDismissBottomSheetOnBackPress Whether to dismiss the bottom sheet on back press.
 * @property shouldDismissBottomSheetOnClickOutside Whether to dismiss the bottom sheet on click
 *   outside.
 * @property nfcReadTimeout The timeout for NFC reading.
 * @property nfcUnsupportedMessage Error message emitted when NFC hardware is unavailable.
 * @property nfcDisabledMessage Error message emitted when NFC is turned off on the device.
 * @property nfcScanTimeoutMessage Error message emitted when scanning exceeds [nfcReadTimeout].
 * @property nfcSuccessMessage Message shown in the native iOS scanning dialog after a successful
 *   read. Has no effect on Android.
 */
@Immutable
data class NfcConfig(
    val titleMessage: String,
    val subtitleMessage: String,
    val buttonText: String,
    val sheetGesturesEnabled: Boolean = true,
    val shouldDismissBottomSheetOnBackPress: Boolean = false,
    val shouldDismissBottomSheetOnClickOutside: Boolean = false,
    val nfcReadTimeout: Duration = 60.seconds,
    val nfcUnsupportedMessage: String = "NFC is not supported on this device",
    val nfcDisabledMessage: String = "NFC is disabled on this device",
    val nfcScanTimeoutMessage: String = "NFC scan timed out",
    val nfcSuccessMessage: String = "Tag scanned successfully",
) {
    init {
        require(titleMessage.isNotBlank()) { "titleMessage cannot be blank" }
        require(subtitleMessage.isNotBlank()) { "subtitleMessage cannot be blank" }
        require(buttonText.isNotBlank()) { "buttonText cannot be blank" }
        require(nfcReadTimeout >= 5.seconds) { "nfcReadTimeout must be at least 5 seconds" }
    }
}
