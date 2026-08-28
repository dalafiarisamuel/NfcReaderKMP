package com.devtamuno.kmp.nfcreader.ndef

import com.devtamuno.kmp.nfcreader.data.ParsedNfcPayload

/**
 * Interface for parsing an [NdefRecord] into a [ParsedNfcPayload].
 */
interface NdefPayloadParser {
    fun canParse(record: NdefRecord): Boolean
    fun parse(record: NdefRecord): ParsedNfcPayload?
}
