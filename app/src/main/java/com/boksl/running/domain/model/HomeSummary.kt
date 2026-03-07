package com.boksl.running.domain.model

data class HomeSummary(
    val totalDistanceMeters: Double,
    val totalDurationMillis: Long,
    val averageSpeedMps: Double,
    val totalCaloriesKcal: Double,
)
