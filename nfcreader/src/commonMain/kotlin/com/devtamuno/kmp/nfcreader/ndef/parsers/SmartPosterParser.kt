package com.devtamuno.kmp.nfcreader.ndef.parsers

import com.devtamuno.kmp.nfcreader.data.ParsedNfcPayload
import com.devtamuno.kmp.nfcreader.ndef.NdefMessageParser
import com.devtamuno.kmp.nfcreader.ndef.NdefPayloadParser
import com.devtamuno.kmp.nfcreader.ndef.NdefRecord
import com.devtamuno.kmp.nfcreader.ndef.Tnf

/**
 * Parses an NFC Forum Smart Poster Record.
 * Smart Posters contain nested NDEF records.
 */
class SmartPosterParser(
    private val textParser: TextParser,
    private val uriParser: UriParser,
) : NdefPayloadParser {

    override fun canParse(record: NdefRecord): Boolean {
        return record.tnf == Tnf.WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_SMART_POSTER)
    }

    override fun parse(record: NdefRecord): ParsedNfcPayload? {
        if (!canParse(record)) return null

        val nestedMessage = NdefMessageParser.parse(record.payload) ?: return null
        var title: String? = null
        var uri: String? = null
        var action: String? = null

        for (nestedRecord in nestedMessage.records) {
            when {
                textParser.canParse(nestedRecord) -> {
                    val parsed = textParser.parse(nestedRecord) as? ParsedNfcPayload.Text
                    if (title == null) title = parsed?.text
                }
                uriParser.canParse(nestedRecord) -> {
                    val parsed = uriParser.parse(nestedRecord) as? ParsedNfcPayload.Uri
                    if (uri == null) uri = parsed?.url
                }
                nestedRecord.tnf == Tnf.WELL_KNOWN && nestedRecord.type.contentEquals(byteArrayOf('a'.code.toByte())) -> {
                    // Action record
                    if (nestedRecord.payload.isNotEmpty()) {
                        action = when (nestedRecord.payload[0].toInt()) {
                            0 -> "Do action"
                            1 -> "Save for later"
                            2 -> "Open for editing"
                            else -> null
                        }
                    }
                }
            }
        }

        return ParsedNfcPayload.SmartPoster(title, uri, action)
    }
}
