package com.devtamuno.kmp.nfcreader.ndef

import com.devtamuno.kmp.nfcreader.data.ParsedNfcPayload

/**
 * Interface for parsing an [NdefRecord] into a [ParsedNfcPayload]. Implementations supplied to an
 * NFC reader must be thread-safe because platform callbacks can occur off the UI thread.
 */
interface NdefPayloadParser {
    fun canParse(record: NdefRecord): Boolean
    fun parse(record: NdefRecord): ParsedNfcPayload?
}
