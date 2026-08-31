package com.devtamuno.kmp.nfcreader.data

import androidx.compose.runtime.Immutable

/**
 * Sealed class representing various NFC-related errors.
 */
@Immutable
sealed class NfcError {
    /** The device does not support NFC. */
    data object Unsupported : NfcError()

    /** NFC is disabled in the device settings. */
    data object Disabled : NfcError()

    /** The NFC scanning operation timed out. */
    data object Timeout : NfcError()

    /** A custom error with a specific message. */
    data class Custom(val message: String) : NfcError()
}
