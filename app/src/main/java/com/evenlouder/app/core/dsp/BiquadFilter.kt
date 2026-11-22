package com.evenlouder.app.core.dsp

import kotlin.math.*

/**
 * Filter types specified in Robert Bristow-Johnson's W3C Audio EQ Cookbook.
 */
enum class FilterType {
    PEAKING,
    LOW_SHELF,
    HIGH_SHELF,
    LOW_PASS,
    HIGH_PASS,
    NOTCH,
    BAND_PASS
}

/**
 * Normalized 2nd-order biquad coefficients where a0 = 1.0.
 * H(z) = (b0 + b1*z^-1 + b2*z^-2) / (1 + a1*z^-1 + a2*z^-2)
 */
data class BiquadCoefficients(
    val b0: Double,
    val b1: Double,
    val b2: Double,
    val a1: Double,
    val a2: Double
) {
    /**
     * Checks if all coefficients are finite numbers (not NaN or Infinite).
     */
    val isFinite: Boolean
        get() = b0.isFinite() && b1.isFinite() && b2.isFinite() && a1.isFinite() && a2.isFinite()

    /**
     * Validates BIBO stability using the Schur-Cohn criterion:
     * Poles of z^2 + a1*z + a2 = 0 must lie strictly inside the unit circle |z| < 1.
     * Equivalent to: |a2| < 1 and |a1| < 1 + a2.
     */
    val isStable: Boolean
        get() = isFinite && abs(a2) < 1.0 && abs(a1) < (1.0 + a2)

    /**
     * Calculates the complex frequency response magnitude in decibels at frequency [fHz]
     * for a given sampling rate [sampleRateHz].
     */
    fun responseDbAt(fHz: Double, sampleRateHz: Double): Double {
        if (!isFinite) return 0.0
        val omega = 2.0 * PI * (fHz / sampleRateHz)
        val cosW = cos(omega)
        val sinW = sin(omega)
        val cos2W = cos(2.0 * omega)
        val sin2W = sin(2.0 * omega)

        val numRe = b0 + b1 * cosW + b2 * cos2W
        val numIm = -b1 * sinW - b2 * sin2W

        val denRe = 1.0 + a1 * cosW + a2 * cos2W
        val denIm = -a1 * sinW - a2 * sin2W

        val numMagSq = numRe * numRe + numIm * numIm
        val denMagSq = denRe * denRe + denIm * denIm

        if (denMagSq <= 1e-12) return 0.0
        val magSq = numMagSq / denMagSq
        return if (magSq > 1e-12) 10.0 * log10(magSq) else -120.0
    }
}

/**
 * Deterministic Biquad calculation engine implementing the W3C Audio EQ Cookbook.
 */
object BiquadCalculator {

