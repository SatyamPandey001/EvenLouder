package com.evenlouder.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evenlouder.app.core.dsp.DspConfig
import com.evenlouder.app.core.dsp.ParametricBandFitter
import com.evenlouder.app.core.dsp.ProfileCategory
import com.evenlouder.app.core.dsp.TuningCategory
import com.evenlouder.app.core.dsp.TuningProfile
import com.evenlouder.app.ui.theme.LocalExtendedColors

/** True when [config] matches this profile (by explicit activeProfileId or parameter values). */
internal fun TuningProfile.matchesConfig(config: DspConfig): Boolean {
    if (!config.eqEnabled) return false
    if (config.activeProfileId != null) {
        return this.acousticProfile?.id == config.activeProfileId
    }
    return kotlin.math.abs(config.highPassCutoffHz - this.config.highPassCutoffHz) < 0.5f &&
        kotlin.math.abs(config.bassGainDb - this.config.bassGainDb) < 0.05f &&
        kotlin.math.abs(config.boxinessCutDb - this.config.boxinessCutDb) < 0.05f &&
        kotlin.math.abs(config.vocalClarityDb - this.config.vocalClarityDb) < 0.05f &&
        kotlin.math.abs(config.trebleGainDb - this.config.trebleGainDb) < 0.05f &&
        kotlin.math.abs(config.loudnessGainDb - this.config.loudnessGainDb) < 0.05f
}

/**
 * Horizontal profile selector displaying scientifically backed profiles across
 * Measurement Corrections, Community OPRA tunings, Phone Speaker modes, and Creative Signatures.
 */
