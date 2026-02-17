@file:OptIn(ExperimentalMaterial3Api::class)

package com.devtamuno.kmp.nfcreader.contract

import android.app.Activity
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devtamuno.kmp.nfcreader.contract.icon.Contactless
import com.devtamuno.kmp.nfcreader.data.NfcConfig
import com.devtamuno.kmp.nfcreader.data.NfcReadResult
import com.devtamuno.kmp.nfcreader.data.NfcTagData
import com.devtamuno.kmp.nfcreader.data.NfcTagType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal actual class NfcReadManager actual constructor(private val config: NfcConfig) :
    NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private var activity: Activity? = null
    private val _tagData = MutableStateFlow<NfcReadResult>(NfcReadResult.Initial)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isScanning by mutableStateOf(false)
    private var timeoutJob: Job? = null

    actual val nfcResult: StateFlow<NfcReadResult>
        get() = _tagData.asStateFlow()

    @Composable
    actual fun RegisterManager() {
        val currentActivity = LocalActivity.current
        val context = LocalContext.current

        DisposableEffect(currentActivity) {
            activity = currentActivity
            nfcAdapter = NfcAdapter.getDefaultAdapter(context)
            onDispose {
                stopScanning()
                activity = null
                nfcAdapter = null
            }
        }

        ScanBottomSheet()
    }

    @Composable
    private fun ScanBottomSheet() {
        val sheetState = rememberModalBottomSheetState()

        if (isScanning) {
            ModalBottomSheet(
                onDismissRequest = {
                    stopScanning()
                    _tagData.value = NfcReadResult.OperationCancelled
                },
                dragHandle = null,
                sheetState = sheetState,
                sheetGesturesEnabled = config.sheetGesturesEnabled,
                properties =
                    ModalBottomSheetProperties(
                        shouldDismissOnBackPress = config.shouldDismissBottomSheetOnBackPress,
                        shouldDismissOnClickOutside = config.shouldDismissBottomSheetOnClickOutside,
                    ),
            ) {
                Column(
                    modifier =
                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier =
                            Modifier.align(Alignment.CenterHorizontally)
                                .width(40.dp)
                                .height(4.dp)
                                .background(
                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.4f
                                        ),
                                    shape = RoundedCornerShape(2.dp),
                                )
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    Text(
                        text = config.titleMessage,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        modifier = Modifier.padding(top = 16.dp),
                        text = config.subtitleMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )

                    Image(
                        imageVector = Contactless,
                        colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.primary),
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                    )

                    Button(
                        modifier = Modifier.padding(top = 32.dp),
                        onClick = {
                            stopScanning()
                            _tagData.value = NfcReadResult.OperationCancelled
                        },
                    ) {
                        Text(config.buttonText)
                    }
                }
            }
        }
    }

    actual fun startScanning() {
        val currentActivity = activity
        val adapter = nfcAdapter

        if (adapter == null) {
            _tagData.value = NfcReadResult.Error("NFC adapter is null")
            return
        }

        if (currentActivity == null) {
            _tagData.value = NfcReadResult.Error("Activity is null")
            return
        }

        if (!adapter.isEnabled) {
            _tagData.value = NfcReadResult.Error("NFC is disabled")
            return
        }

        isScanning = true
        _tagData.value = NfcReadResult.Initial

        timeoutJob?.cancel()
        timeoutJob =
            scope.launch {
                delay(config.nfcReadTimeout)
                if (isScanning) {
                    _tagData.value = NfcReadResult.Error("Scanning timeout reached")
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

    actual fun stopScanning() {
        timeoutJob?.cancel()
        timeoutJob = null
        activity?.let { nfcAdapter?.disableReaderMode(it) }
        isScanning = false
    }

    override fun onTagDiscovered(tag: Tag?) {

        // Stop scanning hardware and cancel timeout UI immediately
        scope.launch { stopScanning() }

        if (tag == null) {
            _tagData.value = NfcReadResult.Error("Tag is null")
            return
        }

        val tagId = tag.id.joinToString(":") { "%02X".format(it) }
        val techList = tag.techList.map { getFriendlyName(it) }
        val ndef = Ndef.get(tag)

        if (ndef == null) {
            val data =
                NfcTagData(
                    serialNumber = tagId,
                    type = NfcTagType.NON_NDEF,
                    payload = null,
                    techList = techList,
                )
            _tagData.value = NfcReadResult.Success(data)
            return
        }

        val ndefMessage: NdefMessage? = ndef.cachedNdefMessage
        if (ndefMessage == null) {
            val data =
                NfcTagData(
                    serialNumber = tagId,
                    type = NfcTagType.NDEF,
                    payload = null,
                    techList = techList,
                )
            _tagData.value = NfcReadResult.Success(data)
            return
        }

        val records: Array<NdefRecord>? = ndefMessage.records
        if (records.isNullOrEmpty()) {
            val data =
                NfcTagData(
                    serialNumber = tagId,
                    type = NfcTagType.NDEF,
                    payload = null,
                    techList = techList,
                )
            _tagData.value = NfcReadResult.Success(data)
            return
        }

        val combinedPayload =
            records.joinToString(separator = "\n") { record ->
                String(record.payload, Charsets.UTF_8)
            }
        val data =
            NfcTagData(
                serialNumber = tagId,
                type = NfcTagType.NDEF,
                payload = combinedPayload,
                techList = techList,
            )
        _tagData.value = NfcReadResult.Success(data)
    }

    private fun getFriendlyName(tech: String): String {
        return when {
            tech.contains("MifareClassic") -> "Mifare Classic"
            tech.contains("MifareUltralight") -> "Mifare Ultralight"
            tech.contains("NfcA") -> "ISO 14443-3A"
            else -> tech.split(".").last()
        }
    }
}
