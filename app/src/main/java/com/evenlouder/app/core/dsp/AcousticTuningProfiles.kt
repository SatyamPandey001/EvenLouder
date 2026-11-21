package com.evenlouder.app.core.dsp

/**
 * Acoustically distinct tuning profiles engineered specifically for smartphone micro-speakers.
 * Designed with bold parameter deltas for immediate, unmistakable hearing differences.
 */
enum class TuningProfile(
    val title: String,
    val subtitle: String,
    val description: String,
    val config: DspConfig
) {
    ULTRA_LOUD(
        title = "Ultra Loud",
        subtitle = "Max Room Volume",
        description = "Pushes maximum safe loudness across all frequencies with high-ratio compression and presence boost.",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 140f,
            bassGainDb = 3.5f,
            vocalClarityDb = 4.5f,
            trebleGainDb = 3.5f,
            loudnessGainDb = 6.0f,
            limiterCeilingDb = -0.5f
        )
    ),
    VOCAL_FOCUS(
        title = "Vocal Boost",
        subtitle = "Dialogue & Podcasts",
        description = "Cuts lower-end mud and pushes speech frequencies (+6.0 dB at 2.8 kHz) straight to the front of the mix.",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 160f,
            bassGainDb = 0.5f,
            vocalClarityDb = 6.0f,
            trebleGainDb = 3.5f,
            loudnessGainDb = 2.5f,
            limiterCeilingDb = -0.5f
        )
    ),
    BASS_PUNCH(
        title = "Bass Punch",
        subtitle = "Warm Chest Resonance",
        description = "Opens lower frequencies (120 Hz) and boosts upper-bass harmonics (+5.5 dB at 350 Hz) for a warm, punchy body.",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 120f,
            bassGainDb = 5.5f,
            vocalClarityDb = 1.5f,
            trebleGainDb = 1.0f,
            loudnessGainDb = 4.0f,
            limiterCeilingDb = -0.5f
        )
    ),
    CRISP_TREBLE(
        title = "Crisp Treble",
        subtitle = "Acoustic & Detail Sparkle",
        description = "Accentuates 7.5 kHz and 14 kHz highs (+6.0 dB) for sharp cymbals, acoustic guitars, and crystalline clarity.",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 150f,
            bassGainDb = 1.0f,
            vocalClarityDb = 2.5f,
            trebleGainDb = 6.0f,
            loudnessGainDb = 2.5f,
            limiterCeilingDb = -0.5f
        )
    ),
    BALANCED_STUDIO(
        title = "Balanced Studio",
        subtitle = "Natural Music Master",
        description = "Evenly calibrated sub-bass excursion protection with balanced vocal and instrument separation.",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 140f,
            bassGainDb = 2.5f,
            vocalClarityDb = 3.5f,
            trebleGainDb = 2.5f,
            loudnessGainDb = 3.0f,
            limiterCeilingDb = -0.5f
        )
    ),
    BYPASS(
        title = "Hardware Bypass",
        subtitle = "Stock Phone Sound",
        description = "Completely disables the DSP engine for instant A/B comparison against stock phone speaker sound.",
        config = DspConfig(
            enabled = false
        )
    )
}
