package com.devtamuno.kmp.nfcreader.ndef

import com.devtamuno.kmp.nfcreader.util.readInt32BE
import com.devtamuno.kmp.nfcreader.util.toIntUnsigned

/**
 * Parsers raw bytes into an [NdefMessage].
 * Follows the NFC Forum NDEF specification.
 */
object NdefMessageParser {

    private const val MB_MASK = 0x80
    private const val ME_MASK = 0x40
    private const val CF_MASK = 0x20
    private const val SR_MASK = 0x10
    private const val IL_MASK = 0x08
    private const val TNF_MASK = 0x07

    fun parse(bytes: ByteArray): NdefMessage? {
        if (bytes.isEmpty()) return null

        val records = mutableListOf<NdefRecord>()
        var index = 0
        var isFirstRecord = true
        val chunkedPayloads = mutableListOf<ByteArray>()
        var chunkedTnf: Tnf? = null
        var chunkedType: ByteArray? = null
        var chunkedId: ByteArray? = null

        while (index < bytes.size) {
            val header = bytes[index++].toIntUnsigned()
            val isMb = (header and MB_MASK) != 0
            val isMe = (header and ME_MASK) != 0
            val isCf = (header and CF_MASK) != 0
            val isSr = (header and SR_MASK) != 0
            val isIl = (header and IL_MASK) != 0
            val tnf = Tnf.fromValue((header and TNF_MASK).toShort())

            if (isMb != isFirstRecord || (isMe && isCf) || index >= bytes.size) return null

            val typeLength = bytes[index++].toIntUnsigned()
            val payloadLength =
                if (isSr) {
                    if (index >= bytes.size) return null
                    bytes[index++].toIntUnsigned().toLong()
                } else {
                    if (bytes.size - index < Int.SIZE_BYTES) return null
                    bytes.readInt32BE(index).also { index += Int.SIZE_BYTES }
                }

            val idLength =
                if (isIl) {
                    if (index >= bytes.size) return null
                    bytes[index++].toIntUnsigned()
                } else {
                    0
                }

            val requiredLength = typeLength.toLong() + idLength.toLong() + payloadLength
            if (requiredLength > (bytes.size - index).toLong()) return null

            val type = bytes.copyOfRange(index, index + typeLength)
            index += typeLength
            val id =
                if (isIl) {
                    bytes.copyOfRange(index, index + idLength).also { index += idLength }
                } else {
                    null
                }
            val payloadSize = payloadLength.toInt()
            val payload = bytes.copyOfRange(index, index + payloadSize)
            index += payloadSize

            if (chunkedTnf != null) {
                if (tnf != Tnf.UNCHANGED || typeLength != 0 || isIl) return null

                chunkedPayloads += payload
                if (!isCf) {
                    val fullPayload = ByteArray(chunkedPayloads.sumOf { it.size })
                    var destinationOffset = 0
                    chunkedPayloads.forEach { chunk ->
                        chunk.copyInto(fullPayload, destinationOffset)
                        destinationOffset += chunk.size
                    }
                    records +=
                        NdefRecord(
                            tnf = chunkedTnf,
                            type = chunkedType ?: return null,
                            id = chunkedId,
                            payload = fullPayload,
                        )
                    chunkedTnf = null
                    chunkedType = null
                    chunkedId = null
                    chunkedPayloads.clear()
                }
            } else {
                if (tnf == Tnf.UNCHANGED || tnf == Tnf.RESERVED) return null
                if (
                    tnf == Tnf.EMPTY &&
                        (isCf || typeLength != 0 || isIl || payloadLength != 0L)
                ) {
                    return null
                }
                if (tnf == Tnf.UNKNOWN && typeLength != 0) return null

                if (isCf) {
                    chunkedTnf = tnf
                    chunkedType = type
                    chunkedId = id
                    chunkedPayloads += payload
                } else {
                    records += NdefRecord(tnf, type, id, payload)
                }
            }

            isFirstRecord = false
            if (isMe) {
                return if (index == bytes.size && chunkedTnf == null) NdefMessage(records) else null
            }
        }

        return null
    }
}
