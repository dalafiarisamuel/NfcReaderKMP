package com.devtamuno.kmp.nfcreader.ndef.parsers

import com.devtamuno.kmp.nfcreader.data.ParsedNfcPayload
import com.devtamuno.kmp.nfcreader.ndef.NdefPayloadParser
import com.devtamuno.kmp.nfcreader.ndef.NdefRecord
import com.devtamuno.kmp.nfcreader.ndef.Tnf

/**
 * Parses an NFC External Type Record.
 */
class ExternalTypeParser : NdefPayloadParser {
    override fun canParse(record: NdefRecord): Boolean {
        return record.tnf == Tnf.EXTERNAL_TYPE
    }

    override fun parse(record: NdefRecord): ParsedNfcPayload? {
        if (!canParse(record)) return null
        val type = record.type.decodeToString()

        return if (type == "android.com:pkg") {
            ParsedNfcPayload.AndroidApplication(record.payload.decodeToString())
        } else {
            ParsedNfcPayload.External(type, record.payload)
        }
    }
}
