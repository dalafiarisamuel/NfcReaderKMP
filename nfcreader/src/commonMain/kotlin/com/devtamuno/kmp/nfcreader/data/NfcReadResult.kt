package com.devtamuno.kmp.nfcreader.data

/**
 * Represents the result of an NFC scanning operation.
 */
sealed class NfcReadResult {

    /**
     * Indicates that an NFC tag was successfully read.
     *
     * @property data The [NfcTagData] extracted from the tag.
     */
    data class Success(val data: NfcTagData) : NfcReadResult()

    /**
     * Indicates that an error occurred during the NFC scanning process.
     *
     * @property message A descriptive error message.
     */
    data class Error(val message: String) : NfcReadResult()

    /**
     * The initial state before any scanning operation has started.
     */
    data object Initial : NfcReadResult()

    /**
     * Indicates that the NFC scanning operation was cancelled by the user or the system.
     */
    data object OperationCancelled: NfcReadResult()
}
