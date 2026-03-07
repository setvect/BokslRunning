package com.boksl.running.data.export

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExportBundleDto(
    @SerialName("schema_version")
    val schemaVersion: Int,
    @SerialName("exported_at_epoch_millis")
    val exportedAtEpochMillis: Long,
    @SerialName("profile")
    val profile: ExportProfileDto?,
    @SerialName("app_preferences")
    val appPreferences: ExportAppPreferencesDto,
    @SerialName("sessions")
    val sessions: List<ExportSessionDto>,
)

@Serializable
data class ExportProfileDto(
    @SerialName("weight_kg")
    val weightKg: Float,
    @SerialName("gender")
    val gender: String,
    @SerialName("age")
    val age: Int,
    @SerialName("updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
)

@Serializable
data class ExportAppPreferencesDto(
    @SerialName("onboarding_completed")
    val onboardingCompleted: Boolean,
    @SerialName("location_rationale_shown")
    val locationRationaleShown: Boolean,
)

@Serializable
data class ExportSessionDto(
    @SerialName("external_id")
    val externalId: String,
    @SerialName("status")
    val status: String,
    @SerialName("started_at_epoch_millis")
    val startedAtEpochMillis: Long,
    @SerialName("ended_at_epoch_millis")
    val endedAtEpochMillis: Long?,
    @SerialName("stats")
    val stats: ExportRunStatsDto,
    @SerialName("created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @SerialName("updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
    @SerialName("track_points")
    val trackPoints: List<ExportTrackPointDto>,
)

@Serializable
data class ExportRunStatsDto(
    @SerialName("duration_millis")
    val durationMillis: Long,
    @SerialName("distance_meters")
    val distanceMeters: Double,
    @SerialName("average_pace_sec_per_km")
    val averagePaceSecPerKm: Double?,
    @SerialName("max_speed_mps")
    val maxSpeedMps: Double,
    @SerialName("calorie_kcal")
    val calorieKcal: Double?,
)

@Serializable
data class ExportTrackPointDto(
    @SerialName("external_id")
    val externalId: String,
    @SerialName("sequence")
    val sequence: Int,
    @SerialName("latitude")
    val latitude: Double,
    @SerialName("longitude")
    val longitude: Double,
    @SerialName("altitude_meters")
    val altitudeMeters: Double?,
    @SerialName("accuracy_meters")
    val accuracyMeters: Float?,
    @SerialName("speed_mps")
    val speedMps: Float?,
    @SerialName("recorded_at_epoch_millis")
    val recordedAtEpochMillis: Long,
)
