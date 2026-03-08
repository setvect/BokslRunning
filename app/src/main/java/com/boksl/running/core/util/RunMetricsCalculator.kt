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
private const val METERS_PER_KILOMETER = 1_000.0
private const val MIN_DISTANCE_FOR_PACE_METERS = 5.0
private const val MIN_DISTANCE_FOR_SPEED_METERS = 3.0
private const val MIN_DURATION_FOR_SPEED_MILLIS = 2_000L
private const val MAX_PLAUSIBLE_SPEED_MPS = 12.5
private const val MIN_SAMPLE_COUNT = 2
private const val MILLIS_PER_SECOND = 1_000.0
private const val MILLIS_PER_MINUTE = 60_000.0
private const val CALORIE_DIVISOR = 200.0
private const val OXYGEN_CONSUMPTION_FACTOR = 3.5
private const val DEGREES_PER_HALF_CIRCLE = 180.0
private val MET_THRESHOLDS =
    listOf(
        SpeedMetThreshold(maxSpeedMps = 2.2352, met = 6.0),
        SpeedMetThreshold(maxSpeedMps = 2.68224, met = 8.3),
        SpeedMetThreshold(maxSpeedMps = 3.12928, met = 9.8),
        SpeedMetThreshold(maxSpeedMps = 3.57632, met = 11.0),
        SpeedMetThreshold(maxSpeedMps = 4.02336, met = 11.8),
    )
private const val DEFAULT_MET = 12.8

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
    if (samples.size < MIN_SAMPLE_COUNT) return null

    val latestTimestamp = samples.last().recordedAtEpochMillis
    val windowSamples = samples.filter { latestTimestamp - it.recordedAtEpochMillis <= windowMillis }
    val distanceMeters = windowSamples.zipWithNext(::calculateSegmentDistanceMeters).sum()
    val durationMillis =
        windowSamples.lastOrNull()?.recordedAtEpochMillis?.minus(
            windowSamples.firstOrNull()?.recordedAtEpochMillis ?: 0L,
        ) ?: 0L

    return when {
        windowSamples.size < MIN_SAMPLE_COUNT -> null
        distanceMeters < MIN_DISTANCE_FOR_PACE_METERS -> null
        durationMillis <= 0L -> null
        else -> (durationMillis / MILLIS_PER_SECOND) / (distanceMeters / METERS_PER_KILOMETER)
    }
}

fun calculateAveragePaceSecPerKm(
    distanceMeters: Double,
    durationMillis: Long,
): Double? {
    if (distanceMeters <= 0.0 || durationMillis <= 0L) return null
    return (durationMillis / MILLIS_PER_SECOND) / (distanceMeters / METERS_PER_KILOMETER)
}

fun estimateCaloriesKcal(
    weightKg: Float,
    averageSpeedMps: Double,
    durationMillis: Long,
): Double {
    if (weightKg <= 0f || durationMillis <= 0L) return 0.0
    val met = resolveMetBySpeed(averageSpeedMps)
    val minutes = durationMillis / MILLIS_PER_MINUTE
    return met * OXYGEN_CONSUMPTION_FACTOR * weightKg / CALORIE_DIVISOR * minutes
}

fun resolveMaxSpeedMps(
    previous: LocationSample?,
    current: LocationSample,
): Double {
    val directSpeed =
        current.speedMps
            ?.toDouble()
            ?.takeIf { it in 0.0..MAX_PLAUSIBLE_SPEED_MPS }
            ?: 0.0
    val segmentSpeed =
        if (previous == null) {
            0.0
        } else {
            val durationMillis = current.recordedAtEpochMillis - previous.recordedAtEpochMillis
            val distanceMeters = calculateSegmentDistanceMeters(previous, current)
            if (durationMillis < MIN_DURATION_FOR_SPEED_MILLIS || distanceMeters < MIN_DISTANCE_FOR_SPEED_METERS) {
                0.0
            } else {
                val resolved = distanceMeters / (durationMillis / MILLIS_PER_SECOND)
                resolved.takeIf { it <= MAX_PLAUSIBLE_SPEED_MPS } ?: 0.0
            }
        }
    return max(directSpeed, segmentSpeed)
}

private fun resolveMetBySpeed(averageSpeedMps: Double): Double =
    MET_THRESHOLDS.firstOrNull { threshold -> averageSpeedMps < threshold.maxSpeedMps }?.met ?: DEFAULT_MET

private fun Double.toRadians(): Double = this * PI / DEGREES_PER_HALF_CIRCLE

private data class SpeedMetThreshold(
    val maxSpeedMps: Double,
    val met: Double,
)
