@file:OptIn(ExperimentalForeignApi::class)

package com.devtamuno.kmp.nfcreader.contract

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import com.devtamuno.kmp.nfcreader.data.NdefParser
import com.devtamuno.kmp.nfcreader.data.NfcConfig
import com.devtamuno.kmp.nfcreader.data.NfcReadResult
import com.devtamuno.kmp.nfcreader.data.NfcTagData
import com.devtamuno.kmp.nfcreader.data.NfcTagType
import com.devtamuno.kmp.nfcreader.data.ParsedNfcPayload
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.CoreNFC.NFCMiFareUnknown
import com.devtamuno.kmp.nfcreader.ndef.NdefMessage
import com.devtamuno.kmp.nfcreader.ndef.NdefRecord
import com.devtamuno.kmp.nfcreader.ndef.Tnf
import platform.CoreNFC.NFCNDEFPayload
import platform.CoreNFC.NFCNDEFMessage
import platform.CoreNFC.NFCNDEFStatusReadOnly
import platform.CoreNFC.NFCNDEFStatusReadWrite
import platform.CoreNFC.NFCPollingISO14443
import platform.CoreNFC.NFCPollingISO15693
import platform.CoreNFC.NFCPollingISO18092
import platform.CoreNFC.NFCTagProtocol
import platform.CoreNFC.NFCTagReaderSession
import platform.CoreNFC.NFCTagReaderSessionDelegateProtocol
import platform.CoreNFC.NFCTagTypeFeliCa
import platform.CoreNFC.NFCTagTypeISO15693
import platform.CoreNFC.NFCTagTypeISO7816Compatible
import platform.CoreNFC.NFCTagTypeMiFare
import platform.Foundation.NSError
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private const val NFC_USER_CANCELLED_ERROR_CODE = 200L
private const val NFC_SESSION_TIMEOUT_ERROR_CODE = 201L

/**
 * iOS implementation of [NfcReadManager]. Handles NFC tag scanning using CoreNFC's
 * [NFCTagReaderSession]. The system manages the scanning UI; [RegisterManager] is a no-op.
 *
 * Tag parsing is delegated to `NfcTagParser.kt`.
 *
 * @param config Configuration for NFC scanning.
 */
