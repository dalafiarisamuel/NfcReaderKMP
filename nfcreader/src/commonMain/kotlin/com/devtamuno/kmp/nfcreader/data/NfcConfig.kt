package com.devtamuno.kmp.nfcreader.data

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import com.devtamuno.kmp.nfcreader.ui.ScanningAnimationDefault
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
 * @property nfcScanningAnimationSlot The composable component for NFC scanning animation.
 */
data class NfcConfig(
    val titleMessage: String,
    val subtitleMessage: String,
    val buttonText: String,
    val sheetGesturesEnabled: Boolean = true,
    val shouldDismissBottomSheetOnBackPress: Boolean = false,
    val shouldDismissBottomSheetOnClickOutside: Boolean = false,
    val nfcReadTimeout: Duration = 60.seconds,
    val nfcScanningAnimationSlot: @Composable ColumnScope.() -> Unit = {
        ScanningAnimationDefault.NfcScanningAnimation()
    },
) {
    init {
        require(titleMessage.isNotBlank()) { "titleMessage cannot be blank" }
        require(subtitleMessage.isNotBlank()) { "subtitleMessage cannot be blank" }
        require(buttonText.isNotBlank()) { "buttonText cannot be blank" }
        require(nfcReadTimeout >= 5.seconds) { "nfcReadTimeout must be at least 5 seconds" }
    }
}
