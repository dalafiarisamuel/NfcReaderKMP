package com.devtamuno.kmp.nfcreader.data

import com.devtamuno.kmp.nfcreader.contract.NFC_URI_PREFIXES
import com.devtamuno.kmp.nfcreader.ndef.NdefMessage
import com.devtamuno.kmp.nfcreader.ndef.NdefMessageParser
import com.devtamuno.kmp.nfcreader.ndef.NdefRecord
import com.devtamuno.kmp.nfcreader.ndef.parsers.ExternalTypeParser
import com.devtamuno.kmp.nfcreader.ndef.parsers.MimeParser
import com.devtamuno.kmp.nfcreader.ndef.parsers.SmartPosterParser
import com.devtamuno.kmp.nfcreader.ndef.parsers.TextParser
import com.devtamuno.kmp.nfcreader.ndef.parsers.UriParser
import com.devtamuno.kmp.nfcreader.ndef.parsers.VCardParser
import com.devtamuno.kmp.nfcreader.ndef.parsers.WifiParser

/**
 * A sealed interface representing different types of parsed NFC payloads.
 */
sealed interface ParsedNfcPayload {
    /**
     * Represents a URI payload (e.g., website URL, mailto, tel).
     * @property url The full URI string.
     */
    data class Uri(val url: String) : ParsedNfcPayload

    /**
     * Represents a plain text payload.
     * @property text The decoded text content.
     */
    data class Text(val text: String) : ParsedNfcPayload

    /**
     * Represents a Contact (vCard) payload.
     * @property name The full name of the contact.
     * @property phone The phone number, if available.
     * @property email The email address, if available.
     */
    data class Contact(
        val name: String,
        val phone: String?,
        val email: String?,
    ) : ParsedNfcPayload

    /**
     * Represents a WiFi configuration payload.
     * @property ssid The network name.
     * @property password The network password, if any.
     * @property encryption The encryption type (e.g., WPA, WEP).
     */
    data class Wifi(
        val ssid: String,
        val password: String?,
        val encryption: String?,
    ) : ParsedNfcPayload

    /**
     * Represents a MIME type payload.
     * @property mimeType The MIME type string.
     * @property data The raw payload data.
     */
    data class Mime(
        val mimeType: String,
        val data: ByteArray,
    ) : ParsedNfcPayload {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Mime) return false
            if (mimeType != other.mimeType) return false
            if (!data.contentEquals(other.data)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = mimeType.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    /**
     * Represents an NFC external type record.
     * @property type The external type name.
     * @property data The raw payload data.
     */
    data class External(
        val type: String,
        val data: ByteArray,
    ) : ParsedNfcPayload {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is External) return false
            if (type != other.type) return false
            if (!data.contentEquals(other.data)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    /**
     * Represents an Android Application Record (AAR).
     * @property packageName The Android package name.
     */
    data class AndroidApplication(
        val packageName: String,
    ) : ParsedNfcPayload

    /**
     * Represents an NFC Forum Smart Poster.
     * @property title The title of the poster, if available.
     * @property uri The URI of the poster, if available.
     * @property action The action defined for the poster, if available.
     */
    data class SmartPoster(
        val title: String?,
        val uri: String?,
        val action: String?,
    ) : ParsedNfcPayload
}

/**
 * A robust parser to extract structured data from NDEF records.
 */
object NdefParser {

    private val textParser = TextParser()
    private val uriParser = UriParser()
    private val vCardParser = VCardParser()
    private val wifiParser = WifiParser()
    private val mimeParser = MimeParser(vCardParser)
    private val externalTypeParser = ExternalTypeParser()
    private val smartPosterParser = SmartPosterParser().apply {
        setParsers(textParser, uriParser)
    }

    private val payloadParsers = listOf(
        textParser,
        uriParser,
        smartPosterParser,
        vCardParser,
        wifiParser,
        mimeParser,
        externalTypeParser
    )

    /**
     * Parses an NDEF record into a structured [ParsedNfcPayload].
     */
    fun parseRecord(record: NdefRecord): ParsedNfcPayload? {
        for (parser in payloadParsers) {
            if (parser.canParse(record)) {
                return parser.parse(record)
            }
        }
        return null
    }

    /**
     * Parses an [NdefMessage] into a list of [ParsedNfcPayload].
     */
    fun parseMessage(message: NdefMessage): List<ParsedNfcPayload> {
        return message.records.mapNotNull { parseRecord(it) }
    }

    /**
     * Parses raw bytes into a list of [ParsedNfcPayload].
     */
    fun parse(bytes: ByteArray): List<ParsedNfcPayload> {
        val message = NdefMessageParser.parse(bytes) ?: return emptyList()
        return parseMessage(message)
    }

}

/**
 * Internal legacy parser for compatibility with existing String-based usage.
 */
private object LegacyNdefParser {
    private const val VCARD_MARKER = "BEGIN:VCARD"
    private const val WIFI_MARKER = "WIFI:"

    private val VCARD_NAME_REGEX = Regex("(?i)FN:(.*)")
    private val VCARD_ALT_NAME_REGEX = Regex("(?i)N:(.*)")
    private val VCARD_PHONE_REGEX = Regex("(?i)TEL.*:(.*)")
    private val VCARD_EMAIL_REGEX = Regex("(?i)EMAIL.*:(.*)")

    private val WIFI_SSID_REGEX = Regex("(?i)S:(.*?);")
    private val WIFI_PASS_REGEX = Regex("(?i)P:(.*?);")
    private val WIFI_TYPE_REGEX = Regex("(?i)T:(.*?);")

    fun parse(payload: String?): ParsedNfcPayload {
        val cleanPayload = payload?.trim()
        if (cleanPayload.isNullOrBlank()) return ParsedNfcPayload.Text("")

        return when {
            isUri(cleanPayload) -> ParsedNfcPayload.Uri(cleanPayload)
            cleanPayload.contains(VCARD_MARKER, ignoreCase = true) -> parseVCard(cleanPayload)
            cleanPayload.startsWith(WIFI_MARKER, ignoreCase = true) -> parseWifi(cleanPayload)
            else -> ParsedNfcPayload.Text(cleanPayload)
        }
    }

    private fun isUri(payload: String): Boolean {
        return NFC_URI_PREFIXES.any { it.isNotEmpty() && payload.startsWith(it, ignoreCase = true) }
    }

    private fun parseVCard(payload: String): ParsedNfcPayload {
        val name = VCARD_NAME_REGEX.findValue(payload)
            ?: VCARD_ALT_NAME_REGEX.findValue(payload)
                ?.replace(";", " ")
                ?.trim()
            ?: "Unknown Contact"

        val phone = VCARD_PHONE_REGEX.findValue(payload)
        val email = VCARD_EMAIL_REGEX.findValue(payload)

        return ParsedNfcPayload.Contact(name, phone, email)
    }

    private fun parseWifi(payload: String): ParsedNfcPayload {
        val ssid = WIFI_SSID_REGEX.findValue(payload) ?: "Unknown SSID"
        val password = WIFI_PASS_REGEX.findValue(payload)
        val encryption = WIFI_TYPE_REGEX.findValue(payload)

        return ParsedNfcPayload.Wifi(ssid, password, encryption)
    }

    private fun Regex.findValue(input: CharSequence): String? {
        return find(input)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    }
}
