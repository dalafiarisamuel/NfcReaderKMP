package com.devtamuno.nfcreader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devtamuno.kmp.nfcreader.data.NfcReadResult

@Composable
internal fun NfcReaderScreen(
    result: NfcReadResult,
    onStartScanning: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.surface)
                .safeContentPadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        Text(
            text = "NFC Reader KMP",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Sample Application",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )

        Spacer(Modifier.height(40.dp))

        Button(
            onClick = onStartScanning,
            modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Default.Info, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Start Scanning", fontSize = 18.sp)
        }

        Spacer(Modifier.height(40.dp))
        NfcResultContent(result)
        Spacer(Modifier.height(40.dp))
    }
}

@Preview
@Composable
private fun NfcReaderScreenPreview() {
    MaterialTheme {
        NfcReaderScreen(
            result = NfcReadResult.Initial,
            onStartScanning = {},
        )
    }
}
