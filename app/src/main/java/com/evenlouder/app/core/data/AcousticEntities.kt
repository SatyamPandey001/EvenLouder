package com.evenlouder.app.core.data

import com.evenlouder.app.core.dsp.FilterType
import com.evenlouder.app.core.dsp.TargetDeviceType
import com.evenlouder.app.core.dsp.TuningCategory

/**
 * Structured relational database entities for acoustic measurements,
 * devices, targets, profiles, and licenses.
 */

data class ManufacturerEntity(
    val id: String,
    val name: String,
    val country: String = ""
)

data class DeviceEntity(
    val id: String,
    val manufacturerId: String,
    val model: String,
    val type: TargetDeviceType,
    val releaseYear: Int? = null
)

data class MeasurementEntity(
    val id: String,
    val deviceId: String,
    val author: String,
    val rig: String,
    val sourceUrl: String,
    val notes: String = ""
)

data class AcousticTargetEntity(
    val id: String,
    val name: String,
    val description: String = ""
)

data class LicenseEntity(
    val id: String,
    val name: String,
    val url: String = "",
    val attributionRequired: Boolean = true,
    val shareAlike: Boolean = false,
    val commercialUsePermitted: Boolean = true
)

data class ProfileSourceEntity(
    val id: String,
    val name: String,
    val url: String,
    val version: String,
    val licenseId: String
)

data class AcousticProfileEntity(
    val id: String,
    val name: String,
    val subtitle: String,
    val description: String,
    val category: TuningCategory,
    val deviceId: String,
    val targetId: String,
    val measurementId: String? = null,
    val sourceId: String,
    val preampGainDb: Float,
    val algorithmVersion: String,
    val validationStatus: String,
    val isCalibrated: Boolean,
    val createdAt: String
)

data class ParametricFilterEntity(
    val id: String,
    val profileId: String,
    val bandIndex: Int,
    val filterType: FilterType,
    val frequencyHz: Float,
    val gainDb: Float,
    val qFactor: Float,
    val enabled: Boolean,
    val slope: Float = 1.0f
)

/**
 * Aggregated entity combining a profile with its complete provenance,
 * device, target, license, and filter configurations.
 */
data class ProfileWithDetails(
    val profile: AcousticProfileEntity,
    val device: DeviceEntity,
    val manufacturer: ManufacturerEntity,
    val target: AcousticTargetEntity,
    val measurement: MeasurementEntity?,
    val source: ProfileSourceEntity,
    val license: LicenseEntity,
    val filters: List<ParametricFilterEntity>
)
