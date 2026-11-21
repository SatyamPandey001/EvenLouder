package com.evenlouder.app.core.dsp

/**
 * Pre-engineered acoustic tuning profiles for different micro-speaker listening modes.
 */
enum class TuningProfile(
    val title: String,
    val description: String,
    val config: DspConfig
) {
    BALANCED_CLARITY(
        title = "Balanced Clarity",
        description = "General media & music: sub-bass protection with balanced vocal presence.",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 140f,
            vocalClarityDb = 3.0f,
            loudnessGainDb = 2.0f,
            limiterCeilingDb = -0.5f
        )
    ),
    VOCAL_FOCUS(
        title = "Vocal & Dialogue",
        description = "Podcasts, YouTube, and calls: aggressive 160Hz cut with max dialogue clarity.",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 160f,
            vocalClarityDb = 5.0f,
            loudnessGainDb = 1.5f,
            limiterCeilingDb = -0.5f
        )
    ),
    MAX_LOUDNESS(
        title = "Max Safe Loudness",
        description = "Maximizes perceived acoustic volume within safe micro-speaker excursion boundaries.",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 150f,
            vocalClarityDb = 2.5f,
            loudnessGainDb = 4.0f,
            limiterCeilingDb = -0.5f
        )
    ),
    BYPASS(
        title = "Hardware Bypass",
        description = "Disables DSP effect for clean A/B comparison against stock sound.",
        config = DspConfig(
            enabled = false
        )
    )
}
