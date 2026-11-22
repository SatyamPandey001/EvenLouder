package com.evenlouder.app.core.dsp

/**
 * Result of validating an AcousticProfile.
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult()

    val isValid: Boolean get() = this is Valid
}

/**
 * Automated Profile Validation Engine enforcing DSP stability, physical acoustic boundaries,
 * licensing attribution, and clipping safety.
 */
object ProfileValidationEngine {

    const val MIN_FREQUENCY_HZ = 20.0
    const val MIN_Q = 0.05
    const val MAX_Q = 20.0
    const val MIN_GAIN_DB = -30.0
    const val MAX_GAIN_DB = 20.0
    const val MAX_CUMULATIVE_BOOST_DB = 18.0

    /**
     * Validates an entire AcousticProfile for DSP stability, audio safety, and data integrity.
     */
    fun validate(profile: AcousticProfile, sampleRateHz: Double = 48000.0): ValidationResult {
        val errors = mutableListOf<String>()

        if (profile.id.isBlank()) errors.add("Profile ID cannot be blank")
        if (profile.name.isBlank()) errors.add("Profile name cannot be blank")
        if (profile.license.isBlank()) errors.add("Profile license cannot be blank")
        if (profile.author.isBlank()) errors.add("Profile author/provenance cannot be blank")

        val nyquist = sampleRateHz / 2.0

        profile.filters.forEachIndexed { index, filter ->
            val filterName = filter.id.ifBlank { "Band #${index + 1}" }

            if (filter.frequencyHz < MIN_FREQUENCY_HZ || filter.frequencyHz >= nyquist) {
                errors.add("$filterName frequency (${filter.frequencyHz} Hz) outside usable range ($MIN_FREQUENCY_HZ Hz to ${nyquist.toInt()} Hz)")
            }

            if (filter.qFactor < MIN_Q || filter.qFactor > MAX_Q) {
                errors.add("$filterName Q-factor (${filter.qFactor}) outside safe bounds ($MIN_Q to $MAX_Q)")
            }

            if (filter.gainDb < MIN_GAIN_DB || filter.gainDb > MAX_GAIN_DB) {
                errors.add("$filterName gain (${filter.gainDb} dB) outside safe range ($MIN_GAIN_DB dB to $MAX_GAIN_DB dB)")
            }

            // Calculate and test biquad coefficients
            val coeffs = filter.toBiquad(sampleRateHz)
            if (!coeffs.isFinite) {
                errors.add("$filterName biquad calculation produced non-finite coefficients (NaN or Inf)")
            } else if (!coeffs.isStable) {
                errors.add("$filterName biquad filter is unstable (poles lie outside or on the unit circle)")
            }
        }

        // Test cumulative response
        if (profile.filters.isNotEmpty()) {
            val peakGain = profile.calculatePeakGainDb(sampleRateHz)
            if (peakGain > MAX_CUMULATIVE_BOOST_DB) {
                errors.add("Cumulative peak boost (+${"%.1f".format(peakGain)} dB) exceeds maximum limit ($MAX_CUMULATIVE_BOOST_DB dB)")
            }

            // Check clipping risk with preamp
            val totalMaxOutput = peakGain + profile.preampGainDb
            if (totalMaxOutput > 0.05f) {
                errors.add("Potential digital clipping: Peak gain (+${"%.1f".format(peakGain)} dB) plus preamp (${"%.1f".format(profile.preampGainDb)} dB) exceeds 0 dBFS (+${"%.1f".format(totalMaxOutput)} dBFS)")
            }
        }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
}
