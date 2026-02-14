package com.devtamuno.kmp.nfcreader.data

data class NfcConfig(
    val titleMessage: String,
    val subtitleMessage: String,
    val buttonText: String,
    val sheetGesturesEnabled: Boolean = true,
    val shouldDismissBottomSheetOnBackPress: Boolean = false,
    val shouldDismissBottomSheetOnClickOutside: Boolean = false,
)
