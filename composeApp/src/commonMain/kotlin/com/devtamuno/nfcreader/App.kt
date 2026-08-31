package com.devtamuno.nfcreader

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devtamuno.kmp.nfcreader.data.NfcConfig
import com.devtamuno.kmp.nfcreader.rememberNfcReadManagerState

@Composable
fun App() {
    MaterialTheme {
        val config = remember { createSampleNfcConfig() }
        val nfcManager = rememberNfcReadManagerState(config)
        val result by nfcManager.nfcReadResult.collectAsStateWithLifecycle()

        NfcReaderScreen(
            result = result,
            onStartScanning = nfcManager::startScanning,
        )
    }
}

private fun createSampleNfcConfig() =
    NfcConfig(
        titleMessage = "Ready to Scan",
        subtitleMessage = "Bring a tag closer to your phone to read it.",
        buttonText = "Cancel",
    )
