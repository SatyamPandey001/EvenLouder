package com.evenlouder.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.evenlouder.app.ui.theme.LocalExtendedColors
import com.evenlouder.app.ui.theme.tactileClickable
import com.evenlouder.app.ui.theme.tactileInset
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Modern soft tactile slider control used inside the "Fine tune" and "Volume booster" sections.
 * Features physical recessed readout inset, tactile reset knob, snap to 0.1 dB, and haptic feedback.
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
    val extendedColors = LocalExtendedColors.current
    val isDark = extendedColors.isDark
    val accent = accentColor ?: MaterialTheme.colorScheme.primary
    val display = if (abs(value) < 0.05f) 0f else value
    val formatted = (if (display > 0) "+" else "") + "%.1f".format(display) + " " + unit
    val isChanged = abs(value - defaultValue) > 0.05f
    val haptic = LocalHapticFeedback.current
    var lastDetent by remember { mutableStateOf(value.roundToInt()) }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isChanged) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .tactileClickable(shapeRadius = 8.dp, isDarkTheme = isDark, elevation = 2.dp) {
                                onValueChange(defaultValue)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.RestartAlt,
                            contentDescription = "Reset $title",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                // Recessed digital readout bezel
                Box(
                    modifier = Modifier
                        .tactileInset(shapeRadius = 8.dp, isDarkTheme = isDark, depth = 2.5.dp)
                        .clickable { onValueChange(defaultValue) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = formatted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = accent
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Recessed track slider
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
                inactiveTrackColor = if (isDark) Color(0xFF13161C) else Color(0xFFD3DAE2)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "$title: $formatted" }
        )
    }
}
