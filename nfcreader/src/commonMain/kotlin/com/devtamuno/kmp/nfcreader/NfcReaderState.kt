package com.devtamuno.kmp.nfcreader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.devtamuno.kmp.nfcreader.contract.NfcReadManagerState
import com.devtamuno.kmp.nfcreader.contract.NfcReadManagerStateImpl
import com.devtamuno.kmp.nfcreader.data.NfcConfig

/**
 * Creates and remembers an [NfcReadManagerState] instance.
 *
 * This composable function is the main entry point for creating and managing the NFC reader state.
 * It ensures the state is preserved across recompositions and initializes the necessary NFC
 * manager.
 *
 * @param config The [NfcConfig] to configure the NFC scanning behavior and UI.
 * @return A remembered [NfcReadManagerState] instance.
 */
@Composable
fun rememberNfcReadManagerState(config: NfcConfig): NfcReadManagerState =
    rememberMutableNfcReadManagerState(config).also { it.InitNfcManager() }

/**
 * Creates and remembers a mutable [NfcReadManagerState] instance.
 *
 * This function is responsible for creating the concrete implementation of the state manager,
 * [NfcReadManagerStateImpl], and wrapping it in a `remember` block to maintain state across
 * recompositions.
 *
 * @param config The [NfcConfig] for the NFC reader.
 * @return A remembered [NfcReadManagerState] instance.
 */
@Composable
private fun rememberMutableNfcReadManagerState(config: NfcConfig): NfcReadManagerState = remember {
    NfcReadManagerStateImpl(config)
}
