package com.devtamuno.kmp.nfcreader.contract

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import com.devtamuno.kmp.nfcreader.data.NfcConfig
import com.devtamuno.kmp.nfcreader.data.NfcReadResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Concrete implementation of [NfcReadManagerState].
 *
 * This class wraps an [NfcReadManager] and delegates state and operations to it. It's responsible
 * for managing the lifecycle of the NFC manager and exposing its scanning results.
 *
 * @property config The [NfcConfig] used to configure the NFC scanning.
 * @property nfcScanningAnimationSlot A [Composable] slot for displaying a scanning animation.
 */
internal class NfcReadManagerStateImpl(
    config: NfcConfig,
    nfcScanningAnimationSlot: @Composable ColumnScope.() -> Unit,
) : NfcReadManagerState {

    private val nfcReadManager = NfcReadManager(config, nfcScanningAnimationSlot)

    /**
     * A [StateFlow] providing the current result of the NFC scanning process. Delegates to the
     * underlying [NfcReadManager].
     */
    override val nfcReadResult: StateFlow<NfcReadResult>
        get() = nfcReadManager.nfcResult

    /** Initializes and registers the underlying [NfcReadManager]. */
    @Composable
    override fun InitNfcManager() {
        nfcReadManager.RegisterManager()
    }

    /** Starts the NFC scanning process. Delegates to the underlying [NfcReadManager]. */
    override fun startScanning() {
        nfcReadManager.startScanning()
    }

    /** Stops the NFC scanning process. Delegates to the underlying [NfcReadManager]. */
    override fun stopScanning() {
        nfcReadManager.stopScanning()
    }
}
