package com.boksl.running.ui.feature.run

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boksl.running.core.network.NetworkMonitor
import com.boksl.running.domain.model.LocationSample
import com.boksl.running.domain.model.RunEngineState
import com.boksl.running.domain.model.RunSnapshot
import com.boksl.running.domain.model.RunningSession
import com.boksl.running.domain.model.TrackPoint
import com.boksl.running.domain.repository.RunEngineRepository
import com.boksl.running.domain.repository.RunningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class RunSessionViewModel
    @Inject
    constructor(
        private val runEngineRepository: RunEngineRepository,
        private val runningRepository: RunningRepository,
        networkMonitor: NetworkMonitor,
    ) : ViewModel() {
        private val isStarting = MutableStateFlow(false)
        private val countdownRemainingSeconds = MutableStateFlow<Int?>(null)
        private var startCountdownJob: Job? = null
        private val activeRun =
            runEngineRepository.observeActiveRun().stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STATE_SUBSCRIPTION_TIMEOUT_MILLIS),
                initialValue = null,
            )
        private val sessionIdFlow =
            activeRun
                .map { it?.sessionId }
                .distinctUntilChanged()

        private val sessionFlow =
            sessionIdFlow.flatMapLatest { sessionId ->
                if (sessionId == null) {
                    flowOf(null)
                } else {
                    runningRepository.observeSession(sessionId)
                }
            }

        private val trackPointsFlow =
            sessionIdFlow.flatMapLatest { sessionId ->
                if (sessionId == null) {
                    flowOf(emptyList())
                } else {
                    runningRepository.observeTrackPoints(sessionId)
                }
            }

        val uiState: StateFlow<RunSessionUiState> =
            combine(
                combine(
                    activeRun,
                    sessionFlow,
                    trackPointsFlow,
                    networkMonitor.observeIsOnline(),
                    isStarting,
                ) { snapshot, savedSession, trackPoints, isOnline, starting ->
                    val pointsWithLatest = appendLatestLocation(trackPoints, snapshot)
                    RunSessionUiState(
                        snapshot = snapshot,
                        isStarting = starting,
                        savedSession = savedSession,
                        trackPoints = pointsWithLatest,
                        isOffline = !isOnline,
                    )
                },
                countdownRemainingSeconds,
            ) { baseState, countdown ->
                baseState.copy(countdownRemainingSeconds = countdown)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STATE_SUBSCRIPTION_TIMEOUT_MILLIS),
                initialValue = RunSessionUiState(),
            )

        fun prepareRun() {
            viewModelScope.launch {
                runEngineRepository.prepareRun()
            }
        }

        fun startRun() {
            if (startCountdownJob?.isActive == true || isStarting.value) return
            if (activeRun.value?.state != RunEngineState.READY) return

            startCountdownJob =
                viewModelScope.launch {
                    try {
                        for (remaining in RUN_START_COUNTDOWN_SECONDS downTo 1) {
                            countdownRemainingSeconds.value = remaining
                            delay(RUN_START_COUNTDOWN_INTERVAL_MILLIS)
                        }
                        startRunImmediately()
                    } finally {
                        countdownRemainingSeconds.value = null
                        startCountdownJob = null
                    }
                }
        }

        fun requestStop() {
            viewModelScope.launch {
                runEngineRepository.requestStop()
            }
        }

        fun confirmSave() {
            viewModelScope.launch {
                runEngineRepository.confirmSave()
            }
        }

        fun cancelStop() {
            viewModelScope.launch {
                runEngineRepository.cancelStop()
            }
        }

        fun discardRun() {
            cancelStartCountdown()
            viewModelScope.launch {
                runEngineRepository.discardRun()
            }
        }

        fun resumeActiveRun() {
            viewModelScope.launch {
                runEngineRepository.resumeActiveRun()
            }
        }

        private suspend fun startRunImmediately() {
            if (isStarting.value || activeRun.value?.state != RunEngineState.READY) return

            isStarting.value = true
            try {
                runEngineRepository.startRun()
            } finally {
                isStarting.value = false
            }
        }

        private fun cancelStartCountdown() {
            startCountdownJob?.cancel()
            startCountdownJob = null
            countdownRemainingSeconds.value = null
        }
    }

data class RunSessionUiState(
    val snapshot: RunSnapshot? = null,
    val isStarting: Boolean = false,
    val countdownRemainingSeconds: Int? = null,
    val savedSession: RunningSession? = null,
    val trackPoints: List<TrackPoint> = emptyList(),
    val isOffline: Boolean = false,
) {
    val shouldNavigateToLive: Boolean
        get() = snapshot?.state == RunEngineState.RUNNING

    val showStopConfirm: Boolean
        get() = snapshot?.state == RunEngineState.STOP_CONFIRM

    val isCountingDown: Boolean
        get() = countdownRemainingSeconds != null

    val canStart: Boolean
        get() = snapshot?.state == RunEngineState.READY && !isStarting && !isCountingDown
}

private fun appendLatestLocation(
    trackPoints: List<TrackPoint>,
    snapshot: RunSnapshot?,
): List<TrackPoint> {
    val latest = snapshot?.latestLocation
    val sessionId = snapshot?.sessionId
    return if (latest == null || sessionId == null || trackPoints.lastOrNull().isSameLocationAs(latest)) {
        trackPoints
    } else {
        trackPoints +
            TrackPoint(
                externalId = "latest-preview",
                sessionId = sessionId,
                sequence = trackPoints.size,
                latitude = latest.latitude,
                longitude = latest.longitude,
                altitudeMeters = latest.altitudeMeters,
                accuracyMeters = latest.accuracyMeters,
                speedMps = latest.speedMps,
                recordedAtEpochMillis = latest.recordedAtEpochMillis,
            )
    }
}

private fun TrackPoint?.isSameLocationAs(latest: LocationSample): Boolean =
    this != null &&
        latitude == latest.latitude &&
        longitude == latest.longitude &&
        recordedAtEpochMillis == latest.recordedAtEpochMillis

private const val STATE_SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L
private const val RUN_START_COUNTDOWN_SECONDS = 3
private const val RUN_START_COUNTDOWN_INTERVAL_MILLIS = 1_000L
