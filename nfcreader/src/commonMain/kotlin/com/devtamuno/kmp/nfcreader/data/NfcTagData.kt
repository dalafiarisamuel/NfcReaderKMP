package com.devtamuno.kmp.nfcreader.data

/**
 * Data class representing the information extracted from an NFC tag.
 *
 * @property serialNumber The unique identifier (UID) of the NFC tag, typically as a hex-encoded
 *   string.
 * @property type The [NfcTagType] of the tag.
 * @property payload The data content read from the tag. This is null if the tag is empty or
 *   [NfcTagType.NON_NDEF].
 * @property techList A list of technologies supported by the tag (e.g., "ISO 14443-3A", "Mifare
 *   Classic").
 */
data class NfcTagData(
    val serialNumber: String,
    val type: NfcTagType,
    val payload: String?,
    val techList: List<String> = emptyList(),
)
