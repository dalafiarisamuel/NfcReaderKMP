package com.devtamuno.kmp.nfcreader.data

import androidx.compose.runtime.Immutable
import com.devtamuno.kmp.nfcreader.ndef.NdefMessage
import com.devtamuno.kmp.nfcreader.ndef.NdefMessageParser
import com.devtamuno.kmp.nfcreader.ndef.NdefPayloadParser
import com.devtamuno.kmp.nfcreader.ndef.NdefRecord
import com.devtamuno.kmp.nfcreader.ndef.parsers.ExternalTypeParser
import com.devtamuno.kmp.nfcreader.ndef.parsers.MimeParser
import com.devtamuno.kmp.nfcreader.ndef.parsers.SmartPosterParser
import com.devtamuno.kmp.nfcreader.ndef.parsers.TextParser
import com.devtamuno.kmp.nfcreader.ndef.parsers.UriParser
import com.devtamuno.kmp.nfcreader.ndef.parsers.VCardParser
import com.devtamuno.kmp.nfcreader.ndef.parsers.WifiParser

/**
 * Represents structured NFC payload data. Applications may implement this interface for custom
 * record types returned by a custom [NdefPayloadParser].
 */
interface ParsedNfcPayload {
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
 * A parser that extracts structured data from NDEF records.
 *
 * Instances are immutable and can safely be shared between platform NFC callback threads. Custom
 * parsers are evaluated in list order before the built-in parsers and must themselves be
 * thread-safe.
 *
 * @param customParsers Application-defined parsers to run before the built-in parsers.
 */
@Immutable
class NdefParser(customParsers: List<NdefPayloadParser> = emptyList()) {

    private val textParser = TextParser()
    private val uriParser = UriParser()
    private val vCardParser = VCardParser()
    private val wifiParser = WifiParser()
    private val mimeParser = MimeParser(vCardParser)
    private val externalTypeParser = ExternalTypeParser()
    private val smartPosterParser = SmartPosterParser(textParser, uriParser)

    private val customParsers = customParsers.toList()
    private val payloadParsers =
        this.customParsers +
            listOf(
                textParser,
                uriParser,
                smartPosterParser,
                vCardParser,
                wifiParser,
                mimeParser,
                externalTypeParser,
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

    companion object {
        /** Shared parser containing only the built-in payload parsers. */
        val Default = NdefParser()
    }
}
