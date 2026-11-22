package com.evenlouder.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evenlouder.app.core.dsp.DspConfig
import com.evenlouder.app.core.dsp.ProfileCategory
import com.evenlouder.app.core.dsp.TuningProfile
import com.evenlouder.app.ui.theme.LocalExtendedColors

/** True when [config] holds exactly this preset's tuning values (power state ignored). */
internal fun TuningProfile.matchesConfig(config: DspConfig): Boolean =
    config.eqEnabled &&
        kotlin.math.abs(config.highPassCutoffHz - this.config.highPassCutoffHz) < 0.5f &&
        kotlin.math.abs(config.bassGainDb - this.config.bassGainDb) < 0.05f &&
        kotlin.math.abs(config.boxinessCutDb - this.config.boxinessCutDb) < 0.05f &&
        kotlin.math.abs(config.vocalClarityDb - this.config.vocalClarityDb) < 0.05f &&
        kotlin.math.abs(config.trebleGainDb - this.config.trebleGainDb) < 0.05f &&
        kotlin.math.abs(config.loudnessGainDb - this.config.loudnessGainDb) < 0.05f

/**
 * Horizontal profile carousel with category filter and a plain-language description of the
 * selected profile. Bypass is not listed here; the power button on the dashboard handles it.
 */
@Composable
fun ProfileSelector(
    currentConfig: DspConfig,
    onSelectProfile: (TuningProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(ProfileCategory.ALL) }
    val visible = remember(selectedCategory) {
        TuningProfile.values().filter { it != TuningProfile.BYPASS && it.matchesCategory(selectedCategory) }
    }
    val active = TuningProfile.values().firstOrNull { it != TuningProfile.BYPASS && it.matchesConfig(currentConfig) }
    val primary = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProfileCategory.values().forEach { category ->
                val on = selectedCategory == category
                FilterChip(
                    selected = on,
                    onClick = { selectedCategory = category },
                    label = { Text(category.label, fontSize = 12.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(visible, key = { it.name }) { profile ->
                ProfileCard(
                    profile = profile,
                    isSelected = profile == active,
                    onClick = { onSelectProfile(profile) }
                )
            }
        }

        Text(
            text = active?.description ?: "Custom tuning. Pick a profile to start from a preset.",
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun iconFor(profile: TuningProfile): ImageVector = when (profile) {
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
private fun ProfileCard(profile: TuningProfile, isSelected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val border = LocalExtendedColors.current.cardBorder
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) accent else border),
        modifier = Modifier.width(150.dp).heightIn(min = 112.dp).semantics { selected = isSelected }
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(36.dp).background(
                    color = if (isSelected) accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                )
            ) {
                Icon(
                    iconFor(profile), contentDescription = null,
                    tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    profile.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Text(
                    profile.subtitle, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
