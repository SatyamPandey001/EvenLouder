package com.evenlouder.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evenlouder.app.core.dsp.DspConfig
import com.evenlouder.app.core.dsp.TuningProfile
import com.evenlouder.app.ui.theme.NeonAmber
import com.evenlouder.app.ui.theme.NeonCyan

@Composable
fun DashboardScreen(
    config: DspConfig,
    activeSessions: List<Pair<Int, String>>,
    isTestAudioPlaying: Boolean,
    onConfigChange: (DspConfig) -> Unit,
    onTestAudioToggle: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "EVEN LOUDER",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = NeonCyan
                )
                Text(
                    text = "Android 15+ Micro-Speaker Engine",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "AIDL · 9 BANDS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = NeonAmber,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Master DSP Bypass / Enable Switch Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (config.enabled) MaterialTheme.colorScheme.surface else Color(0xFF1E1515)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (config.enabled) "DSP ENGINE ACTIVE" else "HARDWARE BYPASS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (config.enabled) NeonCyan else Color(0xFFFF5252)
                    )
                    Text(
                        text = if (config.enabled) "Brickwall limiter locked @ -0.5 dBFS" else "Pure stock phone audio",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = config.enabled,
                    onCheckedChange = { onConfigChange(config.copy(enabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonCyan,
                        checkedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Expanded Preset Tuning Profiles
        Text(
            text = "ACOUSTIC PROFILES (DISTINCT TUNINGS)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 2x3 Grid of Profiles
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val profiles = listOf(
                TuningProfile.ULTRA_LOUD,
                TuningProfile.VOCAL_FOCUS,
                TuningProfile.BASS_PUNCH,
                TuningProfile.CRISP_TREBLE,
                TuningProfile.BALANCED_STUDIO
            )

            profiles.chunked(2).forEach { rowProfiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowProfiles.forEach { profile ->
                        val selected = config.enabled &&
                                config.highPassCutoffHz == profile.config.highPassCutoffHz &&
                                config.vocalClarityDb == profile.config.vocalClarityDb &&
                                config.bassGainDb == profile.config.bassGainDb &&
                                config.loudnessGainDb == profile.config.loudnessGainDb

                        OutlinedCard(
                            onClick = {
                                onConfigChange(profile.config.copy(globalHookEnabled = config.globalHookEnabled))
                            },
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (selected) NeonCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(if (selected) NeonCyan else Color.Transparent)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    profile.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) NeonCyan else Color.White
                                )
                                Text(
                                    profile.subtitle,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (rowProfiles.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fine Tuning Controls Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SPEAKER TUNING CALIBRATION",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Loudness Makeup Gain (MBC)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Loudness Boost (MBC)", fontSize = 13.sp)
                    Text(
                        "+${"%.1f".format(config.loudnessGainDb)} dB",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan
                    )
                }
                Slider(
                    value = config.loudnessGainDb,
                    onValueChange = { onConfigChange(config.copy(loudnessGainDb = it)) },
                    valueRange = 0f..6f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Vocal & Dialogue Clarity (2.8 kHz)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Vocal Presence (2.8 kHz)", fontSize = 13.sp)
                    Text(
                        "+${"%.1f".format(config.vocalClarityDb)} dB",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan
                    )
                }
                Slider(
                    value = config.vocalClarityDb,
                    onValueChange = { onConfigChange(config.copy(vocalClarityDb = it)) },
                    valueRange = 0f..6f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Upper Bass Punch (350 Hz)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Bass Punch / Warmth (350 Hz)", fontSize = 13.sp)
                    Text(
                        "+${"%.1f".format(config.bassGainDb)} dB",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = NeonAmber
                    )
                }
                Slider(
                    value = config.bassGainDb,
                    onValueChange = { onConfigChange(config.copy(bassGainDb = it)) },
                    valueRange = 0f..6f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonAmber,
                        activeTrackColor = NeonAmber
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Treble Sparkle (7.5 kHz)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Treble Crispness (7.5 kHz)", fontSize = 13.sp)
                    Text(
                        "+${"%.1f".format(config.trebleGainDb)} dB",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan
                    )
                }
                Slider(
                    value = config.trebleGainDb,
                    onValueChange = { onConfigChange(config.copy(trebleGainDb = it)) },
                    valueRange = 0f..6f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sub-Bass Excursion Roll-off
                Text("Sub-Bass Excursion Cutoff", fontSize = 13.sp)
                Text(
                    "Cuts non-reproducible sub-bass to free speaker power & stop cone distortion",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(120f, 140f, 160f).forEach { hz ->
                        FilterChip(
                            selected = config.highPassCutoffHz == hz,
                            onClick = { onConfigChange(config.copy(highPassCutoffHz = hz)) },
                            label = { Text("${hz.toInt()} Hz", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonAmber,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Built-In Audio Test Player Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "INTERNAL ACOUSTIC AUDIT AUDIO",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    color = NeonAmber
                )
                Text(
                    text = "Plays bundled acoustic master loop on the device speaker. Toggle 'DSP Engine Active' above to hear the instant A/B difference.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onTestAudioToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTestAudioPlaying) Color(0xFFFF5252) else NeonAmber,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isTestAudioPlaying) "STOP TEST AUDIO" else "PLAY TEST AUDIO ON SPEAKER",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // External Player Hooking Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HOOK EXTERNAL PLAYERS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Text(
                        text = "Applies tuning to SimpMusic, Spotify, YouTube Music, etc. in real time.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = config.globalHookEnabled,
                    onCheckedChange = { onConfigChange(config.copy(globalHookEnabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonAmber,
                        checkedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Sessions List Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ATTACHED AUDIO SESSIONS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (activeSessions.isEmpty()) {
                    Text(
                        text = "No media streams currently active. Play music in any app or tap 'Play Test Audio' above.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    activeSessions.forEach { (id, client) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(client, fontSize = 12.sp, color = Color.White)
                            Text(
                                "Session #$id",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NeonCyan
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
