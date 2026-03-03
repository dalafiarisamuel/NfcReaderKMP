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
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/**
 * Android implementation of [NfcReadManager].
 * This class handles NFC tag scanning using the Android NFC Adapter in Reader Mode.
 *
 * @property config Configuration settings for NFC reading and the associated UI.
 */
internal actual class NfcReadManager actual constructor(private val config: NfcConfig) :
    NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private var activity: Activity? = null
    private val _tagData = MutableStateFlow<NfcReadResult>(NfcReadResult.Initial)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isScanning by mutableStateOf(false)
    private var timeoutJob: Job? = null

    /**
     * A [StateFlow] that emits the current [NfcReadResult] during the scanning process.
     */
    actual val nfcResult: StateFlow<NfcReadResult>
        get() = _tagData.asStateFlow()

    /**
     * A Composable function that registers the manager with the current Activity and Context.
     * It handles the lifecycle of the NFC adapter and displays the scan bottom sheet when active.
     */
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

    /**
     * Displays a bottom sheet to guide the user during the NFC scanning process.
     */
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
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
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

                    Text(
                        text = config.titleMessage,
                        style =
                            MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = config.subtitleMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )

                    config.nfcScanningAnimationSlot(this)

                    OutlinedButton(
                        modifier =
                            Modifier.height(height = 45.dp)
                                .width(200.dp)
                                .align(Alignment.CenterHorizontally),
                        elevation =
                            ButtonDefaults.elevatedButtonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp,
                                disabledElevation = 0.dp,
                                hoveredElevation = 0.dp,
                                focusedElevation = 0.dp,
                            ),
                        interactionSource = remember { MutableInteractionSource() },
                        shape = RoundedCornerShape(10.dp),
                        onClick = {
                            stopScanning()
                            _tagData.value = NfcReadResult.OperationCancelled
                        },
                    ) {
                        Text(text = config.buttonText, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    /**
     * Starts the NFC scanning process.
     * Checks if the NFC adapter is available and enabled, then enables Reader Mode.
     */
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
        _tagData.value = NfcReadResult.Scanning

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

    /**
     * Stops the NFC scanning process and disables Reader Mode.
     */
    actual fun stopScanning() {
        timeoutJob?.cancel()
        timeoutJob = null
        activity?.let { nfcAdapter?.disableReaderMode(it) }
        isScanning = false
    }

    /**
     * Callback triggered when an NFC tag is discovered.
     * Parses the tag data and updates [nfcResult].
     *
     * @param tag The discovered [Tag].
     */
    @OptIn(ExperimentalStdlibApi::class)
    override fun onTagDiscovered(tag: Tag?) {
        scope.launch { stopScanning() }

        if (tag == null) {
            _tagData.value = NfcReadResult.Error("Tag is null")
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

        if (ndef == null) {
            _tagData.value =
                NfcReadResult.Success(NfcTagData(tagId, tagType, null, hardwareTechList))
            return
        }

        val ndefMessage: NdefMessage? = ndef.cachedNdefMessage
        if (ndefMessage == null) {
            _tagData.value =
                NfcReadResult.Success(NfcTagData(tagId, tagType, null, hardwareTechList))
            return
        }

        val records: Array<NdefRecord>? = ndefMessage.records
        if (records.isNullOrEmpty()) {
            _tagData.value =
                NfcReadResult.Success(NfcTagData(tagId, tagType, null, hardwareTechList))
            return
        }

        val combinedPayload = records.joinToString(separator = "\n") { it.toReadableText() }
        val data =
            NfcTagData(
                serialNumber = tagId,
                type = NfcTagType.NDEF,
                payload = combinedPayload,
                techList = hardwareTechList + "NDEF",
            )
        _tagData.value = NfcReadResult.Success(data)
    }

    /**
     * Extension function to convert [NdefRecord] payload to a readable string.
     */
    private fun NdefRecord.toReadableText(): String {
        return try {
            val payload = this.payload
            if (payload.isEmpty()) return ""
            val statusByte = payload[0].toInt()
            val languageCodeLength = statusByte and 0x3F
            val textStartIndex = languageCodeLength + 1
            if (payload.size <= textStartIndex) return ""
            String(payload, textStartIndex, payload.size - textStartIndex, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Maps hardware technology class names to user-friendly names.
     */
    private fun getFriendlyName(tech: String): String {
        return when {
            tech.contains("MifareClassic") -> "Mifare Classic"
            tech.contains("MifareUltralight") -> "Mifare Ultralight"
            tech.contains("NfcA") -> "ISO 14443-3A (NfcA)"
            tech.contains("NfcB") -> "ISO 14443-3B (NfcB)"
            tech.contains("NfcF") -> "FeliCa (NfcF)"
            tech.contains("NfcV") -> "ISO 15693 (NfcV)"
            tech.contains("IsoDep") -> "ISO 14443-4 (ISO-DEP)"
            else -> tech.split(".").last()
        }
    }

    /**
     * Determines the [NfcTagType] based on the tag's technology list.
     */
    private fun getNfcTagType(tag: Tag): NfcTagType {
        val techList = tag.techList
        return when {
            techList.any { it.contains("Mifare") } -> NfcTagType.MIFARE
            techList.any { it.contains("NfcV") } -> NfcTagType.ISO15693
            techList.any { it.contains("NfcF") } -> NfcTagType.FELICA
            techList.any { it.contains("IsoDep") } ||
                techList.any { it.contains("NfcA") } ||
                techList.any { it.contains("NfcB") } -> NfcTagType.ISO7816
            else -> NfcTagType.NON_NDEF
        }
    }
}
