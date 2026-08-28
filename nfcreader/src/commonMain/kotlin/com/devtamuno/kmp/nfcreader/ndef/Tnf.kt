package com.devtamuno.kmp.nfcreader.ndef

/**
 * Type Name Format (TNF) as defined by the NFC Forum NDEF specification.
 */
enum class Tnf(val value: Short) {
    EMPTY(0x00),
    WELL_KNOWN(0x01),
    MIME_MEDIA(0x02),
    ABSOLUTE_URI(0x03),
    EXTERNAL_TYPE(0x04),
    UNKNOWN(0x05),
    UNCHANGED(0x06),
    RESERVED(0x07);

    companion object {
        fun fromValue(value: Short): Tnf {
            return entries.find { it.value == value } ?: UNKNOWN
        }
    }
}
