package com.devtamuno.kmp.nfcreader.ndef

/**
 * Parsers raw bytes into an [NdefMessage].
 * Follows the NFC Forum NDEF specification.
 */
object NdefMessageParser {

    private const val MB_MASK = 0x80.toByte()
    private const val ME_MASK = 0x40.toByte()
    private const val CF_MASK = 0x20.toByte()
    private const val SR_MASK = 0x10.toByte()
    private const val IL_MASK = 0x08.toByte()
    private const val TNF_MASK = 0x07.toByte()

    fun parse(bytes: ByteArray): NdefMessage? {
        val records = mutableListOf<NdefRecord>()
        var index = 0

        try {
            while (index < bytes.size) {
                val header = bytes[index++]
                val isMb = (header.toInt() and MB_MASK.toInt()) != 0
                val isMe = (header.toInt() and ME_MASK.toInt()) != 0
                val isCf = (header.toInt() and CF_MASK.toInt()) != 0
                val isSr = (header.toInt() and SR_MASK.toInt()) != 0
                val isIl = (header.toInt() and IL_MASK.toInt()) != 0
                val tnfValue = (header.toInt() and TNF_MASK.toInt()).toShort()
                val tnf = Tnf.fromValue(tnfValue)

                if (records.isEmpty() && !isMb) {
                    // First record must have MB flag set
                    // We can choose to be strict or lenient. Let's be lenient but aware.
                }

                val typeLength = bytes[index++].toInt() and 0xFF
                val payloadLength: Long = if (isSr) {
                    (bytes[index++].toInt() and 0xFF).toLong()
                } else {
                    val b1 = (bytes[index++].toInt() and 0xFF).toLong()
                    val b2 = (bytes[index++].toInt() and 0xFF).toLong()
                    val b3 = (bytes[index++].toInt() and 0xFF).toLong()
                    val b4 = (bytes[index++].toInt() and 0xFF).toLong()
                    (b1 shl 24) or (b2 shl 16) or (b3 shl 8) or b4
                }

                val idLength = if (isIl) {
                    bytes[index++].toInt() and 0xFF
                } else {
                    0
                }

                val type = ByteArray(typeLength)
                bytes.copyInto(type, 0, index, index + typeLength)
                index += typeLength

                val id = if (isIl) {
                    val idBytes = ByteArray(idLength)
                    bytes.copyInto(idBytes, 0, index, index + idLength)
                    index += idLength
                    idBytes
                } else {
                    null
                }

                val payload = ByteArray(payloadLength.toInt())
                bytes.copyInto(payload, 0, index, index + payloadLength.toInt())
                index += payloadLength.toInt()

                records.add(NdefRecord(tnf, type, id, payload))

                if (isMe) break
            }
        } catch (e: Exception) {
            // Return what we have or null if it's completely malformed
            if (records.isEmpty()) return null
        }

        return NdefMessage(records)
    }
}
