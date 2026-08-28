package com.devtamuno.kmp.nfcreader.ndef.parsers

import com.devtamuno.kmp.nfcreader.data.ParsedNfcPayload
import com.devtamuno.kmp.nfcreader.ndef.NdefRecord
import com.devtamuno.kmp.nfcreader.ndef.Tnf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MimeParserTest {

    private val vCardParser = VCardParser()
    private val mimeParser = MimeParser(vCardParser)

    @Test
    fun testCanParse() {
        val mimeRecord = NdefRecord(
            tnf = Tnf.MIME_MEDIA,
            type = "text/plain".encodeToByteArray(),
            id = null,
            payload = "Hello".encodeToByteArray()
        )
        assertTrue(mimeParser.canParse(mimeRecord))

        val wellKnownRecord = NdefRecord(
            tnf = Tnf.WELL_KNOWN,
            type = NdefRecord.RTD_TEXT,
            id = null,
            payload = byteArrayOf()
        )
        assertFalse(mimeParser.canParse(wellKnownRecord))
    }

    @Test
    fun testParseGenericMime() {
        val type = "application/json"
        val content = "{\"key\": \"value\"}"
        val record = NdefRecord(
            tnf = Tnf.MIME_MEDIA,
            type = type.encodeToByteArray(),
            id = null,
            payload = content.encodeToByteArray()
        )

        val result = mimeParser.parse(record)
        assertIs<ParsedNfcPayload.Mime>(result)
        assertEquals(type, result.mimeType)
        assertEquals(content, result.data.decodeToString())
    }

    @Test
    fun testParseVCardViaMime() {
        val type = "text/vcard"
        val vcard = "BEGIN:VCARD\nVERSION:3.0\nFN:Jane Doe\nEND:VCARD"
        val record = NdefRecord(
            tnf = Tnf.MIME_MEDIA,
            type = type.encodeToByteArray(),
            id = null,
            payload = vcard.encodeToByteArray()
        )

        val result = mimeParser.parse(record)
        assertIs<ParsedNfcPayload.Contact>(result)
        assertEquals("Jane Doe", result.name)
    }
}
