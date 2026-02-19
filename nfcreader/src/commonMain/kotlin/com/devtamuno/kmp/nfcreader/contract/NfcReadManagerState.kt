package com.devtamuno.kmp.nfcreader.contract

import androidx.compose.runtime.Composable
import com.devtamuno.kmp.nfcreader.data.NfcReadResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface defining the state and operations for the NFC reader.
 *
 * This state is typically managed and remembered in a Composable to survive recompositions.
 */
interface NfcReadManagerState {

    /**
     * A [StateFlow] providing the current result of the NFC scanning process.
     */
    val nfcReadResult: StateFlow<NfcReadResult>

    /**
     * A [Composable] function that initializes and registers the underlying NFC manager.
     * This should be called within the Composable where the NFC reader is being used.
     */
    @Composable fun InitNfcManager()

    /**
     * Starts the NFC scanning process.
     */
    fun startScanning()

    /**
     * Stops the NFC scanning process.
     */
    fun stopScanning()
}
