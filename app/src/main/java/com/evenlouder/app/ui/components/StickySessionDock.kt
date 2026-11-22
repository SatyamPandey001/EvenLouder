package com.evenlouder.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evenlouder.app.ui.theme.LocalExtendedColors
import com.evenlouder.app.ui.theme.TactileTokens
import com.evenlouder.app.ui.theme.tactileCta
import com.evenlouder.app.ui.theme.tactileInset
import com.evenlouder.app.ui.theme.tactileRaised

/**
 * Sticky Bottom Dock showing live audio session attachment status
 * and an instant one-tap A/B Acoustic Test button with rich tactile CTA styling.
 */
@Composable
fun StickySessionDock(
    activeSessions: List<Pair<Int, String>>,
    isTestAudioPlaying: Boolean,
    onToggleTestAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalExtendedColors.current
    val isDark = extendedColors.isDark
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .tactileRaised(shapeRadius = 22.dp, elevation = 6.dp, isDarkTheme = isDark)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Status Column
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Tactile Status Beacon Lens
                val beaconColor = if (isTestAudioPlaying) {
                    secondaryColor
                } else if (activeSessions.isNotEmpty()) {
                    extendedColors.signalGreen
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }

                StatusBeacon(
                    color = beaconColor,
                    isDarkTheme = isDark,
                    pulsing = activeSessions.isNotEmpty() || isTestAudioPlaying
                )

                Column {
                    Text(
                        text = if (isTestAudioPlaying) {
                            "ACOUSTIC TEST LOOP ACTIVE"
                        } else if (activeSessions.isNotEmpty()) {
                            "HOOKED: ${activeSessions.first().second}"
                        } else {
                            "ENGINE STANDBY"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isTestAudioPlaying) secondaryColor else if (activeSessions.isNotEmpty()) extendedColors.signalGreen else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = if (isTestAudioPlaying) {
                            "Speaker master loop playing"
                        } else if (activeSessions.isNotEmpty()) {
                            "${activeSessions.size} active session(s) tuned"
                        } else {
                            "Waiting for media app audio stream"
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right Action: Instant A/B Test Button with Rich Glossy CTA Gradient
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val ctaGradient = if (isTestAudioPlaying) {
                listOf(Color(0xFFFF5252), Color(0xFFD32F2F))
            } else {
                TactileTokens.AmberCtaGradient
            }

            Box(
                modifier = Modifier
                    .tactileCta(
                        gradient = ctaGradient,
                        glowColor = if (isTestAudioPlaying) Color(0xFFFF5252) else secondaryColor,
                        shapeRadius = 12.dp,
                        elevation = 3.dp,
                        isPressed = isPressed
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onToggleTestAudio
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp)
                    .semantics {
                        contentDescription = if (isTestAudioPlaying) "Stop test audio" else "Play acoustic test audio"
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isTestAudioPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = if (isTestAudioPlaying) Color.White else Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isTestAudioPlaying) "STOP" else "TEST A/B",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isTestAudioPlaying) Color.White else Color.Black
                    )
                }
            }
        }
    }
}

/** Status LED with recessed physical bezel and pulsing glow. */
@Composable
private fun StatusBeacon(color: Color, isDarkTheme: Boolean, pulsing: Boolean) {
    val alpha = if (pulsing) {
        val transition = rememberInfiniteTransition(label = "pulse")
        val animated by transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
        animated
    } else {
        1f
    }

    Box(
        modifier = Modifier
            .size(16.dp)
            .tactileInset(shapeRadius = 8.dp, isDarkTheme = isDarkTheme, depth = 1.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = alpha), color.copy(alpha = alpha * 0.5f))
                    )
                )
        )
    }
}
