package com.devtamuno.kmp.nfcreader.util

/**
 * Utility extensions for byte manipulation.
 */
internal fun Byte.toIntUnsigned(): Int = this.toInt() and 0xFF

internal fun ByteArray.readInt32BE(index: Int): Long {
    val b1 = this[index].toIntUnsigned().toLong()
    val b2 = this[index + 1].toIntUnsigned().toLong()
    val b3 = this[index + 2].toIntUnsigned().toLong()
    val b4 = this[index + 3].toIntUnsigned().toLong()
    return (b1 shl 24) or (b2 shl 16) or (b3 shl 8) or b4
}
