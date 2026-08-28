package com.devtamuno.kmp.nfcreader.ndef

import com.devtamuno.kmp.nfcreader.data.NdefParser
import com.devtamuno.kmp.nfcreader.data.ParsedNfcPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NdefParserTest {

    @Test
    fun testParseShortRecordText() {
        // TNF=1 (Well Known), SR=1, Type=T, Payload="enHello"
        // 0xD1 = MB=1, ME=1, CF=0, SR=1, IL=0, TNF=1
        val bytes = byteArrayOf(
            0xD1.toByte(), 0x01.toByte(), 0x08.toByte(), 0x54.toByte(),
            0x02.toByte(), 'e'.code.toByte(), 'n'.code.toByte(),
            'H'.code.toByte(), 'e'.code.toByte(), 'l'.code.toByte(), 'l'.code.toByte(), 'o'.code.toByte()
        )

        val results = NdefParser.parse(bytes)
        assertEquals(1, results.size)
        val textResult = results[0] as ParsedNfcPayload.Text
        assertEquals("Hello", textResult.text)
    }

    @Test
    fun testParseUriRecord() {
        // TNF=1, Type=U, Payload=0x04 (https://) + "google.com"
        val bytes = byteArrayOf(
            0xD1.toByte(), 0x01.toByte(), 0x0B.toByte(), 0x55.toByte(),
            0x04.toByte(),
            'g'.code.toByte(), 'o'.code.toByte(), 'o'.code.toByte(), 'g'.code.toByte(), 'l'.code.toByte(), 'e'.code.toByte(),
            '.'.code.toByte(), 'c'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte()
        )

        val results = NdefParser.parse(bytes)
        assertEquals(1, results.size)
        val uriResult = results[0] as ParsedNfcPayload.Uri
        assertEquals("https://google.com", uriResult.url)
    }

    @Test
    fun testParseWifiRecord() {
        val wifiPayload = "WIFI:S:MyNetwork;T:WPA;P:secret;;"
        val record = NdefRecord(
            tnf = Tnf.MIME_MEDIA,
            type = "text/plain".encodeToByteArray(),
            id = null,
            payload = wifiPayload.encodeToByteArray()
        )

        val parsed = NdefParser.parseRecord(record)
        assertIs<ParsedNfcPayload.Wifi>(parsed)
        assertEquals("MyNetwork", parsed.ssid)
        assertEquals("secret", parsed.password)
        assertEquals("WPA", parsed.encryption)
    }

    @Test
    fun testParseVCardRecord() {
        val vcard = "BEGIN:VCARD\nVERSION:3.0\nFN:John Doe\nTEL:+123456789\nEND:VCARD"
        val record = NdefRecord(
            tnf = Tnf.MIME_MEDIA,
            type = "text/vcard".encodeToByteArray(),
            id = null,
            payload = vcard.encodeToByteArray()
        )

        val parsed = NdefParser.parseRecord(record)
        assertIs<ParsedNfcPayload.Contact>(parsed)
        assertEquals("John Doe", parsed.name)
        assertEquals("+123456789", parsed.phone)
    }

    @Test
    fun testParseAarRecord() {
        val packageName = "com.example.app"
        val record = NdefRecord(
            tnf = Tnf.EXTERNAL_TYPE,
            type = "android.com:pkg".encodeToByteArray(),
            id = null,
            payload = packageName.encodeToByteArray()
        )

        val parsed = NdefParser.parseRecord(record)
        assertIs<ParsedNfcPayload.AndroidApplication>(parsed)
        assertEquals(packageName, parsed.packageName)
    }

    @Test
    fun testParseSmartPoster() {
        // Smart Poster containing a URI record
        val nestedUriRecord = byteArrayOf(
            0xD1.toByte(), 0x01.toByte(), 0x0B.toByte(), 0x55.toByte(),
            0x04.toByte(),
            'e'.code.toByte(), 'x'.code.toByte(), 'a'.code.toByte(), 'm'.code.toByte(), 'p'.code.toByte(), 'l'.code.toByte(),
            'e'.code.toByte(), '.'.code.toByte(), 'c'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte()
        )

        val record = NdefRecord(
            tnf = Tnf.WELL_KNOWN,
            type = NdefRecord.RTD_SMART_POSTER,
            id = null,
            payload = nestedUriRecord
        )

        val parsed = NdefParser.parseRecord(record)
        assertIs<ParsedNfcPayload.SmartPoster>(parsed)
        assertEquals("https://example.com", parsed.uri)
    }
}
