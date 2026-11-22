package com.evenlouder.app.core.dsp

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Metric comparing the spectral and parametric acoustic difference between two profiles.
 */
data class ProfileDistanceResult(
    val rmsSpectralDifferenceDb: Double,
    val maxAbsoluteDifferenceDb: Double,
    val isPerceptuallyIndistinct: Boolean,
    val summary: String
)

/**
 * Mathematical profile comparison engine that detects identical or near-duplicate profiles
 * by evaluating their continuous frequency response across a logarithmic frequency grid.
 */
object ProfileComparator {

    const val GRID_POINTS = 100
    const val MIN_FREQ_HZ = 20.0
    const val MAX_FREQ_HZ = 20000.0

    // JND (Just Noticeable Difference) in audio equalization curves is typically 0.5 - 1.0 dB.
    // If the RMS spectral difference is below 0.75 dB and max peak difference is below 1.5 dB,
    // listeners cannot reliably distinguish them.
    const val PERCEPTUAL_RMS_THRESHOLD_DB = 0.75
    const val PERCEPTUAL_MAX_DIFF_THRESHOLD_DB = 1.5

    /**
     * Computes the spectral distance between two profiles.
     */
    fun compare(
        profileA: AcousticProfile,
        profileB: AcousticProfile,
        sampleRateHz: Double = 48000.0
    ): ProfileDistanceResult {
        var sumSqDiff = 0.0
        var maxDiff = 0.0

        for (i in 0 until GRID_POINTS) {
            val f = MIN_FREQ_HZ * (MAX_FREQ_HZ / MIN_FREQ_HZ).pow(i.toDouble() / (GRID_POINTS - 1))
            val respA = profileA.responseDbAt(f.toFloat(), sampleRateHz)
            val respB = profileB.responseDbAt(f.toFloat(), sampleRateHz)

            val diff = abs(respA - respB).toDouble()
            sumSqDiff += diff * diff
            if (diff > maxDiff) {
                maxDiff = diff
            }
        }

        val rms = sqrt(sumSqDiff / GRID_POINTS)
        val isIndistinct = rms < PERCEPTUAL_RMS_THRESHOLD_DB && maxDiff < PERCEPTUAL_MAX_DIFF_THRESHOLD_DB

        val summary = if (isIndistinct) {
            "Profiles '${profileA.name}' and '${profileB.name}' are acoustically indistinct (RMS: ${"%.2f".format(rms)} dB, Max: ${"%.2f".format(maxDiff)} dB)."
        } else {
            "Distinct profiles (RMS difference: ${"%.2f".format(rms)} dB, Max difference: ${"%.2f".format(maxDiff)} dB)."
        }

        return ProfileDistanceResult(
            rmsSpectralDifferenceDb = rms,
            maxAbsoluteDifferenceDb = maxDiff,
            isPerceptuallyIndistinct = isIndistinct,
            summary = summary
        )
    }

    /**
     * Checks if a collection of profiles has any duplicate or indistinguishable pairs.
     */
    fun findIndistinctPairs(
        profiles: List<AcousticProfile>,
        sampleRateHz: Double = 48000.0
    ): List<Pair<AcousticProfile, AcousticProfile>> {
        val indistinct = mutableListOf<Pair<AcousticProfile, AcousticProfile>>()
        for (i in profiles.indices) {
            for (j in i + 1 until profiles.size) {
                val result = compare(profiles[i], profiles[j], sampleRateHz)
                if (result.isPerceptuallyIndistinct) {
                    indistinct.add(profiles[i] to profiles[j])
                }
            }
        }
        return indistinct
    }
}
