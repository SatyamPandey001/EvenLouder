package com.evenlouder.app.core.dsp

import com.evenlouder.app.core.data.*

/**
 * Single source of truth for the EvenLouder acoustic profile catalog.
 * Delivers verified measurement corrections, community profiles, phone speaker tunings,
 * and deterministic creative signatures backed by a structured relational database.
 */
object AcousticProfileRepository {

    // Phone speaker acoustic models (generic conservative tuning recipes, not model-specific calibrations)
    val PHONE_SPEAKER_OPTIMIZED = AcousticProfile(
        id = "phone_speaker_optimized",
        name = "Phone Speaker (Balanced)",
        subtitle = "Generic Tuning · Subsonic Protection",
        description = "Generic conservative speaker tuning recipe with 140 Hz subsonic protection and 950 Hz chassis anti-boxiness cut. Not a model-specific hardware calibration.",
        category = TuningCategory.PHONE_SPEAKER,
        targetDeviceType = TargetDeviceType.PHONE_SPEAKER,
        manufacturer = "Universal Smartphone",
        model = "Micro-Transducer",
        preampGainDb = -3.8f,
        filters = listOf(
            ParametricFilter("f1", FilterType.HIGH_PASS, frequencyHz = 140f, gainDb = 0f, qFactor = 0.71f),
            ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 300f, gainDb = 3.0f, qFactor = 1.2f),
            ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 950f, gainDb = -4.5f, qFactor = 1.4f),
            ParametricFilter("f4", FilterType.PEAKING, frequencyHz = 2800f, gainDb = 3.5f, qFactor = 1.6f),
            ParametricFilter("f5", FilterType.HIGH_SHELF, frequencyHz = 5500f, gainDb = 3.0f, qFactor = 0.71f)
        ),
        author = "EvenLouder Acoustic Lab",
        license = "Apache 2.0",
        targetCurve = "Generic Micro-Speaker Acoustic Protection",
        measurementRig = ""
    )

    val PHONE_SPEAKER_SPEECH = AcousticProfile(
        id = "phone_speaker_speech",
        name = "Phone Speaker (Voice & Call)",
        subtitle = "Generic Tuning · Speech Focus",
        description = "Generic conservative speaker tuning recipe with 160 Hz high-pass and 2.6 kHz vocal presence boost. Not a model-specific hardware calibration.",
        category = TuningCategory.PHONE_SPEAKER,
        targetDeviceType = TargetDeviceType.PHONE_SPEAKER,
        manufacturer = "Universal Smartphone",
        model = "Micro-Transducer",
        preampGainDb = -5.2f,
        filters = listOf(
            ParametricFilter("f1", FilterType.HIGH_PASS, frequencyHz = 160f, gainDb = 0f, qFactor = 0.71f),
            ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 400f, gainDb = -2.5f, qFactor = 1.5f),
            ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 950f, gainDb = -5.0f, qFactor = 1.5f),
            ParametricFilter("f4", FilterType.PEAKING, frequencyHz = 2600f, gainDb = 5.5f, qFactor = 1.5f),
            ParametricFilter("f5", FilterType.HIGH_SHELF, frequencyHz = 6500f, gainDb = -2.0f, qFactor = 0.71f)
        ),
        author = "EvenLouder Acoustic Lab",
        license = "Apache 2.0",
        targetCurve = "Speech Intelligibility Focus",
        measurementRig = ""
    )

    // Measurement-Based Headphone Corrections
    val MEASUREMENT_PROFILES: List<AcousticProfile> = listOf(
        AcousticProfile(
            id = "sennheiser_hd600_harman",
            name = "Sennheiser HD 600",
            subtitle = "Harman Over-Ear 2018 · Oratory1990",
            description = "Empirical acoustic correction for Sennheiser HD 600 measured on GRAS 43AG by Oratory1990. Corrects sub-bass roll-off and balances 3 kHz ear gain.",
            category = TuningCategory.MEASUREMENT_CORRECTION,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "Sennheiser",
            model = "HD 600",
            preampGainDb = -5.8f,
            filters = listOf(
                ParametricFilter("f1", FilterType.LOW_SHELF, frequencyHz = 105f, gainDb = 5.5f, qFactor = 0.71f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 200f, gainDb = -1.8f, qFactor = 1.4f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 1250f, gainDb = -1.5f, qFactor = 2.0f),
                ParametricFilter("f4", FilterType.PEAKING, frequencyHz = 3200f, gainDb = -2.7f, qFactor = 3.0f),
                ParametricFilter("f5", FilterType.PEAKING, frequencyHz = 5700f, gainDb = -5.2f, qFactor = 4.5f),
                ParametricFilter("f6", FilterType.HIGH_SHELF, frequencyHz = 10000f, gainDb = 2.0f, qFactor = 0.71f)
            ),
            author = "Oratory1990",
            sourceUrl = "https://github.com/jaakkopasanen/AutoEq/tree/master/results/oratory1990/harman_over-ear_2018/Sennheiser%20HD%20600",
            license = "CC BY-SA 4.0",
            targetCurve = "Harman Over-Ear 2018",
            measurementRig = "GRAS 43AG / 45CA",
            algorithmVersion = "AutoEQ v4.4.1"
        ),
        AcousticProfile(
            id = "sennheiser_hd650_harman",
            name = "Sennheiser HD 650",
            subtitle = "Harman Over-Ear 2018 · Oratory1990",
            description = "Acoustic correction for Sennheiser HD 650 / HD 6XX measured on GRAS 43AG by Oratory1990. Lifts deep bass and clarifies veiled treble.",
            category = TuningCategory.MEASUREMENT_CORRECTION,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "Sennheiser",
            model = "HD 650",
            preampGainDb = -6.7f,
            filters = listOf(
                ParametricFilter("f1", FilterType.LOW_SHELF, frequencyHz = 105f, gainDb = 6.5f, qFactor = 0.71f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 165f, gainDb = -1.8f, qFactor = 1.2f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 1300f, gainDb = -1.4f, qFactor = 2.0f),
                ParametricFilter("f4", FilterType.PEAKING, frequencyHz = 5500f, gainDb = -4.0f, qFactor = 3.5f),
                ParametricFilter("f5", FilterType.HIGH_SHELF, frequencyHz = 9500f, gainDb = 2.5f, qFactor = 0.71f)
            ),
            author = "Oratory1990",
            sourceUrl = "https://github.com/jaakkopasanen/AutoEq/tree/master/results/oratory1990/harman_over-ear_2018/Sennheiser%20HD%20650",
            license = "CC BY-SA 4.0",
            targetCurve = "Harman Over-Ear 2018",
            measurementRig = "GRAS 43AG / 45CA",
            algorithmVersion = "AutoEQ v4.4.1"
        ),
        AcousticProfile(
            id = "sennheiser_hd560s_harman",
            name = "Sennheiser HD 560S",
            subtitle = "Harman Over-Ear 2018 · Oratory1990",
            description = "Acoustic correction for Sennheiser HD 560S measured on GRAS 43AG. Balances linear sub-bass and smooths 4.6 kHz peak.",
            category = TuningCategory.MEASUREMENT_CORRECTION,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "Sennheiser",
            model = "HD 560S",
            preampGainDb = -5.5f,
            filters = listOf(
                ParametricFilter("f1", FilterType.LOW_SHELF, frequencyHz = 105f, gainDb = 5.0f, qFactor = 0.71f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 1400f, gainDb = -1.8f, qFactor = 2.0f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 4600f, gainDb = -3.5f, qFactor = 3.0f),
                ParametricFilter("f4", FilterType.PEAKING, frequencyHz = 7200f, gainDb = -2.5f, qFactor = 3.5f),
                ParametricFilter("f5", FilterType.HIGH_SHELF, frequencyHz = 10000f, gainDb = 1.5f, qFactor = 0.71f)
            ),
            author = "Oratory1990",
            sourceUrl = "https://github.com/jaakkopasanen/AutoEq/tree/master/results/oratory1990/harman_over-ear_2018/Sennheiser%20HD%20560S",
            license = "CC BY-SA 4.0",
            targetCurve = "Harman Over-Ear 2018",
            measurementRig = "GRAS 43AG",
            algorithmVersion = "AutoEQ v4.4.1"
        ),
        AcousticProfile(
            id = "sony_wh1000xm4_harman",
            name = "Sony WH-1000XM4",
            subtitle = "Harman Over-Ear 2018 · Oratory1990",
            description = "Acoustic correction for Sony WH-1000XM4 measured on GRAS 43AG by Oratory1990. Tames heavy 160 Hz bass boom and restores 3.1 kHz vocal presence.",
            category = TuningCategory.MEASUREMENT_CORRECTION,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "Sony",
            model = "WH-1000XM4",
            preampGainDb = -5.5f,
            filters = listOf(
                ParametricFilter("f1", FilterType.PEAKING, frequencyHz = 45f, gainDb = 3.0f, qFactor = 1.2f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 160f, gainDb = -4.8f, qFactor = 1.0f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 650f, gainDb = -2.2f, qFactor = 1.8f),
                ParametricFilter("f4", FilterType.PEAKING, frequencyHz = 1400f, gainDb = 1.8f, qFactor = 1.5f),
                ParametricFilter("f5", FilterType.PEAKING, frequencyHz = 3100f, gainDb = 5.2f, qFactor = 1.4f),
                ParametricFilter("f6", FilterType.PEAKING, frequencyHz = 6200f, gainDb = -4.0f, qFactor = 2.5f),
                ParametricFilter("f7", FilterType.HIGH_SHELF, frequencyHz = 9000f, gainDb = 2.0f, qFactor = 0.71f)
            ),
            author = "Oratory1990",
            sourceUrl = "https://github.com/jaakkopasanen/AutoEq/tree/master/results/oratory1990/harman_over-ear_2018/Sony%20WH-1000XM4",
            license = "CC BY-SA 4.0",
            targetCurve = "Harman Over-Ear 2018",
            measurementRig = "GRAS 43AG / 45CA",
            algorithmVersion = "AutoEQ v4.4.1"
        ),
        AcousticProfile(
            id = "sony_wh1000xm5_harman",
            name = "Sony WH-1000XM5",
            subtitle = "Harman Over-Ear 2018 · AutoEQ",
            description = "Acoustic correction for Sony WH-1000XM5 measured on standardized artificial ear coupler. Controls mid-bass muddiness and opens up upper mids.",
            category = TuningCategory.MEASUREMENT_CORRECTION,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "Sony",
            model = "WH-1000XM5",
            preampGainDb = -4.8f,
            filters = listOf(
                ParametricFilter("f1", FilterType.PEAKING, frequencyHz = 60f, gainDb = 2.5f, qFactor = 1.1f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 180f, gainDb = -3.8f, qFactor = 1.2f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 1100f, gainDb = 1.5f, qFactor = 1.4f),
                ParametricFilter("f4", FilterType.PEAKING, frequencyHz = 3200f, gainDb = 4.5f, qFactor = 1.5f),
                ParametricFilter("f5", FilterType.PEAKING, frequencyHz = 7000f, gainDb = -3.2f, qFactor = 2.8f),
                ParametricFilter("f6", FilterType.HIGH_SHELF, frequencyHz = 10000f, gainDb = 1.5f, qFactor = 0.71f)
            ),
            author = "AutoEQ / Jaakko Pasanen",
            sourceUrl = "https://github.com/jaakkopasanen/AutoEq/tree/master/results/rtings/harman_over-ear_2018/Sony%20WH-1000XM5",
            license = "MIT",
            targetCurve = "Harman Over-Ear 2018",
            measurementRig = "IEC 60318-4 Coupler",
            algorithmVersion = "AutoEQ v4.4.1"
        ),
        AcousticProfile(
            id = "sony_wf1000xm4_harman",
            name = "Sony WF-1000XM4",
            subtitle = "Harman In-Ear 2019 v2 · Oratory1990",
            description = "Acoustic correction for Sony WF-1000XM4 TWS measured on GRAS 43AG. Relaxes congested lower mids and restores open vocal presence.",
            category = TuningCategory.MEASUREMENT_CORRECTION,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "Sony",
            model = "WF-1000XM4",
            preampGainDb = -4.2f,
            filters = listOf(
                ParametricFilter("f1", FilterType.PEAKING, frequencyHz = 250f, gainDb = -2.8f, qFactor = 1.2f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 1200f, gainDb = 1.8f, qFactor = 1.5f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 2800f, gainDb = 3.5f, qFactor = 1.8f),
                ParametricFilter("f4", FilterType.PEAKING, frequencyHz = 5800f, gainDb = -3.5f, qFactor = 3.0f)
            ),
            author = "Oratory1990",
            sourceUrl = "https://github.com/jaakkopasanen/AutoEq/tree/master/results/oratory1990/harman_in-ear_2019v2/Sony%20WF-1000XM4",
            license = "CC BY-SA 4.0",
            targetCurve = "Harman In-Ear 2019 v2",
            measurementRig = "GRAS 43AG",
            algorithmVersion = "AutoEQ v4.4.1"
        ),
        AcousticProfile(
            id = "bose_qc35_ii_harman",
            name = "Bose QuietComfort 35 II",
            subtitle = "Harman Over-Ear 2018 · Oratory1990",
            description = "Acoustic correction for Bose QC 35 II measured on GRAS 43AG by Oratory1990. Adds sub-bass impact and removes 5.8 kHz resonance.",
            category = TuningCategory.MEASUREMENT_CORRECTION,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "Bose",
            model = "QuietComfort 35 II",
            preampGainDb = -4.5f,
            filters = listOf(
                ParametricFilter("f1", FilterType.LOW_SHELF, frequencyHz = 105f, gainDb = 4.0f, qFactor = 0.71f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 220f, gainDb = -2.5f, qFactor = 1.2f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 1200f, gainDb = 1.8f, qFactor = 1.8f),
                ParametricFilter("f4", FilterType.PEAKING, frequencyHz = 3200f, gainDb = 2.5f, qFactor = 2.0f),
                ParametricFilter("f5", FilterType.PEAKING, frequencyHz = 5800f, gainDb = -4.5f, qFactor = 3.5f),
                ParametricFilter("f6", FilterType.HIGH_SHELF, frequencyHz = 10000f, gainDb = 1.5f, qFactor = 0.71f)
            ),
            author = "Oratory1990",
            sourceUrl = "https://github.com/jaakkopasanen/AutoEq/tree/master/results/oratory1990/harman_over-ear_2018/Bose%20QuietComfort%2035%20II",
            license = "CC BY-SA 4.0",
            targetCurve = "Harman Over-Ear 2018",
            measurementRig = "GRAS 43AG",
            algorithmVersion = "AutoEQ v4.4.1"
        ),
        AcousticProfile(
            id = "bose_qc45_harman",
            name = "Bose QuietComfort 45",
            subtitle = "Harman Over-Ear 2018 · Oratory1990",
            description = "Acoustic correction for Bose QC 45 measured on GRAS 43AG. Neutralizes aggressive 3-5 kHz treble shelf for smooth long-session listening.",
            category = TuningCategory.MEASUREMENT_CORRECTION,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "Bose",
            model = "QuietComfort 45",
            preampGainDb = -4.8f,
            filters = listOf(
                ParametricFilter("f1", FilterType.LOW_SHELF, frequencyHz = 105f, gainDb = 3.5f, qFactor = 0.71f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 180f, gainDb = -2.0f, qFactor = 1.4f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 3800f, gainDb = -4.5f, qFactor = 2.0f),
                ParametricFilter("f4", FilterType.HIGH_SHELF, frequencyHz = 8500f, gainDb = 2.0f, qFactor = 0.71f)
            ),
            author = "Oratory1990",
            sourceUrl = "https://github.com/jaakkopasanen/AutoEq/tree/master/results/oratory1990/harman_over-ear_2018/Bose%20QuietComfort%2045",
            license = "CC BY-SA 4.0",
            targetCurve = "Harman Over-Ear 2018",
            measurementRig = "GRAS 43AG",
            algorithmVersion = "AutoEQ v4.4.1"
        ),
        AcousticProfile(
            id = "apple_airpods_pro_harman",
            name = "Apple AirPods Pro",
            subtitle = "Harman In-Ear 2019 v2 · Oratory1990",
            description = "Empirical in-ear correction for Apple AirPods Pro measured on GRAS 43AG. Enhances deep bass impact and corrects 4.8 kHz ear canal reflection.",
            category = TuningCategory.MEASUREMENT_CORRECTION,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "Apple",
            model = "AirPods Pro",
            preampGainDb = -4.5f,
            filters = listOf(
                ParametricFilter("f1", FilterType.LOW_SHELF, frequencyHz = 100f, gainDb = 4.0f, qFactor = 0.71f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 240f, gainDb = -2.0f, qFactor = 1.3f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 1200f, gainDb = -1.5f, qFactor = 2.0f),
                ParametricFilter("f4", FilterType.PEAKING, frequencyHz = 4800f, gainDb = -3.5f, qFactor = 3.0f),
                ParametricFilter("f5", FilterType.PEAKING, frequencyHz = 7800f, gainDb = -4.2f, qFactor = 3.5f),
                ParametricFilter("f6", FilterType.HIGH_SHELF, frequencyHz = 10000f, gainDb = 2.5f, qFactor = 0.71f)
            ),
            author = "Oratory1990",
            sourceUrl = "https://github.com/jaakkopasanen/AutoEq/tree/master/results/oratory1990/harman_in-ear_2019v2/Apple%20AirPods%20Pro",
            license = "CC BY-SA 4.0",
            targetCurve = "Harman In-Ear 2019 v2",
            measurementRig = "GRAS 43AG",
            algorithmVersion = "AutoEQ v4.4.1"
        ),
        AcousticProfile(
            id = "apple_airpods_pro_2_harman",
            name = "Apple AirPods Pro 2",
            subtitle = "Harman In-Ear 2019 v2 · AutoEQ",
            description = "Acoustic correction for Apple AirPods Pro 2 measured on IEC 60318-4 compliant fixture. Fine-tunes sub-bass depth and ear-canal resonance.",
            category = TuningCategory.MEASUREMENT_CORRECTION,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "Apple",
            model = "AirPods Pro 2",
            preampGainDb = -3.5f,
            filters = listOf(
                ParametricFilter("f1", FilterType.LOW_SHELF, frequencyHz = 80f, gainDb = 3.2f, qFactor = 0.71f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 300f, gainDb = -1.8f, qFactor = 1.5f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 2600f, gainDb = 2.2f, qFactor = 2.0f),
                ParametricFilter("f4", FilterType.PEAKING, frequencyHz = 6200f, gainDb = -3.8f, qFactor = 3.0f),
                ParametricFilter("f5", FilterType.HIGH_SHELF, frequencyHz = 10000f, gainDb = 1.8f, qFactor = 0.71f)
            ),
            author = "AutoEQ / Crinacle",
            sourceUrl = "https://github.com/jaakkopasanen/AutoEq/tree/master/results/crinacle/harman_in-ear_2019v2/Apple%20AirPods%20Pro%202",
            license = "MIT",
            targetCurve = "Harman In-Ear 2019 v2",
            measurementRig = "IEC 60318-4 Coupler",
            algorithmVersion = "AutoEQ v4.4.1"
        ),
        AcousticProfile(
            id = "beyerdynamic_dt770_80_harman",
            name = "Beyerdynamic DT 770 Pro (80Ω)",
            subtitle = "Harman Over-Ear 2018 · Oratory1990",
            description = "Acoustic correction for DT 770 Pro 80 Ohm measured on GRAS 43AG. Neutralizes the sharp 5.9 kHz and 8.4 kHz treble peaks while leveling mid-range.",
            category = TuningCategory.MEASUREMENT_CORRECTION,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "Beyerdynamic",
            model = "DT 770 Pro 80 Ohm",
            preampGainDb = -5.0f,
            filters = listOf(
                ParametricFilter("f1", FilterType.LOW_SHELF, frequencyHz = 105f, gainDb = 4.2f, qFactor = 0.71f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 200f, gainDb = -3.0f, qFactor = 1.1f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 1800f, gainDb = 2.8f, qFactor = 1.6f),
                ParametricFilter("f4", FilterType.PEAKING, frequencyHz = 3500f, gainDb = -2.0f, qFactor = 2.5f),
                ParametricFilter("f5", FilterType.PEAKING, frequencyHz = 5900f, gainDb = -6.8f, qFactor = 4.5f),
                ParametricFilter("f6", FilterType.PEAKING, frequencyHz = 8400f, gainDb = -4.5f, qFactor = 3.8f),
                ParametricFilter("f7", FilterType.HIGH_SHELF, frequencyHz = 11000f, gainDb = 1.5f, qFactor = 0.71f)
            ),
            author = "Oratory1990",
            sourceUrl = "https://github.com/jaakkopasanen/AutoEq/tree/master/results/oratory1990/harman_over-ear_2018/Beyerdynamic%20DT%20770%2080%20Ohm",
            license = "CC BY-SA 4.0",
            targetCurve = "Harman Over-Ear 2018",
            measurementRig = "GRAS 43AG",
            algorithmVersion = "AutoEQ v4.4.1"
        ),
        AcousticProfile(
            id = "audio_technica_ath_m50x_harman",
            name = "Audio-Technica ATH-M50x",
            subtitle = "Harman Over-Ear 2018 · Oratory1990",
            description = "Acoustic correction for ATH-M50x measured on GRAS 43AG by Oratory1990. Cleans up congested upper bass and smooths piercing 9.2 kHz resonance.",
            category = TuningCategory.MEASUREMENT_CORRECTION,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "Audio-Technica",
            model = "ATH-M50x",
            preampGainDb = -4.8f,
            filters = listOf(
                ParametricFilter("f1", FilterType.PEAKING, frequencyHz = 80f, gainDb = -1.5f, qFactor = 1.2f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 180f, gainDb = -4.0f, qFactor = 1.0f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 450f, gainDb = 1.5f, qFactor = 1.8f),
                ParametricFilter("f4", FilterType.PEAKING, frequencyHz = 2200f, gainDb = 2.5f, qFactor = 2.0f),
                ParametricFilter("f5", FilterType.PEAKING, frequencyHz = 9200f, gainDb = -4.8f, qFactor = 3.5f)
            ),
            author = "Oratory1990",
            sourceUrl = "https://github.com/jaakkopasanen/AutoEq/tree/master/results/oratory1990/harman_over-ear_2018/Audio-Technica%20ATH-M50x",
            license = "CC BY-SA 4.0",
            targetCurve = "Harman Over-Ear 2018",
            measurementRig = "GRAS 43AG",
            algorithmVersion = "AutoEQ v4.4.1"
        ),
        AcousticProfile(
            id = "hifiman_sundara_harman",
            name = "Hifiman Sundara",
            subtitle = "Harman Over-Ear 2018 · Oratory1990",
            description = "Acoustic correction for planar magnetic Hifiman Sundara measured on GRAS 43AG. Provides linear sub-bass extension and balances lower treble.",
            category = TuningCategory.MEASUREMENT_CORRECTION,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "Hifiman",
            model = "Sundara",
            preampGainDb = -6.0f,
            filters = listOf(
                ParametricFilter("f1", FilterType.LOW_SHELF, frequencyHz = 80f, gainDb = 5.5f, qFactor = 0.71f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 750f, gainDb = -1.5f, qFactor = 1.6f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 2100f, gainDb = 2.2f, qFactor = 1.8f),
                ParametricFilter("f4", FilterType.PEAKING, frequencyHz = 6000f, gainDb = -3.8f, qFactor = 3.0f),
                ParametricFilter("f5", FilterType.HIGH_SHELF, frequencyHz = 10000f, gainDb = 1.5f, qFactor = 0.71f)
            ),
            author = "Oratory1990",
            sourceUrl = "https://github.com/jaakkopasanen/AutoEq/tree/master/results/oratory1990/harman_over-ear_2018/Hifiman%20Sundara",
            license = "CC BY-SA 4.0",
            targetCurve = "Harman Over-Ear 2018",
            measurementRig = "GRAS 43AG",
            algorithmVersion = "AutoEQ v4.4.1"
        ),
        AcousticProfile(
            id = "akg_k371_harman",
            name = "AKG K371",
            subtitle = "Harman Over-Ear 2018 · Oratory1990",
            description = "Acoustic correction for AKG K371 studio monitor measured on GRAS 43AG. Refines sub-bass linearity and 3.8 kHz upper midrange.",
            category = TuningCategory.MEASUREMENT_CORRECTION,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "AKG",
            model = "K371",
            preampGainDb = -3.8f,
            filters = listOf(
                ParametricFilter("f1", FilterType.PEAKING, frequencyHz = 65f, gainDb = -2.5f, qFactor = 1.2f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 250f, gainDb = 1.5f, qFactor = 1.5f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 3800f, gainDb = -2.0f, qFactor = 2.5f),
                ParametricFilter("f4", FilterType.HIGH_SHELF, frequencyHz = 8500f, gainDb = 2.0f, qFactor = 0.71f)
            ),
            author = "Oratory1990",
            sourceUrl = "https://github.com/jaakkopasanen/AutoEq/tree/master/results/oratory1990/harman_over-ear_2018/AKG%20K371",
            license = "CC BY-SA 4.0",
            targetCurve = "Harman Over-Ear 2018",
            measurementRig = "GRAS 43AG",
            algorithmVersion = "AutoEQ v4.4.1"
        ),
        AcousticProfile(
            id = "moondrop_blessing_2_harman",
            name = "Moondrop Blessing 2",
            subtitle = "Harman In-Ear 2019 v2 · Crinacle",
            description = "Acoustic correction for Moondrop Blessing 2 IEM measured on IEC 60318-4 coupler. Adds satisfying sub-bass punch and smooths 6 kHz peak.",
            category = TuningCategory.MEASUREMENT_CORRECTION,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "Moondrop",
            model = "Blessing 2",
            preampGainDb = -4.8f,
            filters = listOf(
                ParametricFilter("f1", FilterType.LOW_SHELF, frequencyHz = 105f, gainDb = 4.5f, qFactor = 0.71f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 2800f, gainDb = -2.0f, qFactor = 2.5f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 6000f, gainDb = -3.0f, qFactor = 3.0f),
                ParametricFilter("f4", FilterType.HIGH_SHELF, frequencyHz = 9000f, gainDb = 1.5f, qFactor = 0.71f)
            ),
            author = "Crinacle / AutoEQ",
            sourceUrl = "https://github.com/jaakkopasanen/AutoEq/tree/master/results/crinacle/harman_in-ear_2019v2/Moondrop%20Blessing%202",
            license = "MIT",
            targetCurve = "Harman In-Ear 2019 v2",
            measurementRig = "IEC 60318-4 Coupler",
            algorithmVersion = "AutoEQ v4.4.1"
        ),
        AcousticProfile(
            id = "salnotes_zero_harman",
            name = "7Hz Salnotes Zero",
            subtitle = "Harman In-Ear 2019 v2 · Crinacle",
            description = "Acoustic calibration for budget reference 7Hz Zero measured on IEC 60318-4 coupler. Adds warmth and corrects 6 kHz resonance.",
            category = TuningCategory.MEASUREMENT_CORRECTION,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "7Hz",
            model = "Salnotes Zero",
            preampGainDb = -3.5f,
            filters = listOf(
                ParametricFilter("f1", FilterType.LOW_SHELF, frequencyHz = 100f, gainDb = 2.5f, qFactor = 0.71f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 3000f, gainDb = -1.5f, qFactor = 2.0f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 6000f, gainDb = -2.5f, qFactor = 3.0f),
                ParametricFilter("f4", FilterType.HIGH_SHELF, frequencyHz = 10000f, gainDb = 1.5f, qFactor = 0.71f)
            ),
            author = "Crinacle / AutoEQ",
            sourceUrl = "https://github.com/jaakkopasanen/AutoEq/tree/master/results/crinacle/harman_in-ear_2019v2/7Hz%20Zero",
            license = "MIT",
            targetCurve = "Harman In-Ear 2019 v2",
            measurementRig = "IEC 60318-4 Coupler",
            algorithmVersion = "AutoEQ v4.4.1"
        ),
        AcousticProfile(
            id = "samsung_galaxy_buds_pro_harman",
            name = "Samsung Galaxy Buds Pro",
            subtitle = "Harman In-Ear 2019 v2 · Oratory1990",
            description = "Acoustic calibration for Galaxy Buds Pro measured on GRAS 43AG. Balances upper midrange and linearizes treble.",
            category = TuningCategory.MEASUREMENT_CORRECTION,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "Samsung",
            model = "Galaxy Buds Pro",
            preampGainDb = -3.8f,
            filters = listOf(
                ParametricFilter("f1", FilterType.LOW_SHELF, frequencyHz = 90f, gainDb = 3.0f, qFactor = 0.71f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 500f, gainDb = -1.8f, qFactor = 1.5f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 3800f, gainDb = -2.5f, qFactor = 2.8f),
                ParametricFilter("f4", FilterType.PEAKING, frequencyHz = 6800f, gainDb = -3.5f, qFactor = 3.2f)
            ),
            author = "Oratory1990",
            sourceUrl = "https://github.com/jaakkopasanen/AutoEq/tree/master/results/oratory1990/harman_in-ear_2019v2/Samsung%20Galaxy%20Buds%20Pro",
            license = "CC BY-SA 4.0",
            targetCurve = "Harman In-Ear 2019 v2",
            measurementRig = "GRAS 43AG",
            algorithmVersion = "AutoEQ v4.4.1"
        )
    )

    // Community / OPRA Profiles
    val COMMUNITY_PROFILES: List<AcousticProfile> = listOf(
        AcousticProfile(
            id = "sennheiser_hd600_diffuse_field",
            name = "Sennheiser HD 600 (Diffuse Field)",
            subtitle = "ISO 11904-1 · OPRA / AutoEQ",
            description = "Community OPRA equalization tuning HD 600 to ISO 11904-1 Diffuse Field response for analytical studio checking.",
            category = TuningCategory.COMMUNITY_PROFILE,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "Sennheiser",
            model = "HD 600",
            preampGainDb = -3.8f,
            filters = listOf(
                ParametricFilter("f1", FilterType.PEAKING, frequencyHz = 1500f, gainDb = 2.0f, qFactor = 1.5f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 3200f, gainDb = 3.5f, qFactor = 2.0f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 6000f, gainDb = -3.0f, qFactor = 3.5f)
            ),
            author = "OPRA / AutoEQ Community",
            sourceUrl = "https://github.com/opra-project/OPRA",
            license = "CC BY-SA 4.0",
            targetCurve = "Diffuse Field ISO 11904-1",
            algorithmVersion = "OPRA Dist v1.0"
        ),
        AcousticProfile(
            id = "sennheiser_hd560s_relaxed",
            name = "Sennheiser HD 560S (Relaxed)",
            subtitle = "Smooth Treble · OPRA",
            description = "Community profile softening the 4.8 kHz presence region on HD 560S for fatigue-free listening.",
            category = TuningCategory.COMMUNITY_PROFILE,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "Sennheiser",
            model = "HD 560S",
            preampGainDb = -3.8f,
            filters = listOf(
                ParametricFilter("f1", FilterType.LOW_SHELF, frequencyHz = 105f, gainDb = 3.5f, qFactor = 0.71f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 4800f, gainDb = -3.0f, qFactor = 3.0f),
                ParametricFilter("f3", FilterType.HIGH_SHELF, frequencyHz = 8000f, gainDb = -1.5f, qFactor = 0.71f)
            ),
            author = "OPRA Community",
            sourceUrl = "https://github.com/opra-project/OPRA",
            license = "CC BY-SA 4.0",
            targetCurve = "Relaxed Listening Contour",
            algorithmVersion = "OPRA Dist v1.0"
        ),
        AcousticProfile(
            id = "sony_wh1000xm4_warm_audiophile",
            name = "Sony WH-1000XM4 (Warm Audiophile)",
            subtitle = "Super* Review · OPRA",
            description = "Community-authored profile relaxing high-treble fatigue while retaining full bass dynamics.",
            category = TuningCategory.COMMUNITY_PROFILE,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "Sony",
            model = "WH-1000XM4",
            preampGainDb = -4.5f,
            filters = listOf(
                ParametricFilter("f1", FilterType.PEAKING, frequencyHz = 180f, gainDb = -3.5f, qFactor = 1.2f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 1200f, gainDb = 2.0f, qFactor = 1.6f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 3000f, gainDb = 3.8f, qFactor = 1.4f),
                ParametricFilter("f4", FilterType.HIGH_SHELF, frequencyHz = 7500f, gainDb = -2.5f, qFactor = 0.71f)
            ),
            author = "Super* Review (OPRA)",
            sourceUrl = "https://github.com/opra-project/OPRA",
            license = "CC BY-SA 4.0",
            targetCurve = "Warm Audiophile Relaxed Treble",
            algorithmVersion = "OPRA Dist v1.0"
        ),
        AcousticProfile(
            id = "bose_qc35_ii_neutral",
            name = "Bose QC 35 II (Neutral Audiophile)",
            subtitle = "Acoustic Clarity · OPRA",
            description = "Community profile linearizing midrange response on Bose QC 35 II for neutral acoustic reproduction.",
            category = TuningCategory.COMMUNITY_PROFILE,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "Bose",
            model = "QuietComfort 35 II",
            preampGainDb = -3.8f,
            filters = listOf(
                ParametricFilter("f1", FilterType.PEAKING, frequencyHz = 180f, gainDb = -2.5f, qFactor = 1.2f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 2800f, gainDb = 2.5f, qFactor = 1.8f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 6200f, gainDb = -3.0f, qFactor = 2.5f)
            ),
            author = "OPRA Community",
            sourceUrl = "https://github.com/opra-project/OPRA",
            license = "CC BY-SA 4.0",
            targetCurve = "Neutral Acoustic Tilt",
            algorithmVersion = "OPRA Dist v1.0"
        ),
        AcousticProfile(
            id = "airpods_pro_vocal_forward",
            name = "AirPods Pro (Vocal Forward)",
            subtitle = "Speech Intelligibility · OPRA",
            description = "Community profile specifically tuned for speech intelligibility, audiobooks, and conference calls on AirPods Pro.",
            category = TuningCategory.COMMUNITY_PROFILE,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "Apple",
            model = "AirPods Pro",
            preampGainDb = -4.5f,
            filters = listOf(
                ParametricFilter("f1", FilterType.HIGH_PASS, frequencyHz = 60f, gainDb = 0f, qFactor = 0.71f),
                ParametricFilter("f2", FilterType.PEAKING, frequencyHz = 280f, gainDb = -2.5f, qFactor = 1.4f),
                ParametricFilter("f3", FilterType.PEAKING, frequencyHz = 2600f, gainDb = 4.2f, qFactor = 1.6f),
                ParametricFilter("f4", FilterType.PEAKING, frequencyHz = 6500f, gainDb = -3.0f, qFactor = 2.5f)
            ),
            author = "OPRA Community",
            sourceUrl = "https://github.com/opra-project/OPRA",
            license = "CC BY-SA 4.0",
            targetCurve = "Speech Formant Focus",
            algorithmVersion = "OPRA Dist v1.0"
        )
    )

    /**
     * Complete immutable catalog combining all categories.
     */
    val ALL_PROFILES: List<AcousticProfile> = buildList {
        add(PHONE_SPEAKER_OPTIMIZED)
        add(PHONE_SPEAKER_SPEECH)
        addAll(MEASUREMENT_PROFILES)
        addAll(COMMUNITY_PROFILES)
        addAll(DeterministicCreativeRecipes.RECIPES)
    }

    /**
     * Structured relational database instance populated from the verified catalog.
     */
    val database: AcousticProfileDao = InMemoryAcousticDatabase().also { db ->
        seedDatabase(db, ALL_PROFILES)
    }

    init {
        // Enforce validation for all built-in profiles at load time
        for (profile in ALL_PROFILES) {
            val result = ProfileValidationEngine.validate(profile)
            if (result is ValidationResult.Invalid) {
                System.err.println("Built-in profile ${profile.id} failed validation: ${result.errors}")
            }
        }
    }

    fun seedDatabase(db: AcousticProfileDao, profiles: List<AcousticProfile> = ALL_PROFILES) {
        val autoEqSource = ProfileSourceEntity("autoeq", "AutoEQ", "https://github.com/jaakkopasanen/AutoEq", "v4.4.1", "mit")
        val opraSource = ProfileSourceEntity("opra", "OPRA Project", "https://github.com/opra-project/OPRA", "v1.0", "cc_by_sa_4")
        val labSource = ProfileSourceEntity("evenlouder_lab", "EvenLouder Lab", "https://evenlouder.app", "v2.0", "apache_2")

        val licenseMap = mapOf(
            "cc_by_sa_4" to LicenseEntity("cc_by_sa_4", "CC BY-SA 4.0", "https://creativecommons.org/licenses/by-sa/4.0/", attributionRequired = true, shareAlike = true, commercialUsePermitted = true),
            "mit" to LicenseEntity("mit", "MIT License", "https://opensource.org/licenses/MIT", attributionRequired = true, shareAlike = false, commercialUsePermitted = true),
            "apache_2" to LicenseEntity("apache_2", "Apache 2.0", "https://www.apache.org/licenses/LICENSE-2.0", attributionRequired = true, shareAlike = false, commercialUsePermitted = true)
        )

        for (p in profiles) {
            val manufId = p.manufacturer.lowercase().replace(Regex("[^a-z0-9]"), "_").ifBlank { "generic" }
            val manufacturer = ManufacturerEntity(manufId, p.manufacturer.ifBlank { "Generic" })
            val deviceId = "${manufId}_${p.model.lowercase().replace(Regex("[^a-z0-9]"), "_").ifBlank { "generic" }}"
            val device = DeviceEntity(deviceId, manufId, p.model.ifBlank { "Universal" }, p.targetDeviceType)

            val targetId = p.targetCurve.lowercase().replace(Regex("[^a-z0-9]"), "_").ifBlank { "unspecified" }
            val target = AcousticTargetEntity(targetId, p.targetCurve.ifBlank { "Default Target" })

            val measurement = if (p.measurementRig.isNotBlank() || p.author.isNotBlank()) {
                MeasurementEntity("${p.id}_meas", deviceId, p.author, p.measurementRig, p.sourceUrl)
            } else null

            val source = when (p.category) {
                TuningCategory.MEASUREMENT_CORRECTION -> autoEqSource
                TuningCategory.COMMUNITY_PROFILE -> opraSource
                else -> labSource
            }
            val license = licenseMap[source.licenseId] ?: licenseMap["apache_2"]!!

            val profileEntity = AcousticProfileEntity(
                id = p.id,
                name = p.name,
                subtitle = p.subtitle,
                description = p.description,
                category = p.category,
                deviceId = deviceId,
                targetId = targetId,
                measurementId = measurement?.id,
                sourceId = source.id,
                preampGainDb = p.preampGainDb,
                algorithmVersion = p.algorithmVersion,
                validationStatus = p.validationStatus,
                isCalibrated = p.category == TuningCategory.MEASUREMENT_CORRECTION,
                createdAt = p.createdAt
            )

            val filterEntities = p.filters.mapIndexed { idx, f ->
                ParametricFilterEntity(
                    id = "${p.id}_f${idx + 1}",
                    profileId = p.id,
                    bandIndex = idx,
                    filterType = f.type,
                    frequencyHz = f.frequencyHz,
                    gainDb = f.gainDb,
                    qFactor = f.qFactor,
                    enabled = f.enabled,
                    slope = f.slope
                )
            }

            db.insertProfileWithDetails(
                ProfileWithDetails(
                    profile = profileEntity,
                    device = device,
                    manufacturer = manufacturer,
                    target = target,
                    measurement = measurement,
                    source = source,
                    license = license,
                    filters = filterEntities
                )
            )
        }
    }

    fun findById(id: String): AcousticProfile? =
        database.getProfileById(id) ?: ALL_PROFILES.firstOrNull { it.id == id }

    fun filterByCategory(category: TuningCategory): List<AcousticProfile> =
        database.getProfilesByCategory(category)

    fun search(query: String): List<AcousticProfile> =
        database.searchProfiles(query)
}
