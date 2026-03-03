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
 * iOS implementation of [NfcReadManager].
 * This class handles NFC tag scanning using CoreNFC's [NFCTagReaderSession].
 *
 * @property config Configuration settings for NFC reading.
 */
internal actual class NfcReadManager actual constructor(private val config: NfcConfig) :
    NSObject(), NFCTagReaderSessionDelegateProtocol {

    private val _nfcResult = MutableStateFlow<NfcReadResult>(NfcReadResult.Initial)
    private var resultCaptured = false

    /**
     * A [StateFlow] that emits the current [NfcReadResult] during the scanning process.
     */
    actual val nfcResult: StateFlow<NfcReadResult>
        get() = _nfcResult.asStateFlow()

    private var session: NFCTagReaderSession? = null

    /**
     * A Composable function that registers the manager.
     * On iOS, this is currently a no-op as the scanning UI is handled by the system.
     */
    @Composable actual fun RegisterManager() = Unit

    /**
     * Starts the NFC scanning process by initiating an [NFCTagReaderSession].
     * Checks if NFC reading is available on the device before starting.
     */
    actual fun startScanning() {
        if (!NFCTagReaderSession.readingAvailable()) {
            updateState(NfcReadResult.Error("NFC not available"))
            return
        }

        resultCaptured = false
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

    /**
     * Stops the NFC scanning process and invalidates the current session.
     */
    actual fun stopScanning() {
        session?.invalidateSession()
        session = null
    }

    override fun tagReaderSessionDidBecomeActive(session: NFCTagReaderSession) = Unit

    /**
     * Invoked when the session is invalidated, either by the user or due to an error.
     */
    override fun tagReaderSession(session: NFCTagReaderSession, didInvalidateWithError: NSError) {
        this.session = null

        if (resultCaptured) return

        val result =
            when (didInvalidateWithError.code) {
                200L -> NfcReadResult.OperationCancelled
                else -> NfcReadResult.Error(didInvalidateWithError.localizedDescription)
            }

        updateState(result)
    }

    /**
     * Invoked when one or more NFC tags are detected.
     */
    override fun tagReaderSession(session: NFCTagReaderSession, didDetectTags: List<*>) {
        val tag = didDetectTags.firstOrNull() as? NFCTagProtocol ?: return

        session.connectToTag(tag) { error ->
            if (error != null) {
                resultCaptured = true
                updateState(NfcReadResult.Error(error.localizedDescription))
                session.invalidateSession()
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
                    resultCaptured = true
                    updateState(NfcReadResult.Error("Unsupported tag type"))
                    session.invalidateSession()
                }
            }
        }
    }

    /**
     * Attempts to read NDEF data from the tag if supported.
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
            resultCaptured = true
            updateState(NfcReadResult.Success(NfcTagData(uid, type, null, techList)))
            session.invalidateSession()
            return
        }

        ndefTag.queryNDEFStatusWithCompletionHandler { status, _, error ->
            if (error != null) {
                resultCaptured = true
                updateState(NfcReadResult.Error(error.localizedDescription))
                session.invalidateSession()
                return@queryNDEFStatusWithCompletionHandler
            }

            if (status == NFCNDEFStatusReadOnly || status == NFCNDEFStatusReadWrite) {
                ndefTag.readNDEFWithCompletionHandler { message: NFCNDEFMessage?, readError ->
                    resultCaptured = true
                    if (readError != null) {
                        updateState(NfcReadResult.Error(readError.localizedDescription))
                    } else {
                        val payload =
                            message?.records?.filterIsInstance<NFCNDEFPayload>()?.joinToString(
                                "\n"
                            ) {
                                it.readableText()
                            }
                        updateState(
                            NfcReadResult.Success(
                                NfcTagData(uid, NfcTagType.NDEF, payload, techList + "NDEF")
                            )
                        )
                    }
                    session.invalidateSession()
                }
            } else {
                resultCaptured = true
                updateState(NfcReadResult.Success(NfcTagData(uid, type, null, techList)))
                session.invalidateSession()
            }
        }
    }

    /**
     * Determines the list of supported technologies for the detected tag.
     */
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

    /**
     * Extracts the UID (Unique Identifier) from the detected tag.
     */
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

    /**
     * Converts the [NFCNDEFPayload] to a readable string format.
     */
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

    /**
     * Helper to convert [NSData] to a [ByteArray].
     */
    private fun NSData.toByteArray(): ByteArray {
        val bytes = ByteArray(length.toInt())
        memcpy(bytes.refTo(0), this.bytes, length)
        return bytes
    }

    /**
     * Helper to convert a [ByteArray] to a hex string.
     */
    private fun ByteArray.toHex(): String =
        toHexString(
            HexFormat {
                upperCase = true
                bytes { byteSeparator = ":" }
            }
        )

    /**
     * Updates the [_nfcResult] on the main dispatch queue.
     */
    private fun updateState(result: NfcReadResult) {
        dispatch_async(dispatch_get_main_queue()) { _nfcResult.value = result }
    }
}
