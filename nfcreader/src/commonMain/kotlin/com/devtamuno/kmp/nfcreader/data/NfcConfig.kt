package com.devtamuno.kmp.nfcreader.data

import androidx.compose.runtime.Immutable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for NFC scanning.
 *
 * This class groups configuration values by platform to make it clear where they are used,
 * while still allowing them to be configured from common code.
 *
 * @property titleMessage The title message to be displayed during NFC scanning.
 * @property subtitleMessage The subtitle message to be displayed during NFC scanning.
 * @property buttonText The text for the button in the scanning UI.
 * @property nfcUnsupportedMessage Error message emitted when NFC hardware is unavailable.
 * @property nfcDisabledMessage Error message emitted when NFC is turned off on the device.
 * @property android Android-specific configuration options.
 * @property ios iOS-specific configuration options.
 */
@Immutable
data class NfcConfig(
    val titleMessage: String,
    val subtitleMessage: String,
    val buttonText: String,
    val nfcUnsupportedMessage: String = "NFC is not supported on this device",
    val nfcDisabledMessage: String = "NFC is disabled on this device",
    val android: AndroidOptions = AndroidOptions(),
    val ios: IosOptions = IosOptions(),
) {
    init {
        require(titleMessage.isNotBlank()) { "titleMessage cannot be blank" }
        require(subtitleMessage.isNotBlank()) { "subtitleMessage cannot be blank" }
        require(buttonText.isNotBlank()) { "buttonText cannot be blank" }
    }

    @Immutable
    data class AndroidOptions(
        val nfcReadTimeout: Duration = 60.seconds,
        val nfcScanTimeoutMessage: String = "NFC scan timed out",
        val sheetGesturesEnabled: Boolean = true,
        val shouldDismissBottomSheetOnBackPress: Boolean = false,
        val shouldDismissBottomSheetOnClickOutside: Boolean = false,
    ) {
        init {
            require(nfcReadTimeout >= 5.seconds) { "nfcReadTimeout must be at least 5 seconds" }
            require(nfcScanTimeoutMessage.isNotBlank()) { "nfcScanTimeoutMessage cannot be blank" }
        }
    }

    @Immutable
    data class IosOptions(
        val nfcSuccessMessage: String = "Tag scanned successfully",
    ) {
        init {
            require(nfcSuccessMessage.isNotBlank()) { "nfcSuccessMessage cannot be blank" }
        }
    }
}
