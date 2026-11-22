package com.evenlouder.app.core.dsp

import kotlin.math.*

/**
 * Quality classification of the 23-band approximation compared to the original parametric profile.
 */
enum class FitQuality(val label: String, val isCalibratedGrade: Boolean) {
    EXCELLENT("Calibrated (Near Identical)", true),
    GOOD("High Fidelity Fit", true),
    ACCEPTABLE("Acceptable Fit", false),
    APPROXIMATE("Approximated (Bandwidth Limited)", false)
}

data class FitEvaluationResult(
    val rmsErrorDb: Double,
    val peakDiscrepancyDb: Double,
    val quality: FitQuality,
    val fittedPoints: List<EqPoint>
)

/**
 * Mathematically constrained fitting engine converting continuous parametric biquad cascades
 * into the 23 discrete Pre-EQ bands supported by Android's DynamicsProcessing engine.
 */
object ParametricBandFitter {

    private val FIXED_EDGES = floatArrayOf(
        200f, 250f, 315f, 400f, 500f, 630f, 800f, 1000f, 1250f, 1600f,
        2000f, 2500f, 3150f, 4000f, 5000f, 6300f, 8000f, 10000f, 13000f, 20000f
    )

    private const val SUB_SAMPLES_PER_BAND = 5
    private const val EVAL_GRID_POINTS = 100

    /**
     * Fits an acoustic profile and manual slider offsets to the 23-band Pre-EQ architecture.
     * Uses log-spaced sub-band integration to minimize spectral truncation error.
     */
    fun fit(
        profile: AcousticProfile?,
        hpfHz: Float,
        manualOffsets: (Float) -> Float = { 0f },
        sampleRateHz: Double = 48000.0
    ): FitEvaluationResult {
        val hpf = hpfHz.coerceIn(80f, 180f)
        val fittedPoints = ArrayList<EqPoint>(EqLayout.PRE_EQ_BAND_COUNT)

        // Subsonic transducer protection: 3 fixed roll-off bands
        fittedPoints += EqPoint(hpf * 0.5f, -24f)
        fittedPoints += EqPoint(hpf * 0.75f, -14f)
        fittedPoints += EqPoint(hpf, -6f)

        var low = hpf
        for (edge in FIXED_EDGES) {
            val bandGain: Float
            if (profile != null && profile.filters.isNotEmpty()) {
                // Multi-point sub-band integration
                var sumDb = 0.0
                for (k in 0 until SUB_SAMPLES_PER_BAND) {
                    val ratio = (k + 0.5) / SUB_SAMPLES_PER_BAND
                    val subFreq = low * (edge / low).toDouble().pow(ratio)
                    sumDb += profile.responseDbAt(subFreq.toFloat(), sampleRateHz)
                }
                val avgProfileGain = (sumDb / SUB_SAMPLES_PER_BAND).toFloat()
                val centerFreq = sqrt(low * edge)
                val manual = manualOffsets(centerFreq)
                bandGain = (avgProfileGain + manual).coerceIn(-18f, 15f)
            } else {
                val centerFreq = sqrt(low * edge)
                val manual = manualOffsets(centerFreq)
                bandGain = manual.coerceIn(-18f, 15f)
            }

            fittedPoints += EqPoint(edge, bandGain)
            low = edge
        }

        // Evaluate RMS fitting error across 100-point log grid in the active passband
        var sumSqErr = 0.0
        var maxDiscrepancy = 0.0
        var evalPointsCount = 0

        // High-pass filter roll-off takes place below 1.25 * hpf.
        // Evaluation evaluates how accurately the 20 pre-eq bands reconstruct the acoustic profile in the passband.
        val passbandStartHz = max(hpf * 1.25, 120.0)

        for (i in 0 until EVAL_GRID_POINTS) {
            val f = 20.0 * (20000.0 / 20.0).pow(i.toDouble() / (EVAL_GRID_POINTS - 1))
            if (f >= passbandStartHz) {
                val targetDb = if (profile != null) {
                    profile.responseDbAt(f.toFloat(), sampleRateHz) + manualOffsets(f.toFloat())
                } else {
                    manualOffsets(f.toFloat())
                }

                val fittedDb = interpolateFittedResponse(fittedPoints, f.toFloat())
                val err = abs(fittedDb - targetDb).toDouble()
                sumSqErr += err * err
                if (err > maxDiscrepancy) {
                    maxDiscrepancy = err
                }
                evalPointsCount++
            }
        }

        val rms = sqrt(sumSqErr / max(1, evalPointsCount))

        val quality = when {
            rms < 0.65 && maxDiscrepancy < 1.6 -> FitQuality.EXCELLENT
            rms < 1.25 && maxDiscrepancy < 2.8 -> FitQuality.GOOD
            rms < 2.0 -> FitQuality.ACCEPTABLE
            else -> FitQuality.APPROXIMATE
        }

        return FitEvaluationResult(
            rmsErrorDb = rms,
            peakDiscrepancyDb = maxDiscrepancy,
            quality = quality,
            fittedPoints = fittedPoints
        )
    }

    /**
     * Interpolates the response of the 23-band graphic layout at frequency [fHz].
     */
    fun interpolateFittedResponse(points: List<EqPoint>, fHz: Float): Float {
        if (points.isEmpty()) return 0f
        if (fHz <= points.first().edgeHz) return points.first().gainDb
        if (fHz >= points.last().edgeHz) return points.last().gainDb

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            if (fHz in p0.edgeHz..p1.edgeHz) {
                val log0 = ln(p0.edgeHz.toDouble())
                val log1 = ln(p1.edgeHz.toDouble())
                val logF = ln(fHz.toDouble())
                val ratio = ((logF - log0) / (log1 - log0)).toFloat()
                return p0.gainDb + ratio * (p1.gainDb - p0.gainDb)
            }
        }
        return points.last().gainDb
    }
}
