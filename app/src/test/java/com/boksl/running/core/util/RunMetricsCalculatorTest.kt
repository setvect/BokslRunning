package com.boksl.running.core.util

import com.boksl.running.domain.model.LocationSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunMetricsCalculatorTest {
    @Test
    fun calculateSegmentDistanceMetersReturnsZeroForSamePoint() {
        val sample = locationSample(latitude = 37.0, longitude = 127.0, time = 1_000L)
        assertEquals(0.0, calculateSegmentDistanceMeters(sample, sample), 0.0)
    }

    @Test
    fun calculateCurrentPaceUsesRecentWindow() {
        val samples =
            listOf(
                locationSample(latitude = 37.0, longitude = 127.0, time = 0L),
                locationSample(latitude = 37.0009, longitude = 127.0, time = 30_000L),
                locationSample(latitude = 37.0018, longitude = 127.0, time = 36_000L),
            )

        val pace = calculateCurrentPaceSecPerKm(samples, windowMillis = 10_000L)
        assertTrue(pace != null)
        assertTrue((pace ?: 0.0) > 0.0)
    }

    @Test
    fun calculateCurrentPaceReturnsNullWhenDistanceTooShort() {
        val samples =
            listOf(
                locationSample(latitude = 37.0, longitude = 127.0, time = 0L),
                locationSample(latitude = 37.000001, longitude = 127.0, time = 5_000L),
            )

        assertNull(calculateCurrentPaceSecPerKm(samples))
    }

    @Test
    fun estimateCaloriesKcalReturnsPositiveValueForValidInput() {
        val calories =
            estimateCaloriesKcal(
                weightKg = 70f,
                averageSpeedMps = 3.0,
                durationMillis = 600_000L,
            )

        assertTrue(calories > 0.0)
    }

    @Test
    fun resolveMaxSpeedMpsIgnoresTooShortInitialSegment() {
        val previous = locationSample(latitude = 37.0, longitude = 127.0, time = 1_000L)
        val current = locationSample(latitude = 37.0009, longitude = 127.0, time = 1_500L)

        assertEquals(3.0, resolveMaxSpeedMps(previous, current), 0.0)
    }

    @Test
    fun resolveMaxSpeedMpsIgnoresImplausibleSegmentSpeedSpike() {
        val previous = locationSample(latitude = 37.0, longitude = 127.0, time = 1_000L)
        val current =
            LocationSample(
                latitude = 37.01,
                longitude = 127.0,
                accuracyMeters = 5f,
                speedMps = null,
                altitudeMeters = null,
                recordedAtEpochMillis = 4_000L,
            )

        assertEquals(0.0, resolveMaxSpeedMps(previous, current), 0.0)
    }

    private fun locationSample(
        latitude: Double,
        longitude: Double,
        time: Long,
    ): LocationSample =
        LocationSample(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = 5f,
            speedMps = 3f,
            altitudeMeters = null,
            recordedAtEpochMillis = time,
        )
}
