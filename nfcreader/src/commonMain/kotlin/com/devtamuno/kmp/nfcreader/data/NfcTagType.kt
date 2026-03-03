package com.devtamuno.kmp.nfcreader.data

/**
 * Enumeration of supported NFC tag types.
 */
enum class NfcTagType {
    /**
     * NFC Data Exchange Format (NDEF) tag.
     */
    NDEF,

    /**
     * A tag that does not contain NDEF data or whose technology is not NDEF-compatible.
     */
    NON_NDEF,

    /**
     * MIFARE-based tag (e.g., MIFARE Classic, MIFARE Ultralight, MIFARE DESFire).
     */
    MIFARE,

    /**
     * ISO 15693 (Vicinity) tag, typically used for long-range proximity applications.
     */
    ISO15693,

    /**
     * ISO 7816-4 based smart card or tag.
     */
    ISO7816,

    /**
     * FeliCa (Sony) tag system, commonly used for transit and payments in Japan.
     */
    FELICA
}
