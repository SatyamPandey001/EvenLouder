package com.evenlouder.app.core.dsp

/**
 * Audio DSP parameters calibrated for smartphone micro-speakers on Android 15+.
 */
data class DspConfig(
    val enabled: Boolean = true,
    val highPassCutoffHz: Float = 140f,
    val bassGainDb: Float = 2.5f,        // 350 Hz harmonic punch / warmth (0 to 6 dB)
    val vocalClarityDb: Float = 3.5f,    // 2.8 kHz Fletcher-Munson presence (0 to 6 dB)
    val trebleGainDb: Float = 2.5f,      // 7.5 kHz crispness & detail (0 to 6 dB)
    val loudnessGainDb: Float = 3.0f,    // Multi-band compressor makeup gain (0 to 6 dB)
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
