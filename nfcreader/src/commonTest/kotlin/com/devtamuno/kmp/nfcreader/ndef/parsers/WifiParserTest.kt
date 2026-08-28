package com.devtamuno.kmp.nfcreader.ndef.parsers

import com.devtamuno.kmp.nfcreader.data.ParsedNfcPayload
import com.devtamuno.kmp.nfcreader.ndef.NdefRecord
import com.devtamuno.kmp.nfcreader.ndef.Tnf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WifiParserTest {

    private val parser = WifiParser()

    @Test
    fun testCanParse() {
        val wifiRecord = NdefRecord(
            tnf = Tnf.MIME_MEDIA,
            type = "application/vnd.wfa.wsc".encodeToByteArray(),
            id = null,
            payload = "WIFI:S:MyNetwork;T:WPA;P:secret;;".encodeToByteArray()
        )
        assertTrue(parser.canParse(wifiRecord))

        val otherRecord = NdefRecord(
            tnf = Tnf.WELL_KNOWN,
            type = NdefRecord.RTD_TEXT,
            id = null,
            payload = "Hello".encodeToByteArray()
        )
        assertFalse(parser.canParse(otherRecord))
    }

    @Test
    fun testParseFullWifi() {
        val payload = "WIFI:S:MyNetwork;T:WPA;P:secret;;"
        val record = NdefRecord(Tnf.MIME_MEDIA, "application/vnd.wfa.wsc".encodeToByteArray(), null, payload.encodeToByteArray())
        
        val result = parser.parse(record)
        assertIs<ParsedNfcPayload.Wifi>(result)
        assertEquals("MyNetwork", result.ssid)
        assertEquals("secret", result.password)
        assertEquals("WPA", result.encryption)
    }

    @Test
    fun testParseOpenWifi() {
        val payload = "WIFI:S:PublicWiFi;T:nopass;;"
        val record = NdefRecord(Tnf.MIME_MEDIA, "application/vnd.wfa.wsc".encodeToByteArray(), null, payload.encodeToByteArray())
        
        val result = parser.parse(record)
        assertIs<ParsedNfcPayload.Wifi>(result)
        assertEquals("PublicWiFi", result.ssid)
        assertEquals("nopass", result.encryption)
        assertEquals(null, result.password)
    }
}
