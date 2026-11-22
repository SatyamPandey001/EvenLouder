package com.evenlouder.app.core.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.evenlouder.app.core.dsp.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Structured relational SQLite open helper implementing the acoustic profile database schema.
 */
class AcousticSqliteHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "evenlouder_acoustic.db"
        const val DATABASE_VERSION = 1

        const val TABLE_MANUFACTURERS = "manufacturers"
        const val TABLE_DEVICES = "devices"
        const val TABLE_MEASUREMENTS = "measurements"
        const val TABLE_TARGETS = "acoustic_targets"
        const val TABLE_LICENSES = "licenses"
        const val TABLE_SOURCES = "profile_sources"
        const val TABLE_PROFILES = "acoustic_profiles"
        const val TABLE_FILTERS = "parametric_filters"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_MANUFACTURERS (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                country TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_DEVICES (
                id TEXT PRIMARY KEY,
                manufacturer_id TEXT NOT NULL,
                model TEXT NOT NULL,
                type TEXT NOT NULL,
                release_year INTEGER,
                FOREIGN KEY (manufacturer_id) REFERENCES $TABLE_MANUFACTURERS(id)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_devices_manuf ON $TABLE_DEVICES(manufacturer_id)")

        db.execSQL(
            """
            CREATE TABLE $TABLE_MEASUREMENTS (
                id TEXT PRIMARY KEY,
                device_id TEXT NOT NULL,
                author TEXT NOT NULL,
                rig TEXT NOT NULL,
                source_url TEXT,
                notes TEXT,
                FOREIGN KEY (device_id) REFERENCES $TABLE_DEVICES(id)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_TARGETS (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_LICENSES (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                url TEXT,
                attribution_required INTEGER NOT NULL,
                share_alike INTEGER NOT NULL,
                commercial_permitted INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_SOURCES (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                url TEXT,
                version TEXT,
                license_id TEXT NOT NULL,
                FOREIGN KEY (license_id) REFERENCES $TABLE_LICENSES(id)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_PROFILES (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                subtitle TEXT,
                description TEXT,
                category TEXT NOT NULL,
                device_id TEXT NOT NULL,
                target_id TEXT NOT NULL,
                measurement_id TEXT,
                source_id TEXT NOT NULL,
                preamp_gain_db REAL NOT NULL,
                algorithm_version TEXT,
                validation_status TEXT,
                is_calibrated INTEGER NOT NULL,
                created_at TEXT,
                FOREIGN KEY (device_id) REFERENCES $TABLE_DEVICES(id),
                FOREIGN KEY (target_id) REFERENCES $TABLE_TARGETS(id),
                FOREIGN KEY (measurement_id) REFERENCES $TABLE_MEASUREMENTS(id),
                FOREIGN KEY (source_id) REFERENCES $TABLE_SOURCES(id)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_profiles_category ON $TABLE_PROFILES(category)")
        db.execSQL("CREATE INDEX idx_profiles_device ON $TABLE_PROFILES(device_id)")

        db.execSQL(
            """
            CREATE TABLE $TABLE_FILTERS (
                id TEXT PRIMARY KEY,
                profile_id TEXT NOT NULL,
                band_index INTEGER NOT NULL,
                filter_type TEXT NOT NULL,
                frequency_hz REAL NOT NULL,
                gain_db REAL NOT NULL,
                q_factor REAL NOT NULL,
                enabled INTEGER NOT NULL,
                slope REAL NOT NULL,
                FOREIGN KEY (profile_id) REFERENCES $TABLE_PROFILES(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_filters_profile ON $TABLE_FILTERS(profile_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 is initial schema. Future migrations will use ALTER TABLE.
    }
}

/**
 * High-performance, thread-safe acoustic database implementation conforming to AcousticProfileDao.
 * Operates across Android runtime and JVM testing environments.
 */
class InMemoryAcousticDatabase : AcousticProfileDao {

    private val manufacturers = ConcurrentHashMap<String, ManufacturerEntity>()
    private val devices = ConcurrentHashMap<String, DeviceEntity>()
    private val measurements = ConcurrentHashMap<String, MeasurementEntity>()
    private val targets = ConcurrentHashMap<String, AcousticTargetEntity>()
    private val licenses = ConcurrentHashMap<String, LicenseEntity>()
    private val sources = ConcurrentHashMap<String, ProfileSourceEntity>()
    private val profiles = ConcurrentHashMap<String, AcousticProfileEntity>()
    private val filters = ConcurrentHashMap<String, MutableList<ParametricFilterEntity>>()

    override fun getAllProfiles(): List<AcousticProfile> =
        profiles.values.mapNotNull { assembleDomainProfile(it.id) }

    override fun getProfileById(id: String): AcousticProfile? =
        assembleDomainProfile(id)

    override fun getProfilesByCategory(category: TuningCategory): List<AcousticProfile> =
        profiles.values.filter { it.category == category }.mapNotNull { assembleDomainProfile(it.id) }

    override fun searchProfiles(query: String): List<AcousticProfile> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return getAllProfiles()

        return profiles.values.filter { pe ->
            val dev = devices[pe.deviceId]
            val manuf = dev?.let { manufacturers[it.manufacturerId] }
            val target = targets[pe.targetId]

            pe.name.lowercase().contains(q) ||
                (pe.subtitle.lowercase().contains(q)) ||
                (manuf?.name?.lowercase()?.contains(q) == true) ||
                (dev?.model?.lowercase()?.contains(q) == true) ||
                (target?.name?.lowercase()?.contains(q) == true) ||
                pe.category.displayName.lowercase().contains(q)
        }.mapNotNull { assembleDomainProfile(it.id) }
    }

    override fun getAllManufacturers(): List<ManufacturerEntity> =
        manufacturers.values.toList().sortedBy { it.name }

    override fun getDevicesByManufacturer(manufacturerId: String): List<DeviceEntity> =
        devices.values.filter { it.manufacturerId == manufacturerId }.sortedBy { it.model }

    override fun getProfileDetails(profileId: String): ProfileWithDetails? {
        val pe = profiles[profileId] ?: return null
        val dev = devices[pe.deviceId] ?: return null
        val manuf = manufacturers[dev.manufacturerId] ?: return null
        val target = targets[pe.targetId] ?: return null
        val measurement = pe.measurementId?.let { measurements[it] }
        val source = sources[pe.sourceId] ?: return null
        val license = licenses[source.licenseId] ?: return null
        val filterList = filters[pe.id] ?: emptyList()

        return ProfileWithDetails(
            profile = pe,
            device = dev,
            manufacturer = manuf,
            target = target,
            measurement = measurement,
            source = source,
            license = license,
            filters = filterList
        )
    }

    override fun insertProfileWithDetails(details: ProfileWithDetails) {
        manufacturers[details.manufacturer.id] = details.manufacturer
        devices[details.device.id] = details.device
        details.measurement?.let { measurements[it.id] = it }
        targets[details.target.id] = details.target
        licenses[details.license.id] = details.license
        sources[details.source.id] = details.source
        profiles[details.profile.id] = details.profile
        filters[details.profile.id] = details.filters.toMutableList()
    }

    override fun getProfileCount(): Int = profiles.size

    private fun assembleDomainProfile(profileId: String): AcousticProfile? {
        val pe = profiles[profileId] ?: return null
        val dev = devices[pe.deviceId]
        val manuf = dev?.let { manufacturers[it.manufacturerId] }
        val target = targets[pe.targetId]
        val measurement = pe.measurementId?.let { measurements[it] }
        val source = sources[pe.sourceId]
        val license = source?.let { licenses[it.licenseId] }
        val filterEntities = filters[pe.id] ?: emptyList()

        val domainFilters = filterEntities.sortedBy { it.bandIndex }.map { fe ->
            ParametricFilter(
                id = fe.id,
                type = fe.filterType,
                frequencyHz = fe.frequencyHz,
                gainDb = fe.gainDb,
                qFactor = fe.qFactor,
                enabled = fe.enabled,
                slope = fe.slope
            )
        }

        return AcousticProfile(
            id = pe.id,
            name = pe.name,
            subtitle = pe.subtitle,
            description = pe.description,
            category = pe.category,
            targetDeviceType = dev?.type ?: TargetDeviceType.HEADPHONES,
            manufacturer = manuf?.name ?: "",
            model = dev?.model ?: "",
            preampGainDb = pe.preampGainDb,
            filters = domainFilters,
            author = measurement?.author ?: source?.name ?: "EvenLouder",
            sourceUrl = measurement?.sourceUrl ?: source?.url ?: "",
            license = license?.name ?: "CC BY-SA 4.0",
            targetCurve = target?.name ?: "",
            measurementRig = measurement?.rig ?: "",
            algorithmVersion = pe.algorithmVersion,
            validationStatus = pe.validationStatus,
            createdAt = pe.createdAt
        )
    }
}
