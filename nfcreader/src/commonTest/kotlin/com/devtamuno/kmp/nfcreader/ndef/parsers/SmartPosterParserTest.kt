package com.devtamuno.kmp.nfcreader.ndef.parsers

import com.devtamuno.kmp.nfcreader.data.ParsedNfcPayload
import com.devtamuno.kmp.nfcreader.ndef.NdefRecord
import com.devtamuno.kmp.nfcreader.ndef.Tnf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SmartPosterParserTest {

    private val textParser = TextParser()
    private val uriParser = UriParser()
    private val smartPosterParser = SmartPosterParser().apply {
        setParsers(textParser, uriParser)
    }

    @Test
    fun testCanParse() {
        val record = NdefRecord(
            tnf = Tnf.WELL_KNOWN,
            type = NdefRecord.RTD_SMART_POSTER,
            id = null,
            payload = byteArrayOf()
        )
        assertTrue(smartPosterParser.canParse(record))
    }

    @Test
    fun testParseSmartPosterWithUriAndTitle() {
        // Nested URI record: https://google.com
        val uriPayload = byteArrayOf(0x04.toByte()) + "google.com".encodeToByteArray()
        val uriRecordBytes = byteArrayOf(
            0xD1.toByte(), 0x01.toByte(), uriPayload.size.toByte(), 0x55.toByte()
        ) + uriPayload

        // Nested Text record: "Hello" (en)
        val textPayload = byteArrayOf(0x02.toByte()) + "enHello".encodeToByteArray()
        val textRecordBytes = byteArrayOf(
            0x51.toByte(), 0x01.toByte(), textPayload.size.toByte(), 0x54.toByte()
        ) + textPayload
        
        // Note: MB=1 for first nested, ME=1 for last nested.
        // uriRecordBytes has 0xD1 (MB=1, ME=1). Let's fix flags for a sequence.
        // First record: MB=1, ME=0 -> 0x91
        // Last record: MB=0, ME=1 -> 0x51
        val firstNested = uriRecordBytes.copyOf()
        firstNested[0] = 0x91.toByte() 
        
        val lastNested = textRecordBytes.copyOf()
        lastNested[0] = 0x51.toByte()

        val smartPosterPayload = firstNested + lastNested
        val record = NdefRecord(Tnf.WELL_KNOWN, NdefRecord.RTD_SMART_POSTER, null, smartPosterPayload)

        val result = smartPosterParser.parse(record)
        assertIs<ParsedNfcPayload.SmartPoster>(result)
        assertEquals("https://google.com", result.uri)
        assertEquals("Hello", result.title)
    }

    @Test
    fun testParseSmartPosterAction() {
        // Action record: TNF=1, Type='a', Payload=[1] (Save for later)
        val actionRecordBytes = byteArrayOf(
            0xD1.toByte(), 0x01.toByte(), 0x01.toByte(), 'a'.code.toByte(), 0x01.toByte()
        )

        val record = NdefRecord(Tnf.WELL_KNOWN, NdefRecord.RTD_SMART_POSTER, null, actionRecordBytes)
        val result = smartPosterParser.parse(record)

        assertIs<ParsedNfcPayload.SmartPoster>(result)
        assertEquals("Save for later", result.action)
    }
}
