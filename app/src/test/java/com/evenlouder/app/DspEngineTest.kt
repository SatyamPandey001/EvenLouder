package com.evenlouder.app

import com.evenlouder.app.core.dsp.DspConfig
import com.evenlouder.app.core.dsp.TuningProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DspEngineTest {

    @Test
    fun testAndroid15AidlBandLimitCompliance() {
        val totalBands = DspConfig.TOTAL_CONFIGURED_BANDS
        assertTrue(
            "Total effect bands ($totalBands) must strictly not exceed AIDL limit (${DspConfig.MAX_ALLOWED_AIDL_BANDS})",
            totalBands <= DspConfig.MAX_ALLOWED_AIDL_BANDS
        )
        // 6 Pre-EQ + 3 MBC = 9 bands
        assertEquals(9, totalBands)
    }

    @Test
    fun testAcousticSafetyProfiles() {
        for (profile in TuningProfile.values()) {
            val cfg = profile.config
            if (cfg.enabled) {
                assertTrue(
                    "${profile.name}: High pass cutoff (${cfg.highPassCutoffHz}) must be >= 100Hz",
                    cfg.highPassCutoffHz >= 100f
                )

                assertTrue(
                    "${profile.name}: Limiter ceiling (${cfg.limiterCeilingDb}) must be < 0.0 dBFS",
                    cfg.limiterCeilingDb < 0.0f
                )

                assertTrue(
                    "${profile.name}: Vocal clarity (${cfg.vocalClarityDb}) must be between 0 and 6 dB",
                    cfg.vocalClarityDb in 0.0f..6.0f
                )

                assertTrue(
                    "${profile.name}: Bass gain (${cfg.bassGainDb}) must be between 0 and 6 dB",
                    cfg.bassGainDb in 0.0f..6.0f
                )

                assertTrue(
                    "${profile.name}: Boxiness cut (${cfg.boxinessCutDb}) must be between -6 and 0 dB",
                    cfg.boxinessCutDb in -6.0f..0.0f
                )

                assertTrue(
                    "${profile.name}: Treble gain (${cfg.trebleGainDb}) must be between 0 and 6 dB",
                    cfg.trebleGainDb in 0.0f..6.0f
                )

                assertTrue(
                    "${profile.name}: Loudness makeup gain (${cfg.loudnessGainDb}) must be <= 6 dB",
                    cfg.loudnessGainDb in 0.0f..6.0f
                )
            }
        }
    }
}
