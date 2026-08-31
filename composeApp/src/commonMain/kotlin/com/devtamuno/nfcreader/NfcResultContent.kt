package com.devtamuno.nfcreader

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtamuno.kmp.nfcreader.data.NfcReadResult

@Composable
internal fun NfcResultContent(result: NfcReadResult) {
    when (result) {
        is NfcReadResult.Success -> TagResultDetails(result.data)
        is NfcReadResult.Error -> ErrorMessage(result)
        NfcReadResult.Initial -> StatusMessage("Ready to discover tags")
        NfcReadResult.Scanning -> StatusMessage("Scanning for NFC tag…")
        NfcReadResult.OperationCancelled -> StatusMessage("Operation cancelled")
    }
}

@Composable
private fun StatusMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier.padding(20.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ErrorMessage(error: NfcReadResult.Error) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.padding(16.dp),
    ) {
        Text(
            text = "Error: ${error.message}",
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
