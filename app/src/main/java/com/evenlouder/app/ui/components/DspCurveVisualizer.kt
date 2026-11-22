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
import com.evenlouder.app.core.dsp.EqLayout
import com.evenlouder.app.core.dsp.EqPoint
import com.evenlouder.app.ui.theme.LocalExtendedColors
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private const val MIN_FREQ = 20.0
private const val MAX_FREQ = 20000.0
private const val MIN_DB = -26f
private const val MAX_DB = 12f

/**
 * EQ band response viewer.
 *
 * Unlike the previous version, this does NOT use invented bell curves. It reads the exact
 * band table the audio engine applies ([EqLayout.build]) and draws it, interpolating
 * smoothly between band centres on a log-frequency axis. Compression and the limiter are
 * level-dependent, so they are not part of this graph.
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

    // Animate the real parameters, then rebuild the real band table from them
    val animHpf by animateFloatAsState(config.highPassCutoffHz, tween(250), label = "hpf")
    val animBass by animateFloatAsState(config.bassGainDb, tween(250), label = "bass")
    val animBox by animateFloatAsState(config.boxinessCutDb, tween(250), label = "box")
    val animVocal by animateFloatAsState(config.vocalClarityDb, tween(250), label = "vocal")
    val animTreble by animateFloatAsState(config.trebleGainDb, tween(250), label = "treble")

    val bands: List<EqPoint> = EqLayout.build(
        config.copy(
            highPassCutoffHz = animHpf,
            bassGainDb = animBass,
            boxinessCutDb = animBox,
            vocalClarityDb = animVocal,
            trebleGainDb = animTreble
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .padding(14.dp)
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
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (config.enabled) extendedColors.signalGreen else Color(0xFFFF5252))
                )
                Text(
                    text = if (config.enabled) "EQ BAND RESPONSE" else "BYPASS (FLAT)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (config.enabled) primaryColor else Color(0xFFFF5252)
                )
            }
            Text(
                text = if (config.enabled) "LIMITER: ${"%.1f".format(config.limiterCeilingDb)} dBFS" else "LIMITER: OFF",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (config.enabled) secondaryColor else onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

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
                val logMin = ln(MIN_FREQ)
                val logMax = ln(MAX_FREQ)

                fun freqToX(freq: Double): Float =
                    (((ln(freq) - logMin) / (logMax - logMin)) * width).toFloat()

                fun dbToY(db: Float): Float {
                    val clamped = min(MAX_DB, max(MIN_DB, db))
                    return ((MAX_DB - clamped) / (MAX_DB - MIN_DB)) * height
                }

                // Grid
                val gridColor = Color.White.copy(alpha = 0.06f)
                listOf(6f, -12f, -24f).forEach { db ->
                    drawLine(gridColor, Offset(0f, dbToY(db)), Offset(width, dbToY(db)), strokeWidth = 1f)
                }
                drawLine(
                    Color.White.copy(alpha = 0.12f),
                    Offset(0f, dbToY(0f)), Offset(width, dbToY(0f)),
                    strokeWidth = 1.5f
                )
                listOf(100.0, 500.0, 1000.0, 5000.0, 10000.0).forEach { f ->
                    drawLine(gridColor, Offset(freqToX(f), 0f), Offset(freqToX(f), height), strokeWidth = 1f)
                }

                // Curve
                val curvePath = Path()
                val fillPath = Path()
                val steps = 120
                for (i in 0..steps) {
                    val t = i.toDouble() / steps
                    val f = MIN_FREQ * exp(t * (logMax - logMin))
                    val db = if (config.enabled) responseDb(f, bands) else 0f
                    val x = freqToX(f)
                    val y = dbToY(db)
                    if (i == 0) {
                        curvePath.moveTo(x, y)
                        fillPath.moveTo(x, height)
                        fillPath.lineTo(x, y)
                    } else {
                        curvePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }
                fillPath.lineTo(width, height)
                fillPath.close()

                drawPath(
                    fillPath,
                    brush = Brush.verticalGradient(
                        colors = if (config.enabled) {
                            listOf(primaryColor.copy(alpha = 0.28f), primaryColor.copy(alpha = 0.02f))
                        } else {
                            listOf(Color.Gray.copy(alpha = 0.15f), Color.Transparent)
                        },
                        startY = 0f,
                        endY = height
                    )
                )
                drawPath(
                    curvePath,
                    color = if (config.enabled) primaryColor else Color.Gray,
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )

                // Markers at the four tuned regions (values read from the real table)
                if (config.enabled) {
                    listOf(280.0, 950.0, 2800.0, 7500.0).forEach { f ->
                        drawCircle(
                            color = secondaryColor,
                            radius = 4.5f,
                            center = Offset(freqToX(f), dbToY(responseDb(f, bands)))
                        )
                    }
                    drawCircle(
                        color = primaryColor,
                        radius = 4f,
                        center = Offset(freqToX(animHpf.toDouble()), dbToY(responseDb(animHpf.toDouble(), bands)))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Labels positioned at their true log-frequency positions
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(14.dp)) {
            val logSpan = ln(MAX_FREQ) - ln(MIN_FREQ)
            listOf(
                100.0 to "100",
                500.0 to "500",
                2800.0 to "2.8k",
                10000.0 to "10k"
            ).forEach { (f, label) ->
                val frac = ((ln(f) - ln(MIN_FREQ)) / logSpan).toFloat()
                Text(
                    text = "$label Hz",
                    fontSize = 10.sp,
                    color = onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.offset(x = maxWidth * frac - 18.dp)
                )
            }
        }
    }
}

/**
 * Response in dB at [freq], interpolated (log-frequency) between band centres.
 * Band centre = geometric mean of the band's lower and upper edge.
 */
private fun responseDb(freq: Double, bands: List<EqPoint>): Float {
    val centers = DoubleArray(bands.size)
    var low = MIN_FREQ
    for (i in bands.indices) {
        val high = bands[i].edgeHz.toDouble()
        centers[i] = sqrt(low * high)
        low = high
    }
    if (freq <= centers.first()) return bands.first().gainDb
    if (freq >= centers.last()) return bands.last().gainDb
    for (i in 0 until bands.size - 1) {
        if (freq <= centers[i + 1]) {
            val t = ((ln(freq) - ln(centers[i])) / (ln(centers[i + 1]) - ln(centers[i]))).toFloat()
            return bands[i].gainDb + (bands[i + 1].gainDb - bands[i].gainDb) * t
        }
    }
    return bands.last().gainDb
}
