package com.evenlouder.app

import com.evenlouder.app.core.dsp.*
import com.evenlouder.app.tools.ProfileIngestionPipeline
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class ProfileIngestionTest {

    @Test
    fun testFilterTypeNormalization() {
        assertEquals(FilterType.PEAKING, ProfileIngestionPipeline.normalizeFilterType("PK"))
        assertEquals(FilterType.PEAKING, ProfileIngestionPipeline.normalizeFilterType("bell"))
        assertEquals(FilterType.LOW_SHELF, ProfileIngestionPipeline.normalizeFilterType("LS"))
        assertEquals(FilterType.LOW_SHELF, ProfileIngestionPipeline.normalizeFilterType("low_shelf"))
        assertEquals(FilterType.HIGH_SHELF, ProfileIngestionPipeline.normalizeFilterType("HS"))
        assertEquals(FilterType.HIGH_SHELF, ProfileIngestionPipeline.normalizeFilterType("high_shelf"))
        assertEquals(FilterType.HIGH_PASS, ProfileIngestionPipeline.normalizeFilterType("HP"))
        assertEquals(FilterType.LOW_PASS, ProfileIngestionPipeline.normalizeFilterType("LP"))
        assertEquals(FilterType.NOTCH, ProfileIngestionPipeline.normalizeFilterType("notch"))
        assertEquals(FilterType.BAND_PASS, ProfileIngestionPipeline.normalizeFilterType("band_pass"))
    }

    @Test
    fun testValidOpraRecordIngestion() {
        val raw = ProfileIngestionPipeline.RawOpraRecord(
            vendor = "Sennheiser",
            product = "HD 800 S",
            author = "Oratory1990",
            target = "Harman Over-Ear 2018",
            sourceUrl = "https://github.com/opra-project/OPRA",
            license = "CC BY-SA 4.0",
            preampDb = -5.5f,
            filters = listOf(
                ProfileIngestionPipeline.RawFilterRecord("LS", 105f, 5.5f, 0.71f),
                ProfileIngestionPipeline.RawFilterRecord("PK", 200f, -2.0f, 1.4f),
                ProfileIngestionPipeline.RawFilterRecord("PK", 5800f, -5.5f, 4.0f),
                ProfileIngestionPipeline.RawFilterRecord("HS", 10000f, 2.0f, 0.71f)
            )
        )

        val (profile, errors) = ProfileIngestionPipeline.ingestRecord(raw)
        assertNotNull("Valid OPRA record must produce an AcousticProfile", profile)
        assertTrue("Error list must be empty", errors.isEmpty())
        assertEquals("sennheiser_hd_800_s_harman_over_ear_2018", profile!!.id)
        assertEquals("Sennheiser", profile.manufacturer)
        assertEquals("HD 800 S", profile.model)
        assertEquals(4, profile.filters.size)
        assertTrue("Preamp must be <= -5.5 dB to prevent clipping", profile.preampGainDb <= -5.5f)
    }

    @Test
    fun testAutomaticPreampHeadroomCompensation() {
        // Record has +6 dB boost, but author set preamp to 0 dB (risk of digital clipping)
        val rawUnsafe = ProfileIngestionPipeline.RawOpraRecord(
            vendor = "TestVendor",
            product = "BassBoostTest",
            author = "Community",
            target = "Harman Target",
            sourceUrl = "https://example.com",
            license = "CC BY-SA 4.0",
            preampDb = 0.0f, // Unsafe preamp
            filters = listOf(
                ProfileIngestionPipeline.RawFilterRecord("PK", 100f, 6.0f, 1.4f)
            )
        )

        val (profile, errors) = ProfileIngestionPipeline.ingestRecord(rawUnsafe)
        assertNotNull("Record must be ingested successfully", profile)
        assertTrue(errors.isEmpty())
        assertTrue(
            "Ingestion pipeline must automatically clamp preamp to compensate peak boost (+${profile!!.preampGainDb} dB)",
            profile.preampGainDb <= -6.0f
        )
    }

    @Test
    fun testMeasurementCorrectionResidualErrorReduction() {
        // Test theoretical correction of an uncorrected headphone measurement against a target
        // Suppose uncorrected headphone response has a -5 dB dip at 3 kHz
        val dipFrequency = 3000.0
        val uncorrectedDipDb = -5.0

        // Measurement correction filter: +5.0 dB boost at 3 kHz with Q=2.0
        val correctionFilter = BiquadCalculator.calculate(
            type = FilterType.PEAKING,
            frequencyHz = dipFrequency,
            gainDb = 5.0,
            qFactor = 2.0,
            sampleRateHz = 48000.0
        )

        val filterGainAtDip = correctionFilter.responseDbAt(dipFrequency, 48000.0)
        val correctedResponseAtDip = uncorrectedDipDb + filterGainAtDip
        val residualError = abs(correctedResponseAtDip - 0.0) // Target is 0 dB

        assertTrue(
            "Residual error after correction must be negligible (< 0.05 dB), was $residualError dB",
            residualError < 0.05
        )
    }
}
