@file:OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)

package com.devtamuno.kmp.nfcreader.contract

import androidx.compose.runtime.Composable
import com.devtamuno.kmp.nfcreader.data.NfcConfig
import com.devtamuno.kmp.nfcreader.data.NfcReadResult
import com.devtamuno.kmp.nfcreader.data.NfcTagData
import com.devtamuno.kmp.nfcreader.data.NfcTagType
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.CoreNFC.NFCNDEFMessage
import platform.CoreNFC.NFCNDEFPayload
import platform.CoreNFC.NFCNDEFStatusReadOnly
import platform.CoreNFC.NFCNDEFStatusReadWrite
import platform.CoreNFC.NFCPollingISO14443
import platform.CoreNFC.NFCPollingISO15693
import platform.CoreNFC.NFCTagProtocol
import platform.CoreNFC.NFCTagReaderSession
import platform.CoreNFC.NFCTagReaderSessionDelegateProtocol
import platform.CoreNFC.NFCTagTypeFeliCa
import platform.CoreNFC.NFCTagTypeISO15693
import platform.CoreNFC.NFCTagTypeISO7816Compatible
import platform.CoreNFC.NFCTagTypeMiFare
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

/**
 * iOS implementation of [NfcReadManager]. This class handles NFC tag scanning using CoreNFC's
 * [NFCTagReaderSession].
 */
