package com.evenlouder.app.core.dsp

/**
 * Audio DSP parameters calibrated for smartphone micro-speakers on Android 15+.
 */
data class DspConfig(
    val enabled: Boolean = true,
    val highPassCutoffHz: Float = 140f,
    val vocalClarityDb: Float = 3.0f,
    val loudnessGainDb: Float = 2.0f,
    val limiterCeilingDb: Float = -0.5f,
    val globalHookEnabled: Boolean = false // Off by default to avoid interfering with other apps
) {
    companion object {
        const val PRE_EQ_BANDS = 6
        const val MBC_BANDS = 3
        const val POST_EQ_BANDS = 0 // Disabled: unused post-EQ was causing filter NaNs
        const val MAX_ALLOWED_AIDL_BANDS = 32
        const val TOTAL_CONFIGURED_BANDS = PRE_EQ_BANDS + MBC_BANDS + POST_EQ_BANDS // 9 < 32
    }
}
