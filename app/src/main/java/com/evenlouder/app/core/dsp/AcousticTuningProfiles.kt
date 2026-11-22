package com.evenlouder.app.core.dsp

enum class ProfileCategory(val label: String) {
    ALL("All"),
    BASS("Bass"),
    INSTRUMENTAL("Instrumental"),
    DOLBY_DIRAC("Balanced & Hi-Fi")
}

/**
 * Selecting a profile keeps the user's volume boost and the "hook other apps" switch.
 */
fun DspConfig.applyingProfile(profile: TuningProfile): DspConfig =
    profile.config.copy(
        eqEnabled = true,
        volumeBoostDb = volumeBoostDb,
        globalHookEnabled = globalHookEnabled
    )

/**
 * Phone speakers cannot reproduce much below ~200 Hz, so "bass" here means upper bass (~300 Hz).
 * Values are intentionally large (up to +/-10 dB) so the difference is clearly audible.
 * Enum names are kept for compatibility with ProfileSelector and the service.
 */
enum class TuningProfile(
    val title: String,
    val subtitle: String,
    val description: String,
    val config: DspConfig,
    val categories: Set<ProfileCategory> = setOf(ProfileCategory.ALL)
) {
    DOLBY_SPATIAL(
        "Cinema Clarity", "Dialogue & Air",
        "Clear dialogue and bright detail, less boxiness. Good for movies and series.",
        DspConfig(highPassCutoffHz = 140f, bassGainDb = 4f, boxinessCutDb = -4f,
            vocalClarityDb = 6f, trebleGainDb = 6f, loudnessGainDb = 4f),
        setOf(ProfileCategory.DOLBY_DIRAC)
    ),
    MAXX_BASS(
        "Bass Boost", "Upper-Bass Body",
        "Big lift around 300 Hz for a fuller, heavier sound. Vocals and treble stay soft.",
        DspConfig(highPassCutoffHz = 120f, bassGainDb = 9f, boxinessCutDb = -2f,
            vocalClarityDb = 1f, trebleGainDb = 1f, loudnessGainDb = 4f),
        setOf(ProfileCategory.BASS)
    ),
    DIRAC_HIFI(
        "Balanced Reference", "Neutral & Natural",
        "Mild boxiness cut with small presence and treble lifts. The most natural option.",
        DspConfig(highPassCutoffHz = 140f, bassGainDb = 3f, boxinessCutDb = -4f,
            vocalClarityDb = 3f, trebleGainDb = 3f, loudnessGainDb = 2f),
        setOf(ProfileCategory.DOLBY_DIRAC, ProfileCategory.INSTRUMENTAL)
    ),
    VOCAL_PRO(
        "Voice & Podcast", "Speech Forward",
        "Strong 2-4 kHz presence, almost no bass. Makes speech, calls and commentary much clearer.",
        DspConfig(highPassCutoffHz = 160f, bassGainDb = 0f, boxinessCutDb = -5f,
            vocalClarityDb = 10f, trebleGainDb = 4f, loudnessGainDb = 4f),
        emptySet()
    ),
    ULTRA_BOOST(
        "Max Volume", "Loudest Preset",
        "Loud, forward tuning with maximum compressor makeup. For even more, add Volume booster.",
        DspConfig(highPassCutoffHz = 140f, bassGainDb = 4f, boxinessCutDb = -3f,
            vocalClarityDb = 6f, trebleGainDb = 5f, loudnessGainDb = 6f),
        emptySet()
    ),

    DOLBY_DYNAMIC_BASS(
        "Deep Bass Body", "Full Low-End",
        "Strong bass with a deep boxiness cut so it stays full without turning muddy.",
        DspConfig(highPassCutoffHz = 120f, bassGainDb = 8f, boxinessCutDb = -5f,
            vocalClarityDb = 3f, trebleGainDb = 3f, loudnessGainDb = 4f),
        setOf(ProfileCategory.BASS, ProfileCategory.DOLBY_DIRAC)
    ),
    DIRAC_TIGHT_BASS(
        "Tight Punch", "Clean Kick & Bass",
        "Moderate bass, deep mid cut and extra treble for a fast, less boomy sound.",
        DspConfig(highPassCutoffHz = 140f, bassGainDb = 5f, boxinessCutDb = -7f,
            vocalClarityDb = 3f, trebleGainDb = 5f, loudnessGainDb = 3f),
        setOf(ProfileCategory.BASS, ProfileCategory.DOLBY_DIRAC)
    ),
    SUB_BASS_DEEP(
        "Club & Hip-Hop", "Maximum Bass",
        "Maximum bass, little mid cut, dark treble. Best at moderate volume.",
        DspConfig(highPassCutoffHz = 120f, bassGainDb = 10f, boxinessCutDb = -1f,
            vocalClarityDb = 0f, trebleGainDb = 1f, loudnessGainDb = 5f),
        setOf(ProfileCategory.BASS)
    ),
    WARM_ANALOG_BASS(
        "Warm Low-Mids", "Smooth & Relaxed",
        "Full low-mid body with softened highs. A warm, relaxed, less bright sound.",
        DspConfig(highPassCutoffHz = 140f, bassGainDb = 5f, boxinessCutDb = 0f,
            vocalClarityDb = -1f, trebleGainDb = -4f, loudnessGainDb = 3f),
        setOf(ProfileCategory.BASS)
    ),

    DIRAC_VIRTUO_ACOUSTIC(
        "Acoustic & Classical", "Strings & Piano",
        "Cuts box resonance and lifts treble for string and piano detail, with open dynamics.",
        DspConfig(highPassCutoffHz = 140f, bassGainDb = 2f, boxinessCutDb = -5f,
            vocalClarityDb = 2f, trebleGainDb = 6f, loudnessGainDb = 1.5f),
        setOf(ProfileCategory.INSTRUMENTAL, ProfileCategory.DOLBY_DIRAC)
    ),
    DOLBY_SYMPHONY(
        "Orchestral Air", "Open & Bright",
        "Very bright and airy with light bass support.",
        DspConfig(highPassCutoffHz = 140f, bassGainDb = 3f, boxinessCutDb = -4f,
            vocalClarityDb = 3f, trebleGainDb = 9f, loudnessGainDb = 2.5f),
        setOf(ProfileCategory.INSTRUMENTAL, ProfileCategory.DOLBY_DIRAC)
    ),
    LIVE_INSTRUMENTAL(
        "Rock & Live", "Guitars & Drums",
        "Forward mids, bright cymbals, solid bass. Energetic and loud.",
        DspConfig(highPassCutoffHz = 140f, bassGainDb = 6f, boxinessCutDb = -3f,
            vocalClarityDb = 6f, trebleGainDb = 6f, loudnessGainDb = 4.5f),
        setOf(ProfileCategory.INSTRUMENTAL)
    ),

    BYPASS(
        "Bypass", "Stock Phone Sound",
        "Disables all processing for direct comparison.",
        DspConfig(enabled = false),
        emptySet()
    );

    fun matchesCategory(category: ProfileCategory): Boolean =
        category == ProfileCategory.ALL || categories.contains(category)
}
