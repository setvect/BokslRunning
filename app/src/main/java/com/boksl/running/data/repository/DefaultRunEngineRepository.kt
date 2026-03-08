package com.boksl.running.data.repository

import com.boksl.running.core.di.IoDispatcher
import com.boksl.running.core.util.calculateAveragePaceSecPerKm
import com.boksl.running.core.util.calculateCurrentPaceSecPerKm
import com.boksl.running.core.util.calculateSegmentDistanceMeters
import com.boksl.running.core.util.estimateCaloriesKcal
import com.boksl.running.core.util.resolveMaxSpeedMps
import com.boksl.running.data.location.LocationClient
import com.boksl.running.domain.model.LocationSample
import com.boksl.running.domain.model.RunEngineState
import com.boksl.running.domain.model.RunSnapshot
import com.boksl.running.domain.model.RunStats
import com.boksl.running.domain.model.RunningSession
import com.boksl.running.domain.model.SessionStatus
import com.boksl.running.domain.model.TrackPoint
import com.boksl.running.domain.repository.ProfileRepository
import com.boksl.running.domain.repository.RunEngineRepository
import com.boksl.running.domain.repository.RunningRepository
import com.boksl.running.ui.feature.run.RunTrackingServiceController
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
@Suppress("TooManyFunctions")
class DefaultRunEngineRepository
    @Inject
    constructor(
        private val runningRepository: RunningRepository,
        private val profileRepository: ProfileRepository,
        private val locationClient: LocationClient,
        private val serviceController: RunTrackingServiceController,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : RunEngineRepository {
        private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
        private val stateMutex = Mutex()
        private val activeRunState = MutableStateFlow<ActiveRunState?>(null)
        private var tickerJob: Job? = null
        private var recoveryJob: Job? = null

        init {
            recoverActiveSessionIfNeeded()
        }

        override fun observeActiveRun(): Flow<RunSnapshot?> = activeRunState.map { it?.toRunSnapshot() }

        override suspend fun prepareRun() {
            stateMutex.withLock {
                val existing = activeRunState.value
                if (existing != null && existing.state != RunEngineState.SAVED) return

                val currentLocation = locationClient.getCurrentLocation()
                activeRunState.value =
                    ActiveRunState(
                        state = RunEngineState.READY,
                        sessionId = null,
                        startedAtEpochMillis = null,
                        latestLocation = currentLocation,
                        totalDistanceMeters = 0.0,
                        currentPaceSecPerKm = null,
                        averagePaceSecPerKm = null,
                        maxSpeedMps = 0.0,
                        calorieKcal = null,
                        lastFlushAtEpochMillis = null,
                        pointCount = 0,
                        durationMillis = 0L,
                        lastLocationSample = currentLocation,
                        externalId = "",
                        createdAtEpochMillis = null,
                    )
            }
        }

        override suspend fun startRun() {
            stateMutex.withLock {
                val currentState = activeRunState.value ?: return
                if (currentState.state != RunEngineState.READY) return

                val now = now()
                val externalId = UUID.randomUUID().toString()
                val sessionId =
                    runningRepository.insertSession(
                        RunningSession(
                            externalId = externalId,
                            status = SessionStatus.IN_PROGRESS,
                            startedAtEpochMillis = now,
                            endedAtEpochMillis = null,
                            stats = emptyStats(),
                            createdAtEpochMillis = now,
                            updatedAtEpochMillis = now,
                        ),
                    )

                val runningState =
                    currentState.copy(
                        state = RunEngineState.RUNNING,
                        sessionId = sessionId,
                        startedAtEpochMillis = now,
                        lastFlushAtEpochMillis = now,
                        externalId = externalId,
                        createdAtEpochMillis = now,
                    )
                activeRunState.value = runningState
                currentState.latestLocation?.let { sample ->
                    appendLocationSampleLocked(sample)
                }
                startTrackingService()
                ensureTicker()
            }
        }

        override suspend fun requestStop() {
            stateMutex.withLock {
                val currentState = activeRunState.value ?: return
                if (currentState.state != RunEngineState.RUNNING) return

                flushPendingLocked(forceTimestamp = now())
                val flushedState = activeRunState.value ?: currentState
                activeRunState.value = flushedState.copy(state = RunEngineState.STOP_CONFIRM)
                stopTrackingService()
                stopTicker()
            }
        }

        override suspend fun confirmSave() {
            stateMutex.withLock {
                val currentState = activeRunState.value ?: return
                if (currentState.state != RunEngineState.STOP_CONFIRM) return

                val finishedAt = now()
                flushPendingLocked(forceTimestamp = finishedAt)
                val savedState = activeRunState.value ?: currentState
                runningRepository.updateSession(
                    savedState.toRunningSession(
                        status = SessionStatus.SAVED,
                        endedAtEpochMillis = finishedAt,
                        updatedAtEpochMillis = finishedAt,
                    ),
                )
                activeRunState.value =
                    savedState.copy(
                        state = RunEngineState.SAVED,
                        durationMillis =
                            savedState.startedAtEpochMillis?.let { finishedAt - it }
                                ?: savedState.durationMillis,
                    )
                stopTrackingService()
                stopTicker()
            }
        }

        override suspend fun cancelStop() {
            stateMutex.withLock {
                val currentState = activeRunState.value ?: return
                if (currentState.state != RunEngineState.STOP_CONFIRM) return

                activeRunState.value = currentState.copy(state = RunEngineState.RUNNING)
                startTrackingService()
                ensureTicker()
            }
        }

        override suspend fun discardRun() {
            stateMutex.withLock {
                val currentState = activeRunState.value ?: return
                stopTrackingService()
                stopTicker()

                val sessionId = currentState.sessionId
                if (sessionId != null) {
                    val discardedAt = now()
                    flushPendingLocked(forceTimestamp = discardedAt)
                    runningRepository.updateSession(
                        (activeRunState.value ?: currentState).toRunningSession(
                            status = SessionStatus.DISCARDED,
                            endedAtEpochMillis = discardedAt,
                            updatedAtEpochMillis = discardedAt,
                        ),
                    )
                }

                activeRunState.value = null
            }
        }

        override suspend fun resumeActiveRun() {
            recoverActiveSessionIfNeeded().join()
            stateMutex.withLock {
                val currentState = activeRunState.value ?: return
                if (currentState.state == RunEngineState.RUNNING || currentState.state == RunEngineState.STOP_CONFIRM) {
                    activeRunState.value = currentState.copy(state = RunEngineState.RUNNING)
                    startTrackingService()
                    ensureTicker()
                }
            }
        }

        suspend fun onLocationSample(sample: LocationSample) {
            stateMutex.withLock {
                val currentState = activeRunState.value ?: return
                if (currentState.state != RunEngineState.RUNNING || currentState.sessionId == null) return
                appendLocationSampleLocked(sample)
            }
        }

        fun shutdown() {
            stopTicker()
            scope.cancel()
        }

        private fun recoverActiveSessionIfNeeded(): Job {
            recoveryJob?.let { if (it.isActive) return it }
            recoveryJob =
                scope.launch {
                    val activeSession = runningRepository.getActiveSession() ?: return@launch
                    val trackPoints = runningRepository.observeTrackPoints(activeSession.id).first()
                    val lastSample = trackPoints.lastOrNull()?.toLocationSample()
                    val pointCount = trackPoints.size
                    val snapshot =
                        ActiveRunState(
                            state = RunEngineState.RUNNING,
                            sessionId = activeSession.id,
                            startedAtEpochMillis = activeSession.startedAtEpochMillis,
                            durationMillis = activeSession.stats.durationMillis,
                            latestLocation = lastSample,
                            totalDistanceMeters = activeSession.stats.distanceMeters,
                            currentPaceSecPerKm = null,
                            averagePaceSecPerKm = activeSession.stats.averagePaceSecPerKm,
                            maxSpeedMps = activeSession.stats.maxSpeedMps,
                            calorieKcal = activeSession.stats.calorieKcal,
                            lastFlushAtEpochMillis = activeSession.updatedAtEpochMillis,
                            pointCount = pointCount,
                            lastLocationSample = lastSample,
                            recentSamples = trackPoints.takeLast(RECENT_SAMPLE_LIMIT).map { it.toLocationSample() },
                            pendingTrackPoints = emptyList(),
                            externalId = activeSession.externalId,
                            createdAtEpochMillis = activeSession.createdAtEpochMillis,
                        )
                    stateMutex.withLock {
                        if (activeRunState.value == null || activeRunState.value?.state == RunEngineState.SAVED) {
                            activeRunState.value = snapshot
                        }
                    }
                }
            return recoveryJob as Job
        }

        @Suppress("ReturnCount")
        private suspend fun appendLocationSampleLocked(sample: LocationSample) {
            val currentState = activeRunState.value ?: return
            val sessionId = currentState.sessionId ?: return
            val startedAt = currentState.startedAtEpochMillis ?: return
            val previous = currentState.lastLocationSample
            val segmentDistance =
                if (previous == null) {
                    0.0
                } else {
                    calculateSegmentDistanceMeters(previous, sample)
                }
            val totalDistance = currentState.totalDistanceMeters + segmentDistance
            val durationMillis = max(0L, sample.recordedAtEpochMillis - startedAt)
            val recentSamples =
                (currentState.recentSamples + sample).filter {
                    sample.recordedAtEpochMillis - it.recordedAtEpochMillis <= CURRENT_PACE_WINDOW_MILLIS
                }
            val currentPace = calculateCurrentPaceSecPerKm(recentSamples, CURRENT_PACE_WINDOW_MILLIS)
            val averagePace = calculateAveragePaceSecPerKm(totalDistance, durationMillis)
            val averageSpeed = calculateAverageSpeed(totalDistance = totalDistance, durationMillis = durationMillis)
            val calorie = resolveCalories(averageSpeedMps = averageSpeed, durationMillis = durationMillis)
            val pointCount = currentState.pointCount + 1
            val candidateMaxSpeed = resolveMaxSpeedMps(previous, sample)
            val maxSpeed =
                if (pointCount <= INITIAL_MAX_SPEED_SAMPLE_COUNT) {
                    currentState.maxSpeedMps
                } else {
                    max(currentState.maxSpeedMps, candidateMaxSpeed)
                }

            val trackPoint = sample.toTrackPoint(sessionId = sessionId, pointCount = pointCount)

            activeRunState.value =
                currentState.copy(
                    latestLocation = sample,
                    totalDistanceMeters = totalDistance,
                    durationMillis = durationMillis,
                    currentPaceSecPerKm = currentPace,
                    averagePaceSecPerKm = averagePace,
                    maxSpeedMps = maxSpeed,
                    calorieKcal = calorie,
                    pointCount = pointCount,
                    lastLocationSample = sample,
                    recentSamples = recentSamples,
                    pendingTrackPoints = currentState.pendingTrackPoints + trackPoint,
                )
        }

        private fun calculateAverageSpeed(
            totalDistance: Double,
            durationMillis: Long,
        ): Double =
            if (durationMillis > 0L) {
                totalDistance / (durationMillis / MILLIS_PER_SECOND)
            } else {
                0.0
            }

        private fun LocationSample.toTrackPoint(
            sessionId: Long,
            pointCount: Int,
        ): TrackPoint =
            TrackPoint(
                externalId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                sequence = pointCount - 1,
                latitude = latitude,
                longitude = longitude,
                altitudeMeters = altitudeMeters,
                accuracyMeters = accuracyMeters,
                speedMps = speedMps,
                recordedAtEpochMillis = recordedAtEpochMillis,
            )

        private fun ensureTicker() {
            if (tickerJob?.isActive == true) return
            tickerJob =
                scope.launch {
                    while (true) {
                        delay(TICKER_INTERVAL_MILLIS)
                        var shouldStop = false
                        stateMutex.withLock {
                            val currentState = activeRunState.value
                            if (currentState == null) {
                                shouldStop = true
                            } else if (currentState.state == RunEngineState.RUNNING) {
                                val startedAt = currentState.startedAtEpochMillis
                                if (startedAt != null) {
                                    val now = now()
                                    val durationMillis = max(0L, now - startedAt)
                                    val averagePace =
                                        calculateAveragePaceSecPerKm(currentState.totalDistanceMeters, durationMillis)
                                    val averageSpeed =
                                        if (durationMillis > 0L) {
                                            currentState.totalDistanceMeters / (durationMillis / MILLIS_PER_SECOND)
                                        } else {
                                            0.0
                                        }
                                    activeRunState.value =
                                        currentState.copy(
                                            durationMillis = durationMillis,
                                            averagePaceSecPerKm = averagePace,
                                            calorieKcal = resolveCalories(averageSpeed, durationMillis),
                                        )

                                    val lastFlushAt = activeRunState.value?.lastFlushAtEpochMillis ?: now
                                    if (now - lastFlushAt >= FLUSH_INTERVAL_MILLIS) {
                                        flushPendingLocked(forceTimestamp = now)
                                    }
                                }
                            }
                        }
                        if (shouldStop) break
                    }
                }
        }

        private fun stopTicker() {
            tickerJob?.cancel()
            tickerJob = null
        }

        private suspend fun flushPendingLocked(forceTimestamp: Long) {
            val currentState = activeRunState.value ?: return
            if (currentState.pendingTrackPoints.isNotEmpty()) {
                runningRepository.insertTrackPoints(currentState.pendingTrackPoints)
            }
            runningRepository.updateSession(
                currentState.toRunningSession(
                    status = SessionStatus.IN_PROGRESS,
                    endedAtEpochMillis = null,
                    updatedAtEpochMillis = forceTimestamp,
                ),
            )
            activeRunState.value =
                currentState.copy(
                    pendingTrackPoints = emptyList(),
                    lastFlushAtEpochMillis = forceTimestamp,
                )
        }

        private suspend fun resolveCalories(
            averageSpeedMps: Double,
            durationMillis: Long,
        ): Double? {
            val profile = profileRepository.observeProfile().first() ?: return null
            return estimateCaloriesKcal(
                weightKg = profile.weightKg,
                averageSpeedMps = averageSpeedMps,
                durationMillis = durationMillis,
            )
        }

        private fun startTrackingService() {
            serviceController.startTracking()
        }

        private fun stopTrackingService() {
            serviceController.stopTracking()
        }

        private fun now(): Long = System.currentTimeMillis()

        private fun emptyStats(): RunStats =
            RunStats(
                durationMillis = 0L,
                distanceMeters = 0.0,
                averagePaceSecPerKm = null,
                maxSpeedMps = 0.0,
                calorieKcal = null,
            )

        private companion object {
            const val FLUSH_INTERVAL_MILLIS = 5_000L
            const val TICKER_INTERVAL_MILLIS = 1_000L
            const val CURRENT_PACE_WINDOW_MILLIS = 10_000L
            const val MILLIS_PER_SECOND = 1_000.0
            const val RECENT_SAMPLE_LIMIT = 10
            const val INITIAL_MAX_SPEED_SAMPLE_COUNT = 2
        }
    }

private data class ActiveRunState(
    val state: RunEngineState,
    val sessionId: Long?,
    val startedAtEpochMillis: Long?,
    val durationMillis: Long,
    val latestLocation: LocationSample?,
    val totalDistanceMeters: Double,
    val currentPaceSecPerKm: Double?,
    val averagePaceSecPerKm: Double?,
    val maxSpeedMps: Double,
    val calorieKcal: Double?,
    val lastFlushAtEpochMillis: Long?,
    val pointCount: Int,
    val lastLocationSample: LocationSample?,
    val recentSamples: List<LocationSample> = emptyList(),
    val pendingTrackPoints: List<TrackPoint> = emptyList(),
    val externalId: String,
    val createdAtEpochMillis: Long?,
)

private fun ActiveRunState.toRunningSession(
    status: SessionStatus,
    endedAtEpochMillis: Long?,
    updatedAtEpochMillis: Long,
): RunningSession =
    RunningSession(
        id = requireNotNull(sessionId),
        externalId = externalId,
        status = status,
        startedAtEpochMillis = requireNotNull(startedAtEpochMillis),
        endedAtEpochMillis = endedAtEpochMillis,
        stats =
            RunStats(
                durationMillis = durationMillis,
                distanceMeters = totalDistanceMeters,
                averagePaceSecPerKm = averagePaceSecPerKm,
                maxSpeedMps = maxSpeedMps,
                calorieKcal = calorieKcal,
            ),
        createdAtEpochMillis = requireNotNull(createdAtEpochMillis),
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

private fun ActiveRunState.toRunSnapshot(): RunSnapshot =
    RunSnapshot(
        state = state,
        sessionId = sessionId,
        startedAtEpochMillis = startedAtEpochMillis,
        durationMillis = durationMillis,
        latestLocation = latestLocation,
        totalDistanceMeters = totalDistanceMeters,
        currentPaceSecPerKm = currentPaceSecPerKm,
        averagePaceSecPerKm = averagePaceSecPerKm,
        maxSpeedMps = maxSpeedMps,
        calorieKcal = calorieKcal,
        lastFlushAtEpochMillis = lastFlushAtEpochMillis,
        pointCount = pointCount,
    )

private fun TrackPoint.toLocationSample(): LocationSample =
    LocationSample(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracyMeters,
        speedMps = speedMps,
        altitudeMeters = altitudeMeters,
        recordedAtEpochMillis = recordedAtEpochMillis,
    )
