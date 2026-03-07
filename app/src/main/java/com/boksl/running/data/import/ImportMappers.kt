package com.boksl.running.data.import

import com.boksl.running.data.export.ExportAppPreferencesDto
import com.boksl.running.data.export.ExportProfileDto
import com.boksl.running.data.export.ExportSessionDto
import com.boksl.running.data.export.ExportTrackPointDto
import com.boksl.running.data.local.db.entity.RunningSessionEntity
import com.boksl.running.data.local.db.entity.TrackPointEntity
import com.boksl.running.domain.model.AppPreferences
import com.boksl.running.domain.model.Gender
import com.boksl.running.domain.model.Profile
import com.boksl.running.domain.model.SessionStatus

fun ExportProfileDto.toDomain(): Profile =
    Profile(
        weightKg = weightKg,
        gender = Gender.entries.firstOrNull { it.name == gender } ?: Gender.UNSPECIFIED,
        age = age,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

fun ExportAppPreferencesDto.toDomain(): AppPreferences =
    AppPreferences(
        onboardingCompleted = onboardingCompleted,
        locationRationaleShown = locationRationaleShown,
    )

fun ExportSessionDto.toEntity(): RunningSessionEntity =
    RunningSessionEntity(
        externalId = externalId,
        status = SessionStatus.valueOf(status),
        startedAtEpochMillis = startedAtEpochMillis,
        endedAtEpochMillis = endedAtEpochMillis,
        durationMillis = stats.durationMillis,
        distanceMeters = stats.distanceMeters,
        averagePaceSecPerKm = stats.averagePaceSecPerKm,
        maxSpeedMps = stats.maxSpeedMps,
        calorieKcal = stats.calorieKcal,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

fun ExportTrackPointDto.toEntity(sessionId: Long): TrackPointEntity =
    TrackPointEntity(
        externalId = externalId,
        sessionId = sessionId,
        sequence = sequence,
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = altitudeMeters,
        accuracyMeters = accuracyMeters,
        speedMps = speedMps,
        recordedAtEpochMillis = recordedAtEpochMillis,
    )
