package com.evenlouder.app

import com.evenlouder.app.core.dsp.*
import org.junit.Assert.*
import org.junit.Test

class BandFittingTest {

    @Test
    fun testBandCountIsExactly23() {
        val profile = AcousticProfileRepository.findById("sennheiser_hd600_harman")
        assertNotNull(profile)

        val result = ParametricBandFitter.fit(profile, hpfHz = 80f)
        assertEquals(
            "Fitted Pre-EQ bands must exactly match DynamicsProcessing 23-band architecture",
            23,
            result.fittedPoints.size
        )
    }

    @Test
    fun testHarmanCorrectionYieldsHighFidelityFit() {
        val hd600 = AcousticProfileRepository.findById("sennheiser_hd600_harman")
        assertNotNull(hd600)

        val result = ParametricBandFitter.fit(hd600, hpfHz = 80f)

        // RMS error on smooth Harman target corrections with 5 biquads should be well under 0.8 dB
        assertTrue(
            "RMS error must be < 0.8 dB (actual: ${result.rmsErrorDb} dB)",
            result.rmsErrorDb < 0.8
        )
        assertTrue(
            "Fit quality must be EXCELLENT or GOOD",
            result.quality == FitQuality.EXCELLENT || result.quality == FitQuality.GOOD
        )
    }

    @Test
    fun testSubsonicProtectionRollOffEnforcement() {
        val phoneProfile = AcousticProfileRepository.PHONE_SPEAKER_OPTIMIZED
        val hpf = 140f

        val result = ParametricBandFitter.fit(phoneProfile, hpfHz = hpf)
        val points = result.fittedPoints

        // 3 subsonic transducer protection bands
        assertEquals(hpf * 0.5f, points[0].edgeHz, 0.01f)
        assertEquals(-24f, points[0].gainDb, 0.01f)

        assertEquals(hpf * 0.75f, points[1].edgeHz, 0.01f)
        assertEquals(-14f, points[1].gainDb, 0.01f)

        assertEquals(hpf, points[2].edgeHz, 0.01f)
        assertEquals(-6f, points[2].gainDb, 0.01f)

        // All subsequent bands must stay above HPF cutoff
        for (i in 3 until points.size) {
            assertTrue(
                "Band $i (${points[i].edgeHz} Hz) must be strictly >= HPF ($hpf Hz)",
                points[i].edgeHz >= hpf
            )
        }
    }

    @Test
    fun testGainClampingWithinHardwareLimits() {
        // Create an extreme synthetic profile with massive gains
        val extremeProfile = AcousticProfile(
            id = "extreme_test",
            name = "Extreme Test",
            subtitle = "Test",
            description = "Test",
            category = TuningCategory.CREATIVE_SIGNATURE,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = "Test",
            model = "Test",
            preampGainDb = 0f,
            filters = listOf(
                ParametricFilter("f1", FilterType.PEAKING, 1000f, gainDb = 35f, qFactor = 1.0f),
                ParametricFilter("f2", FilterType.PEAKING, 3000f, gainDb = -40f, qFactor = 1.0f)
            ),
            author = "Test",
            sourceUrl = "",
            license = "MIT",
            targetCurve = "Test",
            algorithmVersion = "1.0"
        )

        val result = ParametricBandFitter.fit(
            extremeProfile,
            hpfHz = 100f,
            manualOffsets = { 10f } // Additional +10 dB manual offset
        )

        // The first 3 bands are fixed subsonic roll-off (-24 dB, -14 dB, -6 dB).
        // The remaining 20 Pre-EQ active bands must be clamped strictly within [-18 dB, +15 dB].
        for (i in 3 until result.fittedPoints.size) {
            val point = result.fittedPoints[i]
            assertTrue(
                "Fitted active band at ${point.edgeHz} Hz (${point.gainDb} dB) must be >= -18 dB",
                point.gainDb >= -18.01f
            )
            assertTrue(
                "Fitted active band at ${point.edgeHz} Hz (${point.gainDb} dB) must be <= +15 dB",
                point.gainDb <= 15.01f
            )
        }
    }

    @Test
    fun testManualOffsetIntegration() {
        val hd600 = AcousticProfileRepository.findById("sennheiser_hd600_harman")
        assertNotNull(hd600)

        // Fit without manual offset
        val baseResult = ParametricBandFitter.fit(hd600, hpfHz = 80f, manualOffsets = { 0f })

        // Fit with +4 dB bass offset below 250 Hz
        val offsetResult = ParametricBandFitter.fit(
            hd600,
            hpfHz = 80f,
            manualOffsets = { freq -> if (freq <= 250f) 4f else 0f }
        )

        // Compare points between 80 Hz and 250 Hz
        for (i in 3 until baseResult.fittedPoints.size) {
            val basePt = baseResult.fittedPoints[i]
            val offsetPt = offsetResult.fittedPoints[i]
            if (basePt.edgeHz <= 250f) {
                val diff = offsetPt.gainDb - basePt.gainDb
                assertEquals(
                    "Offset at ${basePt.edgeHz} Hz should be approximately +4 dB",
                    4f,
                    diff,
                    0.1f
                )
            }
        }
    }

    @Test
    fun testEqLayoutIntegrationWithParametricBandFitter() {
        val config = DspConfig(
            eqEnabled = true,
            activeProfileId = "sony_wh1000xm4_harman",
            highPassCutoffHz = 80f,
            bassGainDb = 2.0f
        )

        val fitResult = EqLayout.evaluateFit(config)
        assertTrue("Sony XM4 fit must be evaluated", fitResult.rmsErrorDb >= 0.0)
        assertTrue(
            "Fit quality must be valid",
            fitResult.quality == FitQuality.EXCELLENT || fitResult.quality == FitQuality.GOOD
        )

        // Build EqLayout points
        val eqPoints = EqLayout.build(config)
        assertEquals(23, eqPoints.size)
    }
}
