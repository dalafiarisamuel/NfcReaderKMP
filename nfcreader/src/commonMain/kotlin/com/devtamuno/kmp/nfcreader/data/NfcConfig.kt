package com.devtamuno.kmp.nfcreader.data

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class NfcConfig(
    val titleMessage: String,
    val subtitleMessage: String,
    val buttonText: String,
    val sheetGesturesEnabled: Boolean = true,
    val shouldDismissBottomSheetOnBackPress: Boolean = false,
    val shouldDismissBottomSheetOnClickOutside: Boolean = false,
    val nfcReadTimeout: Duration = 60.seconds,
) {
    init {
        require(titleMessage.isNotBlank()) { "titleMessage cannot be blank" }
        require(subtitleMessage.isNotBlank()) { "subtitleMessage cannot be blank" }
        require(buttonText.isNotBlank()) { "buttonText cannot be blank" }
        require(nfcReadTimeout >= 10.seconds) { "nfcReadTimeout must be at least 10 seconds" }
    }
}
