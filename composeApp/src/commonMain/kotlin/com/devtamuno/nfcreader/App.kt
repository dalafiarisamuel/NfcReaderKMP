package com.devtamuno.nfcreader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devtamuno.kmp.nfcreader.data.NdefParser
import com.devtamuno.kmp.nfcreader.data.NfcConfig
import com.devtamuno.kmp.nfcreader.data.NfcReadResult
import com.devtamuno.kmp.nfcreader.data.NfcTagData
import com.devtamuno.kmp.nfcreader.data.ParsedNfcPayload
import com.devtamuno.kmp.nfcreader.rememberNfcReadManagerState

@Composable
@Preview
fun App() {
    MaterialTheme {
        val nfcManager =
            rememberNfcReadManagerState(
                config =
                    NfcConfig(
                        titleMessage = "Ready to Scan",
                        subtitleMessage = "Bring a tag closer to your phone to read it.",
                        buttonText = "Cancel",
                    )
            )

        val value by nfcManager.nfcReadResult.collectAsState(NfcReadResult.Initial)

        Column(
            modifier =
                Modifier.background(MaterialTheme.colorScheme.surface)
                    .safeContentPadding()
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))
            Text(
                "NFC Reader KMP",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Sample Application",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = { nfcManager.startScanning() },
                modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start Scanning", fontSize = 18.sp)
            }

            Spacer(Modifier.height(40.dp))

            when (val state = value) {
                is NfcReadResult.Success -> {
                    TagResultDetails(state.data)
                }
                is NfcReadResult.Error -> {
                    ErrorMessage(state.message)
                }
                NfcReadResult.Initial -> {
                    StatusMessage("Ready to discover tags")
                }
                NfcReadResult.OperationCancelled -> {
                    StatusMessage("Operation Cancelled")
                }
                NfcReadResult.Scanning -> {
                    StatusMessage("Scanning for NFC Tag....")
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun TagResultDetails(data: NfcTagData) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors =
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Hardware Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                InfoRow("Serial", data.serialNumber)
                InfoRow("Type", data.type.name)
                InfoRow("Techs", data.techList.joinToString(", "))
            }
        }

        if (data.parsedPayloads.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Text(
                    "No NDEF Data Found",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        data.parsedPayloads.forEach { payload ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Record: ${payload::class.simpleName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(12.dp))

                    when (payload) {
                        is ParsedNfcPayload.Uri -> {
                            PayloadItem(Icons.Default.Info, "URI", payload.url)
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { uriHandler.openUri(payload.url) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Open in Browser")
                            }
                        }
                        is ParsedNfcPayload.Contact -> {
                            PayloadItem(Icons.Default.Person, "Name", payload.name)
                            payload.phone?.let { PayloadItem(Icons.Default.Phone, "Phone", it) }
                            payload.email?.let { PayloadItem(Icons.Default.Email, "Email", it) }
                        }
                        is ParsedNfcPayload.Wifi -> {
                            PayloadItem(Icons.Default.Settings, "SSID", payload.ssid)
                            payload.encryption?.let {
                                PayloadItem(Icons.Default.Lock, "Security", it)
                            }
                            payload.password?.let {
                                PayloadItem(Icons.Default.Lock, "Password", it)
                            }
                        }
                        is ParsedNfcPayload.Text -> {
                            Text(payload.text.ifBlank { "Empty Text Record" })
                        }
                        is ParsedNfcPayload.Mime -> {
                            PayloadItem(Icons.Default.Info, "MIME Type", payload.mimeType)
                            Text("Data size: ${payload.data.size} bytes", style = MaterialTheme.typography.bodySmall)
                        }
                        is ParsedNfcPayload.External -> {
                            PayloadItem(Icons.Default.Info, "External Type", payload.type)
                            Text("Data size: ${payload.data.size} bytes", style = MaterialTheme.typography.bodySmall)
                        }
                        is ParsedNfcPayload.AndroidApplication -> {
                            PayloadItem(Icons.Default.Info, "AAR", payload.packageName)
                        }
                        is ParsedNfcPayload.SmartPoster -> {
                            payload.title?.let { PayloadItem(Icons.Default.Info, "Title", it) }
                            payload.uri?.let { PayloadItem(Icons.Default.Info, "URI", it) }
                            payload.action?.let { PayloadItem(Icons.Default.Info, "Action", it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$label: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun PayloadItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun StatusMessage(message: String) {
    Text(
        message,
        modifier = Modifier.padding(20.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun ErrorMessage(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.padding(16.dp),
    ) {
        Text(
            "Error: $message",
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
