@file:OptIn(ExperimentalMaterial3Api::class)

package com.devtamuno.kmp.nfcreader.contract

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devtamuno.kmp.nfcreader.data.NfcConfig

/**
 * Bottom sheet shown during NFC scanning on Android.
 *
 * @param config The [NfcConfig] supplying UI strings, animation slot, and dismissal behaviour.
 * @param isVisible Whether the sheet is currently shown.
 * @param onDismiss Called when the sheet is dismissed (drag, back press, outside tap, or cancel button).
 */
@Composable
internal fun NfcScanBottomSheet(
    config: NfcConfig,
    isVisible: Boolean,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            dragHandle = null,
            sheetState = sheetState,
            sheetGesturesEnabled = config.sheetGesturesEnabled,
            properties =
                ModalBottomSheetProperties(
                    shouldDismissOnBackPress = config.shouldDismissBottomSheetOnBackPress,
                    shouldDismissOnClickOutside = config.shouldDismissBottomSheetOnClickOutside,
                ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier =
                        Modifier.align(Alignment.CenterHorizontally)
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(2.dp),
                            )
                )

                Text(
                    text = config.titleMessage,
                    style =
                        MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = config.subtitleMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )

                config.nfcScanningAnimationSlot(this)

                OutlinedButton(
                    modifier =
                        Modifier.height(height = 45.dp)
                            .width(200.dp)
                            .align(Alignment.CenterHorizontally),
                    elevation =
                        ButtonDefaults.elevatedButtonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            disabledElevation = 0.dp,
                            hoveredElevation = 0.dp,
                            focusedElevation = 0.dp,
                        ),
                    interactionSource = remember { MutableInteractionSource() },
                    shape = RoundedCornerShape(10.dp),
                    onClick = onDismiss,
                ) {
                    Text(text = config.buttonText, fontSize = 11.sp)
                }
            }
        }
    }
}