@Composable
fun ProfileSelector(
    currentConfig: DspConfig,
    onSelectProfile: (TuningProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(ProfileCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val visible = remember(selectedCategory, searchQuery) {
        TuningProfile.values().filter { prof ->
            if (prof == TuningProfile.BYPASS) return@filter false
            val categoryMatch = prof.matchesCategory(selectedCategory)
            if (!categoryMatch) return@filter false
            if (searchQuery.isBlank()) return@filter true

            val q = searchQuery.trim().lowercase()
            prof.title.lowercase().contains(q) ||
                prof.subtitle.lowercase().contains(q) ||
                prof.description.lowercase().contains(q) ||
                (prof.acousticProfile?.manufacturer?.lowercase()?.contains(q) == true) ||
                (prof.acousticProfile?.model?.lowercase()?.contains(q) == true) ||
                (prof.acousticProfile?.targetCurve?.lowercase()?.contains(q) == true)
        }
    }
    val active = visible.firstOrNull { it.matchesConfig(currentConfig) }
        ?: TuningProfile.values().firstOrNull { it != TuningProfile.BYPASS && it.matchesConfig(currentConfig) }
    val primary = MaterialTheme.colorScheme.primary
    val border = LocalExtendedColors.current.cardBorder
    val listState = rememberLazyListState()

    LaunchedEffect(active, selectedCategory) {
        val idx = visible.indexOf(active)
        if (idx >= 0) {
            listState.animateScrollToItem(idx)
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Search Input Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Search headphone model, manufacturer, target...",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = primary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            Icons.Rounded.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = primary,
                unfocusedBorderColor = border,
                cursorColor = primary
            )
        )

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProfileCategory.values().forEach { category ->
                val on = selectedCategory == category
                FilterChip(
                    selected = on,
                    onClick = { selectedCategory = category },
                    label = {
                        Text(
                            text = category.label,
                            fontSize = 12.5.sp,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    leadingIcon = if (on) {
                        {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = on,
                        borderColor = border,
                        selectedBorderColor = primary,
                        borderWidth = if (on) 1.5.dp else 1.dp
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // Profile Cards Carousel or Empty State
        if (visible.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, border),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Rounded.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "No acoustic profiles match \"$searchQuery\" in ${selectedCategory.label}.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(visible, key = { it.name }) { profile ->
                    ProfileCard(
                        profile = profile,
                        isSelected = profile == active,
                        onClick = { onSelectProfile(profile) }
                    )
                }
            }
        }

        // Active Profile Provenance and Technical Details Card
        active?.let { prof ->
            val ac = prof.acousticProfile
            val fitInfo = remember(prof, ac) {
                if (ac != null) {
                    ParametricBandFitter.fit(ac, prof.config.highPassCutoffHz)
                } else null
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, border),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {}
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Verified,
                                contentDescription = null,
                                tint = primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = ac?.category?.displayName ?: "Creative Signature",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primary
                            )
                        }
                        if (ac != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, border)
                            ) {
                                Text(
                                    text = "${ac.filters.size} Biquads · ${ac.license}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = prof.description,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (ac != null && ac.targetCurve.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Target: ${ac.targetCurve}${if (ac.measurementRig.isNotBlank()) " · Rig: ${ac.measurementRig}" else ""}",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (fitInfo != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Analytics,
                                contentDescription = null,
                                tint = primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "23-Band Pre-EQ Fit: ${fitInfo.quality.label} (RMS error: ${"%.2f".format(fitInfo.rmsErrorDb)} dB)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primary
                            )
                        }
                    }
                }
            }
        } ?: run {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, border),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Custom tuning active. Tap any profile card to apply verified equalization.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun iconFor(profile: TuningProfile): ImageVector = when (profile) {
    TuningProfile.SENNHEISER_HD600, TuningProfile.DIRAC_TIGHT_BASS -> Icons.Rounded.Headphones
    TuningProfile.SONY_XM4, TuningProfile.BOSE_QC35, TuningProfile.DOLBY_DYNAMIC_BASS -> Icons.Rounded.Headset
    TuningProfile.AIRPODS_PRO, TuningProfile.LIVE_INSTRUMENTAL -> Icons.Rounded.Hearing
    TuningProfile.BEYER_DT770 -> Icons.Rounded.SurroundSound
    TuningProfile.COMMUNITY_HD600_DIFFUSE -> Icons.Rounded.Equalizer
    TuningProfile.COMMUNITY_XM4_WARM -> Icons.Rounded.MusicNote
    TuningProfile.PHONE_SPEAKER, TuningProfile.ULTRA_BOOST -> Icons.AutoMirrored.Rounded.VolumeUp
    TuningProfile.PHONE_SPEAKER_VOICE, TuningProfile.VOCAL_PRO -> Icons.Rounded.RecordVoiceOver
    TuningProfile.CINEMATIC, TuningProfile.DOLBY_SPATIAL -> Icons.Rounded.Movie
    TuningProfile.DEEP_BASS, TuningProfile.MAXX_BASS, TuningProfile.SUB_BASS_DEEP -> Icons.Rounded.GraphicEq
    TuningProfile.STUDIO_REFERENCE, TuningProfile.DIRAC_HIFI -> Icons.Rounded.Speed
    TuningProfile.TREBLE_AIR, TuningProfile.DOLBY_SYMPHONY -> Icons.Rounded.Audiotrack
    TuningProfile.WARM_ANALOG, TuningProfile.WARM_ANALOG_BASS, TuningProfile.DIRAC_VIRTUO_ACOUSTIC -> Icons.Rounded.Album
    TuningProfile.BYPASS -> Icons.Rounded.PowerSettingsNew
}

@Composable
private fun ProfileCard(profile: TuningProfile, isSelected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val border = LocalExtendedColors.current.cardBorder
    val ac = profile.acousticProfile

    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.dp,
            color = if (isSelected) accent else border
        ),
        modifier = Modifier
            .width(168.dp)
            .heightIn(min = 126.dp)
            .semantics {
                role = Role.RadioButton
                selected = isSelected
                contentDescription = "${profile.title}, ${profile.subtitle}${if (isSelected) ", active profile" else ", tap to activate"}"
            }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (isSelected) accent.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    Icon(
                        iconFor(profile),
                        contentDescription = null,
                        tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "ACTIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                } else if (ac?.category == TuningCategory.MEASUREMENT_CORRECTION) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accent.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "CALIBRATED",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            letterSpacing = 0.4.sp
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = profile.title,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                Text(
                    text = profile.subtitle,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
