package com.evenlouder.app.tools

import com.evenlouder.app.core.dsp.*
import java.security.MessageDigest

/**
 * Standalone Ingestion Pipeline tool for importing, normalizing, validating,
 * and exporting acoustic equalization profiles from AutoEQ and OPRA databases.
 */
object ProfileIngestionPipeline {

    data class RawOpraRecord(
        val vendor: String,
        val product: String,
        val author: String,
        val target: String,
        val sourceUrl: String,
        val license: String,
        val preampDb: Float,
        val filters: List<RawFilterRecord>
    )

    data class RawFilterRecord(
        val typeStr: String,
        val freqHz: Float,
        val gainDb: Float,
        val q: Float
    )

    data class IngestionReport(
        val totalProcessed: Int,
        val importedCount: Int,
        val rejectedCount: Int,
        val licenseDistribution: Map<String, Int>,
        val sha256Checksum: String,
        val rejectedReasons: List<String>
    )

    /**
     * Normalizes filter types from OPRA/AutoEQ schema string formats into standard FilterType.
     */
    fun normalizeFilterType(rawType: String): FilterType = when (rawType.uppercase().trim()) {
        "PK", "PEAK", "PEAKING", "BELL" -> FilterType.PEAKING
        "LS", "LSC", "LOW_SHELF", "LOWSHELF", "SHELF_LOW" -> FilterType.LOW_SHELF
        "HS", "HSC", "HIGH_SHELF", "HIGHSHELF", "SHELF_HIGH" -> FilterType.HIGH_SHELF
        "LP", "LOW_PASS", "LOWPASS" -> FilterType.LOW_PASS
        "HP", "HIGH_PASS", "HIGHPASS" -> FilterType.HIGH_PASS
        "NOTCH", "BAND_STOP", "BS" -> FilterType.NOTCH
        "BP", "BAND_PASS", "BANDPASS" -> FilterType.BAND_PASS
        else -> FilterType.PEAKING
    }

    /**
     * Computes the SHA-256 checksum of raw text content for provenance and tamper detection.
     */
    fun computeSha256(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(content.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Parses standard AutoEQ / EqualizerAPO parametric result text.
     * Example input:
     *   Preamp: -6.5 dB
     *   Filter 1: ON PK Fc 28 Hz Gain 6.1 dB Q 1.41
     *   Filter 2: ON LSC Fc 105 Hz Gain -5.4 dB Q 0.70
     */
    fun parseAutoEqParametricText(
        text: String,
        vendor: String,
        product: String,
        target: String = "Harman target",
        author: String = "AutoEQ / Oratory1990",
        sourceUrl: String = "https://github.com/jaakkopasanen/AutoEq",
        license: String = "MIT / CC BY-SA 4.0"
    ): Pair<AcousticProfile?, List<String>> {
        var preampDb = 0f
        val filters = mutableListOf<RawFilterRecord>()
        val lines = text.lines()

        val preampRegex = Regex("""Preamp:\s*([+-]?\d+(?:\.\d+)?)\s*dB""", RegexOption.IGNORE_CASE)
        val filterRegex = Regex(
            """Filter\s+\d+:\s*(?:ON\s+)?([A-Za-z_]+)\s+Fc\s+(\d+(?:\.\d+)?)\s*Hz\s+Gain\s+([+-]?\d+(?:\.\d+)?)\s*dB(?:\s+Q\s+(\d+(?:\.\d+)?))?""",
            RegexOption.IGNORE_CASE
        )

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            val preampMatch = preampRegex.find(trimmed)
            if (preampMatch != null) {
                preampDb = preampMatch.groupValues[1].toFloatOrNull() ?: 0f
                continue
            }

            val filterMatch = filterRegex.find(trimmed)
            if (filterMatch != null) {
                val typeStr = filterMatch.groupValues[1]
                val fc = filterMatch.groupValues[2].toFloatOrNull() ?: 1000f
                val gain = filterMatch.groupValues[3].toFloatOrNull() ?: 0f
                val q = filterMatch.groupValues.getOrNull(4)?.toFloatOrNull() ?: 0.707f
                filters.add(RawFilterRecord(typeStr, fc, gain, q))
            }
        }

        if (filters.isEmpty()) {
            return null to listOf("No valid parametric filters found in AutoEQ text input.")
        }

        val raw = RawOpraRecord(
            vendor = vendor,
            product = product,
            author = author,
            target = target,
            sourceUrl = sourceUrl,
            license = license,
            preampDb = preampDb,
            filters = filters
        )
        return ingestRecord(raw)
    }

    /**
     * Parses a single line or payload from OPRA (database_v1.jsonl).
     * Extracts vendor, product, author, target, preamp, and filter definitions.
     */
    fun parseOpraJsonLine(line: String): RawOpraRecord? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || !trimmed.startsWith("{")) return null

