package com.devtamuno.kmp.nfcreader.ndef.parsers

import com.devtamuno.kmp.nfcreader.data.ParsedNfcPayload
import com.devtamuno.kmp.nfcreader.ndef.NdefPayloadParser
import com.devtamuno.kmp.nfcreader.ndef.NdefRecord
import com.devtamuno.kmp.nfcreader.ndef.Tnf

/**
 * Parses an NDEF MIME Record.
 */
class MimeParser(
    private val vCardParser: VCardParser
) : NdefPayloadParser {
    override fun canParse(record: NdefRecord): Boolean {
        return record.tnf == Tnf.MIME_MEDIA
    }

    override fun parse(record: NdefRecord): ParsedNfcPayload? {
        if (!canParse(record)) return null
        val mimeType = record.type.decodeToString()

        return when {
            mimeType.contains("vcard", ignoreCase = true) -> vCardParser.parse(record)
            else -> ParsedNfcPayload.Mime(mimeType, record.payload)
        }
    }
}
