package com.devtamuno.kmp.nfcreader.data

enum class NfcTagType {
    NDEF,
    NON_NDEF
}

data class NfcTagData(
    val serialNumber: String,
    val type: NfcTagType,
    val payload: String?,
    val techList: List<String> = emptyList(),
)
