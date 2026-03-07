package com.boksl.running.domain.model

data class RunSnapshot(
    val state: RunEngineState,
    val sessionId: Long?,
    val startedAtEpochMillis: Long?,
    val durationMillis: Long,
    val latestLocation: LocationSample?,
    val totalDistanceMeters: Double,
    val currentPaceSecPerKm: Double?,
    val averagePaceSecPerKm: Double?,
    val maxSpeedMps: Double,
    val calorieKcal: Double?,
    val lastFlushAtEpochMillis: Long?,
    val pointCount: Int,
)
