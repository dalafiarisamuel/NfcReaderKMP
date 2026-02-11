package com.devtamuno.kmp.nfcreader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.devtamuno.kmp.nfcreader.contract.NfcReadManagerState
import com.devtamuno.kmp.nfcreader.contract.NfcReadReadManagerStateImpl
import com.devtamuno.kmp.nfcreader.data.NfcConfig

@Composable
fun rememberNfcReadManagerState(config: NfcConfig = NfcConfig()): NfcReadManagerState {
    return rememberMutableNfcReadManagerState(config).also {
        it.InitNfcManager()
    }
}


@Composable
private fun rememberMutableNfcReadManagerState(config: NfcConfig): NfcReadManagerState {
    return remember { NfcReadReadManagerStateImpl(config) }
}
