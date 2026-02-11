package com.devtamuno.kmp.nfcreader.data

data class NfcConfig(
    val readyToScanMessage: String = "Ready to Scan",
    val cancelMessage: String = "Cancel",
    val bringTagCloserMessage: String = "Bring a tag closer to your phone to read it."
)
