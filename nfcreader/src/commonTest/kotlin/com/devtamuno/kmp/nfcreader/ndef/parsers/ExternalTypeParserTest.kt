package com.devtamuno.kmp.nfcreader.ndef.parsers

import com.devtamuno.kmp.nfcreader.data.ParsedNfcPayload
import com.devtamuno.kmp.nfcreader.ndef.NdefRecord
import com.devtamuno.kmp.nfcreader.ndef.Tnf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExternalTypeParserTest {

    private val parser = ExternalTypeParser()

    @Test
    fun testCanParse() {
        val externalRecord = NdefRecord(
            tnf = Tnf.EXTERNAL_TYPE,
            type = "com.example:external".encodeToByteArray(),
            id = null,
            payload = byteArrayOf(0x01)
        )
        assertTrue(parser.canParse(externalRecord))

        val mimeRecord = NdefRecord(
            tnf = Tnf.MIME_MEDIA,
            type = "text/plain".encodeToByteArray(),
            id = null,
            payload = byteArrayOf()
        )
        assertFalse(parser.canParse(mimeRecord))
    }

    @Test
    fun testParseAndroidApplicationRecord() {
        val packageName = "com.devtamuno.nfc"
        val record = NdefRecord(
            tnf = Tnf.EXTERNAL_TYPE,
            type = "android.com:pkg".encodeToByteArray(),
            id = null,
            payload = packageName.encodeToByteArray()
        )

        val result = parser.parse(record)
        assertIs<ParsedNfcPayload.AndroidApplication>(result)
        assertEquals(packageName, result.packageName)
    }

    @Test
    fun testParseGenericExternalType() {
        val type = "my.domain:customType"
        val payload = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        val record = NdefRecord(
            tnf = Tnf.EXTERNAL_TYPE,
            type = type.encodeToByteArray(),
            id = null,
            payload = payload
        )

        val result = parser.parse(record)
        assertIs<ParsedNfcPayload.External>(result)
        assertEquals(type, result.type)
        assertTrue(payload.contentEquals(result.data))
    }
}
