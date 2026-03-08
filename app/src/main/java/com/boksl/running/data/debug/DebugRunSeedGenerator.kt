@file:Suppress("LongMethod", "LongParameterList", "MagicNumber", "ReturnCount")

package com.boksl.running.data.debug

import androidx.room.withTransaction
import com.boksl.running.core.di.IoDispatcher
import com.boksl.running.core.util.calculateAveragePaceSecPerKm
import com.boksl.running.core.util.estimateCaloriesKcal
import com.boksl.running.data.local.db.AppDatabase
import com.boksl.running.data.local.db.dao.RunningSessionDao
import com.boksl.running.domain.model.Profile
import com.boksl.running.domain.model.RunStats
import com.boksl.running.domain.model.RunningSession
import com.boksl.running.domain.model.SessionStatus
import com.boksl.running.domain.model.TrackPoint
import com.boksl.running.domain.repository.ProfileRepository
import com.boksl.running.domain.repository.RunningRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

interface DebugSeedManager {
    suspend fun regenerateLastYear(): DebugSeedOperationResult

    suspend fun deleteSeedData(): DebugSeedOperationResult
}

@Singleton
class DebugRunSeedGenerator
    @Inject
    constructor(
        private val appDatabase: AppDatabase,
        private val runningSessionDao: RunningSessionDao,
        private val runningRepository: RunningRepository,
        private val profileRepository: ProfileRepository,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val clock: Clock,
    ) : DebugSeedManager {
        override suspend fun regenerateLastYear(): DebugSeedOperationResult =
            withContext(ioDispatcher) {
                val deletedSessionCount =
                    appDatabase.withTransaction {
                        runningSessionDao.deleteByExternalIdPattern(SESSION_EXTERNAL_ID_PATTERN)
                    }

                val weightKg = profileRepository.observeProfile().first().resolveSeedWeightKg()
                val seedRuns = generateSeedRuns(weightKg)
                var generatedTrackPointCount = 0

                seedRuns.forEach { seedRun ->
                    val sessionId = runningRepository.insertSession(seedRun.session)
                    runningRepository.insertTrackPoints(
                        seedRun.trackPoints.map { point -> point.copy(sessionId = sessionId) },
                    )
                    generatedTrackPointCount += seedRun.trackPoints.size
                }

                DebugSeedOperationResult(
                    deletedSessionCount = deletedSessionCount,
                    generatedSessionCount = seedRuns.size,
                    generatedTrackPointCount = generatedTrackPointCount,
                )
            }

        override suspend fun deleteSeedData(): DebugSeedOperationResult =
            withContext(ioDispatcher) {
                val deletedSessionCount =
                    appDatabase.withTransaction {
                        runningSessionDao.deleteByExternalIdPattern(SESSION_EXTERNAL_ID_PATTERN)
                    }

                DebugSeedOperationResult(
                    deletedSessionCount = deletedSessionCount,
                    generatedSessionCount = 0,
                    generatedTrackPointCount = 0,
                )
            }

        private fun generateSeedRuns(weightKg: Float): List<SeedRun> {
            val today = LocalDate.now(clock)
            val endDate = if (today.dayOfMonth > 1) today.minusDays(1) else today
            val startDate = YearMonth.from(endDate).minusMonths(MONTH_COUNT - 1L).atDay(1)
            val firstWeekStart = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

            val runs = mutableListOf<SeedRun>()
            var weekStart = firstWeekStart

            while (!weekStart.isAfter(endDate)) {
                val weekIndex = ((weekStart.toEpochDay() - firstWeekStart.toEpochDay()) / 7L).toInt()
                resolveRunOffsets(weekIndex).forEachIndexed { runIndex, dayOffset ->
                    val runDate = weekStart.plusDays(dayOffset.toLong())
                    if (runDate < startDate || runDate > endDate) return@forEachIndexed
                    runs +=
                        buildSeedRun(
                            runDate = runDate,
                            weekIndex = weekIndex,
                            runIndex = runIndex,
                            weightKg = weightKg,
                        )
                }
                weekStart = weekStart.plusWeeks(1)
            }

            return runs.sortedBy { run -> run.session.startedAtEpochMillis }
        }

        private fun buildSeedRun(
            runDate: LocalDate,
            weekIndex: Int,
            runIndex: Int,
            weightKg: Float,
        ): SeedRun {
            val runSeed = positiveSeed("run", runDate.toEpochDay(), weekIndex.toLong(), runIndex.toLong())
            val isWeekend = runDate.dayOfWeek == DayOfWeek.SATURDAY || runDate.dayOfWeek == DayOfWeek.SUNDAY
            val seasonalDistanceMultiplier =
                when (runDate.monthValue) {
                    in 3..5 -> 1.08
                    in 9..11 -> 1.05
                    else -> 0.96
                }
            val distanceKm =
                (
                    if (isWeekend) {
                        7.4 + deterministicDouble("weekend-distance", runSeed, 0.0, 5.6)
                    } else {
                        3.2 + deterministicDouble("weekday-distance", runSeed, 0.0, 4.8)
                    }
                ) * seasonalDistanceMultiplier
            val clampedDistanceKm = distanceKm.coerceIn(MIN_DISTANCE_KM, MAX_DISTANCE_KM)
            val distanceMeters = clampedDistanceKm * METERS_PER_KILOMETER
            val paceSecPerKm =
                if (isWeekend) {
                    355.0 + deterministicDouble("weekend-pace", runSeed, 0.0, 95.0)
                } else {
                    330.0 + deterministicDouble("weekday-pace", runSeed, 0.0, 85.0)
                }
            val durationMillis = (paceSecPerKm * clampedDistanceKm * MILLIS_PER_SECOND).roundToInt().toLong()
            val averagePaceSecPerKm = calculateAveragePaceSecPerKm(distanceMeters, durationMillis)
            val averageSpeedMps = distanceMeters / (durationMillis / MILLIS_PER_SECOND.toDouble())
            val maxSpeedMps = averageSpeedMps * (1.08 + deterministicDouble("max-speed", runSeed, 0.0, 0.18))
            val calorieKcal =
                estimateCaloriesKcal(
                    weightKg = weightKg,
                    averageSpeedMps = averageSpeedMps,
                    durationMillis = durationMillis,
                )
            val sessionStart =
                resolveSessionStartDateTime(
                    runDate = runDate,
                    isWeekend = isWeekend,
                    runSeed = runSeed,
                )
            val startedAtEpochMillis = sessionStart.toInstant().toEpochMilli()
            val endedAtEpochMillis = startedAtEpochMillis + durationMillis
            val sessionExternalId = "$SESSION_EXTERNAL_ID_PREFIX-$runDate-$weekIndex-$runIndex"
            val trackPointCount =
                (distanceMeters / 260.0).roundToInt().coerceIn(
                    MIN_TRACK_POINT_COUNT,
                    MAX_TRACK_POINT_COUNT,
                )
            val trackPoints =
                buildTrackPoints(
                    sessionExternalId = sessionExternalId,
                    routeSeed = runSeed,
                    targetDistanceMeters = distanceMeters,
                    pointCount = trackPointCount,
                    averageSpeedMps = averageSpeedMps,
                    startedAtEpochMillis = startedAtEpochMillis,
                    durationMillis = durationMillis,
                )

            return SeedRun(
                session =
                    RunningSession(
                        externalId = sessionExternalId,
                        status = SessionStatus.SAVED,
                        startedAtEpochMillis = startedAtEpochMillis,
                        endedAtEpochMillis = endedAtEpochMillis,
                        stats =
                            RunStats(
                                durationMillis = durationMillis,
                                distanceMeters = distanceMeters,
                                averagePaceSecPerKm = averagePaceSecPerKm,
                                maxSpeedMps = maxSpeedMps,
                                calorieKcal = calorieKcal,
                            ),
                        createdAtEpochMillis = startedAtEpochMillis,
                        updatedAtEpochMillis = endedAtEpochMillis,
                    ),
                trackPoints = trackPoints,
            )
        }

        private fun resolveRunOffsets(weekIndex: Int): List<Int> {
            val weekPattern = positiveSeed("week-pattern", weekIndex.toLong()) % 10L
            val runCount =
                when (weekPattern.toInt()) {
                    0 -> 0
                    1, 2 -> 2 + (positiveSeed("light-week", weekIndex.toLong()) % 2L).toInt()
                    else -> 3 + (positiveSeed("normal-week", weekIndex.toLong()) % 3L).toInt()
                }
            val candidateOffsets =
                if (weekIndex % 2 == 0) {
                    EVEN_WEEK_RUN_OFFSETS
                } else {
                    ODD_WEEK_RUN_OFFSETS
                }
            return candidateOffsets.take(runCount)
        }

        private fun resolveSessionStartDateTime(
            runDate: LocalDate,
            isWeekend: Boolean,
            runSeed: Long,
        ): ZonedDateTime {
            val time =
                if (isWeekend) {
                    LocalTime.of(
                        7 + deterministicInt("weekend-hour", runSeed, 0, 2),
                        deterministicInt("weekend-minute", runSeed, 0, 3) * 10,
                    )
                } else {
                    LocalTime.of(
                        18 + deterministicInt("weekday-hour", runSeed, 0, 2),
                        deterministicInt("weekday-minute", runSeed, 0, 5) * 5,
                    )
                }
            return runDate.atTime(time).atZone(clock.zone)
        }

        private fun buildTrackPoints(
            sessionExternalId: String,
            routeSeed: Long,
            targetDistanceMeters: Double,
            pointCount: Int,
            averageSpeedMps: Double,
            startedAtEpochMillis: Long,
            durationMillis: Long,
        ): List<TrackPoint> {
            val centerIndex = (positiveSeed("center", routeSeed) % ROUTE_CENTERS.size.toLong()).toInt()
            val templateIndex = (positiveSeed("template", routeSeed) % ROUTE_TEMPLATES.size.toLong()).toInt()
            val center = ROUTE_CENTERS[centerIndex]
            val baseTemplate = ROUTE_TEMPLATES[templateIndex]
            val loopCount = max(1, (targetDistanceMeters / baseTemplate.lengthMeters).roundToInt())
            val scale = targetDistanceMeters / (baseTemplate.lengthMeters * loopCount)
            val rotationRad = deterministicDouble("rotation", routeSeed, 0.0, PI * 2)
            val mirror = positiveSeed("mirror", routeSeed) % 2L == 0L
            val shiftX = deterministicDouble("shift-x", routeSeed, -120.0, 120.0)
            val shiftY = deterministicDouble("shift-y", routeSeed, -120.0, 120.0)
            val transformedTemplate =
                baseTemplate.points.map { point ->
                    val scaled =
                        if (mirror) {
                            MeterPoint(
                                -point.xMeters * scale,
                                point.yMeters * scale,
                            )
                        } else {
                            MeterPoint(point.xMeters * scale, point.yMeters * scale)
                        }
                    scaled.rotate(rotationRad).shiftedBy(shiftX, shiftY)
                }
            val repeatedPolyline = transformedTemplate.repeat(loopCount)
            val sampledPoints = repeatedPolyline.sample(pointCount)
            val sessionToken =
                sessionExternalId.removePrefix(
                    "$SESSION_EXTERNAL_ID_PREFIX-",
                )

            return sampledPoints.mapIndexed { index, point ->
                val progress =
                    if (pointCount == 1) {
                        0.0
                    } else {
                        index.toDouble() / (pointCount - 1).toDouble()
                    }
                TrackPoint(
                    externalId = "$TRACK_POINT_EXTERNAL_ID_PREFIX-$sessionToken-$index",
                    sessionId = 0L,
                    sequence = index,
                    latitude = center.latitude + point.yMeters.toLatitudeDegrees(),
                    longitude =
                        center.longitude + point.xMeters.toLongitudeDegrees(center.latitude),
                    altitudeMeters = null,
                    accuracyMeters =
                        (
                            3.0 +
                                deterministicDouble(
                                    "accuracy",
                                    routeSeed + index.toLong(),
                                    0.0,
                                    5.0,
                                )
                        ).toFloat(),
                    speedMps =
                        (
                            averageSpeedMps *
                                (
                                    0.92 +
                                        deterministicDouble(
                                            "speed",
                                            routeSeed + index.toLong(),
                                            0.0,
                                            0.16,
                                        )
                                )
                        ).toFloat(),
                    recordedAtEpochMillis =
                        startedAtEpochMillis +
                            (durationMillis.toDouble() * progress).roundToInt().toLong(),
                )
            }
        }
    }

