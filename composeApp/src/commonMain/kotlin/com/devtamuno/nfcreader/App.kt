package com.devtamuno.nfcreader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devtamuno.kmp.nfcreader.data.NfcReadResult
import com.devtamuno.kmp.nfcreader.rememberNfcReadManagerState

@Composable
@Preview
fun App() {
    MaterialTheme {
        val nfcManager = rememberNfcReadManagerState()

        val value by nfcManager.value.collectAsStateWithLifecycle(NfcReadResult.Initial)

        Column(
            modifier =
                Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                    .safeContentPadding()
                    .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = { nfcManager.startScanning() }) { Text("Click to scan!") }

            Spacer(Modifier.height(30.dp))

            when (val state = value) {
                is NfcReadResult.Success -> {
                    Text(
                        "Tag Information",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(10.dp),
                    )

                    Text(
                        "Serial: ${state.data.serialNumber}",
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(10.dp),
                    )
                    Text(
                        "Type: ${state.data.type}",
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(10.dp),
                    )
                    Text(
                        "Payload: ${state.data.payload}",
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(10.dp),
                    )

                    Text(
                        "Technologies: ${state.data.techList.joinToString(separator = ", ")}",
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(10.dp),
                    )
                }
                is NfcReadResult.Error -> {
                    Text("Error: ${state.message}", color = Color.Red)
                }
                NfcReadResult.Initial -> {
                    Text("Tap 'Click to scan!' to begin")
                }
            }
        }
    }
}
