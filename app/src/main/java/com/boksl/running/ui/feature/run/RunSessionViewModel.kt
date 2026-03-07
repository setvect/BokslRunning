package com.boksl.running.ui.feature.run

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boksl.running.core.network.NetworkMonitor
import com.boksl.running.domain.model.RunEngineState
import com.boksl.running.domain.model.RunSnapshot
import com.boksl.running.domain.model.RunningSession
import com.boksl.running.domain.model.TrackPoint
import com.boksl.running.domain.repository.RunEngineRepository
import com.boksl.running.domain.repository.RunningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        private val activeRun = runEngineRepository.observeActiveRun()
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
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = RunSessionUiState(),
            )

        fun prepareRun() {
            viewModelScope.launch {
                runEngineRepository.prepareRun()
            }
        }

        fun startRun() {
            viewModelScope.launch {
                if (isStarting.value) return@launch
                isStarting.value = true
                runEngineRepository.startRun()
                isStarting.value = false
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
            viewModelScope.launch {
                runEngineRepository.discardRun()
            }
        }

        fun resumeActiveRun() {
            viewModelScope.launch {
                runEngineRepository.resumeActiveRun()
            }
        }
    }

data class RunSessionUiState(
    val snapshot: RunSnapshot? = null,
    val isStarting: Boolean = false,
    val savedSession: RunningSession? = null,
    val trackPoints: List<TrackPoint> = emptyList(),
    val isOffline: Boolean = false,
) {
    val shouldNavigateToLive: Boolean
        get() = snapshot?.state == RunEngineState.RUNNING

    val showStopConfirm: Boolean
        get() = snapshot?.state == RunEngineState.STOP_CONFIRM

    val canStart: Boolean
        get() = snapshot?.state == RunEngineState.READY && !isStarting
}

private fun appendLatestLocation(
    trackPoints: List<TrackPoint>,
    snapshot: RunSnapshot?,
): List<TrackPoint> {
    val latest = snapshot?.latestLocation ?: return trackPoints
    val lastPoint = trackPoints.lastOrNull()
    if (
        lastPoint != null &&
        lastPoint.latitude == latest.latitude &&
        lastPoint.longitude == latest.longitude &&
        lastPoint.recordedAtEpochMillis == latest.recordedAtEpochMillis
    ) {
        return trackPoints
    }
    val sessionId = snapshot.sessionId ?: return trackPoints
    return trackPoints +
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
