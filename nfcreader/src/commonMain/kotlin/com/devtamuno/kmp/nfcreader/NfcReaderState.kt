package com.devtamuno.kmp.nfcreader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.devtamuno.kmp.nfcreader.contract.NfcReadManagerState
import com.devtamuno.kmp.nfcreader.contract.NfcReadManagerStateImpl
import com.devtamuno.kmp.nfcreader.data.NfcConfig

@Composable
fun rememberNfcReadManagerState(config: NfcConfig): NfcReadManagerState =
    rememberMutableNfcReadManagerState(config).also { it.InitNfcManager() }

@Composable
private fun rememberMutableNfcReadManagerState(config: NfcConfig): NfcReadManagerState =
    remember { NfcReadManagerStateImpl(config) }
