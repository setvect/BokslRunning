package com.boksl.running.domain.model

data class TrackPoint(
    val id: Long = 0L,
    val externalId: String,
    val sessionId: Long,
    val sequence: Int,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val accuracyMeters: Float?,
    val speedMps: Float?,
    val recordedAtEpochMillis: Long,
)
