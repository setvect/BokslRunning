package com.boksl.running.data.mapper

import com.boksl.running.data.local.db.entity.RunningSessionEntity
import com.boksl.running.data.local.db.entity.TrackPointEntity
import com.boksl.running.domain.model.RunStats
import com.boksl.running.domain.model.RunningSession
import com.boksl.running.domain.model.TrackPoint

fun RunningSessionEntity.toDomain(): RunningSession =
    RunningSession(
        id = id,
        externalId = externalId,
        status = status,
        startedAtEpochMillis = startedAtEpochMillis,
        endedAtEpochMillis = endedAtEpochMillis,
        stats =
            RunStats(
                durationMillis = durationMillis,
                distanceMeters = distanceMeters,
                averagePaceSecPerKm = averagePaceSecPerKm,
                maxSpeedMps = maxSpeedMps,
                calorieKcal = calorieKcal,
            ),
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

fun RunningSession.toEntity(): RunningSessionEntity =
    RunningSessionEntity(
        id = id,
        externalId = externalId,
        status = status,
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

fun TrackPointEntity.toDomain(): TrackPoint =
    TrackPoint(
        id = id,
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

fun TrackPoint.toEntity(): TrackPointEntity =
    TrackPointEntity(
        id = id,
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
