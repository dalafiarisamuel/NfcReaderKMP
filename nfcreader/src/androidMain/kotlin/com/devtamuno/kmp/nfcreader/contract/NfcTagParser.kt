package com.devtamuno.kmp.nfcreader.contract

import android.nfc.NdefRecord
import android.nfc.Tag
import com.devtamuno.kmp.nfcreader.data.NfcTagType

/**
 * Determines the [NfcTagType] based on the tag's technology list.
 * Only [IsoDep] maps to [NfcTagType.ISO7816]; bare NfcA/NfcB fall through to [NfcTagType.NON_NDEF].
 */
internal fun getNfcTagType(tag: Tag): NfcTagType {
    val techList = tag.techList
    return when {
        techList.any { it.contains("Mifare") } -> NfcTagType.MIFARE
        techList.any { it.contains("NfcV") } -> NfcTagType.ISO15693
        techList.any { it.contains("NfcF") } -> NfcTagType.FELICA
        techList.any { it.contains("IsoDep") } -> NfcTagType.ISO7816
        else -> NfcTagType.NON_NDEF
    }
}

/**
 * Maps a fully-qualified Android NFC technology class name to a user-friendly label.
 */
internal fun getFriendlyName(tech: String): String {
    return when {
        tech.contains("MifareClassic") -> "Mifare Classic"
        tech.contains("MifareUltralight") -> "Mifare Ultralight"
        tech.contains("NfcA") -> "ISO 14443-3A (NfcA)"
        tech.contains("NfcB") -> "ISO 14443-3B (NfcB)"
        tech.contains("NfcF") -> "FeliCa (NfcF)"
        tech.contains("NfcV") -> "ISO 15693 (NfcV)"
        tech.contains("IsoDep") -> "ISO 14443-4 (ISO-DEP)"
        else -> tech.split(".").last()
    }
}

/**
 * Decodes an [NdefRecord] payload to a readable string.
 * Handles Well-Known Text (RTD_TEXT), Well-Known URI (RTD_URI), and MIME/other records.
 */
internal fun NdefRecord.toReadableText(): String {
    return try {
        val payload = this.payload
        if (payload.isEmpty()) return ""
        when (// Well-Known Text: status byte (UTF flag + lang length) + lang code + text
            tnf) {
            NdefRecord.TNF_WELL_KNOWN if type.contentEquals(NdefRecord.RTD_TEXT) -> {
                val statusByte = payload[0].toInt()
                val isUtf16 = (statusByte and 0x80) != 0
                val languageCodeLength = statusByte and 0x3F
                val textStartIndex = languageCodeLength + 1
                if (payload.size <= textStartIndex) return ""
                val charset = if (isUtf16) Charsets.UTF_16 else Charsets.UTF_8
                String(payload, textStartIndex, payload.size - textStartIndex, charset)
            }
            // Well-Known URI: identifier byte prefix + URI string
            NdefRecord.TNF_WELL_KNOWN if type.contentEquals(NdefRecord.RTD_URI) -> {
                val prefix = NFC_URI_PREFIXES.getOrElse(payload[0].toInt() and 0xFF) { "" }
                prefix + String(payload, 1, payload.size - 1, Charsets.UTF_8)
            }
            // MIME or external type: raw payload as UTF-8
            else -> String(payload, Charsets.UTF_8)
        }
    } catch (_: Exception) {
        ""
    }
}