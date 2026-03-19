@file:OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)

package com.devtamuno.kmp.nfcreader.contract

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import platform.CoreNFC.NFCNDEFPayload
import platform.CoreNFC.NFCTagProtocol
import platform.CoreNFC.NFCTagTypeFeliCa
import platform.CoreNFC.NFCTagTypeISO15693
import platform.CoreNFC.NFCTagTypeISO7816Compatible
import platform.CoreNFC.NFCTagTypeMiFare
import platform.CoreNFC.NFCTypeNameFormatNFCWellKnown
import platform.Foundation.NSData
import platform.posix.memcpy

/**
 * Extracts the UID from an [NFCTagProtocol], formatted as an uppercase hex string
 * with ":" byte separators (e.g. "04:1A:2B:3C").
 */
internal fun extractUid(tag: NFCTagProtocol): String {
    return when (tag.type) {
        NFCTagTypeMiFare -> tag.asNFCMiFareTag()?.identifier?.toByteArray()?.toHex() ?: ""
        NFCTagTypeISO15693 -> tag.asNFCISO15693Tag()?.identifier?.toByteArray()?.toHex() ?: ""
        NFCTagTypeISO7816Compatible ->
            tag.asNFCISO7816Tag()?.identifier?.toByteArray()?.toHex() ?: ""
        NFCTagTypeFeliCa -> tag.asNFCFeliCaTag()?.currentIDm?.toByteArray()?.toHex() ?: ""
        else -> ""
    }
}

/**
 * Returns a list of user-friendly technology names supported by the given [NFCTagProtocol].
 */
internal fun getTechList(tag: NFCTagProtocol): List<String> {
    return when (tag.type) {
        NFCTagTypeMiFare -> listOf("ISO 14443-3A", "NfcA", "Mifare")
        NFCTagTypeISO15693 -> listOf("ISO 15693", "NfcV")
        NFCTagTypeISO7816Compatible -> {
            val list = mutableListOf("ISO 14443-4", "ISO-DEP")
            val iso7816 = tag.asNFCISO7816Tag()
            if (iso7816?.historicalBytes != null) {
                list.add("ISO 14443-3A")
                list.add("NfcA")
            } else if (iso7816?.applicationData != null) {
                list.add("ISO 14443-3B")
                list.add("NfcB")
            }
            list.add("ISO 7816")
            list
        }
        NFCTagTypeFeliCa -> listOf("FeliCa", "NfcF", "JIS 6319-4")
        else -> listOf("Unknown")
    }
}

/**
 * Decodes an [NFCNDEFPayload] to a readable string.
 * Handles Well-Known Text (type 'T'), Well-Known URI (type 'U'), and MIME/other records.
 */
internal fun NFCNDEFPayload.readableText(): String {
    return try {
        val bytes = payload.toByteArray()
        if (bytes.isEmpty()) return ""
        val typeBytes = type.toByteArray()

        when (typeNameFormat) {
            // Well-Known Text: status byte (encoding + lang length) + lang code + text
            NFCTypeNameFormatNFCWellKnown if typeBytes.size == 1 && typeBytes[0] == 'T'.code.toByte() -> {
                val statusByte = bytes[0].toInt()
                val isUtf16 = (statusByte and 0x80) != 0
                val languageCodeLength = statusByte and 0x3F
                val textStartIndex = languageCodeLength + 1
                if (bytes.size <= textStartIndex) return ""
                val textBytes = bytes.copyOfRange(textStartIndex, bytes.size)
                if (isUtf16) textBytes.decodeUtf16BE() else textBytes.decodeToString()
            }
            // Well-Known URI: identifier byte prefix + URI string
            NFCTypeNameFormatNFCWellKnown if typeBytes.size == 1 && typeBytes[0] == 'U'.code.toByte() -> {
                val prefix = NFC_URI_PREFIXES.getOrElse(bytes[0].toInt() and 0xFF) { "" }
                prefix + bytes.copyOfRange(1, bytes.size).decodeToString()
            }
            // MIME or external type: raw bytes as UTF-8
            else -> bytes.decodeToString()
        }
    } catch (_: Exception) {
        ""
    }
}

/** Decodes a UTF-16 Big Endian byte array to a String. */
private fun ByteArray.decodeUtf16BE(): String {
    val sb = StringBuilder(size / 2)
    var i = 0
    while (i + 1 < size) {
        val codeUnit = ((this[i].toInt() and 0xFF) shl 8) or (this[i + 1].toInt() and 0xFF)
        sb.append(codeUnit.toChar())
        i += 2
    }
    return sb.toString()
}

/** Copies an [NSData] buffer into a [ByteArray]. */
internal fun NSData.toByteArray(): ByteArray {
    val bytes = ByteArray(length.toInt())
    memcpy(bytes.refTo(0), this.bytes, length)
    return bytes
}

/** Formats a [ByteArray] as an uppercase hex string with ":" byte separators. */
internal fun ByteArray.toHex(): String =
    toHexString(
        HexFormat {
            upperCase = true
            bytes { byteSeparator = ":" }
        }
    )