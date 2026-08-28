package com.devtamuno.kmp.nfcreader.ndef.parsers

import com.devtamuno.kmp.nfcreader.data.ParsedNfcPayload
import com.devtamuno.kmp.nfcreader.ndef.NdefRecord
import com.devtamuno.kmp.nfcreader.ndef.Tnf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UriParserTest {

    private val parser = UriParser()

    @Test
    fun testCanParse() {
        val uriRecord = NdefRecord(
            tnf = Tnf.WELL_KNOWN,
            type = NdefRecord.RTD_URI,
            id = null,
            payload = byteArrayOf(0x01.toByte()) // http://www.
        )
        assertTrue(parser.canParse(uriRecord))

        val textRecord = NdefRecord(
            tnf = Tnf.WELL_KNOWN,
            type = NdefRecord.RTD_TEXT,
            id = null,
            payload = byteArrayOf()
        )
        assertFalse(parser.canParse(textRecord))
    }

    @Test
    fun testParseHttpWww() {
        // Prefix 0x01: http://www.
        val payload = byteArrayOf(0x01.toByte()) + "google.com".encodeToByteArray()
        val record = NdefRecord(Tnf.WELL_KNOWN, NdefRecord.RTD_URI, null, payload)

        val result = parser.parse(record)
        assertIs<ParsedNfcPayload.Uri>(result)
        assertEquals("http://www.google.com", result.url)
    }

    @Test
    fun testParseHttps() {
        // Prefix 0x04: https://
        val payload = byteArrayOf(0x04.toByte()) + "github.com".encodeToByteArray()
        val record = NdefRecord(Tnf.WELL_KNOWN, NdefRecord.RTD_URI, null, payload)

        val result = parser.parse(record)
        assertIs<ParsedNfcPayload.Uri>(result)
        assertEquals("https://github.com", result.url)
    }

    @Test
    fun testParseNoPrefix() {
        // Prefix 0x00: No prefix
        val payload = byteArrayOf(0x00.toByte()) + "custom:scheme".encodeToByteArray()
        val record = NdefRecord(Tnf.WELL_KNOWN, NdefRecord.RTD_URI, null, payload)

        val result = parser.parse(record)
        assertIs<ParsedNfcPayload.Uri>(result)
        assertEquals("custom:scheme", result.url)
    }
}
