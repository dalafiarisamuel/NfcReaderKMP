package com.devtamuno.kmp.nfcreader.contract

import androidx.compose.runtime.Composable
import com.devtamuno.kmp.nfcreader.data.NfcConfig
import com.devtamuno.kmp.nfcreader.data.NfcReadResult
import kotlinx.coroutines.flow.StateFlow

internal expect class NfcReadManager(config: NfcConfig) {

    val value: StateFlow<NfcReadResult>

    @Composable fun RegisterManager()

    fun startScanning()

    fun stopScanning()
}
