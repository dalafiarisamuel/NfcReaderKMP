package com.devtamuno.kmp.nfcreader.ndef

/**
 * Represents an NDEF message containing one or more NDEF records.
 */
data class NdefMessage(
    val records: List<NdefRecord>
)
