package com.devtamuno.kmp.nfcreader.ndef.parsers

import com.devtamuno.kmp.nfcreader.contract.NFC_URI_PREFIXES
import com.devtamuno.kmp.nfcreader.data.ParsedNfcPayload
import com.devtamuno.kmp.nfcreader.ndef.NdefPayloadParser
import com.devtamuno.kmp.nfcreader.ndef.NdefRecord
import com.devtamuno.kmp.nfcreader.ndef.Tnf

/**
 * Parses an NFC Forum Well-Known URI Record.
 */
class UriParser : NdefPayloadParser {
    override fun canParse(record: NdefRecord): Boolean {
        return record.tnf == Tnf.WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_URI)
    }

    override fun parse(record: NdefRecord): ParsedNfcPayload? {
        if (!canParse(record)) return null
        val payload = record.payload
        if (payload.isEmpty()) return null

        val prefixIndex = payload[0].toInt() and 0xFF
        val prefix = NFC_URI_PREFIXES.getOrElse(prefixIndex) { "" }
        val uriBody = payload.copyOfRange(1, payload.size).decodeToString()

        return ParsedNfcPayload.Uri(prefix + uriBody)
    }
}
