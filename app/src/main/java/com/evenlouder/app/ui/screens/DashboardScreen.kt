package com.evenlouder.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evenlouder.app.core.dsp.DspConfig
import com.evenlouder.app.core.dsp.TuningProfile
import com.evenlouder.app.core.dsp.applyingProfile
import com.evenlouder.app.core.session.EvenLouderMediaNotificationListener
import com.evenlouder.app.tools.ShizukuHelper
import com.evenlouder.app.ui.components.DspCurveVisualizer
import com.evenlouder.app.ui.components.EvenLouderLoader
import com.evenlouder.app.ui.components.LoaderVariant
import com.evenlouder.app.ui.components.ProfileSelector
import com.evenlouder.app.ui.components.TactileSliderCard
import com.evenlouder.app.ui.components.VolumeBoostCard
import com.evenlouder.app.ui.components.matchesConfig
import com.evenlouder.app.ui.theme.LocalExtendedColors
import com.evenlouder.app.ui.theme.TactileTokens
import com.evenlouder.app.ui.theme.tactileClickable
import com.evenlouder.app.ui.theme.tactileCta
import com.evenlouder.app.ui.theme.tactileInset
import com.evenlouder.app.ui.theme.tactileRaised

/**
 * Modern soft tactile dashboard with subtle skeuomorphic depth,
 * sculpted hardware controls, dual shadows, and physical press feedback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    config: DspConfig,
    activeSessions: List<Pair<Int, String>>,
    isTestAudioPlaying: Boolean,
    isDarkTheme: Boolean = true,
    isDumpPermissionGranted: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onConfigChange: (DspConfig) -> Unit,
    onTestAudioToggle: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current
    val isDark = extendedColors.isDark
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val latestConfig by rememberUpdatedState(config)

    var showSettings by remember { mutableStateOf(false) }
    var showFineTune by remember { mutableStateOf(false) }
    var comparing by remember { mutableStateOf(false) }

    val activeProfile = TuningProfile.canonicalProfiles.firstOrNull { it.matchesConfig(config) }
    val headline = when {
        config.enabled && !config.eqEnabled && config.volumeBoostDb > 0.05f -> "Volume boost"
        else -> activeProfile?.title
            ?: config.activeProfileId?.let { com.evenlouder.app.core.dsp.AcousticProfileRepository.findById(it)?.name }
            ?: "Custom"
    }
    val onTune: (DspConfig) -> Unit = { onConfigChange(it.copy(eqEnabled = true)) }
    val statusLine = when {
        isTestAudioPlaying -> "Playing test sound"
        !config.enabled -> "Original phone sound"
        activeSessions.isNotEmpty() ->
            "Tuning ${activeSessions.size} stream${if (activeSessions.size == 1) "" else "s"} · ${activeSessions.first().second}"
        else -> "Waiting for audio. Play something."
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .tactileRaised(shapeRadius = 12.dp, elevation = 2.dp, isDarkTheme = isDark),
                        contentAlignment = Alignment.Center
                    ) {
                        EvenLouderLoader(
                            size = 28.dp,
                            variant = LoaderVariant.StaticIcon,
                            contentDescription = "EvenLouder logo"
                        )
                    }
                    Text(
                        "EvenLouder",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .tactileClickable(shapeRadius = 12.dp, isDarkTheme = isDark, elevation = 2.dp) {
                            showSettings = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Hero: status + tactile power button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = if (config.enabled) "Tuning on" else "Bypassed",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (config.enabled) primary else muted
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = headline,
                        fontSize = 28.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = statusLine,
                        fontSize = 13.5.sp,
                        color = muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Physical Tactile Power Button
                TactilePowerButton(
                    enabled = config.enabled,
                    isDarkTheme = isDark,
                    onClick = { onConfigChange(config.copy(enabled = !config.enabled)) }
                )
            }

            Spacer(Modifier.height(18.dp))
            DspCurveVisualizer(config = config, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))

            // Action Buttons: Hold to Compare & Test Sound
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TactileCompareButton(
                    available = config.enabled || comparing,
                    isDarkTheme = isDark,
                    onHold = { holding ->
                        comparing = holding
                        onConfigChange(latestConfig.copy(enabled = !holding))
                    },
                    modifier = Modifier.weight(1f)
                )

                TactileTestSoundButton(
                    isPlaying = isTestAudioPlaying,
                    isDarkTheme = isDark,
                    onClick = onTestAudioToggle,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(28.dp))

            // Profiles Section
            Text("Profiles", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            VolumeBoostCard(config = config, onConfigChange = onConfigChange)
            Spacer(Modifier.height(20.dp))
            ProfileSelector(
                currentConfig = config,
                onSelectProfile = { profile ->
                    onConfigChange(config.applyingProfile(profile))
                }
            )

            Spacer(Modifier.height(24.dp))

            // Fine tune (collapsed by default)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .tactileRaised(shapeRadius = 22.dp, elevation = 5.dp, isDarkTheme = isDark)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFineTune = !showFineTune }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Fine tune", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Adjust each band yourself", fontSize = 12.sp, color = muted)
                        }
                        Icon(
                            imageVector = if (showFineTune) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (showFineTune) "Collapse fine tune" else "Expand fine tune",
                            tint = muted
                        )
                    }

                    AnimatedVisibility(visible = showFineTune) {
                        Column(modifier = Modifier.padding(top = 4.dp)) {
                            TactileSliderCard("Loudness", "Compressor makeup gain", config.loudnessGainDb, 0f..6f,
                                onValueChange = { onTune(config.copy(loudnessGainDb = it)) })
                            TactileSliderCard("Voice clarity", "Boosts 2-4 kHz", config.vocalClarityDb, -6f..10f,
                                onValueChange = { onTune(config.copy(vocalClarityDb = it)) })
                            TactileSliderCard("Bass punch", "Boosts ~300 Hz upper bass", config.bassGainDb, 0f..10f,
                                onValueChange = { onTune(config.copy(bassGainDb = it)) })
                            TactileSliderCard("Less boxy", "Cuts ~950 Hz", config.boxinessCutDb, -10f..0f,
                                onValueChange = { onTune(config.copy(boxinessCutDb = it)) })
                            TactileSliderCard("Treble detail", "Shelf from ~5.5 kHz up", config.trebleGainDb, -6f..10f,
                                onValueChange = { onTune(config.copy(trebleGainDb = it)) })

                            Spacer(Modifier.height(10.dp))
                            Text("Speaker size", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Cuts deep lows the speaker cannot reproduce", fontSize = 12.sp, color = muted)
                            Spacer(Modifier.height(10.dp))

                            // Tactile Speaker Size Rocker Pills
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tactileInset(shapeRadius = 12.dp, isDarkTheme = isDark, depth = 2.dp)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(120f to "Large", 140f to "Standard", 160f to "Compact").forEach { (hz, label) ->
                                    val isSelected = kotlin.math.abs(config.highPassCutoffHz - hz) < 0.5f
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(9.dp))
                                            .then(
                                                if (isSelected) {
                                                    Modifier.tactileRaised(shapeRadius = 9.dp, elevation = 2.dp, isDarkTheme = isDark)
                                                } else Modifier
                                            )
                                            .clickable { onTune(config.copy(highPassCutoffHz = hz)) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "$label · ${hz.toInt()} Hz",
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) primary else muted
                                        )
                                    }
                                }
                            }

                            TextButton(
                                onClick = {
                                    onConfigChange(config.applyingProfile(TuningProfile.STUDIO_REFERENCE))
                                },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Reset to Balanced", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Profiles are equalizer and compressor presets, not measurements of your phone. Trust your ears.",
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = muted
            )
            Spacer(Modifier.height(32.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (showSettings) {
        SettingsSheet(
            config = config,
            activeSessions = activeSessions,
            isDarkTheme = isDarkTheme,
            isDumpPermissionGranted = isDumpPermissionGranted,
            onToggleTheme = onToggleTheme,
            onConfigChange = onConfigChange,
            onDismiss = { showSettings = false }
        )
    }
}

/**
 * Tactile circular power button with rich glossy gradient CTA when active,
 * soft extruded tactile neutral when bypassed, and smooth physical press feedback.
 */