data class DebugSeedOperationResult(
    val deletedSessionCount: Int,
    val generatedSessionCount: Int,
    val generatedTrackPointCount: Int,
)

private data class SeedRun(
    val session: RunningSession,
    val trackPoints: List<TrackPoint>,
)

private data class RouteCenter(
    val latitude: Double,
    val longitude: Double,
)

private data class RouteTemplate(
    val points: List<MeterPoint>,
) {
    val lengthMeters: Double =
        points
            .zipWithNext()
            .sumOf { (start, end) -> start.distanceTo(end) }
}

private data class MeterPoint(
    val xMeters: Double,
    val yMeters: Double,
) {
    fun distanceTo(other: MeterPoint): Double {
        val deltaX = other.xMeters - xMeters
        val deltaY = other.yMeters - yMeters
        return kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)
    }

    fun rotate(angleRadians: Double): MeterPoint =
        MeterPoint(
            xMeters = xMeters * cos(angleRadians) - yMeters * sin(angleRadians),
            yMeters = xMeters * sin(angleRadians) + yMeters * cos(angleRadians),
        )

    fun shiftedBy(
        deltaXMeters: Double,
        deltaYMeters: Double,
    ): MeterPoint =
        MeterPoint(
            xMeters = xMeters + deltaXMeters,
            yMeters = yMeters + deltaYMeters,
        )
}

