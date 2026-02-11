package com.devtamuno.kmp.nfcreader.contract

import androidx.compose.runtime.Composable
import com.devtamuno.kmp.nfcreader.data.NfcReadResult
import kotlinx.coroutines.flow.StateFlow

interface NfcReadManagerState {

    val value: StateFlow<NfcReadResult>

    @Composable fun InitNfcManager()

    fun startScanning()

    fun stopScanning()
}
