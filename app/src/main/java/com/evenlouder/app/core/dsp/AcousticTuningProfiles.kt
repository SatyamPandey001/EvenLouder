package com.evenlouder.app.core.dsp

/**
 * Filter categories for acoustic tuning profiles.
 */
enum class ProfileCategory(val label: String) {
    ALL("All"),
    BASS("Bass Modes"),
    INSTRUMENTAL("Instrumental"),
    DOLBY_DIRAC("Spatial & Hi-Fi")
}

/**
 * Calibrated acoustic tuning profiles for physical smartphone micro-speakers.
 * Engineered using 6-band parametric EQ, 3-band dynamics compression, and brickwall limiting.
 */
enum class TuningProfile(
    val title: String,
    val subtitle: String,
    val description: String,
    val config: DspConfig,
    val categories: Set<ProfileCategory> = setOf(ProfileCategory.ALL)
) {
    // --- 1. Core Acoustic Presets ---
    DOLBY_SPATIAL(
        title = "Cinema 3D Spatial",
        subtitle = "Surround & Speech Clarity",
        description = "De-boxes chassis resonance (-2.5 dB @ 950 Hz), boosts dialogue presence (+5.0 dB @ 2.8 kHz), and adds airy sparkle (+4.5 dB @ 7.5 kHz) with soft-knee mastering compression.",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 135f,
            bassGainDb = 3.5f,
            boxinessCutDb = -2.5f,
            vocalClarityDb = 5.0f,
            trebleGainDb = 4.5f,
            loudnessGainDb = 4.5f,
            limiterCeilingDb = -0.5f
        ),
        categories = setOf(ProfileCategory.DOLBY_DIRAC)
    ),
    MAXX_BASS(
        title = "Club Bass Punch",
        subtitle = "Upper-Bass Harmonic Resonance",
        description = "Boosts 280 Hz punch (+6.0 dB) while rolling off sub-120 Hz rumble to maximize perceived bass body without speaker cone over-excursion.",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 120f,
            bassGainDb = 6.0f,
            boxinessCutDb = -1.5f,
            vocalClarityDb = 2.0f,
            trebleGainDb = 2.0f,
            loudnessGainDb = 5.0f,
            limiterCeilingDb = -0.5f
        ),
        categories = setOf(ProfileCategory.BASS)
    ),
    DIRAC_HIFI(
        title = "Studio Hi-Fi Master",
        subtitle = "Acoustic Neutral Balance",
        description = "Linearized speaker tuning eliminating nasal chassis coloration (-3.0 dB @ 950 Hz) for transparent, reference music fidelity.",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 140f,
            bassGainDb = 2.0f,
            boxinessCutDb = -3.0f,
            vocalClarityDb = 3.0f,
            trebleGainDb = 3.0f,
            loudnessGainDb = 2.5f,
            limiterCeilingDb = -0.5f
        ),
        categories = setOf(ProfileCategory.DOLBY_DIRAC, ProfileCategory.INSTRUMENTAL)
    ),
    VOCAL_PRO(
        title = "Dialogue & Voice Max",
        subtitle = "Podcasts & Clear Speech",
        description = "Vocal forwardness boost (+6.0 dB @ 2.8 kHz) with tight 165 Hz rumble cutoff, bringing dialogue, podcasts, and commentary forward.",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 165f,
            bassGainDb = 0.5f,
            boxinessCutDb = -2.0f,
            vocalClarityDb = 6.0f,
            trebleGainDb = 3.5f,
            loudnessGainDb = 3.5f,
            limiterCeilingDb = -0.5f
        ),
        categories = emptySet()
    ),
    ULTRA_BOOST(
        title = "Ultra Loudroom",
        subtitle = "Max Projection Volume",
        description = "Pushes speaker output to maximum loudness (+6.0 dB MBC makeup) with lookahead brickwall safety against digital clipping.",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 145f,
            bassGainDb = 4.0f,
            boxinessCutDb = -1.5f,
            vocalClarityDb = 4.5f,
            trebleGainDb = 4.0f,
            loudnessGainDb = 6.0f,
            limiterCeilingDb = -0.5f
        ),
        categories = emptySet()
    ),

    // --- 2. Bass Variations ---
    DOLBY_DYNAMIC_BASS(
        title = "Dynamic Low-End Bass",
        subtitle = "Low-End Excursion Extension",
        description = "125 Hz excursion cutoff, +5.2 dB punch at 280 Hz, and -2.0 dB chassis notch for low-end body without midrange mud.",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 125f,
            bassGainDb = 5.2f,
            boxinessCutDb = -2.0f,
            vocalClarityDb = 3.5f,
            trebleGainDb = 3.0f,
            loudnessGainDb = 4.5f,
            limiterCeilingDb = -0.5f
        ),
        categories = setOf(ProfileCategory.BASS, ProfileCategory.DOLBY_DIRAC)
    ),
    DIRAC_TIGHT_BASS(
        title = "Tight Transient Bass",
        subtitle = "Fast Attack & Punch",
        description = "Deep -4.0 dB chassis anti-resonance notch, 130 Hz cutoff, +4.2 dB punch, and clean transient attack on kick drums and basslines.",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 130f,
            bassGainDb = 4.2f,
            boxinessCutDb = -4.0f,
            vocalClarityDb = 2.5f,
            trebleGainDb = 3.5f,
            loudnessGainDb = 3.0f,
            limiterCeilingDb = -0.5f
        ),
        categories = setOf(ProfileCategory.BASS, ProfileCategory.DOLBY_DIRAC)
    ),
    SUB_BASS_DEEP(
        title = "Deep Sub-Harmonics",
        subtitle = "Max Club & 808 Rumble",
        description = "Aggressive low-end extension (110 Hz HPF) with +6.0 dB upper-bass punch and +5.5 dB compression for electronic music and hip-hop.",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 110f,
            bassGainDb = 6.0f,
            boxinessCutDb = -1.5f,
            vocalClarityDb = 2.0f,
            trebleGainDb = 2.5f,
            loudnessGainDb = 5.5f,
            limiterCeilingDb = -0.5f
        ),
        categories = setOf(ProfileCategory.BASS)
    ),
    WARM_ANALOG_BASS(
        title = "Warm Studio Bass",
        subtitle = "Round Analog Warmth",
        description = "Round 280 Hz punch (+4.5 dB), gentle -1.0 dB boxiness cut preserving warm low-mid body, and smooth treble roll-off.",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 125f,
            bassGainDb = 4.5f,
            boxinessCutDb = -1.0f,
            vocalClarityDb = 2.5f,
            trebleGainDb = 2.0f,
            loudnessGainDb = 3.5f,
            limiterCeilingDb = -0.5f
        ),
        categories = setOf(ProfileCategory.BASS)
    ),

    // --- 3. Instrumental Modes ---
    DIRAC_VIRTUO_ACOUSTIC(
        title = "Acoustic & Classical",
        subtitle = "Strings, Piano & Unplugged",
        description = "Chassis anti-resonance filter (-3.8 dB @ 950 Hz), +4.5 dB overtone shimmer for violins and guitars, with open dynamics.",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 135f,
            bassGainDb = 2.5f,
            boxinessCutDb = -3.8f,
            vocalClarityDb = 3.0f,
            trebleGainDb = 4.5f,
            loudnessGainDb = 2.0f,
            limiterCeilingDb = -0.5f
        ),
        categories = setOf(ProfileCategory.INSTRUMENTAL, ProfileCategory.DOLBY_DIRAC)
    ),
    DOLBY_SYMPHONY(
        title = "Concert Symphony Hall",
        subtitle = "Orchestral Staging & Air",
        description = "Wide air sparkle (+5.5 dB @ 7.5 kHz), -3.0 dB chassis notch, and rich double-bass/timpani depth (+3.5 dB @ 280 Hz).",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 130f,
            bassGainDb = 3.5f,
            boxinessCutDb = -3.0f,
            vocalClarityDb = 3.5f,
            trebleGainDb = 5.5f,
            loudnessGainDb = 3.5f,
            limiterCeilingDb = -0.5f
        ),
        categories = setOf(ProfileCategory.INSTRUMENTAL, ProfileCategory.DOLBY_DIRAC)
    ),
    LIVE_INSTRUMENTAL(
        title = "Live Stage & Rock",
        subtitle = "Electric Guitars & Brass",
        description = "Forward instrument presence (+4.5 dB @ 2.8 kHz), crisp cymbal sheen (+4.8 dB @ 7.5 kHz), and snappy attack (+4.0 dB @ 280 Hz).",
        config = DspConfig(
            enabled = true,
            highPassCutoffHz = 140f,
            bassGainDb = 4.0f,
            boxinessCutDb = -2.5f,
            vocalClarityDb = 4.5f,
            trebleGainDb = 4.8f,
            loudnessGainDb = 4.5f,
            limiterCeilingDb = -0.5f
        ),
        categories = setOf(ProfileCategory.INSTRUMENTAL)
    ),

    // --- 4. Bypass ---
    BYPASS(
        title = "Hardware Bypass",
        subtitle = "Stock Phone Sound",
        description = "Completely neutralizes and disconnects DSP for immediate A/B acoustic comparison against raw phone speakers.",
        config = DspConfig(
            enabled = false
        ),
        categories = emptySet()
    );

    fun matchesCategory(category: ProfileCategory): Boolean {
        return category == ProfileCategory.ALL || categories.contains(category)
    }
}