    /**
     * Computes normalized biquad coefficients for the specified filter parameters.
     *
     * @param type The biquad filter type.
     * @param frequencyHz Center or cutoff frequency (0 < f0 < sampleRateHz / 2).
     * @param gainDb Gain in decibels (applicable for PEAKING, LOW_SHELF, HIGH_SHELF).
     * @param qFactor Quality factor Q (Q > 0).
     * @param sampleRateHz Audio sampling frequency (e.g. 44100.0 or 48000.0).
     */
    fun calculate(
        type: FilterType,
        frequencyHz: Double,
        gainDb: Double,
        qFactor: Double,
        sampleRateHz: Double = 48000.0
    ): BiquadCoefficients {
        val nyquist = sampleRateHz / 2.0
        val f0 = frequencyHz.coerceIn(10.0, nyquist - 1.0)
        val q = max(0.01, qFactor)

        val omega0 = 2.0 * PI * (f0 / sampleRateHz)
        val sinW0 = sin(omega0)
        val cosW0 = cos(omega0)
        val alpha = sinW0 / (2.0 * q)
        val a = 10.0.pow(gainDb / 40.0) // sqrt(10^(gainDb/20))

        val b0: Double
        val b1: Double
        val b2: Double
        val a0: Double
        val a1: Double
        val a2: Double

        when (type) {
            FilterType.PEAKING -> {
                b0 = 1.0 + alpha * a
                b1 = -2.0 * cosW0
                b2 = 1.0 - alpha * a
                a0 = 1.0 + alpha / a
                a1 = -2.0 * cosW0
                a2 = 1.0 - alpha / a
            }
            FilterType.LOW_SHELF -> {
                val sqrtA = sqrt(a)
                val twoSqrtAAlpha = 2.0 * sqrtA * alpha
                b0 = a * ((a + 1.0) - (a - 1.0) * cosW0 + twoSqrtAAlpha)
                b1 = 2.0 * a * ((a - 1.0) - (a + 1.0) * cosW0)
                b2 = a * ((a + 1.0) - (a - 1.0) * cosW0 - twoSqrtAAlpha)
                a0 = (a + 1.0) + (a - 1.0) * cosW0 + twoSqrtAAlpha
                a1 = -2.0 * ((a - 1.0) + (a + 1.0) * cosW0)
                a2 = (a + 1.0) + (a - 1.0) * cosW0 - twoSqrtAAlpha
            }
            FilterType.HIGH_SHELF -> {
                val sqrtA = sqrt(a)
                val twoSqrtAAlpha = 2.0 * sqrtA * alpha
                b0 = a * ((a + 1.0) + (a - 1.0) * cosW0 + twoSqrtAAlpha)
                b1 = -2.0 * a * ((a - 1.0) + (a + 1.0) * cosW0)
                b2 = a * ((a + 1.0) + (a - 1.0) * cosW0 - twoSqrtAAlpha)
                a0 = (a + 1.0) - (a - 1.0) * cosW0 + twoSqrtAAlpha
                a1 = 2.0 * ((a - 1.0) - (a + 1.0) * cosW0)
                a2 = (a + 1.0) - (a - 1.0) * cosW0 - twoSqrtAAlpha
            }
            FilterType.LOW_PASS -> {
                b0 = (1.0 - cosW0) / 2.0
                b1 = 1.0 - cosW0
                b2 = (1.0 - cosW0) / 2.0
                a0 = 1.0 + alpha
                a1 = -2.0 * cosW0
                a2 = 1.0 - alpha
            }
            FilterType.HIGH_PASS -> {
                b0 = (1.0 + cosW0) / 2.0
                b1 = -(1.0 + cosW0)
                b2 = (1.0 + cosW0) / 2.0
                a0 = 1.0 + alpha
                a1 = -2.0 * cosW0
                a2 = 1.0 - alpha
            }
            FilterType.NOTCH -> {
                b0 = 1.0
                b1 = -2.0 * cosW0
                b2 = 1.0
                a0 = 1.0 + alpha
                a1 = -2.0 * cosW0
                a2 = 1.0 - alpha
            }
            FilterType.BAND_PASS -> {
                b0 = alpha
                b1 = 0.0
                b2 = -alpha
                a0 = 1.0 + alpha
                a1 = -2.0 * cosW0
                a2 = 1.0 - alpha
            }
        }

        if (abs(a0) < 1e-12) {
            return BiquadCoefficients(1.0, 0.0, 0.0, 0.0, 0.0) // Passthrough fallback
        }

        return BiquadCoefficients(
            b0 = b0 / a0,
            b1 = b1 / a0,
            b2 = b2 / a0,
            a1 = a1 / a0,
            a2 = a2 / a0
        )
    }

    /**
     * Evaluates the combined response in decibels of a cascade of filters at frequency [fHz].
     */
    fun evaluateCascadeDb(
        filters: List<BiquadCoefficients>,
        fHz: Double,
        sampleRateHz: Double = 48000.0
    ): Double {
        var sumDb = 0.0
        for (f in filters) {
            sumDb += f.responseDbAt(fHz, sampleRateHz)
        }
        return sumDb
    }
}
