package com.devtamuno.kmp.nfcreader.ndef

import com.devtamuno.kmp.nfcreader.data.NdefParser
import com.devtamuno.kmp.nfcreader.data.ParsedNfcPayload
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class NdefMessageParserTest {

    @Test
    fun parsesAChunkedRecord() {
        val bytes =
            byteArrayOf(
                0xB1.toByte(), // MB, CF, SR, TNF well-known
                0x01,
                0x05,
                'T'.code.toByte(),
                0x02,
                'e'.code.toByte(),
                'n'.code.toByte(),
                'H'.code.toByte(),
                'e'.code.toByte(),
                0x56, // ME, SR, TNF unchanged
                0x00,
                0x03,
                'l'.code.toByte(),
                'l'.code.toByte(),
                'o'.code.toByte(),
            )

        val message = NdefMessageParser.parse(bytes)

        assertEquals(1, message?.records?.size)
        assertContentEquals(NdefRecord.RTD_TEXT, message?.records?.single()?.type)
        val payload = NdefParser().parseMessage(message!!).single()
        assertIs<ParsedNfcPayload.Text>(payload)
        assertEquals("Hello", payload.text)
    }

    @Test
    fun rejectsPayloadLengthLargerThanRemainingInput() {
        val bytes =
            byteArrayOf(
                0xC1.toByte(), // MB, ME, non-short record, TNF well-known
                0x01,
                0x7F,
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                'T'.code.toByte(),
            )

        assertNull(NdefMessageParser.parse(bytes))
    }

    @Test
    fun rejectsAnIncompleteChunkedRecord() {
        val bytes =
            byteArrayOf(
                0xB1.toByte(),
                0x01,
                0x01,
                'T'.code.toByte(),
                0x02,
            )

        assertNull(NdefMessageParser.parse(bytes))
    }

    @Test
    fun rejectsAChunkContinuationWithChangedTnf() {
        val bytes =
            byteArrayOf(
                0xB1.toByte(),
                0x01,
                0x01,
                'T'.code.toByte(),
                0x02,
                0x51,
                0x00,
                0x01,
                'x'.code.toByte(),
            )

        assertNull(NdefMessageParser.parse(bytes))
    }

    @Test
    fun rejectsMessagesWithoutTheMessageEndFlag() {
        val bytes = byteArrayOf(0x91.toByte(), 0x01, 0x00, 'T'.code.toByte())

        assertNull(NdefMessageParser.parse(bytes))
    }
}
