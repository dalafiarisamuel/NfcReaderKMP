package com.devtamuno.kmp.nfcreader.ndef.parsers

import com.devtamuno.kmp.nfcreader.data.ParsedNfcPayload
import com.devtamuno.kmp.nfcreader.ndef.NdefRecord
import com.devtamuno.kmp.nfcreader.ndef.Tnf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VCardParserTest {

    private val parser = VCardParser()

    @Test
    fun testCanParse() {
        val vcardRecord = NdefRecord(
            tnf = Tnf.MIME_MEDIA,
            type = "text/vcard".encodeToByteArray(),
            id = null,
            payload = "BEGIN:VCARD\nEND:VCARD".encodeToByteArray()
        )
        assertTrue(parser.canParse(vcardRecord))

        val xVcardRecord = NdefRecord(
            tnf = Tnf.MIME_MEDIA,
            type = "text/x-vcard".encodeToByteArray(),
            id = null,
            payload = "BEGIN:VCARD\nEND:VCARD".encodeToByteArray()
        )
        assertTrue(parser.canParse(xVcardRecord))

        val wellKnownVcard = NdefRecord(
            tnf = Tnf.WELL_KNOWN,
            type = "T".encodeToByteArray(),
            id = null,
            payload = "BEGIN:VCARD\nEND:VCARD".encodeToByteArray()
        )
        assertTrue(parser.canParse(wellKnownVcard))
    }

    @Test
    fun testParseSimpleVCard() {
        val vcard = """
            BEGIN:VCARD
            VERSION:3.0
            FN:John Smith
            TEL:+15551234567
            EMAIL:john@example.com
            END:VCARD
        """.trimIndent()

        val record = NdefRecord(Tnf.MIME_MEDIA, "text/vcard".encodeToByteArray(), null, vcard.encodeToByteArray())
        val result = parser.parse(record)

        assertIs<ParsedNfcPayload.Contact>(result)
        assertEquals("John Smith", result.name)
        assertEquals("+15551234567", result.phone)
        assertEquals("john@example.com", result.email)
    }

    @Test
    fun testParseStructuredNameFallback() {
        val vcard = """
            BEGIN:VCARD
            VERSION:3.0
            N:Doe;Jane;;;
            END:VCARD
        """.trimIndent()

        val record = NdefRecord(Tnf.MIME_MEDIA, "text/vcard".encodeToByteArray(), null, vcard.encodeToByteArray())
        val result = parser.parse(record)

        assertIs<ParsedNfcPayload.Contact>(result)
        assertEquals("Doe Jane", result.name)
    }

    @Test
    fun testUnfoldVCardLines() {
        val vcard = "BEGIN:VCARD\r\nFN:John \r\n Smith\r\nEND:VCARD"
        val record = NdefRecord(Tnf.MIME_MEDIA, "text/vcard".encodeToByteArray(), null, vcard.encodeToByteArray())
        val result = parser.parse(record)

        assertIs<ParsedNfcPayload.Contact>(result)
        assertEquals("John Smith", result.name)
    }
}
