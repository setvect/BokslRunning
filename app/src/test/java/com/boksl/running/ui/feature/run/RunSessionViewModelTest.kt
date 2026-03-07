package com.boksl.running.ui.feature.run

import com.boksl.running.MainDispatcherRule
import com.boksl.running.core.network.NetworkMonitor
import com.boksl.running.domain.model.HomeSummary
import com.boksl.running.domain.model.LocationSample
import com.boksl.running.domain.model.RunEngineState
import com.boksl.running.domain.model.RunSnapshot
import com.boksl.running.domain.model.RunStats
import com.boksl.running.domain.model.RunningSession
import com.boksl.running.domain.model.SessionStatus
import com.boksl.running.domain.model.TrackPoint
import com.boksl.running.domain.repository.RunEngineRepository
import com.boksl.running.domain.repository.RunningRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RunSessionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun uiStateAppendsLatestLocationAndShowsOfflineBanner() =
        runTest {
            val snapshot =
                RunSnapshot(
                    state = RunEngineState.RUNNING,
                    sessionId = 7L,
                    startedAtEpochMillis = 1_000L,
                    durationMillis = 12_000L,
                    latestLocation =
                        LocationSample(
                            latitude = 37.1,
                            longitude = 127.1,
                            accuracyMeters = 5f,
                            speedMps = 3f,
                            altitudeMeters = null,
                            recordedAtEpochMillis = 12_000L,
                        ),
                    totalDistanceMeters = 100.0,
                    currentPaceSecPerKm = 320.0,
                    averagePaceSecPerKm = 330.0,
                    maxSpeedMps = 4.5,
                    calorieKcal = 11.0,
                    lastFlushAtEpochMillis = 10_000L,
                    pointCount = 2,
                )
            val repository = FakeRunEngineRepository(snapshot)
            val runningRepository =
                FakeRunningRepository(
                    session =
                        RunningSession(
                            id = 7L,
                            externalId = "session-7",
                            status = SessionStatus.IN_PROGRESS,
                            startedAtEpochMillis = 1_000L,
                            endedAtEpochMillis = null,
                            stats = RunStats(12_000L, 100.0, 330.0, 4.5, 11.0),
                            createdAtEpochMillis = 1_000L,
                            updatedAtEpochMillis = 12_000L,
                        ),
                    trackPoints =
                        listOf(
                            TrackPoint(
                                externalId = "tp-1",
                                sessionId = 7L,
                                sequence = 0,
                                latitude = 37.0,
                                longitude = 127.0,
                                altitudeMeters = null,
                                accuracyMeters = 5f,
                                speedMps = 3f,
                                recordedAtEpochMillis = 1_000L,
                            ),
                        ),
                )
            val viewModel =
                RunSessionViewModel(
                    runEngineRepository = repository,
                    runningRepository = runningRepository,
                    networkMonitor = FakeNetworkMonitor(isOnline = false),
                )
            backgroundScope.launch { viewModel.uiState.collect {} }

            advanceUntilIdle()

            val uiState = viewModel.uiState.value
            assertTrue(uiState.isOffline)
            assertEquals(2, uiState.trackPoints.size)
            assertEquals("latest-preview", uiState.trackPoints.last().externalId)
            assertTrue(uiState.shouldNavigateToLive)
            assertFalse(uiState.showStopConfirm)
        }

    @Test
    fun uiStateLoadsSavedSessionForSummary() =
        runTest {
            val snapshot =
                RunSnapshot(
                    state = RunEngineState.SAVED,
                    sessionId = 9L,
                    startedAtEpochMillis = 2_000L,
                    durationMillis = 25_000L,
                    latestLocation = null,
                    totalDistanceMeters = 200.0,
                    currentPaceSecPerKm = null,
                    averagePaceSecPerKm = 310.0,
                    maxSpeedMps = 5.0,
                    calorieKcal = 20.0,
                    lastFlushAtEpochMillis = 27_000L,
                    pointCount = 1,
                )
            val savedSession =
                RunningSession(
                    id = 9L,
                    externalId = "session-9",
                    status = SessionStatus.SAVED,
                    startedAtEpochMillis = 2_000L,
                    endedAtEpochMillis = 27_000L,
                    stats = RunStats(25_000L, 200.0, 310.0, 5.0, 20.0),
                    createdAtEpochMillis = 2_000L,
                    updatedAtEpochMillis = 27_000L,
                )
            val viewModel =
                RunSessionViewModel(
                    runEngineRepository = FakeRunEngineRepository(snapshot),
                    runningRepository = FakeRunningRepository(session = savedSession, trackPoints = emptyList()),
                    networkMonitor = FakeNetworkMonitor(isOnline = true),
                )
            backgroundScope.launch { viewModel.uiState.collect {} }

            advanceUntilIdle()

            val uiState = viewModel.uiState.value
            assertNotNull(uiState.savedSession)
            assertEquals(SessionStatus.SAVED, uiState.savedSession?.status)
            assertFalse(uiState.isOffline)
            assertFalse(uiState.shouldNavigateToLive)
        }
}

private class FakeRunEngineRepository(
    snapshot: RunSnapshot?,
) : RunEngineRepository {
    private val activeRun = MutableStateFlow(snapshot)

    override fun observeActiveRun(): Flow<RunSnapshot?> = activeRun

    override suspend fun prepareRun() = Unit

    override suspend fun startRun() = Unit

    override suspend fun requestStop() = Unit

    override suspend fun confirmSave() = Unit

    override suspend fun cancelStop() = Unit

    override suspend fun discardRun() = Unit

    override suspend fun resumeActiveRun() = Unit
}

private class FakeRunningRepository(
    session: RunningSession?,
    trackPoints: List<TrackPoint>,
) : RunningRepository {
    private val sessionState = MutableStateFlow(session)
    private val trackPointsState = MutableStateFlow(trackPoints)

    override fun observeHomeSummary(): Flow<HomeSummary> = flowOf(HomeSummary(0.0, 0L, 0.0, 0.0))

    override fun observeSession(sessionId: Long): Flow<RunningSession?> = sessionState

    override fun observeRecentSessions(limit: Int): Flow<List<RunningSession>> =
        flowOf(listOfNotNull(sessionState.value))

    override fun observeTrackPoints(sessionId: Long): Flow<List<TrackPoint>> = trackPointsState

    override suspend fun getSession(sessionId: Long): RunningSession? = sessionState.value

    override suspend fun getActiveSession(): RunningSession? = null

    override suspend fun insertSession(session: RunningSession): Long = error("unused")

    override suspend fun updateSession(session: RunningSession) {
        sessionState.value = session
    }

    override suspend fun insertTrackPoints(points: List<TrackPoint>) {
        trackPointsState.value = trackPointsState.value + points
    }

    override suspend fun deleteSession(sessionId: Long) = Unit
}

private class FakeNetworkMonitor(
    isOnline: Boolean,
) : NetworkMonitor {
    private val isOnlineFlow = MutableStateFlow(isOnline)

    override fun observeIsOnline(): Flow<Boolean> = isOnlineFlow
}
