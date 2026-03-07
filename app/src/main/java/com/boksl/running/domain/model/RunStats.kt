package com.boksl.running.domain.model

data class RunStats(
    val durationMillis: Long,
    val distanceMeters: Double,
    val averagePaceSecPerKm: Double?,
    val maxSpeedMps: Double,
    val calorieKcal: Double?,
)