private fun Profile?.resolveSeedWeightKg(): Float = this?.weightKg?.takeIf { it > 0f } ?: DEFAULT_WEIGHT_KG

private fun positiveSeed(
    label: String,
    vararg values: Long,
): Long {
    var accumulator = label.hashCode().toLong()
    values.forEach { value ->
        accumulator = accumulator * 1_103_515_245L + value * 12_345L + 7_046_029_254_386_353_131L
    }
    return accumulator and Long.MAX_VALUE
}

private fun deterministicDouble(
    label: String,
    seed: Long,
    minValue: Double,
    maxValue: Double,
): Double {
    val normalized = (positiveSeed(label, seed) % 10_000L) / 10_000.0
    return minValue + (maxValue - minValue) * normalized
}

private fun deterministicInt(
    label: String,
    seed: Long,
    minValue: Int,
    maxValue: Int,
): Int {
    val range = maxValue - minValue + 1
    return minValue + (positiveSeed(label, seed) % range.toLong()).toInt()
}

private fun List<MeterPoint>.repeat(loopCount: Int): List<MeterPoint> {
    if (isEmpty()) return emptyList()
    val originalPoints = this
    return buildList {
        repeat(loopCount) { loopIndex ->
            if (loopIndex == 0) {
                addAll(originalPoints)
            } else {
                addAll(originalPoints.drop(1))
            }
        }
    }
}

