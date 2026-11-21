package com.evenlouder.app.core.dsp

/**
 * Premium acoustic tuning profiles modeled after industry standards:
 * Dolby Atmos Mobile, Waves MaxxAudio, and Dirac HD Sound.
 * Specifically calibrated to transform physical smartphone micro-speakers.
 */
enum class TuningProfile(
    val title: String,
    val subtitle: String,
    val description: String,
    val config: DspConfig
) {
    DOLBY_SPATIAL(
        title = "Dolby Cinema 3D",
        subtitle = "Surround & Spatial Clarity",
        description = "De-boxes chassis resonance (-2.5 dB @ 950 Hz), boosts dialogue presence (+5.0 dB @ 2.8 kHz), and adds airy spatial sparkle (+4.5 dB) with soft-knee mastering compression.",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 135f,
            bassGainDb = 3.5f,
            boxinessCutDb = -2.5f,
            vocalClarityDb = 5.0f,
            trebleGainDb = 4.5f,
            loudnessGainDb = 4.5f,
            limiterCeilingDb = -0.5f
        )
    ),
    MAXX_BASS(
        title = "MaxxBass Club",
        subtitle = "Deep Psychoacoustic Punch",
        description = "Emulates Waves MaxxBass by synthesizing 280 Hz 2nd and 3rd harmonics (+6.0 dB) while rolling off speaker-destroying sub-bass, producing deep, rich perceived bass body.",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 120f,
            bassGainDb = 6.0f,
            boxinessCutDb = -1.5f,
            vocalClarityDb = 2.0f,
            trebleGainDb = 2.0f,
            loudnessGainDb = 5.0f,
            limiterCeilingDb = -0.5f
        )
    ),
    DIRAC_HIFI(
        title = "Dirac Hi-Fi Master",
        subtitle = "Acoustic Neutral Balance",
        description = "Linearized impulse response tuning eliminating nasal speaker coloration (-3.0 dB @ 950 Hz) for transparent, studio-reference music fidelity.",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 140f,
            bassGainDb = 2.0f,
            boxinessCutDb = -3.0f,
            vocalClarityDb = 3.0f,
            trebleGainDb = 3.0f,
            loudnessGainDb = 2.5f,
            limiterCeilingDb = -0.5f
        )
    ),
    VOCAL_PRO(
        title = "Dialogue & Voice Max",
        subtitle = "Podcasts & Clear Speech",
        description = "Broadcast-grade vocal forwardness (+6.0 dB @ 2.8 kHz) with tight 165 Hz rumble cutoff, bringing dialogue, podcasts, and video commentary directly to your ears.",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 165f,
            bassGainDb = 0.5f,
            boxinessCutDb = -2.0f,
            vocalClarityDb = 6.0f,
            trebleGainDb = 3.5f,
            loudnessGainDb = 3.5f,
            limiterCeilingDb = -0.5f
        )
    ),
    ULTRA_BOOST(
        title = "Ultra Loudroom",
        subtitle = "Max Party Volume",
        description = "Drives speaker hardware to maximum acoustic projection (+6.0 dB MBC makeup) with lookahead brickwall safety against digital clipping.",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 145f,
            bassGainDb = 4.0f,
            boxinessCutDb = -1.5f,
            vocalClarityDb = 4.5f,
            trebleGainDb = 4.0f,
            loudnessGainDb = 6.0f,
            limiterCeilingDb = -0.5f
        )
    ),
    BYPASS(
        title = "Hardware Bypass",
        subtitle = "Stock Phone Sound",
        description = "Completely neutralizes and disconnects DSP for immediate A/B acoustic comparison against raw phone speakers.",
        config = DspConfig(
            enabled = false
        )
    )
}
