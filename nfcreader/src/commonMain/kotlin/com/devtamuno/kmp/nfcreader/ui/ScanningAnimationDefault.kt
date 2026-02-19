package com.devtamuno.kmp.nfcreader.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtamuno.kmp.nfcreader.resources.Res
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter

object ScanningAnimationDefault {

    @Composable
    fun NfcScanningAnimation(modifier: Modifier = Modifier) {

        val composition by rememberLottieComposition {
            val bytes = Res.readBytes("files/nfc_reading.json")
            LottieCompositionSpec.JsonString(bytes.decodeToString())
        }

        Image(
            modifier = modifier.size(150.dp),
            painter =
                rememberLottiePainter(
                    composition = composition,
                    iterations = Compottie.IterateForever,
                ),
            contentDescription = "NFC scanning animation",
        )
    }
}
