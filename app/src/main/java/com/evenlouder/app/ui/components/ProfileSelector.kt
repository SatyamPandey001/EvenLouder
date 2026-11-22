package com.evenlouder.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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

/**
 * Display layout mode for selecting profiles.
 */
enum class ProfileViewMode {
    CAROUSEL,
    GRID,
    LIST
}

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

private fun categoryShortLabel(category: ProfileCategory): String = when (category) {
    ProfileCategory.ALL -> "All"
    ProfileCategory.HEADPHONES -> "Headphones"
    ProfileCategory.COMMUNITY -> "Community"
    ProfileCategory.PHONE_SPEAKER -> "Speaker"
    ProfileCategory.CREATIVE -> "Creative"
    ProfileCategory.BASS -> "Bass"
    ProfileCategory.INSTRUMENTAL -> "Acoustic"
    ProfileCategory.DOLBY_DIRAC -> "Balanced"
}

private fun iconForCategory(category: ProfileCategory): ImageVector = when (category) {
    ProfileCategory.ALL -> Icons.Rounded.Dashboard
    ProfileCategory.HEADPHONES -> Icons.Rounded.Headphones
    ProfileCategory.COMMUNITY -> Icons.Rounded.Groups
    ProfileCategory.PHONE_SPEAKER -> Icons.AutoMirrored.Rounded.VolumeUp
    ProfileCategory.CREATIVE -> Icons.Rounded.AutoAwesome
    ProfileCategory.BASS -> Icons.Rounded.GraphicEq
    ProfileCategory.INSTRUMENTAL -> Icons.Rounded.MusicNote
    ProfileCategory.DOLBY_DIRAC -> Icons.Rounded.Speed
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

/**
 * Modern profile selector with tactile category pills, multi-view layout switching (Carousel, Grid, List),
 * real-time acoustic frequency sparklines, and verified calibration provenance.
 */
@Composable
fun ProfileSelector(
    currentConfig: DspConfig,
    onSelectProfile: (TuningProfile) -> Unit,
    modifier: Modifier = Modifier,
    initialViewMode: ProfileViewMode = ProfileViewMode.CAROUSEL
) {
    var selectedCategory by remember { mutableStateOf(ProfileCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var viewMode by remember { mutableStateOf(initialViewMode) }

    val canonicalProfiles = remember { TuningProfile.canonicalProfiles }

    // Precalculate counts for each category using only verified, calibrated profiles
    val categoryCounts = remember(canonicalProfiles) {
        ProfileCategory.entries.associateWith { cat ->
            canonicalProfiles.count { it.matchesCategory(cat) }
        }
    }

    val visible = remember(selectedCategory, searchQuery, canonicalProfiles) {
        canonicalProfiles.filter { prof ->
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
        ?: canonicalProfiles.firstOrNull { it.matchesConfig(currentConfig) }

    val primary = MaterialTheme.colorScheme.primary
    val border = LocalExtendedColors.current.cardBorder
    val listState = rememberLazyListState()

    LaunchedEffect(active, selectedCategory) {
        val idx = visible.indexOf(active)
        if (idx >= 0 && viewMode == ProfileViewMode.CAROUSEL) {
            listState.animateScrollToItem(idx)
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Modern Search Input Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Search headphone model, target, profile...",
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
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = primary,
                unfocusedBorderColor = border,
                cursorColor = primary
            )
        )

        // Refined Category Pills with Icons & Count Badges
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileCategory.entries.forEach { category ->
                val on = selectedCategory == category
                val count = categoryCounts[category] ?: 0

                CategoryPill(
                    label = categoryShortLabel(category),
                    icon = iconForCategory(category),
                    count = count,
                    isSelected = on,
                    onClick = { selectedCategory = category }
                )
            }
        }

        // Section Subheader: Category Title & Count + View Mode Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = selectedCategory.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, border)
                ) {
                    Text(
                        text = "${visible.size} ${if (visible.size == 1) "profile" else "profiles"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // View Mode Selector (Carousel, Grid, List)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, border)
            ) {
                Row(
                    modifier = Modifier.padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ViewModeIconButton(
                        icon = Icons.Rounded.ViewCarousel,
                        contentDescription = "Carousel View",
                        isSelected = viewMode == ProfileViewMode.CAROUSEL,
                        onClick = { viewMode = ProfileViewMode.CAROUSEL }
                    )
                    ViewModeIconButton(
                        icon = Icons.Rounded.GridView,
                        contentDescription = "Grid View",
                        isSelected = viewMode == ProfileViewMode.GRID,
                        onClick = { viewMode = ProfileViewMode.GRID }
                    )
                    ViewModeIconButton(
                        icon = Icons.AutoMirrored.Rounded.ViewList,
                        contentDescription = "List View",
                        isSelected = viewMode == ProfileViewMode.LIST,
                        onClick = { viewMode = ProfileViewMode.LIST }
                    )
                }
            }
        }

        // Profiles Presentation based on chosen view mode
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
            androidx.compose.animation.AnimatedContent(
                targetState = viewMode,
                label = "viewModeTransition"
            ) { mode ->
                when (mode) {
                    ProfileViewMode.CAROUSEL -> {
                        LazyRow(
                            state = listState,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(visible, key = { it.name }) { profile ->
                                ProfileCard(
                                    profile = profile,
                                    isSelected = profile == active,
                                    onClick = { onSelectProfile(profile) },
                                    modifier = Modifier.width(180.dp)
                                )
                            }
                        }
                    }
                    ProfileViewMode.GRID -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            visible.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    pair.forEach { profile ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            ProfileCard(
                                                profile = profile,
                                                isSelected = profile == active,
                                                onClick = { onSelectProfile(profile) },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                    ProfileViewMode.LIST -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            visible.forEach { profile ->
                                ProfileListItem(
                                    profile = profile,
                                    isSelected = profile == active,
                                    onClick = { onSelectProfile(profile) }
                                )
                            }
                        }
                    }
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

/**
 * Modern tactile category pill with icon, label, and profile count badge.
 */
@Composable
private fun CategoryPill(
    label: String,
    icon: ImageVector,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val border = LocalExtendedColors.current.cardBorder

    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        label = "pillBg"
    )
    val animatedBorder by animateColorAsState(
        targetValue = if (isSelected) primary else border,
        label = "pillBorder"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = animatedBg,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = animatedBorder
        ),
        modifier = Modifier.semantics {
            role = Role.Tab
            selected = isSelected
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = CircleShape,
                color = if (isSelected) Color.Black.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(start = 2.dp)
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Icon button for switching profile layout views.
 */
@Composable
private fun ViewModeIconButton(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(32.dp)
            .background(
                color = if (isSelected) primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Rich profile card featuring tone characteristics, acoustic curve sparkline, and verification badge.
 */
@Composable
private fun ProfileCard(
    profile: TuningProfile,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val border = LocalExtendedColors.current.cardBorder
    val ac = profile.acousticProfile

    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accent else border
        ),
        modifier = modifier
            .heightIn(min = 148.dp)
            .semantics {
                role = Role.RadioButton
                selected = isSelected
                contentDescription = "${profile.title}, ${profile.subtitle}${if (isSelected) ", active profile" else ", tap to activate"}"
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Top Row: Icon + Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                color = if (isSelected) accent.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        Icon(
                            iconFor(profile),
                            contentDescription = null,
                            tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accent,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(11.dp))
                                Text(
                                    text = "ACTIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    } else {
                        val badgeText = when {
                            ac?.category == TuningCategory.MEASUREMENT_CORRECTION -> "CALIBRATED"
                            ac?.category == TuningCategory.COMMUNITY_PROFILE -> "OPRA"
                            profile.matchesCategory(ProfileCategory.PHONE_SPEAKER) -> "SPEAKER"
                            else -> "TUNING"
                        }
                        val badgeColor = when {
                            ac?.category == TuningCategory.MEASUREMENT_CORRECTION -> accent
                            ac?.category == TuningCategory.COMMUNITY_PROFILE -> LocalExtendedColors.current.accentAmber
                            profile.matchesCategory(ProfileCategory.PHONE_SPEAKER) -> LocalExtendedColors.current.signalGreen
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeColor.copy(alpha = 0.12f),
                            border = BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                letterSpacing = 0.4.sp
                            )
                        }
                    }
                }

                // Title & Subtitle
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = profile.title,
                        fontSize = 13.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )
                    Text(
                        text = profile.subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Mini Frequency Sparkline Curve & Tag
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ProfileMiniCurve(
                    profile = profile,
                    isSelected = isSelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                )

                val tagText = ac?.targetCurve?.takeIf { it.isNotBlank() }
                    ?: if (profile.config.highPassCutoffHz > 100f) "${profile.config.highPassCutoffHz.toInt()} Hz HPF"
                    else "Deterministic EQ"

                Text(
                    text = tagText,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) accent.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Compact profile list item row for the LIST view mode.
 */
@Composable
private fun ProfileListItem(
    profile: TuningProfile,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val border = LocalExtendedColors.current.cardBorder
    val ac = profile.acousticProfile

    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accent else border
        ),
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.RadioButton
                selected = isSelected
                contentDescription = "${profile.title}, ${profile.subtitle}${if (isSelected) ", active profile" else ", tap to activate"}"
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
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

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = profile.title,
                        fontSize = 13.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (ac?.category == TuningCategory.MEASUREMENT_CORRECTION) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = accent.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "CALIBRATED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = accent,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = profile.subtitle,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right side: Mini Curve sparkline
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(24.dp)
            ) {
                ProfileMiniCurve(
                    profile = profile,
                    isSelected = isSelected,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Selection indicator
            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = accent,
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "Active",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.size(22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(1.2.dp, border),
                        modifier = Modifier.size(16.dp)
                    ) {}
                }
            }
        }
    }
}

/**
 * Lightweight canvas-rendered frequency response sparkline curve.
 */
@Composable
private fun ProfileMiniCurve(
    profile: TuningProfile,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val strokeColor = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    val fillColor = if (isSelected) accent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)

    val points = remember(profile) {
        val freqs = floatArrayOf(40f, 100f, 300f, 1000f, 3000f, 7000f, 14000f)
        val dbs = FloatArray(freqs.size)
        val ac = profile.acousticProfile
        if (ac != null) {
            for (i in freqs.indices) {
                dbs[i] = ac.responseDbAt(freqs[i])
            }
        } else {
            val c = profile.config
            val hpf = c.highPassCutoffHz
            dbs[0] = if (hpf > 100f) -9f else c.bassGainDb * 0.7f
            dbs[1] = if (hpf > 130f) -4f else c.bassGainDb
            dbs[2] = c.bassGainDb * 0.4f
            dbs[3] = c.boxinessCutDb
            dbs[4] = c.vocalClarityDb
            dbs[5] = c.trebleGainDb
            dbs[6] = c.trebleGainDb * 0.8f
        }
        FloatArray(freqs.size) { i ->
            val clamped = dbs[i].coerceIn(-9f, 9f)
            1f - ((clamped + 9f) / 18f)
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // Dashed center zero dB line
        drawLine(
            color = muted,
            start = Offset(0f, h * 0.5f),
            end = Offset(w, h * 0.5f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
        )

        val path = Path()
        val fillPath = Path()

        val stepX = w / (points.size - 1)
        val startY = points[0] * h
        path.moveTo(0f, startY)
        fillPath.moveTo(0f, h)
        fillPath.lineTo(0f, startY)

        for (i in 0 until points.size - 1) {
            val x0 = i * stepX
            val y0 = points[i] * h
            val x1 = (i + 1) * stepX
            val y1 = points[i + 1] * h
            val cx = (x0 + x1) / 2f

            path.cubicTo(cx, y0, cx, y1, x1, y1)
            fillPath.cubicTo(cx, y0, cx, y1, x1, y1)
        }

        fillPath.lineTo(w, h)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor, Color.Transparent),
                startY = 0f,
                endY = h
            )
        )

        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(
                width = 1.8.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
fun ProfileSelectorPreview() {
    com.evenlouder.app.ui.theme.EvenLouderTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.background) {
            ProfileSelector(
                currentConfig = DspConfig(activeProfileId = "sennheiser_hd600_harman"),
                onSelectProfile = {}
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
fun ProfileSelectorGridPreview() {
    com.evenlouder.app.ui.theme.EvenLouderTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.background) {
            ProfileSelector(
                currentConfig = DspConfig(activeProfileId = "sony_wh1000xm4_harman"),
                onSelectProfile = {},
                initialViewMode = ProfileViewMode.GRID
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
fun ProfileSelectorListPreview() {
    com.evenlouder.app.ui.theme.EvenLouderTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.background) {
            ProfileSelector(
                currentConfig = DspConfig(activeProfileId = "apple_airpods_pro_harman"),
                onSelectProfile = {},
                initialViewMode = ProfileViewMode.LIST
            )
        }
    }
}



