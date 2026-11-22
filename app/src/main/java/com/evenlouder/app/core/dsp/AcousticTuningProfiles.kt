package com.evenlouder.app.core.dsp

enum class ProfileCategory(val label: String) {
    ALL("All"),
    HEADPHONES("Headphones (Measured)"),
    COMMUNITY("Community (OPRA)"),
    PHONE_SPEAKER("Phone Speaker"),
    CREATIVE("Creative Signatures"),
    // Legacy categories preserved for backward compatibility
    BASS("Bass"),
    INSTRUMENTAL("Instrumental"),
    DOLBY_DIRAC("Balanced & Hi-Fi")
}

/**
 * Selecting a profile preserves the user's volume boost and global hook configuration.
 */
fun DspConfig.applyingProfile(profile: TuningProfile): DspConfig =
    profile.config.copy(
        enabled = true,
        eqEnabled = true,
        volumeBoostDb = volumeBoostDb,
        globalHookEnabled = globalHookEnabled,
        activeProfileId = profile.acousticProfile?.id ?: activeProfileId
    )

/**
 * Unified Acoustic Tuning Profile repository providing evidence-based equalization.
 * Profiles are backed by empirical measurements (GRAS 43AG / IEC 60318-4), OPRA community records,
 * and deterministic W3C biquad filter calculations.
 */
