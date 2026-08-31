package com.devtamuno.nfcreader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devtamuno.kmp.nfcreader.data.ParsedNfcPayload

@Composable
internal fun NdefPayloadCard(payload: ParsedNfcPayload) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Record: ${payload.displayName()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            PayloadContent(payload)
        }
    }
}

@Composable
private fun PayloadContent(payload: ParsedNfcPayload) {
    when (payload) {
        is ParsedNfcPayload.Uri -> UriPayloadContent(payload)
        is ParsedNfcPayload.Contact -> ContactPayloadContent(payload)
        is ParsedNfcPayload.Wifi -> WifiPayloadContent(payload)
        is ParsedNfcPayload.Text -> Text(payload.text.ifBlank { "Empty Text Record" })
        is ParsedNfcPayload.Mime -> {
            PayloadItem(Icons.Default.Info, "MIME Type", payload.mimeType)
            Text(
                text = "Data size: ${payload.data.size} bytes",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        is ParsedNfcPayload.External -> {
            PayloadItem(Icons.Default.Info, "External Type", payload.type)
            Text(
                text = "Data size: ${payload.data.size} bytes",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        is ParsedNfcPayload.AndroidApplication -> {
            PayloadItem(Icons.Default.Info, "AAR", payload.packageName)
        }
        is ParsedNfcPayload.SmartPoster -> SmartPosterPayloadContent(payload)
        else -> Text("This record was parsed by an application-defined payload parser.")
    }
}

@Composable
private fun UriPayloadContent(payload: ParsedNfcPayload.Uri) {
    val uriHandler = LocalUriHandler.current

    PayloadItem(Icons.Default.Info, "URI", payload.url)
    Spacer(Modifier.height(16.dp))
    OutlinedButton(
        onClick = { uriHandler.openUri(payload.url) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Open URI")
    }
}

@Composable
private fun ContactPayloadContent(payload: ParsedNfcPayload.Contact) {
    PayloadItem(Icons.Default.Person, "Name", payload.name)
    payload.phone?.let { PayloadItem(Icons.Default.Phone, "Phone", it) }
    payload.email?.let { PayloadItem(Icons.Default.Email, "Email", it) }
}

@Composable
private fun WifiPayloadContent(payload: ParsedNfcPayload.Wifi) {
    var isPasswordVisible by rememberSaveable(payload.ssid, payload.password) { mutableStateOf(false) }

    PayloadItem(Icons.Default.Settings, "SSID", payload.ssid)
    payload.encryption?.let { PayloadItem(Icons.Default.Lock, "Security", it) }
    payload.password?.let { password ->
        PayloadItem(
            icon = Icons.Default.Lock,
            label = "Password",
            value = if (isPasswordVisible) password else MASKED_PASSWORD,
        )
        TextButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
            Text(if (isPasswordVisible) "Hide password" else "Show password")
        }
    }
}

@Composable
private fun SmartPosterPayloadContent(payload: ParsedNfcPayload.SmartPoster) {
    payload.title?.let { PayloadItem(Icons.Default.Info, "Title", it) }
    payload.uri?.let { PayloadItem(Icons.Default.Info, "URI", it) }
    payload.action?.let { PayloadItem(Icons.Default.Info, "Action", it) }
}

@Composable
private fun PayloadItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun ParsedNfcPayload.displayName(): String =
    when (this) {
        is ParsedNfcPayload.Uri -> "URI"
        is ParsedNfcPayload.Text -> "Text"
        is ParsedNfcPayload.Contact -> "Contact"
        is ParsedNfcPayload.Wifi -> "Wi-Fi"
        is ParsedNfcPayload.Mime -> "MIME"
        is ParsedNfcPayload.External -> "External"
        is ParsedNfcPayload.AndroidApplication -> "Android Application"
        is ParsedNfcPayload.SmartPoster -> "Smart Poster"
        else -> "Custom"
    }

private const val MASKED_PASSWORD = "••••••••"
