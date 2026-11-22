package com.evenlouder.app

import com.evenlouder.app.core.dsp.*
import org.junit.Assert.*
import org.junit.Test

class ProfileValidationTest {

    @Test
    fun testAllBuiltInProfilesPassValidation() {
        for (profile in AcousticProfileRepository.ALL_PROFILES) {
            val result = ProfileValidationEngine.validate(profile)
            assertTrue(
                "Profile '${profile.name}' (${profile.id}) must be fully valid: ${(result as? ValidationResult.Invalid)?.errors}",
                result.isValid
            )
        }
    }

    @Test
    fun testBlankIdOrNameRejected() {
        val blankId = AcousticProfile(
            id = "",
            name = "Test",
            description = "Test desc",
            category = TuningCategory.CREATIVE_SIGNATURE
        )
        val result1 = ProfileValidationEngine.validate(blankId)
        assertTrue("Blank ID must fail validation", result1 is ValidationResult.Invalid)

        val blankName = AcousticProfile(
            id = "valid_id",
            name = "",
            description = "Test desc",
            category = TuningCategory.CREATIVE_SIGNATURE
        )
        val result2 = ProfileValidationEngine.validate(blankName)
        assertTrue("Blank Name must fail validation", result2 is ValidationResult.Invalid)
    }

    @Test
    fun testFrequencyBeyondNyquistRejected() {
        val invalidFreq = AcousticProfile(
            id = "invalid_freq_test",
            name = "Invalid Frequency Test",
            description = "Test",
            category = TuningCategory.CREATIVE_SIGNATURE,
            filters = listOf(
                ParametricFilter("f1", FilterType.PEAKING, frequencyHz = 25000f, gainDb = 3f, qFactor = 1.0f)
            )
        )
        val result = ProfileValidationEngine.validate(invalidFreq, sampleRateHz = 48000.0) // Nyquist = 24000 Hz
        assertTrue("Frequency above Nyquist must be rejected", result is ValidationResult.Invalid)
        val errors = (result as ValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("outside usable range") })
    }

    @Test
    fun testExcessiveGainOrQRejected() {
        val invalidQ = AcousticProfile(
            id = "invalid_q_test",
            name = "Invalid Q Test",
            description = "Test",
            category = TuningCategory.CREATIVE_SIGNATURE,
            filters = listOf(
                ParametricFilter("f1", FilterType.PEAKING, frequencyHz = 1000f, gainDb = 3f, qFactor = 0.01f)
            )
        )
        val resultQ = ProfileValidationEngine.validate(invalidQ)
        assertTrue("Excessively low Q must be rejected", resultQ is ValidationResult.Invalid)

        val invalidGain = AcousticProfile(
            id = "invalid_gain_test",
            name = "Invalid Gain Test",
            description = "Test",
            category = TuningCategory.CREATIVE_SIGNATURE,
            filters = listOf(
                ParametricFilter("f1", FilterType.PEAKING, frequencyHz = 1000f, gainDb = 25f, qFactor = 1.0f)
            )
        )
        val resultGain = ProfileValidationEngine.validate(invalidGain)
        assertTrue("Excessive gain (> 20 dB) must be rejected", resultGain is ValidationResult.Invalid)
    }

    @Test
    fun testDigitalClippingDetection() {
        val clippingProfile = AcousticProfile(
            id = "clipping_test",
            name = "Clipping Profile",
            description = "Has 6 dB boost but 0 dB preamp",
            category = TuningCategory.CREATIVE_SIGNATURE,
            preampGainDb = 0f, // No preamp compensation
            filters = listOf(
                ParametricFilter("f1", FilterType.PEAKING, frequencyHz = 1000f, gainDb = 6f, qFactor = 1.4f)
            )
        )
        val result = ProfileValidationEngine.validate(clippingProfile)
        assertTrue("Profile with uncompensated peak gain must be flagged for digital clipping", result is ValidationResult.Invalid)
        val errors = (result as ValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("Potential digital clipping") })
    }

    @Test
    fun testRequiredHeadroomPreampCalculation() {
        val profile = AcousticProfile(
            id = "headroom_test",
            name = "Headroom Test",
            description = "Test",
            category = TuningCategory.CREATIVE_SIGNATURE,
            filters = listOf(
                ParametricFilter("f1", FilterType.PEAKING, frequencyHz = 1000f, gainDb = 6f, qFactor = 1.4f)
            )
        )
        val peakGain = profile.calculatePeakGainDb(48000.0)
        assertEquals(6.0f, peakGain, 0.1f)

        val recPreamp = profile.recommendedPreampDb(48000.0)
        assertEquals(-6.2f, recPreamp, 0.1f)
        assertTrue("Recommended preamp must be negative and compensate peak", recPreamp <= -6.0f)
    }
}
