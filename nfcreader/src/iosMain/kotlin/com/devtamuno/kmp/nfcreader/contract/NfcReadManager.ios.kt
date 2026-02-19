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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.CoreNFC.NFCNDEFMessage
import platform.CoreNFC.NFCNDEFPayload
import platform.CoreNFC.NFCNDEFReaderSession
import platform.CoreNFC.NFCNDEFReaderSessionDelegateProtocol
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.darwin.NSObject
import platform.posix.memcpy

/**
 * Manager class for handling NFC reading operations on iOS.
 *
 * This class implements the [NFCNDEFReaderSessionDelegateProtocol] to handle NFC session events. It
 * uses the native iOS `NFCNDEFReaderSession` for scanning NDEF tags.
 *
 * **Note:** On iOS, only the `subtitleMessage` from [NfcConfig] is used as the `alertMessage` in
 * the native scanning dialog.
 *
 * @property config The [NfcConfig] used to configure the scanning behavior.
 */
internal actual class NfcReadManager actual constructor(private val config: NfcConfig) :
    NSObject(), NFCNDEFReaderSessionDelegateProtocol {
    private val _tagData = MutableStateFlow<NfcReadResult>(NfcReadResult.Initial)
    private var session: NFCNDEFReaderSession? = null

    /** A [StateFlow] that emits the current [NfcReadResult] of the NFC scanning process. */
    actual val nfcResult: StateFlow<NfcReadResult>
        get() = _tagData.asStateFlow()

    /**
     * Registers the manager. On iOS, this is a NO-OP as the scanning UI is handled natively by the
     * system.
     */
    @Composable
    actual fun RegisterManager() {
        // NO-OP
    }

    /**
     * Starts the NFC scanning process using [NFCNDEFReaderSession]. Checks if NFC reading is
     * available on the device before starting.
     */
    actual fun startScanning() {
        if (NFCNDEFReaderSession.readingAvailable()) {
            session = NFCNDEFReaderSession(this, null, false)
            session?.alertMessage = config.subtitleMessage
            _tagData.value = NfcReadResult.Initial
            session?.beginSession()
        } else {
            _tagData.value = NfcReadResult.Error("NFC reading is not available")
        }
    }

    /** Stops the NFC scanning process and invalidates the session. */
    actual fun stopScanning() {
        session?.invalidateSession()
        session = null
    }

    /**
     * Called when the NFC session is invalidated, either due to an error or user cancellation.
     * Updates [nfcResult] with either [NfcReadResult.OperationCancelled] (code 200) or
     * [NfcReadResult.Error].
     */
    override fun readerSession(session: NFCNDEFReaderSession, didInvalidateWithError: NSError) {
        if (didInvalidateWithError.code == 200L) {
            _tagData.value = NfcReadResult.OperationCancelled
        } else {
            _tagData.value = NfcReadResult.Error(didInvalidateWithError.localizedDescription)
        }
        this.session = null
    }

    /** Called when the NFC reader session becomes active. */
    override fun readerSessionDidBecomeActive(session: NFCNDEFReaderSession) {
        // NO-OP
    }

    /**
     * Called when NDEF messages are detected. Extracts records from the first detected message,
     * joins their payloads into a single string, and updates [nfcResult] with
     * [NfcReadResult.Success]. The session is invalidated after a successful read.
     */
    override fun readerSession(session: NFCNDEFReaderSession, didDetectNDEFs: List<*>) {
        val message = didDetectNDEFs.firstOrNull() as? NFCNDEFMessage
        val records = message?.records?.filterIsInstance<NFCNDEFPayload>()

        val combinedPayload =
            records?.joinToString(separator = "\n") { record ->
                record.payload.toByteArray().decodeToString()
            }

        val data =
            NfcTagData(
                serialNumber = "",
                type = NfcTagType.NDEF,
                payload = combinedPayload,
                techList = listOf("NDEF"),
            )
        _tagData.value = NfcReadResult.Success(data)

        session.invalidateSession()
    }

    /** Extension function to convert [NSData] to a [ByteArray]. */
    private fun NSData.toByteArray(): ByteArray {
        val bytes = ByteArray(this.length.toInt())
        memScoped { memcpy(bytes.refTo(0), this@toByteArray.bytes, this@toByteArray.length) }
        return bytes
    }
}