internal actual class NfcReadManager actual constructor(private val config: NfcConfig) :
    NSObject(), NFCTagReaderSessionDelegateProtocol {

    private val _nfcResult = MutableStateFlow<NfcReadResult>(NfcReadResult.Initial)
    private var pendingResult: NfcReadResult? = null

    /** A [StateFlow] that emits the current [NfcReadResult]. */
    actual val nfcResult: StateFlow<NfcReadResult>
        get() = _nfcResult.asStateFlow()

    private var session: NFCTagReaderSession? = null

    /** Registers the manager (no-op on iOS as the system handles the UI). */
    @Composable actual fun RegisterManager() = Unit

    /** Starts the NFC scanning process. */
    actual fun startScanning() {
        if (!NFCTagReaderSession.readingAvailable()) {
            updateState(NfcReadResult.Error("NFC not available"))
            return
        }

        pendingResult = null
        updateState(NfcReadResult.Scanning)
        session =
            NFCTagReaderSession(
                    pollingOption = NFCPollingISO14443 or NFCPollingISO15693,
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
     * Invoked when the session is invalidated. We emit the captured result only HERE, after the
     * system UI has dismissed.
     */
    override fun tagReaderSession(session: NFCTagReaderSession, didInvalidateWithError: NSError) {
        this.session = null

        val result =
            pendingResult
                ?: run {
                    when (didInvalidateWithError.code) {
                        200L -> NfcReadResult.OperationCancelled
                        else -> NfcReadResult.Error(didInvalidateWithError.localizedDescription)
                    }
                }

        updateState(result)
        pendingResult = null
    }

    /** Invoked when tags are detected. */
    override fun tagReaderSession(session: NFCTagReaderSession, didDetectTags: List<*>) {
        val tag = didDetectTags.firstOrNull() as? NFCTagProtocol ?: return

        session.connectToTag(tag) { error ->
            if (error != null) {
                finishSession(session, NfcReadResult.Error(error.localizedDescription))
                return@connectToTag
            }

            val uid = extractUid(tag)
            val techList = getTechList(tag)

            when (tag.type) {
                NFCTagTypeMiFare -> {
                    val miFare = tag.asNFCMiFareTag()
                    readNdefIfAvailable(session, uid, techList, NfcTagType.MIFARE, miFare)
                }
                NFCTagTypeISO15693 -> {
                    val iso15693 = tag.asNFCISO15693Tag()
                    readNdefIfAvailable(session, uid, techList, NfcTagType.ISO15693, iso15693)
                }
                NFCTagTypeISO7816Compatible -> {
                    val iso7816 = tag.asNFCISO7816Tag()
                    readNdefIfAvailable(session, uid, techList, NfcTagType.ISO7816, iso7816)
                }
                NFCTagTypeFeliCa -> {
                    val felica = tag.asNFCFeliCaTag()
                    readNdefIfAvailable(session, uid, techList, NfcTagType.FELICA, felica)
                }
                else -> {
                    finishSession(session, NfcReadResult.Error("Unsupported tag type"))
                }
            }
        }
    }

    /** Attempts to read NDEF data. */
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
                finishSession(session, NfcReadResult.Error(error.localizedDescription))
                return@queryNDEFStatusWithCompletionHandler
            }

            if (status == NFCNDEFStatusReadOnly || status == NFCNDEFStatusReadWrite) {
                ndefTag.readNDEFWithCompletionHandler { message: NFCNDEFMessage?, readError ->
                    if (readError != null) {
                        finishSession(session, NfcReadResult.Error(readError.localizedDescription))
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
     * [tagReaderSession(session, didInvalidateWithError)] once the system dialog is dismissed.
     */
    private fun finishSession(session: NFCTagReaderSession, result: NfcReadResult) {
        pendingResult = result
        when (result) {
            is NfcReadResult.Error -> session.invalidateSessionWithErrorMessage(result.message)
            is NfcReadResult.Success -> {
                session.alertMessage = "Tag Scanned Successfully"
                session.invalidateSession()
            }
            else -> session.invalidateSession()
        }
    }

    private fun getTechList(tag: NFCTagProtocol): List<String> {
        return when (tag.type) {
            NFCTagTypeMiFare -> listOf("ISO 14443-3A", "NfcA", "Mifare")
            NFCTagTypeISO15693 -> listOf("ISO 15693", "NfcV")
            NFCTagTypeISO7816Compatible -> {
                val list = mutableListOf("ISO 14443-4", "ISO-DEP")
                val iso7816 = tag.asNFCISO7816Tag()
                if (iso7816?.historicalBytes != null) {
                    list.add("ISO 14443-3A")
                    list.add("NfcA")
                } else if (iso7816?.applicationData != null) {
                    list.add("ISO 14443-3B")
                    list.add("NfcB")
                }
                list.add("ISO 7816")
                list
            }
            NFCTagTypeFeliCa -> listOf("FeliCa", "NfcF", "JIS 6319-4")
            else -> listOf("Unknown")
        }
    }

    private fun extractUid(tag: NFCTagProtocol): String {
        return when (tag.type) {
            NFCTagTypeMiFare -> tag.asNFCMiFareTag()?.identifier?.toByteArray()?.toHex() ?: ""
            NFCTagTypeISO15693 -> tag.asNFCISO15693Tag()?.identifier?.toByteArray()?.toHex() ?: ""
            NFCTagTypeISO7816Compatible ->
                tag.asNFCISO7816Tag()?.identifier?.toByteArray()?.toHex() ?: ""
            NFCTagTypeFeliCa -> tag.asNFCFeliCaTag()?.currentIDm?.toByteArray()?.toHex() ?: ""
            else -> ""
        }
    }

    private fun NFCNDEFPayload.readableText(): String {
        return try {
            val bytes = payload.toByteArray()
            if (bytes.isEmpty()) return ""
            val status = bytes[0].toInt()
            val languageCodeLength = status and 0x3F
            val textStartIndex = languageCodeLength + 1
            if (bytes.size <= textStartIndex) return ""
            bytes.copyOfRange(textStartIndex, bytes.size).decodeToString()
        } catch (_: Exception) {
            ""
        }
    }

    private fun NSData.toByteArray(): ByteArray {
        val bytes = ByteArray(length.toInt())
        memcpy(bytes.refTo(0), this.bytes, length)
        return bytes
    }

    private fun ByteArray.toHex(): String =
        toHexString(
            HexFormat {
                upperCase = true
                bytes { byteSeparator = ":" }
            }
        )

    private fun updateState(result: NfcReadResult) {
        dispatch_async(dispatch_get_main_queue()) { _nfcResult.value = result }
    }
}
