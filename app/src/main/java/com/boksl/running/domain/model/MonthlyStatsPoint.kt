package com.boksl.running.domain.model

import java.time.YearMonth

data class MonthlyStatsPoint(
    val yearMonth: YearMonth,
    val totalDistanceMeters: Double,
    val totalDurationMillis: Long,
    val averageSpeedMps: Double,
)
