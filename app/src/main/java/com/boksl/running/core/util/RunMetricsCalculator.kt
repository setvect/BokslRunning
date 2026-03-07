package com.boksl.running.core.util

import com.boksl.running.domain.model.LocationSample
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0
private const val MIN_DISTANCE_FOR_PACE_METERS = 5.0

fun calculateSegmentDistanceMeters(
    previous: LocationSample,
    current: LocationSample,
): Double {
    val latitudeDelta = (current.latitude - previous.latitude).toRadians()
    val longitudeDelta = (current.longitude - previous.longitude).toRadians()
    val latitude1 = previous.latitude.toRadians()
    val latitude2 = current.latitude.toRadians()

    val haversine =
        sin(latitudeDelta / 2).pow(2.0) +
            cos(latitude1) * cos(latitude2) * sin(longitudeDelta / 2).pow(2.0)
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(haversine))
}

fun calculateCurrentPaceSecPerKm(
    samples: List<LocationSample>,
    windowMillis: Long = 10_000L,
): Double? {
    if (samples.size < 2) return null

    val latestTimestamp = samples.last().recordedAtEpochMillis
    val windowSamples = samples.filter { latestTimestamp - it.recordedAtEpochMillis <= windowMillis }
    if (windowSamples.size < 2) return null

    var distanceMeters = 0.0
    for (index in 1 until windowSamples.size) {
        distanceMeters += calculateSegmentDistanceMeters(windowSamples[index - 1], windowSamples[index])
    }
    if (distanceMeters < MIN_DISTANCE_FOR_PACE_METERS) return null

    val durationMillis = windowSamples.last().recordedAtEpochMillis - windowSamples.first().recordedAtEpochMillis
    if (durationMillis <= 0L) return null

    return (durationMillis / 1_000.0) / (distanceMeters / 1_000.0)
}

fun calculateAveragePaceSecPerKm(
    distanceMeters: Double,
    durationMillis: Long,
): Double? {
    if (distanceMeters <= 0.0 || durationMillis <= 0L) return null
    return (durationMillis / 1_000.0) / (distanceMeters / 1_000.0)
}

fun estimateCaloriesKcal(
    weightKg: Float,
    averageSpeedMps: Double,
    durationMillis: Long,
): Double {
    if (weightKg <= 0f || durationMillis <= 0L) return 0.0
    val met = resolveMetBySpeed(averageSpeedMps)
    val minutes = durationMillis / 60_000.0
    return met * 3.5 * weightKg / 200.0 * minutes
}

fun resolveMaxSpeedMps(
    previous: LocationSample?,
    current: LocationSample,
): Double {
    val directSpeed = current.speedMps?.toDouble() ?: 0.0
    val segmentSpeed =
        if (previous == null) {
            0.0
        } else {
            val durationSeconds =
                (current.recordedAtEpochMillis - previous.recordedAtEpochMillis).coerceAtLeast(1L) / 1_000.0
            calculateSegmentDistanceMeters(previous, current) / durationSeconds
        }
    return max(directSpeed, segmentSpeed)
}

private fun resolveMetBySpeed(averageSpeedMps: Double): Double =
    when {
        averageSpeedMps < 2.2352 -> 6.0
        averageSpeedMps < 2.68224 -> 8.3
        averageSpeedMps < 3.12928 -> 9.8
        averageSpeedMps < 3.57632 -> 11.0
        averageSpeedMps < 4.02336 -> 11.8
        else -> 12.8
    }

private fun Double.toRadians(): Double = this * PI / 180.0
