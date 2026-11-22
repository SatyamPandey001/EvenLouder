package com.evenlouder.app

import com.evenlouder.app.core.data.*
import com.evenlouder.app.core.dsp.*
import com.evenlouder.app.tools.ProfileIngestionPipeline
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DatabaseIngestionTest {

    private lateinit var db: InMemoryAcousticDatabase

    @Before
    fun setup() {
        db = InMemoryAcousticDatabase()
    }

    @Test
    fun testDatabaseEntityInsertionAndAssembly() {
        val mfr = ManufacturerEntity("sennheiser", "Sennheiser", "Germany")
        val dev = DeviceEntity("sennheiser_hd600", "sennheiser", "HD 600", TargetDeviceType.HEADPHONES, 1997)
        val lcn = LicenseEntity("cc-by-sa-4.0", "CC BY-SA 4.0", "https://creativecommons.org/licenses/by-sa/4.0/", attributionRequired = true, shareAlike = true, commercialUsePermitted = true)
        val src = ProfileSourceEntity("autoeq-git", "AutoEQ Repository", "https://github.com/jaakkopasanen/AutoEq", "v4.4.1", "cc-by-sa-4.0")
        val tgt = AcousticTargetEntity("harman-oe-2018", "Harman Over-Ear 2018", "Harman Research / Sean Olive")
        val meas = MeasurementEntity("meas-hd600", "sennheiser_hd600", "Oratory1990", "GRAS 43AG-7", "https://example.com")
        val prof = AcousticProfileEntity(
            id = "test_hd600_harman",
            name = "Sennheiser HD 600 (Harman Over-Ear)",
            subtitle = "Oratory1990 · Harman",
            description = "Acoustic correction for Sennheiser HD 600",
            category = TuningCategory.MEASUREMENT_CORRECTION,
            deviceId = "sennheiser_hd600",
            targetId = "harman-oe-2018",
            measurementId = "meas-hd600",
            sourceId = "autoeq-git",
            preampGainDb = -6.5f,
            algorithmVersion = "AutoEQ v4.4.1",
            validationStatus = "VALID",
            isCalibrated = true,
            createdAt = "2024-01-01"
        )
        val filter1 = ParametricFilterEntity("f1", "test_hd600_harman", 1, FilterType.LOW_SHELF, 105f, 5.5f, 0.71f, true)
        val filter2 = ParametricFilterEntity("f2", "test_hd600_harman", 2, FilterType.PEAKING, 3500f, -3.0f, 2.0f, true)

        val details = ProfileWithDetails(
            profile = prof,
            device = dev,
            manufacturer = mfr,
            target = tgt,
            measurement = meas,
            source = src,
            license = lcn,
            filters = listOf(filter1, filter2)
        )

        db.insertProfileWithDetails(details)

        val assembled = db.getProfileDetails("test_hd600_harman")
        assertNotNull("Assembled profile details must not be null", assembled)
        assertEquals("Sennheiser", assembled!!.manufacturer.name)
        assertEquals("HD 600", assembled.device.model)
        assertEquals("Harman Over-Ear 2018", assembled.target.name)
        assertEquals(2, assembled.filters.size)
        assertEquals("CC BY-SA 4.0", assembled.license.name)

        val domainProfile = db.getProfileById("test_hd600_harman")
        assertNotNull("Domain profile assembly must succeed", domainProfile)
        assertEquals("test_hd600_harman", domainProfile!!.id)
        assertEquals("Sennheiser", domainProfile.manufacturer)
        assertEquals(2, domainProfile.filters.size)
    }

    @Test
    fun testDatabaseSearchByQuery() {
        AcousticProfileRepository.seedDatabase(db)

        val sennheiserResults = db.searchProfiles("Sennheiser")
        assertTrue("Must find Sennheiser models", sennheiserResults.isNotEmpty())
        assertTrue("All returned models must contain Sennheiser", sennheiserResults.all {
            it.name.contains("Sennheiser", ignoreCase = true) || it.manufacturer.contains("Sennheiser", ignoreCase = true)
        })

        val sonyResults = db.searchProfiles("WH-1000XM4")
        assertTrue("Must find Sony XM4", sonyResults.isNotEmpty())
        assertTrue(sonyResults.any { it.model.contains("WH-1000XM4", ignoreCase = true) })

        val harmanResults = db.searchProfiles("Harman")
        assertTrue("Must find multiple Harman target corrections", harmanResults.size >= 5)
    }

    @Test
    fun testAutoEqTextParser() {
        val autoEqText = """
            Preamp: -6.1 dB
            Filter 1: ON PK Fc 28 Hz Gain 6.1 dB Q 1.41
            Filter 2: ON LSC Fc 105 Hz Gain -5.4 dB Q 0.70
            Filter 3: ON PK Fc 1200 Hz Gain -2.1 dB Q 2.00
            Filter 4: ON HSC Fc 10000 Hz Gain 1.5 dB Q 0.70
        """.trimIndent()

        val (profile, errors) = ProfileIngestionPipeline.parseAutoEqParametricText(
            text = autoEqText,
            vendor = "AKG",
            product = "K371",
            target = "Harman Target",
            author = "AutoEQ / Oratory1990"
        )

        assertNotNull("AutoEQ text parser must return valid profile", profile)
        assertTrue("Errors must be empty", errors.isEmpty())
        assertEquals("akg_k371_harman_target", profile!!.id)
        assertEquals(4, profile.filters.size)
        assertEquals(FilterType.LOW_SHELF, profile.filters[1].type)
        assertEquals(105f, profile.filters[1].frequencyHz)
        assertEquals(FilterType.HIGH_SHELF, profile.filters[3].type)
        assertTrue("Preamp must be <= -6.1 dB", profile.preampGainDb <= -6.1f)
    }

    @Test
    fun testOpraJsonLineParser() {
        val jsonLine = """
            {"vendor": "Audio-Technica", "product": "ATH-M50x", "author": "Oratory1990", "target": "Harman Target", "preampDb": -4.8, "license": "CC BY-SA 4.0", "filters": [{"type": "LS", "freq": 105, "gain": 4.5, "q": 0.71}, {"type": "PK", "freq": 350, "gain": -2.0, "q": 1.4}]}
        """.trimIndent()

        val record = ProfileIngestionPipeline.parseOpraJsonLine(jsonLine)
        assertNotNull("OPRA jsonl parser must extract record", record)
        assertEquals("Audio-Technica", record!!.vendor)
        assertEquals("ATH-M50x", record.product)
        assertEquals(2, record.filters.size)
        assertEquals("LS", record.filters[0].typeStr)
        assertEquals(105f, record.filters[0].freqHz)

        val (profile, errors) = ProfileIngestionPipeline.ingestRecord(record)
        assertNotNull(profile)
        assertTrue(errors.isEmpty())
        assertEquals("audio_technica_ath_m50x_harman_target", profile!!.id)
    }

    @Test
    fun testBatchIngestionAndReport() {
        val records = listOf(
            ProfileIngestionPipeline.RawOpraRecord(
                vendor = "BrandA", product = "Model1", author = "Engineer1", target = "Harman",
                sourceUrl = "https://example.com", license = "MIT", preampDb = -3.0f,
                filters = listOf(ProfileIngestionPipeline.RawFilterRecord("PK", 1000f, 2.0f, 1.0f))
            ),
            ProfileIngestionPipeline.RawOpraRecord(
                vendor = "BrandB", product = "Model2", author = "Engineer2", target = "Diffuse Field",
                sourceUrl = "https://example.com", license = "CC BY-SA 4.0", preampDb = -4.0f,
                filters = listOf(ProfileIngestionPipeline.RawFilterRecord("PK", 3000f, 3.0f, 1.4f))
            ),
            ProfileIngestionPipeline.RawOpraRecord(
                vendor = "InvalidBrand", product = "BadFilters", author = "Nobody", target = "Test",
                sourceUrl = "https://example.com", license = "Unknown", preampDb = 0f,
                filters = listOf(ProfileIngestionPipeline.RawFilterRecord("PK", 1000f, 25.0f, 1.0f)) // Exceeds gain limit (+25 dB)
            )
        )

        val (imported, report) = ProfileIngestionPipeline.ingestBatch(records)
        assertEquals(2, imported.size)
        assertEquals(3, report.totalProcessed)
        assertEquals(2, report.importedCount)
        assertEquals(1, report.rejectedCount)
        assertEquals(1, report.licenseDistribution["MIT"])
        assertEquals(1, report.licenseDistribution["CC BY-SA 4.0"])
        assertNotNull(report.sha256Checksum)
        assertEquals(64, report.sha256Checksum.length)
        assertTrue(report.rejectedReasons.any { it.contains("InvalidBrand") })
    }
}
