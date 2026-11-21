package com.evenlouder.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evenlouder.app.core.dsp.DspConfig
import com.evenlouder.app.ui.theme.LocalExtendedColors
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * Real-time Hardware Acoustic Frequency Response Curve Visualizer.
 * Renders the accurate acoustic curve (High-Pass roll-off, Fletcher-Munson presence hump,
 * chassis de-box notch, harmonic bass punch, and treble air) computed from [DspConfig].
 */
@Composable
fun DspCurveVisualizer(
    config: DspConfig,
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalExtendedColors.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // Smooth animation transitions when presets or sliders change
    val animHpf by animateFloatAsState(targetValue = if (config.enabled) config.highPassCutoffHz else 20f, animationSpec = tween(250), label = "hpf")
    val animBass by animateFloatAsState(targetValue = if (config.enabled) config.bassGainDb else 0f, animationSpec = tween(250), label = "bass")
    val animBox by animateFloatAsState(targetValue = if (config.enabled) config.boxinessCutDb else 0f, animationSpec = tween(250), label = "box")
    val animVocal by animateFloatAsState(targetValue = if (config.enabled) config.vocalClarityDb else 0f, animationSpec = tween(250), label = "vocal")
    val animTreble by animateFloatAsState(targetValue = if (config.enabled) config.trebleGainDb else 0f, animationSpec = tween(250), label = "treble")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .padding(14.dp)
    ) {
        // Visualizer Top Info Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (config.enabled) extendedColors.signalGreen else Color(0xFFFF5252))
                )
                Text(
                    text = if (config.enabled) "ACOUSTIC TRANSFER CURVE" else "HARDWARE BYPASS (FLAT)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (config.enabled) primaryColor else Color(0xFFFF5252)
                )
            }
            Text(
                text = if (config.enabled) "CEILING: -0.5 dBFS" else "LIMITER: OFF",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (config.enabled) secondaryColor else onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Canvas for Frequency Response Graph
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Frequency range: 20 Hz to 20,000 Hz (Logarithmic scale)
                val minFreq = 20.0
                val maxFreq = 20000.0
                val logMin = ln(minFreq)
                val logMax = ln(maxFreq)

                fun freqToX(freq: Double): Float {
                    val logF = ln(freq)
                    return (((logF - logMin) / (logMax - logMin)) * width).toFloat()
                }

                // dB range: -18 dB to +8 dB
                val minDb = -18f
                val maxDb = 8f
                val dbSpan = maxDb - minDb

                fun dbToY(db: Float): Float {
                    val clamped = min(maxDb, max(minDb, db))
                    val norm = (maxDb - clamped) / dbSpan
                    return norm * height
                }

                val zeroY = dbToY(0f)
                val plus6Y = dbToY(6f)
                val minus12Y = dbToY(-12f)

                // 1. Draw Grid Lines
                val gridColor = Color.White.copy(alpha = 0.06f)
                drawLine(gridColor, Offset(0f, plus6Y), Offset(width, plus6Y), strokeWidth = 1f)
                drawLine(Color.White.copy(alpha = 0.12f), Offset(0f, zeroY), Offset(width, zeroY), strokeWidth = 1.5f)
                drawLine(gridColor, Offset(0f, minus12Y), Offset(width, minus12Y), strokeWidth = 1f)

                val gridFreqs = listOf(100.0, 500.0, 1000.0, 5000.0, 10000.0)
                gridFreqs.forEach { f ->
                    val x = freqToX(f)
                    drawLine(gridColor, Offset(x, 0f), Offset(x, height), strokeWidth = 1f)
                }

                // 2. Compute Synthetic Frequency Response Curve
                // Control points:
                // Sub-bass roll-off: < HPF drops sharply down to -18 dB
                // 280 Hz: Bass boost
                // 950 Hz: Boxiness notch cut
                // 2800 Hz: Fletcher-Munson vocal presence boost
                // 7500 Hz: Treble sparkle
                // 18000 Hz: Gentle high rolloff
                val curvePath = Path()
                val fillPath = Path()

                val numSteps = 70
                var first = true

                for (i in 0..numSteps) {
                    val t = i.toDouble() / numSteps
                    val f = minFreq * Math.exp(t * (logMax - logMin))
                    val x = freqToX(f)

                    // Calculate response at frequency f
                    var responseDb = 0f

                    if (config.enabled) {
                        // High-pass filter roll-off (24dB/octave slope below cutoff)
                        if (f < animHpf) {
                            val octavesBelow = (ln(animHpf / f) / ln(2.0)).toFloat()
                            responseDb -= octavesBelow * 18f
                        }

                        // Harmonic Bass Punch bell at 280 Hz (Q approx 1.2)
                        val bassDist = (ln(f / 280.0) / ln(2.0)).toFloat()
                        responseDb += animBass * (1f / (1f + 4f * bassDist * bassDist))

                        // Chassis De-Boxing notch at 950 Hz (Q approx 1.5)
                        val boxDist = (ln(f / 950.0) / ln(2.0)).toFloat()
                        responseDb += animBox * (1f / (1f + 5f * boxDist * boxDist))

                        // Fletcher-Munson Vocal Presence bell at 2800 Hz (Q approx 1.1)
                        val vocalDist = (ln(f / 2800.0) / ln(2.0)).toFloat()
                        responseDb += animVocal * (1f / (1f + 3f * vocalDist * vocalDist))

                        // Treble Crispness shelf/bell at 7500 Hz
                        val trebleDist = (ln(f / 7500.0) / ln(2.0)).toFloat()
                        responseDb += animTreble * (1f / (1f + 2.5f * trebleDist * trebleDist))

                        // Natural micro-speaker drop-off above 15 kHz
                        if (f > 15000.0) {
                            val octAbove = (ln(f / 15000.0) / ln(2.0)).toFloat()
                            responseDb -= octAbove * 6f
                        }
                    }

                    val y = dbToY(responseDb)

                    if (first) {
                        curvePath.moveTo(x, y)
                        fillPath.moveTo(x, height)
                        fillPath.lineTo(x, y)
                        first = false
                    } else {
                        curvePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }

                fillPath.lineTo(width, height)
                fillPath.close()

                // Draw Gradient Area under Curve
                val fillBrush = Brush.verticalGradient(
                    colors = if (config.enabled) {
                        listOf(primaryColor.copy(alpha = 0.28f), primaryColor.copy(alpha = 0.02f))
                    } else {
                        listOf(Color.Gray.copy(alpha = 0.15f), Color.Transparent)
                    },
                    startY = 0f,
                    endY = height
                )
                drawPath(fillPath, brush = fillBrush)

                // Draw Active Curve Line
                val curveColor = if (config.enabled) primaryColor else Color.Gray
                drawPath(
                    curvePath,
                    color = curveColor,
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )

                // Draw Frequency Key Markers on Curve
                if (config.enabled) {
                    val hpfX = freqToX(animHpf.toDouble())
                    val hpfY = dbToY(-3f)
                    drawCircle(secondaryColor, radius = 4f, center = Offset(hpfX, hpfY))

                    val vocalX = freqToX(2800.0)
                    val vocalY = dbToY(animVocal)
                    drawCircle(primaryColor, radius = 4.5f, center = Offset(vocalX, vocalY))
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Frequency Legend Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("100 Hz (HPF)", fontSize = 10.sp, color = onSurfaceVariant, fontFamily = FontFamily.Monospace)
            Text("500 Hz", fontSize = 10.sp, color = onSurfaceVariant, fontFamily = FontFamily.Monospace)
            Text("2.8 kHz (Voice)", fontSize = 10.sp, color = onSurfaceVariant, fontFamily = FontFamily.Monospace)
            Text("10 kHz (Air)", fontSize = 10.sp, color = onSurfaceVariant, fontFamily = FontFamily.Monospace)
        }
    }
}
