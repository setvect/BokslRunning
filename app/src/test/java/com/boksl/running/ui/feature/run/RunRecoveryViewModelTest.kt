package com.boksl.running.ui.feature.run

import androidx.paging.PagingData
import com.boksl.running.MainDispatcherRule
import com.boksl.running.core.network.NetworkMonitor
import com.boksl.running.domain.model.HomeSummary
import com.boksl.running.domain.model.MonthlyStatsPoint
import com.boksl.running.domain.model.RunSnapshot
import com.boksl.running.domain.model.RunStats
import com.boksl.running.domain.model.RunningSession
import com.boksl.running.domain.model.SessionStatus
import com.boksl.running.domain.model.TrackPoint
import com.boksl.running.domain.repository.RunEngineRepository
import com.boksl.running.domain.repository.RunningRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RunRecoveryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadRecoveryStateExposesActiveSession() =
        runTest {
            val session = activeSession()
            val viewModel =
                RunRecoveryViewModel(
                    runEngineRepository = FakeRecoveryRunEngineRepository(),
                    runningRepository = FakeRecoveryRunningRepository(activeSession = session),
                    networkMonitor = FakeRecoveryNetworkMonitor(isOnline = false),
                )
            backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.loadRecoveryState()
            advanceUntilIdle()

            assertEquals(session.id, viewModel.uiState.value.activeSession?.id)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(true, viewModel.uiState.value.isOffline)
        }

    @Test
    fun continueRunEmitsNavigateToLive() =
        runTest {
            val runEngineRepository = FakeRecoveryRunEngineRepository()
            val viewModel =
                RunRecoveryViewModel(
                    runEngineRepository = runEngineRepository,
                    runningRepository = FakeRecoveryRunningRepository(activeSession = activeSession()),
                    networkMonitor = FakeRecoveryNetworkMonitor(isOnline = true),
                )
            backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.loadRecoveryState()
            advanceUntilIdle()
            val eventDeferred = async { viewModel.event.first() }

            viewModel.continueRun()
            advanceUntilIdle()

            assertEquals(1, runEngineRepository.resumeCount)
            assertEquals(RunRecoveryEvent.NavigateToLive, eventDeferred.await())
        }

    @Test
    fun discardRunEmitsNavigateToHistory() =
        runTest {
            val runEngineRepository = FakeRecoveryRunEngineRepository()
            val viewModel =
                RunRecoveryViewModel(
                    runEngineRepository = runEngineRepository,
                    runningRepository = FakeRecoveryRunningRepository(activeSession = activeSession()),
                    networkMonitor = FakeRecoveryNetworkMonitor(isOnline = true),
                )
            backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.loadRecoveryState()
            advanceUntilIdle()
            val eventDeferred = async { viewModel.event.first() }

            viewModel.discardRun()
            advanceUntilIdle()

            assertEquals(1, runEngineRepository.discardCount)
            assertEquals(RunRecoveryEvent.NavigateToHistory, eventDeferred.await())
            assertNull(viewModel.uiState.value.activeSession)
        }

    @Test
    fun loadRecoveryStateFallsBackToHomeWhenNoActiveSession() =
        runTest {
            val viewModel =
                RunRecoveryViewModel(
                    runEngineRepository = FakeRecoveryRunEngineRepository(),
                    runningRepository = FakeRecoveryRunningRepository(activeSession = null),
                    networkMonitor = FakeRecoveryNetworkMonitor(isOnline = true),
                )
            backgroundScope.launch { viewModel.uiState.collect {} }
            val eventDeferred = async { viewModel.event.first() }

            viewModel.loadRecoveryState()
            advanceUntilIdle()

            assertEquals(RunRecoveryEvent.NavigateHome, eventDeferred.await())
            assertFalse(viewModel.uiState.value.isLoading)
        }
}

private fun activeSession(): RunningSession =
    RunningSession(
        id = 11L,
        externalId = "active-session",
        status = SessionStatus.IN_PROGRESS,
        startedAtEpochMillis = 1_000L,
        endedAtEpochMillis = null,
        stats =
            RunStats(
                durationMillis = 10_000L,
                distanceMeters = 120.0,
                averagePaceSecPerKm = 300.0,
                maxSpeedMps = 4.0,
                calorieKcal = 14.0,
            ),
        createdAtEpochMillis = 1_000L,
        updatedAtEpochMillis = 11_000L,
    )

private class FakeRecoveryRunEngineRepository : RunEngineRepository {
    var resumeCount = 0
    var discardCount = 0

    override fun observeActiveRun(): Flow<RunSnapshot?> = flowOf(null)

    override suspend fun prepareRun() = Unit

    override suspend fun startRun() = Unit

    override suspend fun requestStop() = Unit

    override suspend fun confirmSave() = Unit

    override suspend fun cancelStop() = Unit

    override suspend fun discardRun() {
        discardCount += 1
    }

    override suspend fun resumeActiveRun() {
        resumeCount += 1
    }
}

private class FakeRecoveryRunningRepository(
    private val activeSession: RunningSession?,
) : RunningRepository {
    override fun observeHomeSummary(): Flow<HomeSummary> = flowOf(HomeSummary(0.0, 0L, 0.0, 0.0))

    override fun observeMonthlyStats(): Flow<List<MonthlyStatsPoint>> = flowOf(emptyList())

    override fun observeSession(sessionId: Long): Flow<RunningSession?> = flowOf(null)

    override fun observeSavedSessionsPaged(): Flow<PagingData<RunningSession>> = flowOf(PagingData.empty())

    override fun observeRecentSessions(limit: Int): Flow<List<RunningSession>> = flowOf(emptyList())

    override fun observeTrackPoints(sessionId: Long): Flow<List<TrackPoint>> = flowOf(emptyList())

    override suspend fun getSession(sessionId: Long): RunningSession? = null

    override suspend fun getActiveSession(): RunningSession? = activeSession

    override suspend fun insertSession(session: RunningSession): Long = error("unused")

    override suspend fun updateSession(session: RunningSession) = Unit

    override suspend fun insertTrackPoints(points: List<TrackPoint>) = Unit

    override suspend fun deleteSession(sessionId: Long) = Unit
}

private class FakeRecoveryNetworkMonitor(
    isOnline: Boolean,
) : NetworkMonitor {
    private val isOnlineFlow = MutableStateFlow(isOnline)

    override fun observeIsOnline(): Flow<Boolean> = isOnlineFlow
}
