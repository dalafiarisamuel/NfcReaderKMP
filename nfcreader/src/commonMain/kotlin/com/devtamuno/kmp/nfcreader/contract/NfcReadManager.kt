package com.devtamuno.kmp.nfcreader.contract

import androidx.compose.runtime.Composable
import com.devtamuno.kmp.nfcreader.data.NfcConfig
import com.devtamuno.kmp.nfcreader.data.NfcReadResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Manager class for handling NFC reading operations.
 *
 * This is an expected class with platform-specific implementations for Android and iOS. It manages
 * the NFC scanning process, provides results via a [StateFlow], and handles the UI (like bottom
 * sheets on Android or native dialogs on iOS).
 *
 * @property config The [NfcConfig] used to configure the scanning behavior and UI.
 */
internal expect class NfcReadManager(config: NfcConfig) {

    /** A [StateFlow] that emits the current [NfcReadResult] of the NFC scanning process. */
    val nfcResult: StateFlow<NfcReadResult>

    /**
     * A [Composable] function that registers the manager within the Compose UI hierarchy. This is
     * typically used to handle lifecycle events and show scanning-related UI components.
     */
    @Composable fun RegisterManager()

    /** Starts the NFC scanning process. */
    fun startScanning()

    /** Stops the NFC scanning process. */
    fun stopScanning()
}
