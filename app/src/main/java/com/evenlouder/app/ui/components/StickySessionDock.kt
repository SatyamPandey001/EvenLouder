package com.evenlouder.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evenlouder.app.ui.theme.LocalExtendedColors

/**
 * Sticky Bottom Dock showing live audio session attachment status
 * and an instant one-tap A/B Acoustic Test button.
 */
@Composable
fun StickySessionDock(
    activeSessions: List<Pair<Int, String>>,
    isTestAudioPlaying: Boolean,
    onToggleTestAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalExtendedColors.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(extendedColors.cardBorder)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Status Column
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Status Beacon Dot
                val beaconColor = if (isTestAudioPlaying) {
                    secondaryColor
                } else if (activeSessions.isNotEmpty()) {
                    extendedColors.signalGreen
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }

                StatusBeacon(
                    color = beaconColor,
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

            Spacer(modifier = Modifier.width(8.dp))

            // Right Action: Instant A/B Test Button
            Button(
                onClick = onToggleTestAudio,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTestAudioPlaying) Color(0xFFFF5252) else secondaryColor,
                    contentColor = if (isTestAudioPlaying) Color.White else Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.semantics {
                    contentDescription = if (isTestAudioPlaying) "Stop test audio" else "Play acoustic test audio"
                }
            ) {
                Icon(
                    imageVector = if (isTestAudioPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isTestAudioPlaying) "STOP" else "TEST A/B",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}


/** Status dot. The infinite animation only runs while pulsing, so an idle dock costs no frames. */
@Composable
private fun StatusBeacon(color: Color, pulsing: Boolean) {
    val alpha = if (pulsing) {
        val transition = rememberInfiniteTransition(label = "pulse")
        val animated by transition.animateFloat(
            initialValue = 0.4f,
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
            .size(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = alpha))
    )
}
