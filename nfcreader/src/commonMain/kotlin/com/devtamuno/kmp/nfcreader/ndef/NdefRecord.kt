package com.devtamuno.kmp.nfcreader.ndef

/**
 * Represents an NDEF record as defined by the NFC Forum NDEF specification.
 *
 * @property tnf Type Name Format.
 * @property type Record type.
 * @property id Record identifier.
 * @property payload Record payload.
 */
data class NdefRecord(
    val tnf: Tnf,
    val type: ByteArray,
    val id: ByteArray?,
    val payload: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NdefRecord) return false

        if (tnf != other.tnf) return false
        if (!type.contentEquals(other.type)) return false
        if (id != null) {
            if (other.id == null) return false
            if (!id.contentEquals(other.id)) return false
        } else if (other.id != null) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tnf.hashCode()
        result = 31 * result + type.contentHashCode()
        result = 31 * result + (id?.contentHashCode() ?: 0)
        result = 31 * result + payload.contentHashCode()
        return result
    }

    companion object {
        val RTD_TEXT = byteArrayOf('T'.code.toByte())
        val RTD_URI = byteArrayOf('U'.code.toByte())
        val RTD_SMART_POSTER = byteArrayOf('S'.code.toByte(), 'p'.code.toByte())
        val RTD_ANDROID_APP = "android.com:pkg".encodeToByteArray()
    }
}
