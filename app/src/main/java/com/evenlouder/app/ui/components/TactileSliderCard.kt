package com.evenlouder.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Flat slider row (no card chrome) used inside the "Fine tune" section.
 * Snaps to 0.1 dB, gives a haptic tick at each whole dB, and offers a one-tap reset.
 */
@Composable
fun TactileSliderCard(
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    defaultValue: Float = 0f,
    unit: String = "dB",
    accentColor: Color? = null,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = accentColor ?: MaterialTheme.colorScheme.primary
    val display = if (abs(value) < 0.05f) 0f else value
    val formatted = (if (display > 0) "+" else "") + "%.1f".format(display) + " " + unit
    val isChanged = abs(value - defaultValue) > 0.05f
    val haptic = LocalHapticFeedback.current
    var lastDetent by remember { mutableStateOf(value.roundToInt()) }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isChanged) {
                    Surface(
                        onClick = { onValueChange(defaultValue) },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.RestartAlt,
                                contentDescription = "Reset $title",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accent.copy(alpha = 0.12f),
                    modifier = Modifier.clickable { onValueChange(defaultValue) }
                ) {
                    Text(
                        formatted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Slider(
            value = value,
            onValueChange = { raw ->
                val snapped = (raw * 10f).roundToInt() / 10f
                if (snapped != value) {
                    val detent = snapped.roundToInt()
                    if (detent != lastDetent) {
                        lastDetent = detent
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onValueChange(snapped)
                }
            },
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$title: $formatted" }
        )
    }
}
