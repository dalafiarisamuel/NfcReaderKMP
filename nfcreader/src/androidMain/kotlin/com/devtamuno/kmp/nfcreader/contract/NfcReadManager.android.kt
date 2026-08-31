package com.devtamuno.kmp.nfcreader.contract

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.devtamuno.kmp.nfcreader.data.NfcConfig
import com.devtamuno.kmp.nfcreader.data.NfcError
import com.devtamuno.kmp.nfcreader.data.NfcReadResult
import com.devtamuno.kmp.nfcreader.data.NfcTagData
import com.devtamuno.kmp.nfcreader.data.NfcTagType
import com.devtamuno.kmp.nfcreader.data.ParsedNfcPayload
import com.devtamuno.kmp.nfcreader.ndef.NdefMessage
import com.devtamuno.kmp.nfcreader.ndef.NdefRecord
import com.devtamuno.kmp.nfcreader.ndef.Tnf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Typealias to avoid ambiguity with android.nfc.NdefMessage
private typealias AndroidNdefMessage = android.nfc.NdefMessage

/**
 * Android implementation of [NfcReadManager]. Handles NFC tag scanning using the Android NFC
 * Adapter in Reader Mode.
 *
 * UI is delegated to [NfcScanBottomSheet]; tag parsing to `NfcTagParser.kt`.
 *
 * @property config Configuration settings for NFC reading and the associated UI.
 */
internal actual class NfcReadManager
actual constructor(private val config: NfcConfig) : NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private var activity: Activity? = null
    private val _tagData = MutableStateFlow<NfcReadResult>(NfcReadResult.Initial)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var isScanning by mutableStateOf(false)
    @Volatile private var timeoutJob: Job? = null

    /** A [StateFlow] that emits the current [NfcReadResult] during the scanning process. */
    actual val nfcResult: StateFlow<NfcReadResult>
        get() = _tagData.asStateFlow()

    /**
     * Registers the manager with the current Activity and Context, and renders the scan UI. The
     * scope is cancelled when the composable leaves composition entirely. The NFC adapter reference
     * is refreshed on Activity recreation (e.g. configuration change).
     *
     * @param nfcScanningAnimationSlot A [Composable] slot for displaying a scanning animation.
     */
    @Composable
    actual fun RegisterManager(nfcScanningAnimationSlot: @Composable ColumnScope.() -> Unit) {
        val currentActivity = LocalActivity.current
        val context = LocalContext.current

        DisposableEffect(Unit) {
            onDispose {
                scope.coroutineContext.cancelChildren()
            }
        }

        DisposableEffect(currentActivity) {
            activity = currentActivity
            nfcAdapter = context.getSystemService(NfcManager::class.java)?.defaultAdapter
            onDispose {
                stopScanning()
                activity = null
                nfcAdapter = null
            }
        }

        NfcScanBottomSheet(
            config = config,
            isVisible = isScanning,
            nfcScanningAnimationSlot = nfcScanningAnimationSlot,
            onDismiss = {
                stopScanning()
                _tagData.value = NfcReadResult.OperationCancelled
            },
        )
    }

    /**
     * Starts the NFC scanning process. Validates the adapter and NFC state, then enables Reader
     * Mode with a timeout.
     */
    actual fun startScanning() {
        _tagData.value = NfcReadResult.Initial

        val currentActivity = activity
        val adapter = nfcAdapter

        if (adapter == null) {
            _tagData.value =
                NfcReadResult.Error(
                    error = NfcError.Unsupported,
                    message = config.nfcUnsupportedMessage,
                )
            return
        }

        if (currentActivity == null) {
            val message = config.nfcReadErrorMessage
            _tagData.value =
                NfcReadResult.Error(message = message, error = NfcError.Custom(message))
            return
        }

        if (!adapter.isEnabled) {
            _tagData.value =
                NfcReadResult.Error(
                    error = NfcError.Disabled,
                    message = config.nfcDisabledMessage,
                )
            return
        }

        isScanning = true
        _tagData.value = NfcReadResult.Scanning

        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(config.android.nfcReadTimeout)
            if (isScanning) {
                _tagData.value =
                    NfcReadResult.Error(
                        error = NfcError.Timeout,
                        message = config.android.nfcScanTimeoutMessage,
                    )
                stopScanning()
            }
        }

        val options = Bundle().apply { putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 500) }

        adapter.enableReaderMode(
            currentActivity,
            this,
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
            options,
        )
    }

    /** Stops the NFC scanning process and disables Reader Mode. */
    actual fun stopScanning() {
        timeoutJob?.cancel()
        timeoutJob = null
        activity?.let { nfcAdapter?.disableReaderMode(it) }
        isScanning = false
    }

    /**
     * Callback triggered when an NFC tag is discovered. Cancels the timeout immediately, then
     * parses the tag and emits the result.
     *
     * @param tag The discovered [Tag].
     */
    @OptIn(ExperimentalStdlibApi::class)
    override fun onTagDiscovered(tag: Tag?) {
        timeoutJob?.cancel()

        if (tag == null) {
            scope.launch {
                stopScanning()
                val message = config.nfcReadErrorMessage
                _tagData.value =
                    NfcReadResult.Error(message = message, error = NfcError.Custom(message))
            }
            return
        }

        val tagId =
            tag.id.toHexString(
                HexFormat {
                    upperCase = true
                    bytes { byteSeparator = ":" }
                }
            )

        val hardwareTechList =
            tag.techList.filter { !it.contains("tech.Ndef") }.map { getFriendlyName(it) }

        val tagType = getNfcTagType(tag)
        val ndef = Ndef.get(tag)
        val ndefMessage: AndroidNdefMessage? = ndef?.cachedNdefMessage

        val result = if (ndefMessage == null) {
            NfcReadResult.Success(NfcTagData(tagId, tagType, null, hardwareTechList))
        } else {
            val commonRecords = ndefMessage.records.map {
                NdefRecord(
                    tnf = Tnf.fromValue(it.tnf),
                    type = it.type,
                    id = it.id,
                    payload = it.payload
                )
            }
            val parsedMessage = NdefMessage(commonRecords)
            val parsedPayloads = config.ndefParser.parseMessage(parsedMessage)
            val combinedPayload = parsedPayloads.joinToString(separator = "\n") {
                when (it) {
                    is ParsedNfcPayload.Text -> it.text
                    is ParsedNfcPayload.Uri -> it.url
                    else -> it.toString()
                }
            }

            NfcReadResult.Success(
                NfcTagData(
                    serialNumber = tagId,
                    type = NfcTagType.NDEF,
                    payload = combinedPayload,
                    techList = hardwareTechList + "NDEF",
                    parsedPayloads = parsedPayloads
                )
            )
        }

        scope.launch {
            stopScanning()
            _tagData.value = result
        }
    }
}
