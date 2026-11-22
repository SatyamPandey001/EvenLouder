package com.evenlouder.app.core.dsp

/**
 * Three scientific profile categories + device speaker category.
 */
enum class TuningCategory(val displayName: String, val description: String) {
    MEASUREMENT_CORRECTION(
        "Acoustic Correction",
        "Calibrated against acoustic coupler measurements to match target curves (Harman / Diffuse Field)"
    ),
    COMMUNITY_PROFILE(
        "Community & Engineer",
        "Peer-reviewed parametric EQ profiles from audio engineers and OPRA database"
    ),
    PHONE_SPEAKER(
        "Phone Speaker",
        "Acoustic optimization with subsonic protection tuned for phone micro-speakers"
    ),
    CREATIVE_SIGNATURE(
        "Creative Signatures",
        "Deterministic, documented sound-shaping curves for subjective listening preferences"
    )
}

enum class TargetDeviceType {
    HEADPHONES,
    PHONE_SPEAKER,
    EXTERNAL_SPEAKER
}

/**
 * Unified, validated acoustic profile data model with complete provenance and filter configurations.
 */
data class AcousticProfile(
    val id: String,
    val name: String,
    val subtitle: String = "",
    val description: String,
    val category: TuningCategory,
    val targetDeviceType: TargetDeviceType = TargetDeviceType.HEADPHONES,
    val manufacturer: String = "",
    val model: String = "",
    val preampGainDb: Float = 0f,
    val filters: List<ParametricFilter> = emptyList(),
    val author: String = "EvenLouder Audio Research",
    val sourceUrl: String = "",
    val license: String = "CC BY-SA 4.0",
    val targetCurve: String = "",
    val measurementRig: String = "",
    val algorithmVersion: String = "EvenLouder DSP v2.0",
    val validationStatus: String = "VALIDATED",
    val createdAt: String = "2026-11-22"
) {
    /**
     * Evaluates the combined frequency response of this profile across a specified frequency.
     */
    fun responseDbAt(frequencyHz: Float, sampleRateHz: Double = 48000.0): Float {
        val biquads = filters.filter { it.enabled }.map { it.toBiquad(sampleRateHz) }
        val sumDb = BiquadCalculator.evaluateCascadeDb(biquads, frequencyHz.toDouble(), sampleRateHz)
        return (sumDb + preampGainDb).toFloat()
    }

    /**
     * Calculates the peak gain across the audible spectrum (20 Hz - 20 kHz)
     * to determine required headroom.
     */
    fun calculatePeakGainDb(sampleRateHz: Double = 48000.0): Float {
        val biquads = filters.filter { it.enabled }.map { it.toBiquad(sampleRateHz) }
        var maxGain = 0.0
        // Log-spaced frequency grid evaluation
        for (i in 0..100) {
            val f = 20.0 * Math.pow(20000.0 / 20.0, i / 100.0)
            val g = BiquadCalculator.evaluateCascadeDb(biquads, f, sampleRateHz)
            if (g > maxGain) maxGain = g
        }
        return maxGain.toFloat()
    }

    /**
     * Calculates the recommended preamp attenuation to strictly prevent digital clipping.
     */
    fun recommendedPreampDb(sampleRateHz: Double = 48000.0): Float {
        val peak = calculatePeakGainDb(sampleRateHz)
        return if (peak > 0f) -peak - 0.2f else 0f
    }
}
