package com.evenlouder.app

import com.evenlouder.app.core.dsp.*
import com.evenlouder.app.ui.components.matchesConfig
import org.junit.Assert.*
import org.junit.Test

class ProfileDifferentiationTest {

    @Test
    fun testSelfComparisonYieldsZeroDistance() {
        val hd600 = AcousticProfileRepository.MEASUREMENT_PROFILES[0]
        val result = ProfileComparator.compare(hd600, hd600)
        assertEquals("Self comparison RMS difference must be 0 dB", 0.0, result.rmsSpectralDifferenceDb, 0.001)
        assertEquals("Self comparison Max difference must be 0 dB", 0.0, result.maxAbsoluteDifferenceDb, 0.001)
        assertTrue("Self comparison is marked as indistinct", result.isPerceptuallyIndistinct)
    }

    @Test
    fun testDistinctProfilesYieldSignificantSpectralDistance() {
        val hd600 = AcousticProfileRepository.findById("sennheiser_hd600_harman")!! // Neutral reference headphone
        val xm4 = AcousticProfileRepository.findById("sony_wh1000xm4_harman")!!   // Sony XM4 (Heavy bass cut and ear-gain boost)

        val result = ProfileComparator.compare(hd600, xm4)
        assertFalse("HD 600 and Sony XM4 are distinct acoustic profiles", result.isPerceptuallyIndistinct)
        assertTrue("RMS difference must be substantial (> 1.5 dB)", result.rmsSpectralDifferenceDb > 1.5)
        assertTrue("Max peak difference must be substantial (> 3.0 dB)", result.maxAbsoluteDifferenceDb > 3.0)

        // Compare Deep Bass vs Treble Air
        val deepBass = DeterministicCreativeRecipes.RECIPES[0]
        val trebleAir = DeterministicCreativeRecipes.RECIPES[3]
        val creativeDiff = ProfileComparator.compare(deepBass, trebleAir)
        assertFalse("Deep Bass and Treble Air must be clearly distinguishable", creativeDiff.isPerceptuallyIndistinct)
        assertTrue(creativeDiff.rmsSpectralDifferenceDb > 3.0)
    }

    @Test
    fun testDuplicateDetectionFlagsNearlyIdenticalProfiles() {
        val profileA = AcousticProfile(
            id = "profile_a",
            name = "Profile A",
            description = "A",
            category = TuningCategory.CREATIVE_SIGNATURE,
            preampGainDb = -3.0f,
            filters = listOf(
                ParametricFilter("f1", FilterType.PEAKING, frequencyHz = 1000f, gainDb = 3.0f, qFactor = 1.4f)
            )
        )

        // Profile B has only a tiny 0.1 dB difference - perceptually inaudible
        val profileB = AcousticProfile(
            id = "profile_b",
            name = "Profile B (Clone)",
            description = "B",
            category = TuningCategory.CREATIVE_SIGNATURE,
            preampGainDb = -3.0f,
            filters = listOf(
                ParametricFilter("f1", FilterType.PEAKING, frequencyHz = 1000f, gainDb = 3.1f, qFactor = 1.4f)
            )
        )

        val comparison = ProfileComparator.compare(profileA, profileB)
        assertTrue("Tiny 0.1 dB difference must be detected as perceptually indistinct", comparison.isPerceptuallyIndistinct)
        assertTrue(comparison.rmsSpectralDifferenceDb < ProfileComparator.PERCEPTUAL_RMS_THRESHOLD_DB)

        val indistinctPairs = ProfileComparator.findIndistinctPairs(listOf(profileA, profileB))
        assertEquals(1, indistinctPairs.size)
        assertEquals("Profile A", indistinctPairs[0].first.name)
        assertEquals("Profile B (Clone)", indistinctPairs[0].second.name)
    }

    @Test
    fun testCatalogHasNoAccidentalDuplicates() {
        // Verify that in our built-in catalog, all profiles within the same category are acoustically distinct
        val measurementProfiles = AcousticProfileRepository.MEASUREMENT_PROFILES
        val indistinct = ProfileComparator.findIndistinctPairs(measurementProfiles)
        assertTrue(
            "Measurement profiles must all be acoustically distinct. Found duplicates: ${indistinct.map { "${it.first.name} <-> ${it.second.name}" }}",
            indistinct.isEmpty()
        )
    }

    @Test
    fun testProfileMatchingDistinguishesProfiles() {
        val baseConfig = DspConfig()
        val tested = mutableSetOf<String>()

        for (profile in TuningProfile.values()) {
            if (profile == TuningProfile.BYPASS) continue
            val config = baseConfig.applyingProfile(profile)
            assertTrue("Profile ${profile.name} must match its applied config", profile.matchesConfig(config))
            profile.acousticProfile?.id?.let { tested.add(it) }
        }

        // Specifically test that 80 Hz profiles do not falsely match Sennheiser HD 600
        val xm4Config = baseConfig.applyingProfile(TuningProfile.SONY_XM4)
        assertFalse("Sennheiser HD 600 must not match Sony XM4 config", TuningProfile.SENNHEISER_HD600.matchesConfig(xm4Config))
        assertTrue("Sony XM4 must match Sony XM4 config", TuningProfile.SONY_XM4.matchesConfig(xm4Config))

        val airpodsConfig = baseConfig.applyingProfile(TuningProfile.AIRPODS_PRO)
        assertFalse("Sennheiser HD 600 must not match AirPods Pro config", TuningProfile.SENNHEISER_HD600.matchesConfig(airpodsConfig))
        assertTrue("AirPods Pro must match AirPods Pro config", TuningProfile.AIRPODS_PRO.matchesConfig(airpodsConfig))
    }
}
