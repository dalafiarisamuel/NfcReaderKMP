package com.devtamuno.kmp.nfcreader.ndef.parsers

import com.devtamuno.kmp.nfcreader.data.ParsedNfcPayload
import com.devtamuno.kmp.nfcreader.ndef.NdefRecord
import com.devtamuno.kmp.nfcreader.ndef.Tnf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextParserTest {

    private val parser = TextParser()

    @Test
    fun testCanParse() {
        val textRecord = NdefRecord(
            tnf = Tnf.WELL_KNOWN,
            type = NdefRecord.RTD_TEXT,
            id = null,
            payload = byteArrayOf()
        )
        assertTrue(parser.canParse(textRecord))

        val uriRecord = NdefRecord(
            tnf = Tnf.WELL_KNOWN,
            type = NdefRecord.RTD_URI,
            id = null,
            payload = byteArrayOf()
        )
        assertFalse(parser.canParse(uriRecord))
    }

    @Test
    fun testParseUtf8() {
        // Status byte: 0x02 (UTF-8, language code "en" length 2)
        val payload = byteArrayOf(0x02.toByte()) + "enHello".encodeToByteArray()
        val record = NdefRecord(Tnf.WELL_KNOWN, NdefRecord.RTD_TEXT, null, payload)

        val result = parser.parse(record)
        assertIs<ParsedNfcPayload.Text>(result)
        assertEquals("Hello", result.text)
    }

    @Test
    fun testParseUtf16() {
        // Status byte: 0x82 (UTF-16, language code "en" length 2)
        // UTF-16 text for "Hi" (BE): 0x00 0x48 0x00 0x69
        val payload = byteArrayOf(0x82.toByte()) + "en".encodeToByteArray() + byteArrayOf(0x00, 0x48, 0x00, 0x69)
        val record = NdefRecord(Tnf.WELL_KNOWN, NdefRecord.RTD_TEXT, null, payload)

        val result = parser.parse(record)
        assertIs<ParsedNfcPayload.Text>(result)
        assertEquals("Hi", result.text)
    }

    @Test
    fun testEmptyPayload() {
        val record = NdefRecord(Tnf.WELL_KNOWN, NdefRecord.RTD_TEXT, null, byteArrayOf())
        val result = parser.parse(record)
        assertIs<ParsedNfcPayload.Text>(result)
        assertEquals("", result.text)
    }
}
