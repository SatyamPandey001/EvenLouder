package com.evenlouder.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evenlouder.app.core.dsp.DspConfig
import com.evenlouder.app.core.dsp.TuningProfile
import com.evenlouder.app.ui.components.*
import com.evenlouder.app.ui.theme.LocalExtendedColors

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
    val scrollState = rememberScrollState()
    val extendedColors = LocalExtendedColors.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Main Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 1. App Bar Header: Perfectly balanced Brand Identity + Action Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EvenLouderLoader(
                        size = 40.dp,
                        variant = LoaderVariant.StaticIcon,
                        contentDescription = "EvenLouder Logo"
                    )
                    Column {
                        Text(
                            text = "EVENLOUDER",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            color = primaryColor
                        )
                        Text(
                            text = "Android 15+ Micro-Speaker Engine",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Theme Toggle Action Button (Clean, unified 42x42dp squircle)
                Surface(
                    onClick = onToggleTheme,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, extendedColors.cardBorder),
                    modifier = Modifier
                        .size(42.dp)
                        .semantics {
                            contentDescription = if (isDarkTheme) "Switch to light theme" else "Switch to dark theme"
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Master DSP Bypass / Enable Switch Card (with integrated AIDL spec badge)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (config.enabled) MaterialTheme.colorScheme.surface else Color(0xFF1E1515)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (config.enabled) extendedColors.cardBorder else Color(0x66FF5252)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (config.enabled) extendedColors.signalGreen else Color(0xFFFF5252))
                            )
                            Text(
                                text = if (config.enabled) "DSP ENGINE ACTIVE" else "HARDWARE BYPASS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (config.enabled) primaryColor else Color(0xFFFF5252)
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "AIDL · 9 BANDS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = secondaryColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (config.enabled) "AIDL DynamicsProcessing active on micro-speaker" else "Stock phone audio without acoustic correction",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = config.enabled,
                        onCheckedChange = { onConfigChange(config.copy(enabled = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = primaryColor,
                            checkedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = if (config.enabled) "DSP Engine enabled" else "DSP Engine disabled"
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Real-Time Hardware Acoustic Response Visualizer
            DspCurveVisualizer(
                config = config,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Acoustic Presets Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACOUSTIC PROFILES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "HARDWARE-AWARE TUNING",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            ProfileSelector(
                currentConfig = config,
                onSelectProfile = { profile ->
                    onConfigChange(profile.config.copy(globalHookEnabled = config.globalHookEnabled))
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 5. Fine Tuning Controls Section
            Text(
                text = "SPEAKER TUNING CALIBRATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Loudness Makeup Gain
                TactileSliderCard(
                    title = "Loudness Boost (MBC)",
                    subtitle = "Multi-band makeup gain lifting quiet details",
                    value = config.loudnessGainDb,
                    valueRange = 0f..6f,
                    defaultValue = 0f,
                    accentColor = primaryColor,
                    onValueChange = { onConfigChange(config.copy(loudnessGainDb = it)) }
                )

                // Vocal Presence
                TactileSliderCard(
                    title = "Vocal Presence (2.8 kHz)",
                    subtitle = "Fletcher-Munson ear sensitivity compensation",
                    value = config.vocalClarityDb,
                    valueRange = 0f..6f,
                    defaultValue = 0f,
                    accentColor = primaryColor,
                    onValueChange = { onConfigChange(config.copy(vocalClarityDb = it)) }
                )

                // Harmonic Bass Punch
                TactileSliderCard(
                    title = "Harmonic Bass Punch (280 Hz)",
                    subtitle = "Psychoacoustic 2nd/3rd missing fundamental harmonics",
                    value = config.bassGainDb,
                    valueRange = 0f..6f,
                    defaultValue = 0f,
                    accentColor = secondaryColor,
                    onValueChange = { onConfigChange(config.copy(bassGainDb = it)) }
                )

                // Chassis De-Boxing
                TactileSliderCard(
                    title = "Chassis De-Boxing (950 Hz)",
                    subtitle = "Removes hollow plastic/aluminum cabinet resonance",
                    value = config.boxinessCutDb,
                    valueRange = -6f..0f,
                    defaultValue = 0f,
                    accentColor = primaryColor,
                    onValueChange = { onConfigChange(config.copy(boxinessCutDb = it)) }
                )

                // Treble Crispness
                TactileSliderCard(
                    title = "Treble Crispness (7.5 kHz)",
                    subtitle = "Air band compensating for micro-speaker roll-off",
                    value = config.trebleGainDb,
                    valueRange = 0f..6f,
                    defaultValue = 0f,
                    accentColor = primaryColor,
                    onValueChange = { onConfigChange(config.copy(trebleGainDb = it)) }
                )

                // Sub-Bass Excursion Roll-off Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(extendedColors.cardBorder)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Sub-Bass Excursion Cutoff",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Drops sub-audible frequencies to save amp power",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = secondaryColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "${config.highPassCutoffHz.toInt()} Hz",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = secondaryColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                120f to "Large Phone",
                                140f to "Standard",
                                160f to "Small / Compact"
                            ).forEach { (hz, desc) ->
                                val selected = config.highPassCutoffHz == hz
                                FilterChip(
                                    selected = selected,
                                    onClick = { onConfigChange(config.copy(highPassCutoffHz = hz)) },
                                    label = {
                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Text(
                                                text = "${hz.toInt()} Hz",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = desc,
                                                fontSize = 9.sp,
                                                color = if (selected) Color.Black.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = secondaryColor,
                                        selectedLabelColor = Color.Black
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 6. External Player Hook Switch Card
            var hasCopiedAdb by remember { mutableStateOf(false) }
            val clipboardManager = LocalClipboardManager.current
            val adbCommand = "adb shell pm grant com.evenlouder.app android.permission.DUMP"

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(extendedColors.cardBorder)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "HOOK EXTERNAL PLAYERS",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (config.globalHookEnabled) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isDumpPermissionGranted) extendedColors.signalGreen.copy(alpha = 0.15f) else secondaryColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (isDumpPermissionGranted) "ENHANCED ADB" else "BROADCAST",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isDumpPermissionGranted) extendedColors.signalGreen else secondaryColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isDumpPermissionGranted) {
                                    "Auto-detects YouTube, Browsers, Spotify, SimpMusic via system stream dumps."
                                } else {
                                    "Captures standard broadcast players (Spotify, Poweramp, VLC)."
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = config.globalHookEnabled,
                            onCheckedChange = { onConfigChange(config.copy(globalHookEnabled = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = secondaryColor,
                                checkedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.semantics {
                                contentDescription = "Hook external audio players"
                            }
                        )
                    }

                    if (config.globalHookEnabled && !isDumpPermissionGranted) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, extendedColors.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Tip for YouTube (Browsers), SimpMusic & Media Players:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Non-broadcasting players require one-time ADB permission to read active audio streams. Standard players (Spotify) work without it.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, extendedColors.cardBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(adbCommand))
                                            hasCopiedAdb = true
                                        }
                                        .padding(vertical = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = adbCommand,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = secondaryColor,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = if (hasCopiedAdb) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                            contentDescription = if (hasCopiedAdb) "Copied" else "Copy ADB command",
                                            tint = if (hasCopiedAdb) extendedColors.signalGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7. Attached Audio Sessions List Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(extendedColors.cardBorder)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ATTACHED AUDIO SESSIONS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (activeSessions.isNotEmpty()) extendedColors.signalGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${activeSessions.size} ATTACHED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (activeSessions.isNotEmpty()) extendedColors.signalGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (activeSessions.isEmpty()) {
                        Text(
                            text = "No media streams currently active. Play music in any app or tap 'TEST A/B' in the bottom dock.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        activeSessions.forEach { (id, client) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(extendedColors.signalGreen)
                                    )
                                    Text(
                                        text = client,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "Session #$id",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = primaryColor
                                )
                            }
                        }
                    }
                }
            }

            // Bottom padding to clear sticky dock
            Spacer(modifier = Modifier.height(96.dp))
        }

        // 8. Sticky Bottom Sessions Dock
        StickySessionDock(
            activeSessions = activeSessions,
            isTestAudioPlaying = isTestAudioPlaying,
            onToggleTestAudio = onTestAudioToggle,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}