enum class TuningProfile(
    val title: String,
    val subtitle: String,
    val description: String,
    val config: DspConfig,
    val categories: Set<ProfileCategory>,
    val acousticProfile: AcousticProfile?
) {
    // --- CATEGORY A: VERIFIED MEASUREMENT CORRECTIONS ---
    SENNHEISER_HD600(
        "Sennheiser HD 600",
        "Harman Over-Ear · Oratory1990",
        "Empirical acoustic correction for Sennheiser HD 600 measured on GRAS 43AG by Oratory1990. Corrects sub-bass roll-off and balances 3 kHz ear gain.",
        DspConfig(activeProfileId = "sennheiser_hd600_harman", highPassCutoffHz = 80f),
        setOf(ProfileCategory.HEADPHONES, ProfileCategory.DOLBY_DIRAC, ProfileCategory.INSTRUMENTAL),
        AcousticProfileRepository.findById("sennheiser_hd600_harman")
    ),
    SONY_XM4(
        "Sony WH-1000XM4",
        "Harman Over-Ear · Oratory1990",
        "Acoustic correction for Sony WH-1000XM4 measured on GRAS 43AG. Tames bloated 160 Hz mid-bass boom and restores 3.1 kHz vocal presence.",
        DspConfig(activeProfileId = "sony_wh1000xm4_harman", highPassCutoffHz = 80f),
        setOf(ProfileCategory.HEADPHONES, ProfileCategory.BASS),
        AcousticProfileRepository.findById("sony_wh1000xm4_harman")
    ),
    AIRPODS_PRO(
        "Apple AirPods Pro",
        "Harman In-Ear · Oratory1990",
        "Empirical in-ear correction for Apple AirPods Pro measured on GRAS 43AG. Enhances deep bass impact and corrects 4.8 kHz ear-canal reflection.",
        DspConfig(activeProfileId = "apple_airpods_pro_harman", highPassCutoffHz = 80f),
        setOf(ProfileCategory.HEADPHONES, ProfileCategory.DOLBY_DIRAC),
        AcousticProfileRepository.findById("apple_airpods_pro_harman")
    ),
    BEYER_DT770(
        "Beyerdynamic DT 770",
        "Harman Over-Ear · Oratory1990",
        "Acoustic correction for DT 770 Pro 80 Ohm measured on GRAS 43AG. Neutralizes the sharp 5.9 kHz and 8.4 kHz treble peaks while leveling mid-range.",
        DspConfig(activeProfileId = "beyerdynamic_dt770_80_harman", highPassCutoffHz = 80f),
        setOf(ProfileCategory.HEADPHONES, ProfileCategory.INSTRUMENTAL),
        AcousticProfileRepository.findById("beyerdynamic_dt770_80_harman")
    ),
    BOSE_QC35(
        "Bose QC 35 II",
        "Harman Over-Ear · Oratory1990",
        "Acoustic correction for Bose QuietComfort 35 II measured on GRAS 43AG. Provides linear bass response and smooth upper harmonics.",
        DspConfig(activeProfileId = "bose_qc35_ii_harman", highPassCutoffHz = 80f),
        setOf(ProfileCategory.HEADPHONES, ProfileCategory.DOLBY_DIRAC),
        AcousticProfileRepository.findById("bose_qc35_ii_harman")
    ),

    // --- CATEGORY B: COMMUNITY & OPRA PROFILES ---
    COMMUNITY_HD600_DIFFUSE(
        "HD 600 (Diffuse Field)",
        "ISO 11904-1 · OPRA",
        "Community OPRA equalization tuning HD 600 to ISO 11904-1 Diffuse Field response for analytical studio checking.",
        DspConfig(activeProfileId = "sennheiser_hd600_diffuse_field", highPassCutoffHz = 80f),
        setOf(ProfileCategory.COMMUNITY, ProfileCategory.INSTRUMENTAL),
        AcousticProfileRepository.findById("sennheiser_hd600_diffuse_field")
    ),
    COMMUNITY_XM4_WARM(
        "XM4 (Warm Audiophile)",
        "Super* Review · OPRA",
        "Community-authored profile relaxing high-treble fatigue while retaining full bass dynamics.",
        DspConfig(activeProfileId = "sony_wh1000xm4_warm_audiophile", highPassCutoffHz = 80f),
        setOf(ProfileCategory.COMMUNITY, ProfileCategory.BASS),
        AcousticProfileRepository.findById("sony_wh1000xm4_warm_audiophile")
    ),

    // --- PHONE SPEAKER ACOUSTIC TUNINGS ---
    PHONE_SPEAKER(
        "Phone Speaker (Balanced)",
        "Anti-Boxiness & Subsonic Protection",
        "Conservative speaker tuning recipe with 140 Hz subsonic protection and 950 Hz boxiness cut. Not a model-specific hardware calibration.",
        DspConfig(activeProfileId = "phone_speaker_optimized", highPassCutoffHz = 140f),
        setOf(ProfileCategory.PHONE_SPEAKER, ProfileCategory.DOLBY_DIRAC),
        AcousticProfileRepository.PHONE_SPEAKER_OPTIMIZED
    ),
    PHONE_SPEAKER_VOICE(
        "Phone Speaker (Voice)",
        "Speech Intelligibility & Anti-Rumble",
        "Conservative speaker tuning recipe with steep 160 Hz high-pass and 2.6 kHz speech presence boost. Not a model-specific hardware calibration.",
        DspConfig(activeProfileId = "phone_speaker_speech", highPassCutoffHz = 160f),
        setOf(ProfileCategory.PHONE_SPEAKER),
        AcousticProfileRepository.PHONE_SPEAKER_SPEECH
    ),

    // --- CATEGORY C: DETERMINISTIC CREATIVE SIGNATURES ---
    CINEMATIC(
        "Cinema Clarity",
        "Dialogue & Atmospheric Air",
        "V-shaped dynamic curve tuned for movie soundtracks, impactful low-end, and centered speech dialogue.",
        DspConfig(activeProfileId = "creative_cinematic", highPassCutoffHz = 90f),
        setOf(ProfileCategory.CREATIVE, ProfileCategory.DOLBY_DIRAC),
        DeterministicCreativeRecipes.RECIPES[5]
    ),
    DEEP_BASS(
        "Bass Boost",
        "Sub-Bass & Punch",
        "Fletcher-Munson low-frequency boost targeting sub-bass and kick drums with anti-muddiness cut.",
        DspConfig(activeProfileId = "creative_deep_bass", highPassCutoffHz = 85f),
        setOf(ProfileCategory.CREATIVE, ProfileCategory.BASS),
        DeterministicCreativeRecipes.RECIPES[0]
    ),
    STUDIO_REFERENCE(
        "Balanced Reference",
        "Neutral & Linear",
        "Flat transmission baseline with zero tonal coloration for critical monitoring and uncolored balance.",
        DspConfig(activeProfileId = "creative_studio_reference", highPassCutoffHz = 80f),
        setOf(ProfileCategory.CREATIVE, ProfileCategory.DOLBY_DIRAC, ProfileCategory.INSTRUMENTAL),
        DeterministicCreativeRecipes.RECIPES[1]
    ),
    VOCAL_PRO(
        "Voice & Podcast",
        "Speech Forward",
        "Boosts speech formants in the 2.5 kHz to 4 kHz range, cuts nasal chest boxiness, and attenuates sibilance.",
        DspConfig(activeProfileId = "creative_vocal_focus", highPassCutoffHz = 95f),
        setOf(ProfileCategory.CREATIVE),
        DeterministicCreativeRecipes.RECIPES[2]
    ),
    TREBLE_AIR(
        "Treble Detail",
        "Shimmer & Brilliance",
        "High-frequency shelving boost opening up cymbal shimmer and ambient room reverberation.",
        DspConfig(activeProfileId = "creative_treble_air", highPassCutoffHz = 80f),
        setOf(ProfileCategory.CREATIVE, ProfileCategory.INSTRUMENTAL),
        DeterministicCreativeRecipes.RECIPES[3]
    ),
    WARM_ANALOG(
        "Warm Acoustic",
        "Smooth & Organic",
        "Gentle low-mid warmth shelf with softened upper harmonics for relaxed, fatigue-free listening.",
        DspConfig(activeProfileId = "creative_warm_acoustic", highPassCutoffHz = 80f),
        setOf(ProfileCategory.CREATIVE, ProfileCategory.BASS),
        DeterministicCreativeRecipes.RECIPES[4]
    ),

    // --- BACKWARD COMPATIBLE ALIASES ---
    DOLBY_SPATIAL(
        "Cinema Clarity", "Dialogue & Air",
        "Clear dialogue and bright detail. Backed by deterministic W3C biquad filter formulation.",
        DspConfig(activeProfileId = "creative_cinematic", highPassCutoffHz = 90f),
        setOf(ProfileCategory.DOLBY_DIRAC),
        DeterministicCreativeRecipes.RECIPES[5]
    ),
    MAXX_BASS(
        "Bass Boost", "Upper-Bass Body",
        "Full low-end lift backed by Fletcher-Munson equal-loudness biquad filters.",
        DspConfig(activeProfileId = "creative_deep_bass", highPassCutoffHz = 85f),
        setOf(ProfileCategory.BASS),
        DeterministicCreativeRecipes.RECIPES[0]
    ),
    DIRAC_HIFI(
        "Balanced Reference", "Neutral & Natural",
        "Flat transmission baseline with uncolored frequency response.",
        DspConfig(activeProfileId = "creative_studio_reference", highPassCutoffHz = 80f),
        setOf(ProfileCategory.DOLBY_DIRAC, ProfileCategory.INSTRUMENTAL),
        DeterministicCreativeRecipes.RECIPES[1]
    ),
    ULTRA_BOOST(
        "Max Volume", "Loudest Preset",
        "Optimized micro-speaker tuning paired with maximum dynamic headroom.",
        DspConfig(activeProfileId = "phone_speaker_optimized", highPassCutoffHz = 140f, loudnessGainDb = 5f),
        emptySet(),
        AcousticProfileRepository.PHONE_SPEAKER_OPTIMIZED
    ),
    DOLBY_DYNAMIC_BASS(
        "Deep Bass Body", "Full Low-End",
        "Strong sub-bass response with acoustic resonance attenuation.",
        DspConfig(activeProfileId = "creative_deep_bass", highPassCutoffHz = 85f),
        setOf(ProfileCategory.BASS, ProfileCategory.DOLBY_DIRAC),
        DeterministicCreativeRecipes.RECIPES[0]
    ),
    DIRAC_TIGHT_BASS(
        "Tight Punch", "Clean Kick & Bass",
        "Measured acoustic correction with controlled low-frequency transient speed.",
        DspConfig(activeProfileId = "sennheiser_hd600_harman", highPassCutoffHz = 80f),
        setOf(ProfileCategory.BASS, ProfileCategory.DOLBY_DIRAC),
        AcousticProfileRepository.findById("sennheiser_hd600_harman")
    ),
    SUB_BASS_DEEP(
        "Club & Hip-Hop", "Maximum Bass",
        "Fletcher-Munson sub-bass boost curve.",
        DspConfig(activeProfileId = "creative_deep_bass", highPassCutoffHz = 85f),
        setOf(ProfileCategory.BASS),
        DeterministicCreativeRecipes.RECIPES[0]
    ),
    WARM_ANALOG_BASS(
        "Warm Low-Mids", "Smooth & Relaxed",
        "Full low-mid body with relaxed high-frequency shelving.",
        DspConfig(activeProfileId = "creative_warm_acoustic", highPassCutoffHz = 80f),
        setOf(ProfileCategory.BASS),
        DeterministicCreativeRecipes.RECIPES[4]
    ),
    DIRAC_VIRTUO_ACOUSTIC(
        "Acoustic & Classical", "Strings & Piano",
        "Natural string and acoustic instrument balance with open dynamics.",
        DspConfig(activeProfileId = "creative_warm_acoustic", highPassCutoffHz = 80f),
        setOf(ProfileCategory.INSTRUMENTAL, ProfileCategory.DOLBY_DIRAC),
        DeterministicCreativeRecipes.RECIPES[4]
    ),
    DOLBY_SYMPHONY(
        "Orchestral Air", "Open & Bright",
        "Airy treble extension with wide spatial acoustic cues.",
        DspConfig(activeProfileId = "creative_treble_air", highPassCutoffHz = 80f),
        setOf(ProfileCategory.INSTRUMENTAL, ProfileCategory.DOLBY_DIRAC),
        DeterministicCreativeRecipes.RECIPES[3]
    ),
    LIVE_INSTRUMENTAL(
        "Rock & Live", "Guitars & Drums",
        "Accurate ear-canal response with solid dynamics.",
        DspConfig(activeProfileId = "apple_airpods_pro_harman", highPassCutoffHz = 80f),
        setOf(ProfileCategory.INSTRUMENTAL),
        AcousticProfileRepository.findById("apple_airpods_pro_harman")
    ),
    BYPASS(
        "Bypass", "Stock Phone Sound",
        "Disables all processing for direct acoustic A/B comparison.",
        DspConfig(enabled = false, activeProfileId = null),
        emptySet(),
        null
    );

    fun matchesCategory(category: ProfileCategory): Boolean =
        category == ProfileCategory.ALL || categories.contains(category)
}
