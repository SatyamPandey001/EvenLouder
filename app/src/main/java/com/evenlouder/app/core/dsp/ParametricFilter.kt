package com.evenlouder.app.core.dsp

/**
 * Represents a single parametric equalizer filter band.
 */
data class ParametricFilter(
    val id: String = "",
    val type: FilterType = FilterType.PEAKING,
    val frequencyHz: Float = 1000f,
    val gainDb: Float = 0f,
    val qFactor: Float = 1.414f,
    val enabled: Boolean = true,
    val slope: Float = 1.0f
) {
    /**
     * Converts this parametric definition into normalized biquad coefficients
     * conforming to the W3C Audio EQ Cookbook.
     */
    fun toBiquad(sampleRateHz: Double = 48000.0): BiquadCoefficients {
        if (!enabled) {
            return BiquadCoefficients(1.0, 0.0, 0.0, 0.0, 0.0) // Unity passthrough
        }
        return BiquadCalculator.calculate(
            type = type,
            frequencyHz = frequencyHz.toDouble(),
            gainDb = gainDb.toDouble(),
            qFactor = qFactor.toDouble(),
            sampleRateHz = sampleRateHz
        )
    }
}
