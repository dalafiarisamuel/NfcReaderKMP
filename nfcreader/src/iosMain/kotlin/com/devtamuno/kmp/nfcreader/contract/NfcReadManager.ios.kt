@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.devtamuno.kmp.nfcreader.contract

import androidx.compose.runtime.Composable
import com.devtamuno.kmp.nfcreader.data.NfcConfig
import com.devtamuno.kmp.nfcreader.data.NfcReadResult
import com.devtamuno.kmp.nfcreader.data.NfcTagData
import com.devtamuno.kmp.nfcreader.data.NfcTagType
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreNFC.NFCISO7816APDU
import platform.CoreNFC.NFCNDEFMessage
import platform.CoreNFC.NFCNDEFPayload
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
import platform.CoreNFC.NFCMiFareUnknown
import platform.CoreNFC.NFCTagTypeMiFare
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.create
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
 */
internal actual class NfcReadManager actual constructor(private val config: NfcConfig) :
    NSObject(), NFCTagReaderSessionDelegateProtocol {

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

    private var discoveredTag: NFCTagProtocol? = null

    /** A [StateFlow] that emits the current [NfcReadResult]. */
    actual val nfcResult: StateFlow<NfcReadResult>
        get() = _nfcResult.asStateFlow()

    /** Registers the manager (no-op on iOS — the system owns the scanning UI). */
    @Composable actual fun RegisterManager() = Unit

    /** Starts the NFC scanning process. */
    actual fun startScanning() {
        if (session != null) return

        _nfcResult.value = NfcReadResult.Initial

        if (!NFCTagReaderSession.readingAvailable()) {
            updateState(NfcReadResult.Error(config.nfcUnsupportedMessage))
            return
        }

        pendingResult = null
        discoveredTag = null
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
        discoveredTag = null
    }

    override fun tagReaderSessionDidBecomeActive(session: NFCTagReaderSession) = Unit

    /**
     * Invoked when the session is invalidated. The pending result (captured before invalidation) is
     * emitted here — after the system dialog has fully dismissed — to avoid UI conflicts.
     */
    override fun tagReaderSession(session: NFCTagReaderSession, didInvalidateWithError: NSError) {
        this.session = null
        this.discoveredTag = null

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
        this.discoveredTag = tag

        // MIFARE Classic (NFCMiFareUnknown) uses Crypto1 which CoreNFC does not support.
        // The UID and mifareFamily are set during ISO 14443-3A anticollision and are available
        // before connectToTag. Calling connectToTag or any NDEF operation causes the session to
        // hang and eventually time out, so we return the UID immediately without connecting.
        if (tag.type == NFCTagTypeMiFare &&
            tag.asNFCMiFareTag()?.mifareFamily == NFCMiFareUnknown) {
            val uid = extractUid(tag)
            val techList = getTechList(tag)
            finishSession(session, NfcReadResult.Success(NfcTagData(uid, NfcTagType.MIFARE, null, techList)))
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
                        val payload =
                            message?.records?.filterIsInstance<NFCNDEFPayload>()?.joinToString(
                                "\n"
                            ) {
                                it.readableText()
                            }
                        finishSession(
                            session,
                            NfcReadResult.Success(
                                NfcTagData(uid, NfcTagType.NDEF, payload, techList + "NDEF")
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
                session.alertMessage = config.nfcSuccessMessage
                session.invalidateSession()
            }
            else -> session.invalidateSession()
        }
    }

    private fun updateState(result: NfcReadResult) {
        dispatch_async(dispatch_get_main_queue()) { _nfcResult.value = result }
    }

    /**
     * Sends an APDU (Application Protocol Data Unit) command to an ISO 7816-4 compatible tag.
     *
     * @param command The APDU command bytes.
     * @return The response bytes from the tag.
     * @throws Exception if the tag does not support ISO 7816 or the command fails.
     */
    actual suspend fun sendApdu(command: ByteArray): ByteArray = suspendCancellableCoroutine { continuation ->
        val tag = discoveredTag?.asNFCISO7816Tag()
        if (tag == null) {
            continuation.resumeWithException(Exception("Tag does not support ISO 7816-4"))
            return@suspendCancellableCoroutine
        }

        val apdu = command.toNSData()?.let { NFCISO7816APDU(data = it) }
        if (apdu == null) {
            continuation.resumeWithException(Exception("Invalid APDU command"))
            return@suspendCancellableCoroutine
        }

        tag.sendCommandAPDU(apdu) { responseData, sw1, sw2, error ->
            if (error != null) {
                continuation.resumeWithException(Exception(error.localizedDescription))
            } else {
                val responseBytes = responseData?.toByteArray() ?: byteArrayOf()
                continuation.resume(responseBytes + sw1.toByte() + sw2.toByte())
            }
        }
    }

    private fun ByteArray.toNSData(): NSData? = if (isEmpty()) null else {
        this.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
        }
    }
}
