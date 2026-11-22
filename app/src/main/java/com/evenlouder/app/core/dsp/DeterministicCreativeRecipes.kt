package com.evenlouder.app.core.dsp

/**
 * Deterministic sound-shaping recipe system built on psychoacoustic principles
 * and exact W3C biquad filter calculations.
 *
 * These recipes represent subjective preference tunings, not transducer corrections,
 * and are explicitly labeled as CREATIVE_SIGNATURE.
 */
object DeterministicCreativeRecipes {

    val RECIPES: List<AcousticProfile> = listOf(
        AcousticProfile(
            id = "creative_deep_bass",
            name = "Deep Bass Body",
            subtitle = "Sub-Bass & Punch",
            description = "Fletcher-Munson low-frequency boost targeting sub-bass and kick drums with acoustic anti-muddiness cut.",
            category = TuningCategory.CREATIVE_SIGNATURE,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "EvenLouder",
            model = "Acoustic Tuning Recipe",
            preampGainDb = -5.8f,
            filters = listOf(
                ParametricFilter("f1", FilterType.LOW_SHELF, frequencyHz = 85f, gainDb = 5.5f, qFactor = 0.71f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 180f, gainDb = 2.5f, qFactor = 1.4f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 850f, gainDb = -3.0f, qFactor = 1.8f),
                ParametricFilter("f4", FilterType.HIGH_SHELF, frequencyHz = 8000f, gainDb = -1.5f, qFactor = 0.71f)
            ),
            author = "EvenLouder DSP Lab",
            license = "Apache 2.0",
            targetCurve = "Equal-Loudness Sub-Bass Contour",
            algorithmVersion = "EvenLouder Recipe v2.0"
        ),
        AcousticProfile(
            id = "creative_studio_reference",
            name = "Studio Reference",
            subtitle = "Linear & Uncolored",
            description = "Flat transmission baseline with zero tonal coloration for critical monitoring and mastering balance.",
            category = TuningCategory.CREATIVE_SIGNATURE,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "EvenLouder",
            model = "Acoustic Tuning Recipe",
            preampGainDb = 0.0f,
            filters = listOf(
                ParametricFilter("f1", FilterType.PEAKING, frequencyHz = 1000f, gainDb = 0.0f, qFactor = 1.414f)
            ),
            author = "EvenLouder DSP Lab",
            license = "Apache 2.0",
            targetCurve = "Linear Flat 20Hz-20kHz",
            algorithmVersion = "EvenLouder Recipe v2.0"
        ),
        AcousticProfile(
            id = "creative_vocal_focus",
            name = "Vocal Clarity & Presence",
            subtitle = "Speech & Dialogue Forward",
            description = "Boosts speech formants in the 2.5 kHz to 4 kHz range, cuts nasal chest boxiness, and attenuates harsh sibilance.",
            category = TuningCategory.CREATIVE_SIGNATURE,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "EvenLouder",
            model = "Acoustic Tuning Recipe",
            preampGainDb = -4.5f,
            filters = listOf(
                ParametricFilter("f1", FilterType.HIGH_PASS, frequencyHz = 75f, gainDb = 0f, qFactor = 0.71f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 350f, gainDb = -2.0f, qFactor = 1.5f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 2800f, gainDb = 4.2f, qFactor = 1.8f),
                ParametricFilter("f4", FilterType.PEAKING, frequencyHz = 6500f, gainDb = -2.5f, qFactor = 2.5f),
                ParametricFilter("f5", FilterType.HIGH_SHELF, frequencyHz = 10000f, gainDb = 1.0f, qFactor = 0.71f)
            ),
            author = "EvenLouder DSP Lab",
            license = "Apache 2.0",
            targetCurve = "Vocal Intelligibility Index (ANSI S3.5)",
            algorithmVersion = "EvenLouder Recipe v2.0"
        ),
        AcousticProfile(
            id = "creative_treble_air",
            name = "Treble Detail & Air",
            subtitle = "Brilliance & Spatial Cues",
            description = "High-frequency shelving boost opening up cymbal shimmer and ambient room reverberation.",
            category = TuningCategory.CREATIVE_SIGNATURE,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "EvenLouder",
            model = "Acoustic Tuning Recipe",
            preampGainDb = -4.2f,
            filters = listOf(
                ParametricFilter("f1", FilterType.PEAKING, frequencyHz = 900f, gainDb = -2.0f, qFactor = 1.6f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 4500f, gainDb = 2.0f, qFactor = 2.0f),
                ParametricFilter("f3", FilterType.HIGH_SHELF, frequencyHz = 7500f, gainDb = 4.0f, qFactor = 0.71f)
            ),
            author = "EvenLouder DSP Lab",
            license = "Apache 2.0",
            targetCurve = "High Frequency Brilliance Contour",
            algorithmVersion = "EvenLouder Recipe v2.0"
        ),
        AcousticProfile(
            id = "creative_warm_acoustic",
            name = "Warm Acoustic & Strings",
            subtitle = "Smooth & Organic",
            description = "Gentle low-mid warmth shelf with softened upper harmonics for relaxed, fatigue-free listening.",
            category = TuningCategory.CREATIVE_SIGNATURE,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "EvenLouder",
            model = "Acoustic Tuning Recipe",
            preampGainDb = -3.2f,
            filters = listOf(
                ParametricFilter("f1", FilterType.LOW_SHELF, frequencyHz = 220f, gainDb = 3.0f, qFactor = 0.71f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 1100f, gainDb = -1.5f, qFactor = 1.4f),
                ParametricFilter("f3", FilterType.HIGH_SHELF, frequencyHz = 7000f, gainDb = -3.0f, qFactor = 0.71f)
            ),
            author = "EvenLouder DSP Lab",
            license = "Apache 2.0",
            targetCurve = "Warm Acoustic Tilt Curve",
            algorithmVersion = "EvenLouder Recipe v2.0"
        ),
        AcousticProfile(
            id = "creative_cinematic",
            name = "Cinematic Immersion",
            subtitle = "Dialogue Clarity & Deep Impact",
            description = "V-shaped dynamic curve tuned for movie soundtracks, impactful explosions, and centered speech dialogue.",
            category = TuningCategory.CREATIVE_SIGNATURE,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "EvenLouder",
            model = "Acoustic Tuning Recipe",
            preampGainDb = -4.8f,
            filters = listOf(
                ParametricFilter("f1", FilterType.LOW_SHELF, frequencyHz = 90f, gainDb = 4.5f, qFactor = 0.71f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 400f, gainDb = -2.5f, qFactor = 1.6f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 2800f, gainDb = 3.2f, qFactor = 1.8f),
                ParametricFilter("f4", FilterType.HIGH_SHELF, frequencyHz = 9500f, gainDb = 3.5f, qFactor = 0.71f)
            ),
            author = "EvenLouder DSP Lab",
            license = "Apache 2.0",
            targetCurve = "Theatrical Audio Curve",
            algorithmVersion = "EvenLouder Recipe v2.0"
        ),
        AcousticProfile(
            id = "creative_podcast",
            name = "Podcast & Audiobook",
            subtitle = "Plosive Filter & Speech Boost",
            description = "Steep rumble high-pass cut to remove microphone handling noise with focused speech presence.",
            category = TuningCategory.CREATIVE_SIGNATURE,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "EvenLouder",
            model = "Acoustic Tuning Recipe",
            preampGainDb = -4.8f,
            filters = listOf(
                ParametricFilter("f1", FilterType.HIGH_PASS, frequencyHz = 95f, gainDb = 0f, qFactor = 0.71f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 300f, gainDb = -3.0f, qFactor = 1.6f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 2500f, gainDb = 4.5f, qFactor = 1.5f),
                ParametricFilter("f4", FilterType.PEAKING, frequencyHz = 6800f, gainDb = -3.5f, qFactor = 2.5f)
            ),
            author = "EvenLouder DSP Lab",
            license = "Apache 2.0",
            targetCurve = "Broadcast Voice Standardization",
            algorithmVersion = "EvenLouder Recipe v2.0"
        )
    )
}
