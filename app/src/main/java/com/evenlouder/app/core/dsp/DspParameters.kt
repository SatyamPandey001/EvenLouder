package com.evenlouder.app.core.dsp

/**
 * Audio DSP parameters integrating real acoustic measurement profiles,
 * deterministic filter calculations, and manual tactile adjustments.
 */
data class DspConfig(
    val enabled: Boolean = true,          // master power (false = full bypass)
    val eqEnabled: Boolean = true,        // false = "boost only" (no tone shaping, keeps the low cut)
    val highPassCutoffHz: Float = 140f,
    val bassGainDb: Float = 0.0f,
    val boxinessCutDb: Float = 0.0f,
    val vocalClarityDb: Float = 0.0f,
    val trebleGainDb: Float = 0.0f,
    val loudnessGainDb: Float = 2.0f,
    val limiterCeilingDb: Float = -0.5f,
    val volumeBoostDb: Float = 0f,        // extra loudness pushed into the limiter
    val globalHookEnabled: Boolean = true,
    val activeProfileId: String? = "phone_speaker_optimized"
) {
    companion object {
        const val MAX_BOOST_DB = 12f
    }
}
