package com.devtamuno.kmp.nfcreader.contract

import androidx.compose.runtime.Composable
import com.devtamuno.kmp.nfcreader.data.NfcConfig
import com.devtamuno.kmp.nfcreader.data.NfcReadResult
import kotlinx.coroutines.flow.StateFlow

internal class NfcReadManagerStateImpl(config: NfcConfig) : NfcReadManagerState {

    private val nfcReadManager = NfcReadManager(config)

    override val nfcReadResult: StateFlow<NfcReadResult>
        get() = nfcReadManager.nfcResult

    @Composable
    override fun InitNfcManager() {
        nfcReadManager.RegisterManager()
    }

    override fun startScanning() {
        nfcReadManager.startScanning()
    }

    override fun stopScanning() {
        nfcReadManager.stopScanning()
    }
}
