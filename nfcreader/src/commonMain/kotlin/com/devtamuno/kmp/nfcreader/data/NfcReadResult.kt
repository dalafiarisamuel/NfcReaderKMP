package com.devtamuno.kmp.nfcreader.data

/**
 * Represents the result of an NFC scanning operation.
 *
 * This sealed class defines the various states and outcomes of an NFC scan,
 * including success, error, scanning progress, and cancellation.
 */
sealed class NfcReadResult {

    /**
     * Indicates that an NFC tag was successfully read.
     *
     * @property data The [NfcTagData] extracted from the tag, including its UID,
     * type, payload, and supported technology list.
     */
    data class Success(val data: NfcTagData) : NfcReadResult()

    /**
     * Indicates that an error occurred during the NFC scanning process.
     *
     * @property message The configured, user-facing description of the error.
     * @property error The [NfcError] detailing why the scanning operation failed.
     */
    data class Error(
        val message: String,
        val error: NfcError,
    ) : NfcReadResult() {
        /** Creates a custom error while preserving the original string-based API. */
        constructor(message: String) : this(message, NfcError.Custom(message))
    }

    /**
     * The initial state of the NFC manager before any scanning operation has been initiated.
     */
    data object Initial : NfcReadResult()

    /**
     * Indicates that the NFC manager is currently actively scanning for tags.
     */
    data object Scanning : NfcReadResult()

    /**
     * Indicates that scanning was cancelled by the user or ended by the system without an error.
     */
    data object OperationCancelled : NfcReadResult()
}