internal actual class NfcReadManager
actual constructor(private val config: NfcConfig) : NSObject(), NFCTagReaderSessionDelegateProtocol {

    private val _nfcResult = MutableStateFlow<NfcReadResult>(NfcReadResult.Initial)

    /**
     * Written on the CoreNFC callback thread before session invalidation; read on the same thread
     * in `tagReaderSession(_:didInvalidateWithError:)` after the system dialog dismisses. CoreNFC
     * serialises its own callbacks, so no additional synchronisation is needed.
     */
    private var pendingResult: NfcReadResult? = null

    /**
     * Written from the UI thread ([startScanning]/[stopScanning]) and nulled from the CoreNFC
     * callback thread. A new session cannot begin while an existing one is active, so writes are
     * effectively serialised in practice.
     */
    private var session: NFCTagReaderSession? = null

    /** A [StateFlow] that emits the current [NfcReadResult]. */
    actual val nfcResult: StateFlow<NfcReadResult>
        get() = _nfcResult.asStateFlow()

    /** Registers the manager (no-op on iOS — the system owns the scanning UI). */
    @Composable
    actual fun RegisterManager(nfcScanningAnimationSlot: @Composable ColumnScope.() -> Unit) = Unit

    /** Starts the NFC scanning process. */
    actual fun startScanning() {
        if (session != null) return

        _nfcResult.value = NfcReadResult.Initial

        if (!NFCTagReaderSession.readingAvailable()) {
            updateState(NfcReadResult.Error(config.nfcUnsupportedMessage))
            return
        }

        pendingResult = null
        updateState(NfcReadResult.Scanning)
        session =
            NFCTagReaderSession(
                    // ISO 14443: MiFare + ISO 7816 | ISO 15693: NfcV | ISO 18092: FeliCa
                    pollingOption = NFCPollingISO14443 or NFCPollingISO15693 or NFCPollingISO18092,
                    delegate = this,
                    queue = null,
                )
                .apply {
                    alertMessage = config.subtitleMessage
                    beginSession()
                }
    }

    /** Stops the NFC scanning process. */
    actual fun stopScanning() {
        session?.invalidateSession()
        session = null
    }

    override fun tagReaderSessionDidBecomeActive(session: NFCTagReaderSession) = Unit

    /**
     * Invoked when the session is invalidated. The pending result (captured before invalidation) is
     * emitted here — after the system dialog has fully dismissed — to avoid UI conflicts.
     */
    override fun tagReaderSession(session: NFCTagReaderSession, didInvalidateWithError: NSError) {
        this.session = null

        val result =
            pendingResult
                ?: when (didInvalidateWithError.code) {
                    NFC_USER_CANCELLED_ERROR_CODE,
                    NFC_SESSION_TIMEOUT_ERROR_CODE -> NfcReadResult.OperationCancelled
                    else -> NfcReadResult.Error(didInvalidateWithError.localizedDescription)
                }

        updateState(result)
        pendingResult = null
    }

    /** Invoked when tags are detected. Only the first tag in the field is processed. */
    override fun tagReaderSession(session: NFCTagReaderSession, didDetectTags: List<*>) {
        val tag = didDetectTags.firstOrNull() as? NFCTagProtocol ?: return

        // MIFARE Classic (NFCMiFareUnknown) uses Crypto1 which CoreNFC does not support.
        // The UID and mifareFamily are set during ISO 14443-3A anticollision and are available
        // before connectToTag. Calling connectToTag or any NDEF operation causes the session to
        // hang and eventually time out, so we return the UID immediately without connecting.
        if (
            tag.type == NFCTagTypeMiFare && tag.asNFCMiFareTag()?.mifareFamily == NFCMiFareUnknown
        ) {
            val uid = extractUid(tag)
            val techList = getTechList(tag)
            finishSession(
                session,
                NfcReadResult.Success(NfcTagData(uid, NfcTagType.MIFARE, null, techList)),
            )
            return
        }

        session.connectToTag(tag) { error ->
            if (error != null) {
                finishSession(session, NfcReadResult.Error(error.localizedDescription))
                return@connectToTag
            }

            val uid = extractUid(tag)
            val techList = getTechList(tag)

            when (tag.type) {
                NFCTagTypeMiFare ->
                    readNdefIfAvailable(
                        session,
                        uid,
                        techList,
                        NfcTagType.MIFARE,
                        tag.asNFCMiFareTag(),
                    )
                NFCTagTypeISO15693 ->
                    readNdefIfAvailable(
                        session,
                        uid,
                        techList,
                        NfcTagType.ISO15693,
                        tag.asNFCISO15693Tag(),
                    )
                NFCTagTypeISO7816Compatible ->
                    readNdefIfAvailable(
                        session,
                        uid,
                        techList,
                        NfcTagType.ISO7816,
                        tag.asNFCISO7816Tag(),
                    )
                NFCTagTypeFeliCa ->
                    readNdefIfAvailable(
                        session,
                        uid,
                        techList,
                        NfcTagType.FELICA,
                        tag.asNFCFeliCaTag(),
                    )
                else -> finishSession(session, NfcReadResult.Error(config.nfcUnsupportedMessage))
            }
        }
    }

    /**
     * Attempts to read NDEF data from the tag if it supports the NDEF protocol. Falls back to
     * [NfcReadResult.Success] with a null payload if NDEF is unavailable or the read fails,
     * aligning with the Android implementation's behaviour.
     */
    private fun readNdefIfAvailable(
        session: NFCTagReaderSession,
        uid: String,
        techList: List<String>,
        type: NfcTagType,
        tag: Any?,
    ) {
        val ndefTag = tag as? platform.CoreNFC.NFCNDEFTagProtocol
        if (ndefTag == null) {
            finishSession(session, NfcReadResult.Success(NfcTagData(uid, type, null, techList)))
            return
        }

        ndefTag.queryNDEFStatusWithCompletionHandler { status, _, error ->
            if (error != null) {
                finishSession(session, NfcReadResult.Success(NfcTagData(uid, type, null, techList)))
                return@queryNDEFStatusWithCompletionHandler
            }

            if (status == NFCNDEFStatusReadOnly || status == NFCNDEFStatusReadWrite) {
                ndefTag.readNDEFWithCompletionHandler { message: NFCNDEFMessage?, readError ->
                    if (readError != null) {
                        // Align with Android: an unreadable NDEF tag → Success with null payload
                        finishSession(
                            session,
                            NfcReadResult.Success(NfcTagData(uid, type, null, techList)),
                        )
                    } else {
                        val records = message?.records?.mapNotNull { it as? NFCNDEFPayload }?.map {
                            NdefRecord(
                                tnf = Tnf.fromValue(it.typeNameFormat.toShort()),
                                type = it.type.toByteArray(),
                                id = it.identifier.toByteArray(),
                                payload = it.payload.toByteArray()
                            )
                        } ?: emptyList()
                        
                        val parsedMessage = NdefMessage(records)
                        val parsedPayloads = NdefParser.parseMessage(parsedMessage)
                        val combinedPayload = parsedPayloads.joinToString(separator = "\n") {
                            when (it) {
                                is ParsedNfcPayload.Text -> it.text
                                is ParsedNfcPayload.Uri -> it.url
                                else -> it.toString()
                            }
                        }
                        finishSession(
                            session,
                            NfcReadResult.Success(
                                NfcTagData(
                                    serialNumber = uid,
                                    type = NfcTagType.NDEF,
                                    payload = combinedPayload,
                                    techList = techList + "NDEF",
                                    parsedPayloads = parsedPayloads
                                )
                            ),
                        )
                    }
                }
            } else {
                finishSession(session, NfcReadResult.Success(NfcTagData(uid, type, null, techList)))
            }
        }
    }

    /**
     * Captures the result and invalidates the session. The actual state update happens in
     * [tagReaderSession] once the system dialog is dismissed.
     */
    private fun finishSession(session: NFCTagReaderSession, result: NfcReadResult) {
        pendingResult = result
        when (result) {
            is NfcReadResult.Error -> session.invalidateSessionWithErrorMessage(result.message)
            is NfcReadResult.Success -> {
                session.alertMessage = config.ios.nfcSuccessMessage
                session.invalidateSession()
            }
            else -> session.invalidateSession()
        }
    }

    private fun updateState(result: NfcReadResult) {
        dispatch_async(dispatch_get_main_queue()) { _nfcResult.value = result }
    }
}
