package com.boksl.running.data.local.db.dao

data class MonthlyStatsProjection(
    val yearMonth: String,
    val totalDistanceMeters: Double?,
    val totalDurationMillis: Long?,
)
