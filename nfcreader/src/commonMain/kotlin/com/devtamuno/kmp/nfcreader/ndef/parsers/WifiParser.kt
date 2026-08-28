package com.devtamuno.kmp.nfcreader.ndef.parsers

import com.devtamuno.kmp.nfcreader.data.ParsedNfcPayload
import com.devtamuno.kmp.nfcreader.ndef.NdefPayloadParser
import com.devtamuno.kmp.nfcreader.ndef.NdefRecord
import com.devtamuno.kmp.nfcreader.ndef.Tnf

/**
 * Parses a WiFi configuration record.
 * Supports the "WIFI:" prefix format often found in NDEF records.
 */
class WifiParser : NdefPayloadParser {
    override fun canParse(record: NdefRecord): Boolean {
        val payloadStr = record.payload.decodeToString()
        return payloadStr.startsWith("WIFI:", ignoreCase = true)
    }

    override fun parse(record: NdefRecord): ParsedNfcPayload? {
        val payload = record.payload.decodeToString()
        if (!payload.startsWith("WIFI:", ignoreCase = true)) return null

        val ssid = extractField(payload, "S:") ?: "Unknown SSID"
        val password = extractField(payload, "P:")
        val encryption = extractField(payload, "T:")

        return ParsedNfcPayload.Wifi(ssid, password, encryption)
    }

    private fun extractField(payload: String, prefix: String): String? {
        val start = payload.indexOf(prefix, ignoreCase = true)
        if (start == -1) return null
        val valueStart = start + prefix.length
        val end = payload.indexOf(';', valueStart)
        return if (end != -1) {
            payload.substring(valueStart, end).trim()
        } else {
            payload.substring(valueStart).trim()
        }
    }
}
