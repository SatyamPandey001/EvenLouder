package com.evenlouder.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evenlouder.app.core.dsp.DspConfig
import com.evenlouder.app.ui.theme.LocalExtendedColors
import com.evenlouder.app.ui.theme.tactileInset
import com.evenlouder.app.ui.theme.tactileRaised

@Composable
fun VolumeBoostCard(
    config: DspConfig,
    onConfigChange: (DspConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalExtendedColors.current
    val isDark = extendedColors.isDark
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val boost = config.volumeBoostDb

    Column(
        modifier = modifier
            .fillMaxWidth()
            .tactileRaised(shapeRadius = 22.dp, elevation = 5.dp, isDarkTheme = isDark)
            .padding(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .tactileInset(shapeRadius = 10.dp, isDarkTheme = isDark, depth = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text("Volume booster", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Text(
            "Extra loudness on top of your system volume, kept clean by a limiter.",
            fontSize = 12.sp,
            color = muted,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        // Mode Rocker Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .tactileInset(shapeRadius = 12.dp, isDarkTheme = isDark, depth = 2.dp)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .then(
                        if (!config.eqEnabled) {
                            Modifier.tactileRaised(shapeRadius = 9.dp, elevation = 2.dp, isDarkTheme = isDark)
                        } else Modifier
                    )
                    .clickable { onConfigChange(config.copy(eqEnabled = false)) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Boost only",
                    fontSize = 12.5.sp,
                    fontWeight = if (!config.eqEnabled) FontWeight.Bold else FontWeight.Medium,
                    color = if (!config.eqEnabled) primary else muted
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .then(
                        if (config.eqEnabled) {
                            Modifier.tactileRaised(shapeRadius = 9.dp, elevation = 2.dp, isDarkTheme = isDark)
                        } else Modifier
                    )
                    .clickable { onConfigChange(config.copy(eqEnabled = true)) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "With profile",
                    fontSize = 12.5.sp,
                    fontWeight = if (config.eqEnabled) FontWeight.Bold else FontWeight.Medium,
                    color = if (config.eqEnabled) primary else muted
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Preset Steps: Off, Low, Medium, High, Max
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(0f to "Off", 4f to "Low", 7f to "Med", 10f to "High", 12f to "Max").forEach { (db, label) ->
                val isSelected = kotlin.math.abs(boost - db) < 0.25f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (isSelected) {
                                Modifier.tactileRaised(shapeRadius = 8.dp, elevation = 2.dp, isDarkTheme = isDark)
                            } else {
                                Modifier.tactileInset(shapeRadius = 8.dp, isDarkTheme = isDark, depth = 1.5.dp)
                            }
                        )
                        .clickable { onConfigChange(config.copy(volumeBoostDb = db)) }
                        .semantics {
                            role = Role.RadioButton
                            selected = isSelected
                        }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) primary else muted
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        TactileSliderCard(
            title = "Boost amount",
            subtitle = if (config.eqEnabled) "Added on top of the selected profile" else "Only the speaker-safe low cut is applied",
            value = boost,
            valueRange = 0f..DspConfig.MAX_BOOST_DB,
            onValueChange = { onConfigChange(config.copy(volumeBoostDb = it)) }
        )

        if (!config.enabled) {
            Text(
                "Tuning is off. Turn it on to hear the boost.",
                fontSize = 12.sp,
                color = muted,
                modifier = Modifier.padding(top = 2.dp)
            )
        } else if (boost > 7f) {
            Text(
                "High boost can distort and strain a small speaker. If you hear crackling, lower the boost or the system volume. Keep it low with headphones.",
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
