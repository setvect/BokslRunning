package com.boksl.running.domain.model

data class LocationSample(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val speedMps: Float?,
    val altitudeMeters: Double?,
    val recordedAtEpochMillis: Long,
)