private fun List<MeterPoint>.sample(pointCount: Int): List<MeterPoint> {
    if (isEmpty()) return emptyList()
    if (size == 1 || pointCount <= 1) return listOf(first())

    val cumulativeDistances = mutableListOf(0.0)
    for (index in 1 until size) {
        cumulativeDistances += cumulativeDistances.last() + this[index - 1].distanceTo(this[index])
    }

    val totalDistance = cumulativeDistances.last().takeIf { it > 0.0 } ?: return List(pointCount) { first() }
    return List(pointCount) { pointIndex ->
        val targetDistance = totalDistance * pointIndex / (pointCount - 1).toDouble()
        interpolatePoint(targetDistance = targetDistance, polyline = this, cumulativeDistances = cumulativeDistances)
    }
}

private fun interpolatePoint(
    targetDistance: Double,
    polyline: List<MeterPoint>,
    cumulativeDistances: List<Double>,
): MeterPoint {
    val segmentIndex =
        cumulativeDistances.indexOfFirst { distance -> distance >= targetDistance }.let { resolved ->
            if (resolved <= 0) 1 else resolved
        }
    val startIndex = segmentIndex - 1
    val segmentStartDistance = cumulativeDistances[startIndex]
    val segmentEndDistance = cumulativeDistances[segmentIndex]
    val segmentLength = segmentEndDistance - segmentStartDistance
    if (segmentLength <= 0.0) return polyline[segmentIndex]
    val segmentProgress = (targetDistance - segmentStartDistance) / segmentLength
    val start = polyline[startIndex]
    val end = polyline[segmentIndex]
    return MeterPoint(
        xMeters = start.xMeters + (end.xMeters - start.xMeters) * segmentProgress,
        yMeters = start.yMeters + (end.yMeters - start.yMeters) * segmentProgress,
    )
}

