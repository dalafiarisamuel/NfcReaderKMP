package com.devtamuno.kmp.nfcreader.ndef.parsers

import com.devtamuno.kmp.nfcreader.data.ParsedNfcPayload
import com.devtamuno.kmp.nfcreader.ndef.NdefPayloadParser
import com.devtamuno.kmp.nfcreader.ndef.NdefRecord
import com.devtamuno.kmp.nfcreader.ndef.Tnf

/**
 * Parses a vCard contact payload.
 */
class VCardParser : NdefPayloadParser {
    override fun canParse(record: NdefRecord): Boolean {
        // vCards can be WELL_KNOWN (rare) or MIME_MEDIA (text/vcard or text/x-vcard)
        val type = record.type.decodeToString()
        return (record.tnf == Tnf.MIME_MEDIA && (type.contains("vcard", ignoreCase = true) || type.contains("x-vcard", ignoreCase = true))) ||
                (record.tnf == Tnf.WELL_KNOWN && record.payload.decodeToString().contains("BEGIN:VCARD", ignoreCase = true))
    }

    override fun parse(record: NdefRecord): ParsedNfcPayload? {
        val rawText = record.payload.decodeToString()
        if (!rawText.contains("BEGIN:VCARD", ignoreCase = true)) return null

        val lines = unfoldVCard(rawText)
        var name: String? = null
        var structuredName: String? = null
        var phone: String? = null
        var email: String? = null

        for (line in lines) {
            val parts = line.split(":", limit = 2)
            if (parts.size < 2) continue
            val key = parts[0].uppercase()
            val value = parts[1].trim()

            when {
                key.startsWith("FN") -> name = value
                key.startsWith("N") && structuredName == null -> structuredName = value.replace(";", " ").trim()
                key.startsWith("TEL") && phone == null -> phone = extractValue(value)
                key.startsWith("EMAIL") && email == null -> email = extractValue(value)
            }
        }

        val displayName = name ?: structuredName ?: "Unknown Contact"
        return ParsedNfcPayload.Contact(displayName, phone, email)
    }

    private fun unfoldVCard(input: String): List<String> {
        val unfolded = input.replace(Regex("\r?\n[ \t]"), "")
        return unfolded.split(Regex("\r?\n")).filter { it.isNotBlank() }
    }

    private fun extractValue(value: String): String {
        // Simple extraction, could be improved to handle parameters more robustly if needed
        return value
    }
}
