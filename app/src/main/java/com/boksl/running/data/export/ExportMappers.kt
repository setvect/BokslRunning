package com.boksl.running.data.export

import com.boksl.running.data.local.db.entity.RunningSessionEntity
import com.boksl.running.data.local.db.entity.TrackPointEntity
import com.boksl.running.domain.model.AppPreferences
import com.boksl.running.domain.model.Profile

fun Profile.toExportDto(): ExportProfileDto =
    ExportProfileDto(
        weightKg = weightKg,
        gender = gender.name,
        age = age,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

fun AppPreferences.toExportDto(): ExportAppPreferencesDto =
    ExportAppPreferencesDto(
        onboardingCompleted = onboardingCompleted,
        locationRationaleShown = locationRationaleShown,
    )

fun RunningSessionEntity.toExportDto(trackPoints: List<ExportTrackPointDto>): ExportSessionDto =
    ExportSessionDto(
        externalId = externalId,
        status = status.name,
        startedAtEpochMillis = startedAtEpochMillis,
        endedAtEpochMillis = endedAtEpochMillis,
        stats =
            ExportRunStatsDto(
                durationMillis = durationMillis,
                distanceMeters = distanceMeters,
                averagePaceSecPerKm = averagePaceSecPerKm,
                maxSpeedMps = maxSpeedMps,
                calorieKcal = calorieKcal,
            ),
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        trackPoints = trackPoints,
    )

fun TrackPointEntity.toExportDto(): ExportTrackPointDto =
    ExportTrackPointDto(
        externalId = externalId,
        sequence = sequence,
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = altitudeMeters,
        accuracyMeters = accuracyMeters,
        speedMps = speedMps,
        recordedAtEpochMillis = recordedAtEpochMillis,
    )
