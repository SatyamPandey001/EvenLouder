package com.evenlouder.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evenlouder.app.core.dsp.DspConfig
import com.evenlouder.app.ui.theme.LocalExtendedColors

@Composable
fun VolumeBoostCard(
    config: DspConfig,
    onConfigChange: (DspConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val boost = config.volumeBoostDb

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, LocalExtendedColors.current.cardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = MaterialTheme.colorScheme.primary)
                Text("Volume booster", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "Extra loudness on top of your system volume, kept clean by a limiter.",
                fontSize = 12.sp, color = muted, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !config.eqEnabled,
                    onClick = { onConfigChange(config.copy(eqEnabled = false)) },
                    label = { Text("Boost only", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = config.eqEnabled,
                    onClick = { onConfigChange(config.copy(eqEnabled = true)) },
                    label = { Text("With profile", fontSize = 12.sp) }
                )
            }

            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0f to "Off", 4f to "Low", 7f to "Medium", 10f to "High", 12f to "Max").forEach { (db, label) ->
                    FilterChip(
                        selected = kotlin.math.abs(boost - db) < 0.25f,
                        onClick = { onConfigChange(config.copy(volumeBoostDb = db)) },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }

            TactileSliderCard(
                title = "Boost amount",
                subtitle = if (config.eqEnabled) "Added on top of the selected profile" else "Only the speaker-safe low cut is applied",
                value = boost,
                valueRange = 0f..DspConfig.MAX_BOOST_DB,
                onValueChange = { onConfigChange(config.copy(volumeBoostDb = it)) }
            )

            if (!config.enabled) {
                Text("Tuning is off. Turn it on to hear the boost.", fontSize = 12.sp, color = muted)
            } else if (boost > 7f) {
                Text(
                    "High boost can distort and strain a small speaker. If you hear crackling, lower the boost or the system volume. Keep it low with headphones.",
                    fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
