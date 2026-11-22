package com.evenlouder.app.core.dsp

/**
 * Audio DSP parameters for phone micro-speakers.
 * Ranges: bass 0..10, box -10..0, vocal -6..10, treble -6..10, loudness 0..6, boost 0..12 dB.
 */
data class DspConfig(
    val enabled: Boolean = true,          // master power (false = full bypass)
    val eqEnabled: Boolean = true,        // false = "boost only" (no tone shaping, keeps the low cut)
    val highPassCutoffHz: Float = 140f,
    val bassGainDb: Float = 3.0f,
    val boxinessCutDb: Float = -3.0f,
    val vocalClarityDb: Float = 3.0f,
    val trebleGainDb: Float = 3.0f,
    val loudnessGainDb: Float = 2.0f,
    val limiterCeilingDb: Float = -0.5f,
    val volumeBoostDb: Float = 0f,        // extra loudness pushed into the limiter
    val globalHookEnabled: Boolean = true
) {
    companion object {
        const val MAX_BOOST_DB = 12f
    }
}
