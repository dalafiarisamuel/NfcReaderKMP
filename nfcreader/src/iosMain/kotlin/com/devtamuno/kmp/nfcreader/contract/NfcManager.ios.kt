@file:OptIn(ExperimentalForeignApi::class)

package com.devtamuno.kmp.nfcreader.contract

import androidx.compose.runtime.Composable
import com.devtamuno.kmp.nfcreader.data.NfcConfig
import com.devtamuno.kmp.nfcreader.data.NfcReadResult
import com.devtamuno.kmp.nfcreader.data.NfcTagData
import com.devtamuno.kmp.nfcreader.data.NfcTagType
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import platform.CoreNFC.NFCNDEFMessage
import platform.CoreNFC.NFCNDEFPayload
import platform.CoreNFC.NFCNDEFReaderSession
import platform.CoreNFC.NFCNDEFReaderSessionDelegateProtocol
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.darwin.NSObject
import platform.posix.memcpy

internal actual class NfcReadManager actual constructor(private val config: NfcConfig) :
    NSObject(), NFCNDEFReaderSessionDelegateProtocol {

    private val scope = CoroutineScope(SupervisorJob())
    private val _tagData: MutableStateFlow<NfcReadResult> = MutableStateFlow(NfcReadResult.Initial)
    private var session: NFCNDEFReaderSession? = null

    actual val value: StateFlow<NfcReadResult>
        get() = _tagData

    @Composable
    actual fun RegisterManager() {
        // No-op
    }

    actual fun startScanning() {
        if (NFCNDEFReaderSession.readingAvailable()) {
            session = NFCNDEFReaderSession(this, null, false)
            session?.alertMessage = config.bringTagCloserMessage
            session?.beginSession()
        } else {
            scope.launch { _tagData.emit(NfcReadResult.Error("NFC reading is not available")) }
        }
    }

    actual fun stopScanning() {
        session?.invalidateSession()
        session = null
    }

    override fun readerSession(session: NFCNDEFReaderSession, didInvalidateWithError: NSError) {
        println("reader session error ${didInvalidateWithError.description}")
        scope.launch {
            _tagData.emit(NfcReadResult.Error(didInvalidateWithError.localizedDescription))
        }
    }

    override fun readerSessionDidBecomeActive(session: NFCNDEFReaderSession) {
        println("reader session active")
    }

    override fun readerSession(session: NFCNDEFReaderSession, didDetectNDEFs: List<*>) {
        val message = didDetectNDEFs.firstOrNull() as? NFCNDEFMessage
        val records = message?.records?.filterIsInstance<NFCNDEFPayload>()

        val combinedPayload =
            records?.joinToString(separator = "\n") { record ->
                record.payload.toByteArray().decodeToString()
            }

        scope.launch {
            val data =
                NfcTagData(
                    serialNumber = "",
                    type = NfcTagType.NDEF,
                    payload = combinedPayload,
                    techList = listOf("NDEF"),
                )
            _tagData.emit(NfcReadResult.Success(data))
        }

        session.invalidateSession()
    }

    private fun NSData.toByteArray(): ByteArray {
        val bytes = ByteArray(this.length.toInt())
        memScoped { memcpy(bytes.refTo(0), this@toByteArray.bytes, this@toByteArray.length) }
        return bytes
    }
}