        fun extractString(key: String): String {
            val match = Regex(""""$key"\s*:\s*"([^"]*)"""").find(trimmed)
            return match?.groupValues?.get(1) ?: ""
        }

        fun extractFloat(key: String, default: Float = 0f): Float {
            val match = Regex(""""$key"\s*:\s*([+-]?\d+(?:\.\d+)?)""").find(trimmed)
            return match?.groupValues?.get(1)?.toFloatOrNull() ?: default
        }

        val vendor = extractString("vendor").ifBlank { extractString("manufacturer") }.ifBlank { "Generic" }
        val product = extractString("product").ifBlank { extractString("model") }.ifBlank { "Headphone" }
        val author = extractString("author").ifBlank { "OPRA Community" }
        val target = extractString("target").ifBlank { "Harman Target" }
        val sourceUrl = extractString("sourceUrl").ifBlank { extractString("url") }.ifBlank { "https://github.com/opra-project/OPRA" }
        val license = extractString("license").ifBlank { "CC BY-SA 4.0" }
        val preampDb = extractFloat("preampDb", extractFloat("preamp_db", 0f))

        // Extract filters array
        val filtersMatch = Regex(""""filters"\s*:\s*\[(.*?)\]""").find(trimmed)
        val filters = mutableListOf<RawFilterRecord>()
        if (filtersMatch != null) {
            val filterObjects = Regex("""\{([^}]+)\}""").findAll(filtersMatch.groupValues[1])
            for (fo in filterObjects) {
                val block = fo.groupValues[1]
                val typeMatch = Regex(""""(?:type|typeStr)"\s*:\s*"([^"]+)"""").find(block)
                val typeStr = typeMatch?.groupValues?.get(1) ?: "PK"
                val freqMatch = Regex(""""(?:freq|freqHz|frequency)"\s*:\s*([+-]?\d+(?:\.\d+)?)""").find(block)
                val freqHz = freqMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 1000f
                val gainMatch = Regex(""""(?:gain|gainDb)"\s*:\s*([+-]?\d+(?:\.\d+)?)""").find(block)
                val gainDb = gainMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                val qMatch = Regex(""""(?:q|qFactor)"\s*:\s*([+-]?\d+(?:\.\d+)?)""").find(block)
                val q = qMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0.707f
                filters.add(RawFilterRecord(typeStr, freqHz, gainDb, q))
            }
        }

        if (filters.isEmpty()) return null

        return RawOpraRecord(
            vendor = vendor,
            product = product,
            author = author,
            target = target,
            sourceUrl = sourceUrl,
            license = license,
            preampDb = preampDb,
            filters = filters
        )
    }

    /**
     * Ingestion process: converts raw record, validates with DSP rules,
     * calculates headroom compensation, and returns verified AcousticProfile.
     */
    fun ingestRecord(
        raw: RawOpraRecord,
        category: TuningCategory = TuningCategory.MEASUREMENT_CORRECTION
    ): Pair<AcousticProfile?, List<String>> {
        val parsedFilters = raw.filters.mapIndexed { idx, rf ->
            ParametricFilter(
                id = "f${idx + 1}",
                type = normalizeFilterType(rf.typeStr),
                frequencyHz = rf.freqHz,
                gainDb = rf.gainDb,
                qFactor = rf.q,
                enabled = true
            )
        }

        // Generate normalized ID
        val sanitizedVendor = raw.vendor.lowercase().replace(Regex("[^a-z0-9]"), "_")
        val sanitizedProduct = raw.product.lowercase().replace(Regex("[^a-z0-9]"), "_")
        val sanitizedTarget = raw.target.lowercase().replace(Regex("[^a-z0-9]"), "_")
        val profileId = "${sanitizedVendor}_${sanitizedProduct}_${sanitizedTarget}"

        var profile = AcousticProfile(
            id = profileId,
            name = "${raw.vendor} ${raw.product} (${raw.target})",
            subtitle = "${raw.author} · ${raw.target}",
            description = "Acoustic equalization for ${raw.vendor} ${raw.product} targeting ${raw.target} by ${raw.author}.",
            category = category,
            targetDeviceType = TargetDeviceType.HEADPHONES,
            manufacturer = raw.vendor,
            model = raw.product,
            preampGainDb = raw.preampDb,
            filters = parsedFilters,
            author = raw.author,
            sourceUrl = raw.sourceUrl,
            license = raw.license,
            targetCurve = raw.target,
            algorithmVersion = "AutoEQ / OPRA Ingestion v2.0"
        )

        // Ensure preamp safely covers peak gain plus 0.2 dB intersample peak margin
        val peak = profile.calculatePeakGainDb()
        val requiredPreamp = if (peak > 0f) -peak - 0.2f else 0f
        if (profile.preampGainDb > requiredPreamp) {
            profile = profile.copy(preampGainDb = requiredPreamp)
        }

        val validation = ProfileValidationEngine.validate(profile)
        return when (validation) {
            is ValidationResult.Valid -> profile to emptyList()
            is ValidationResult.Invalid -> null to validation.errors
        }
    }

    /**
     * Executes batch ingestion over a list of raw records, calculating checksums,
     * tracking rejected items, and compiling an IngestionReport.
     */
    fun ingestBatch(
        records: List<RawOpraRecord>,
        category: TuningCategory = TuningCategory.MEASUREMENT_CORRECTION
    ): Pair<List<AcousticProfile>, IngestionReport> {
        val successful = mutableListOf<AcousticProfile>()
        val rejectedReasons = mutableListOf<String>()
        val licenseCounts = mutableMapOf<String, Int>()

        for (record in records) {
            val (profile, errors) = ingestRecord(record, category)
            if (profile != null) {
                successful.add(profile)
                licenseCounts[profile.license] = (licenseCounts[profile.license] ?: 0) + 1
            } else {
                val msg = "${record.vendor} ${record.product}: ${errors.joinToString("; ")}"
                rejectedReasons.add(msg)
            }
        }

        val digestInput = successful.joinToString("|") { "${it.id}:${it.preampGainDb}:${it.filters.size}" }
        val checksum = computeSha256(digestInput)

        val report = IngestionReport(
            totalProcessed = records.size,
            importedCount = successful.size,
            rejectedCount = rejectedReasons.size,
            licenseDistribution = licenseCounts,
            sha256Checksum = checksum,
            rejectedReasons = rejectedReasons
        )

        return successful to report
    }
}
