package com.devtamuno.kmp.nfcreader.data

sealed class NfcReadResult {
    data class Success(val data: NfcTagData) : NfcReadResult()
    data class Error(val message: String) : NfcReadResult()
    data object Initial : NfcReadResult()
}
