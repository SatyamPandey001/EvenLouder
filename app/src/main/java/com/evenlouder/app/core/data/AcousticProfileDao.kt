package com.evenlouder.app.core.data

import com.evenlouder.app.core.dsp.AcousticProfile
import com.evenlouder.app.core.dsp.TuningCategory

/**
 * Data Access Object interface for querying and persisting acoustic profiles,
 * manufacturers, devices, and licensing metadata.
 */
interface AcousticProfileDao {

    fun getAllProfiles(): List<AcousticProfile>

    fun getProfileById(id: String): AcousticProfile?

    fun getProfilesByCategory(category: TuningCategory): List<AcousticProfile>

    fun searchProfiles(query: String): List<AcousticProfile>

    fun getAllManufacturers(): List<ManufacturerEntity>

    fun getDevicesByManufacturer(manufacturerId: String): List<DeviceEntity>

    fun getProfileDetails(profileId: String): ProfileWithDetails?

    fun insertProfileWithDetails(details: ProfileWithDetails)

    fun getProfileCount(): Int
}
