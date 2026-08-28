package com.devtamuno.kmp.nfcreader.ndef.parsers

import com.devtamuno.kmp.nfcreader.data.ParsedNfcPayload
import com.devtamuno.kmp.nfcreader.ndef.NdefPayloadParser
import com.devtamuno.kmp.nfcreader.ndef.NdefRecord
import com.devtamuno.kmp.nfcreader.ndef.Tnf

/**
 * Parses an NFC Forum Well-Known Text Record.
 */
class TextParser : NdefPayloadParser {
    override fun canParse(record: NdefRecord): Boolean {
        return record.tnf == Tnf.WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT)
    }

    override fun parse(record: NdefRecord): ParsedNfcPayload? {
        if (!canParse(record)) return null
        val payload = record.payload
        if (payload.isEmpty()) return ParsedNfcPayload.Text("")

        return try {
            val statusByte = payload[0].toInt()
            val isUtf16 = (statusByte and 0x80) != 0
            val languageCodeLength = statusByte and 0x3F
            val textStartIndex = languageCodeLength + 1

            if (payload.size < textStartIndex) return null

            val textBytes = payload.copyOfRange(textStartIndex, payload.size)
            // KMP doesn't have a direct Charset API for UTF-16 in commonMain easily without helpers
            // But we can decode UTF-8 directly. For UTF-16, we might need a helper if we want to be fully compliant.
            // For now, let's use a basic implementation.
            val text = if (isUtf16) {
                decodeUtf16(textBytes)
            } else {
                textBytes.decodeToString()
            }
            ParsedNfcPayload.Text(text)
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeUtf16(bytes: ByteArray): String {
        // Simple UTF-16 BE/LE decoding logic could be complex due to BOM.
        // As a shortcut for KMP, if we don't have multi-platform charset support,
        // we can implement a basic one or just handle UTF-8 for now if that's the common case.
        // Let's try to support it.
        val sb = StringBuilder()
        var i = 0
        while (i + 1 < bytes.size) {
            val codeUnit = ((bytes[i].toInt() and 0xFF) shl 8) or (bytes[i + 1].toInt() and 0xFF)
            sb.append(codeUnit.toChar())
            i += 2
        }
        return sb.toString()
    }
}
