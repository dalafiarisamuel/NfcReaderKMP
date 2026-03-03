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
     * @property message A descriptive message detailing why the scanning operation failed.
     */
    data class Error(val message: String) : NfcReadResult()

    /**
     * The initial state of the NFC manager before any scanning operation has been initiated.
     */
    data object Initial : NfcReadResult()

    /**
     * Indicates that the NFC manager is currently actively scanning for tags.
     */
    data object Scanning : NfcReadResult()

    /**
     * Indicates that the NFC scanning operation was cancelled, either by the user
     * (e.g., by dismissing the scanning UI) or by the system.
     */
    data object OperationCancelled : NfcReadResult()
}
