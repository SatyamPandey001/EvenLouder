package com.evenlouder.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evenlouder.app.ui.theme.LocalExtendedColors

/**
 * Premium tactile slider card with high-contrast text, smooth continuous track,
 * accessible screen-reader tags, and quick reset-to-unity action.
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
    val effectiveAccent = accentColor ?: MaterialTheme.colorScheme.primary
    val formattedValue = if (value > 0) "+${"%.1f".format(value)} $unit" else "${"%.1f".format(value)} $unit"
    val isNonZero = kotlin.math.abs(value - defaultValue) > 0.05f

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(extendedColors.cardBorder)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header Row: Title & dB Badge + Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Reset Button (visible or enabled when modified from default)
                    if (isNonZero) {
                        Surface(
                            onClick = { onValueChange(defaultValue) },
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.RestartAlt,
                                    contentDescription = "Reset $title to $defaultValue $unit",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Value Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = effectiveAccent.copy(alpha = 0.12f),
                        modifier = Modifier.clickable { onValueChange(defaultValue) }
                    ) {
                        Text(
                            text = formattedValue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = effectiveAccent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Smooth Continuous Slider (No steps = 11 visual dot clutter)
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = effectiveAccent,
                    activeTrackColor = effectiveAccent,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "$title: $formattedValue"
                    }
            )

            // Range Labels at extremes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${valueRange.start.toInt()} $unit",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${valueRange.endInclusive.toInt()} $unit",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