@Composable
private fun TactilePowerButton(
    enabled: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "powerBtnScale"
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (enabled) {
                    Modifier.tactileCta(
                        gradient = extendedColors.primaryCtaGradient,
                        glowColor = primary,
                        shapeRadius = 32.dp,
                        elevation = 6.dp,
                        isPressed = isPressed
                    )
                } else {
                    Modifier.tactileRaised(
                        shapeRadius = 32.dp,
                        elevation = 4.dp,
                        isDarkTheme = isDarkTheme,
                        pressed = isPressed
                    )
                }
            )
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics {
                contentDescription = if (enabled) "Turn tuning off" else "Turn tuning on"
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.PowerSettingsNew,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onPrimary else muted,
            modifier = Modifier.size(30.dp)
        )
    }
}

/**
 * Press and hold to hear the original sound; release to return to the tuned sound.
 * Physically depresses into a recessed inset state when held.
 */
@Composable
private fun TactileCompareButton(
    available: Boolean,
    isDarkTheme: Boolean,
    onHold: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var held by remember { mutableStateOf(false) }
    val availableNow by rememberUpdatedState(available)
    val onHoldNow by rememberUpdatedState(onHold)
    val primary = MaterialTheme.colorScheme.primary

    val scale by animateFloatAsState(
        targetValue = if (held) 0.97f else 1.0f,
        animationSpec = tween(durationMillis = 80),
        label = "compareScale"
    )

    Box(
        modifier = modifier
            .height(52.dp)
            .alpha(if (available) 1f else 0.4f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (held) {
                    Modifier.tactileInset(shapeRadius = 16.dp, isDarkTheme = isDarkTheme, depth = 3.dp)
                } else {
                    Modifier.tactileRaised(shapeRadius = 16.dp, elevation = 3.dp, isDarkTheme = isDarkTheme)
                }
            )
            .clip(RoundedCornerShape(16.dp))
            .semantics { contentDescription = "Hold to hear the original, unprocessed sound" }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    if (availableNow) {
                        held = true
                        onHoldNow(true)
                        try {
                            tryAwaitRelease()
                        } finally {
                            held = false
                            onHoldNow(false)
                        }
                    }
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.CompareArrows,
                contentDescription = null,
                tint = if (held) primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (held) "Original" else "Hold to compare",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (held) primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Tactile button for toggling acoustic test playback.
 */
@Composable
private fun TactileTestSoundButton(
    isPlaying: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val secondary = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier
            .height(52.dp)
            .tactileClickable(
                shapeRadius = 16.dp,
                isDarkTheme = isDarkTheme,
                elevation = 3.dp,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = if (isPlaying) secondary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (isPlaying) "Stop test" else "Test sound",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isPlaying) secondary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    config: DspConfig,
    activeSessions: List<Pair<Int, String>>,
    isDarkTheme: Boolean,
    isDumpPermissionGranted: Boolean,
    onToggleTheme: () -> Unit,
    onConfigChange: (DspConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val green = LocalExtendedColors.current.signalGreen
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val adbCommand = "adb shell pm grant com.evenlouder.app android.permission.DUMP"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))

            SettingRow("Dark theme", "Switch between dark and light", isDarkTheme) { onToggleTheme() }
            Divider1(isDarkTheme)
            SettingRow(
                "Tune other apps",
                if (isDumpPermissionGranted) "Enhanced detection is on: browsers, video and music apps."
                else "Works with players that announce their audio session (Spotify, Poweramp, VLC).",
                config.globalHookEnabled
            ) { onConfigChange(config.copy(globalHookEnabled = it)) }

            if (config.globalHookEnabled && !isDumpPermissionGranted) {
                val context = LocalContext.current
                val isShizukuAvailable by remember { mutableStateOf(ShizukuHelper.isShizukuAvailable()) }
                var hasShizukuPerm by remember { mutableStateOf(ShizukuHelper.hasShizukuPermission()) }
                val isNotifListenerGranted by remember { mutableStateOf(EvenLouderMediaNotificationListener.isGranted(context)) }

                LaunchedEffect(Unit) {
                    ShizukuHelper.registerPermissionListener {
                        hasShizukuPerm = true
                        ShizukuHelper.grantDumpPermissionViaShizuku(context)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .tactileRaised(shapeRadius = 16.dp, elevation = 3.dp, isDarkTheme = isDarkTheme)
                        .padding(16.dp)
                ) {
                    Column {
                        Text("Want browsers & video apps too?", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Grant permission directly on your phone without a PC using any of these methods:",
                            fontSize = 12.sp, color = muted, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                        )

                        // Method 1: Shizuku
                        Text("1. Shizuku (1-Tap Grant on Phone)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            if (isShizukuAvailable) "Shizuku is active! Tap below to grant permission instantly."
                            else "Shizuku is not running. Start Shizuku via Wireless Debugging on your phone.",
                            fontSize = 11.sp, color = muted, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                        )
                        if (isShizukuAvailable) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .tactileClickable(shapeRadius = 10.dp, isDarkTheme = isDarkTheme, elevation = 2.dp) {
                                        if (hasShizukuPerm) {
                                            ShizukuHelper.grantDumpPermissionViaShizuku(context)
                                        } else {
                                            ShizukuHelper.requestShizukuPermission()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (hasShizukuPerm) "Grant Permission Now" else "Authorize via Shizuku",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        // Method 2: Wireless Debugging / LADB / Termux Shell
                        Text("2. Wireless Debugging / LADB / Termux (No PC)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "Tap either command to copy for your shell app:",
                            fontSize = 11.sp, color = muted, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                        )

                        val shellCommand = "pm grant com.evenlouder.app android.permission.DUMP"
                        var copiedShell by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tactileInset(shapeRadius = 10.dp, isDarkTheme = isDarkTheme, depth = 2.dp)
                                .clickable {
                                    clipboard.setText(AnnotatedString(shellCommand))
                                    copiedShell = true
                                    copied = false
                                }
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("For LADB / Termux / Device Shell:", fontSize = 10.sp, color = muted)
                                    Text(
                                        shellCommand, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    if (copiedShell) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                    contentDescription = if (copiedShell) "Copied" else "Copy shell command",
                                    tint = if (copiedShell) green else muted, modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tactileInset(shapeRadius = 10.dp, isDarkTheme = isDarkTheme, depth = 2.dp)
                                .clickable {
                                    clipboard.setText(AnnotatedString(adbCommand))
                                    copied = true
                                    copiedShell = false
                                }
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("For PC ADB Terminal:", fontSize = 10.sp, color = muted)
                                    Text(
                                        adbCommand, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                    contentDescription = if (copied) "Copied" else "Copy ADB command",
                                    tint = if (copied) green else muted, modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))

                        // Method 3: Notification Listener Access (Standard System Settings)
                        Text("3. Notification Access (Standard Settings)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "Grant Notification Listener access in Android settings to detect active playback.",
                            fontSize = 11.sp, color = muted, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .tactileClickable(shapeRadius = 10.dp, isDarkTheme = isDarkTheme, elevation = 2.dp) {
                                    EvenLouderMediaNotificationListener.openSettings(context)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (isNotifListenerGranted) "Notification Access Granted ✓" else "Open System Settings",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isNotifListenerGranted) green else MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(10.dp))

                        // Method 4: Classic PC ADB
                        Text("4. Classic USB Debugging (PC)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "Connect to a PC with USB debugging on and run the command copied above in terminal.",
                            fontSize = 11.sp, color = muted, modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Divider1(isDarkTheme)
            Text("Active streams", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (activeSessions.isEmpty()) {
                Text("None right now. Play audio in any app, or use Test sound.", fontSize = 13.sp, color = muted)
            } else {
                activeSessions.forEach { (id, client) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .tactileInset(shapeRadius = 8.dp, isDarkTheme = isDarkTheme, depth = 1.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(client, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text("#$id", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChecked, modifier = Modifier.semantics { contentDescription = title })
    }
}

@Composable
private fun Divider1(isDarkTheme: Boolean) {
    Spacer(Modifier.height(8.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(if (isDarkTheme) Color(0x22FFFFFF) else Color(0x1F718096))
    )
    Spacer(Modifier.height(8.dp))
}
