package com.evenlouder.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evenlouder.app.core.dsp.DspConfig
import com.evenlouder.app.core.dsp.ProfileCategory
import com.evenlouder.app.core.dsp.TuningProfile
import com.evenlouder.app.ui.theme.LocalExtendedColors

/**
 * Premium Filterable Grid for Acoustic Tuning Profiles.
 * Includes category filters (All, Bass Modes, Instrumental, Dolby & Dirac)
 * with dedicated vector icons, tactile selection state, and micro-speaker acoustic specs.
 */
@Composable
fun ProfileSelector(
    currentConfig: DspConfig,
    onSelectProfile: (TuningProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalExtendedColors.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val categoryScrollState = rememberScrollState()

    var selectedCategory by remember { mutableStateOf(ProfileCategory.ALL) }

    val visibleProfiles = remember(selectedCategory) {
        TuningProfile.values().filter { it.matchesCategory(selectedCategory) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(categoryScrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProfileCategory.values().forEach { category ->
                val isCatSelected = selectedCategory == category
                FilterChip(
                    selected = isCatSelected,
                    onClick = { selectedCategory = category },
                    label = {
                        Text(
                            text = category.label,
                            fontSize = 11.sp,
                            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = primaryColor,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        // Profiles 2-Column Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            visibleProfiles.chunked(2).forEach { rowProfiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowProfiles.forEach { profile ->
                        val isSelected = if (!profile.config.enabled) {
                            !currentConfig.enabled
                        } else {
                            currentConfig.enabled &&
                                    kotlin.math.abs(currentConfig.highPassCutoffHz - profile.config.highPassCutoffHz) < 0.1f &&
                                    kotlin.math.abs(currentConfig.vocalClarityDb - profile.config.vocalClarityDb) < 0.1f &&
                                    kotlin.math.abs(currentConfig.bassGainDb - profile.config.bassGainDb) < 0.1f &&
                                    kotlin.math.abs(currentConfig.boxinessCutDb - profile.config.boxinessCutDb) < 0.1f &&
                                    kotlin.math.abs(currentConfig.trebleGainDb - profile.config.trebleGainDb) < 0.1f &&
                                    kotlin.math.abs(currentConfig.loudnessGainDb - profile.config.loudnessGainDb) < 0.1f
                        }

                        ProfileCard(
                            profile = profile,
                            icon = getProfileIcon(profile),
                            isSelected = isSelected,
                            accentColor = if (profile == TuningProfile.BYPASS) Color(0xFFFF5252) else primaryColor,
                            onClick = { onSelectProfile(profile) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Keep even width if row has only one item
                    if (rowProfiles.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun getProfileIcon(profile: TuningProfile): ImageVector = when (profile) {
    TuningProfile.DOLBY_SPATIAL -> Icons.Rounded.Movie
    TuningProfile.MAXX_BASS -> Icons.Rounded.GraphicEq
    TuningProfile.DIRAC_HIFI -> Icons.Rounded.Headphones
    TuningProfile.VOCAL_PRO -> Icons.Rounded.RecordVoiceOver
    TuningProfile.ULTRA_BOOST -> Icons.AutoMirrored.Rounded.VolumeUp
    TuningProfile.BYPASS -> Icons.Rounded.PowerSettingsNew
    TuningProfile.DOLBY_DYNAMIC_BASS -> Icons.Rounded.SurroundSound
    TuningProfile.DIRAC_TIGHT_BASS -> Icons.Rounded.Speed
    TuningProfile.SUB_BASS_DEEP -> Icons.Rounded.SpeakerGroup
    TuningProfile.WARM_ANALOG_BASS -> Icons.Rounded.Album
    TuningProfile.DIRAC_VIRTUO_ACOUSTIC -> Icons.Rounded.Piano
    TuningProfile.DOLBY_SYMPHONY -> Icons.Rounded.Audiotrack
    TuningProfile.LIVE_INSTRUMENTAL -> Icons.Rounded.MusicNote
}

@Composable
private fun ProfileCard(
    profile: TuningProfile,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalExtendedColors.current

    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) accentColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accentColor else extendedColors.cardBorder
        ),
        modifier = modifier.heightIn(min = 72.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (isSelected) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = profile.subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