private fun Double.toLatitudeDegrees(): Double = this / METERS_PER_LATITUDE_DEGREE

private fun Double.toLongitudeDegrees(latitude: Double): Double =
    this / (METERS_PER_LATITUDE_DEGREE * cos(latitude * PI / 180.0))

private const val MONTH_COUNT = 12L
private const val DEFAULT_WEIGHT_KG = 70f
private const val MIN_DISTANCE_KM = 3.0
private const val MAX_DISTANCE_KM = 14.0
private const val METERS_PER_KILOMETER = 1_000.0
private const val MILLIS_PER_SECOND = 1_000
private const val METERS_PER_LATITUDE_DEGREE = 111_320.0
private const val MIN_TRACK_POINT_COUNT = 20
private const val MAX_TRACK_POINT_COUNT = 60

const val SESSION_EXTERNAL_ID_PREFIX: String = "debug-seed-session"
const val TRACK_POINT_EXTERNAL_ID_PREFIX: String = "debug-seed-point"

private const val SESSION_EXTERNAL_ID_PATTERN = "$SESSION_EXTERNAL_ID_PREFIX-%"

private val EVEN_WEEK_RUN_OFFSETS = listOf(1, 3, 5, 6, 0)
private val ODD_WEEK_RUN_OFFSETS = listOf(0, 2, 4, 5, 6)

private val ROUTE_CENTERS =
    listOf(
        RouteCenter(latitude = 37.5245, longitude = 126.9272),
        RouteCenter(latitude = 37.5456, longitude = 127.0409),
        RouteCenter(latitude = 37.5144, longitude = 127.1059),
    )

private val ROUTE_TEMPLATES =
    listOf(
        RouteTemplate(
            points =
                listOf(
                    MeterPoint(0.0, 0.0),
                    MeterPoint(120.0, 90.0),
                    MeterPoint(280.0, 80.0),
                    MeterPoint(420.0, -10.0),
                    MeterPoint(350.0, -180.0),
                    MeterPoint(140.0, -260.0),
                    MeterPoint(-80.0, -180.0),
                    MeterPoint(-150.0, -20.0),
                    MeterPoint(0.0, 0.0),
                ),
        ),
        RouteTemplate(
            points =
                listOf(
                    MeterPoint(0.0, 0.0),
                    MeterPoint(80.0, 180.0),
                    MeterPoint(250.0, 250.0),
                    MeterPoint(390.0, 150.0),
                    MeterPoint(420.0, -30.0),
                    MeterPoint(260.0, -180.0),
                    MeterPoint(50.0, -220.0),
                    MeterPoint(-120.0, -120.0),
                    MeterPoint(-150.0, 40.0),
                    MeterPoint(0.0, 0.0),
                ),
        ),
        RouteTemplate(
            points =
                listOf(
                    MeterPoint(0.0, 0.0),
                    MeterPoint(160.0, 60.0),
                    MeterPoint(320.0, 0.0),
                    MeterPoint(420.0, -120.0),
                    MeterPoint(520.0, -40.0),
                    MeterPoint(620.0, 140.0),
                    MeterPoint(520.0, 260.0),
                    MeterPoint(340.0, 220.0),
                    MeterPoint(160.0, 100.0),
                    MeterPoint(0.0, 0.0),
                ),
        ),
    )
