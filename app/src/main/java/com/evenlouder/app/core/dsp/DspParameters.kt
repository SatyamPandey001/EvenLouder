package com.evenlouder.app.core.dsp

/**
 * Audio DSP parameters calibrated for smartphone micro-speakers on Android 15+.
 */
data class DspConfig(
    val enabled: Boolean = true,
    val highPassCutoffHz: Float = 135f,
    val bassGainDb: Float = 3.5f,        // 280 Hz harmonic punch / MaxxBass resonance (0 to 6 dB)
    val boxinessCutDb: Float = -2.5f,    // 950 Hz anti-boxiness / chassis resonance cut (-6 to 0 dB)
    val vocalClarityDb: Float = 4.5f,    // 2.8 kHz Fletcher-Munson presence (0 to 6 dB)
    val trebleGainDb: Float = 3.5f,      // 7.5 kHz crispness & detail (0 to 6 dB)
    val loudnessGainDb: Float = 4.0f,    // Multi-band compressor makeup gain (0 to 6 dB)
    val limiterCeilingDb: Float = -0.5f, // Brickwall safety ceiling
    val globalHookEnabled: Boolean = true // Hook external media players by default
) {
    companion object {
        const val PRE_EQ_BANDS = 6
        const val MBC_BANDS = 3
        const val POST_EQ_BANDS = 0
        const val MAX_ALLOWED_AIDL_BANDS = 32
        const val TOTAL_CONFIGURED_BANDS = PRE_EQ_BANDS + MBC_BANDS + POST_EQ_BANDS // 9 < 32
    }
}
