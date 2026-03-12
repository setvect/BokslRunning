package com.boksl.running.ui.feature.stats

import androidx.paging.PagingData
import com.boksl.running.MainDispatcherRule
import com.boksl.running.domain.model.HomeSummary
import com.boksl.running.domain.model.MonthlyStatsPoint
import com.boksl.running.domain.model.RunningSession
import com.boksl.running.domain.model.StatsChartMetric
import com.boksl.running.domain.model.TrackPoint
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun uiStateCombinesSummaryAndMonthlyStatsWithDistanceAsDefaultMetric() =
        runTest {
            val repository =
                FakeStatsRunningRepository(
                    summary = HomeSummary(12_345.0, 3_600_000L, 3.4291666667, 0.0),
                    monthlyStats =
                        listOf(
                            MonthlyStatsPoint(YearMonth.of(2025, 10), 2_000.0, 600_000L, 3.3333333333),
                            MonthlyStatsPoint(YearMonth.of(2025, 11), 0.0, 0L, 0.0),
                        ),
                )
            val viewModel = StatsViewModel(runningRepository = repository)
            backgroundScope.launch { viewModel.uiState.collect {} }

            advanceUntilIdle()

            val uiState = viewModel.uiState.value
            assertEquals(12_345.0, uiState.totalDistanceMeters, 0.0)
            assertEquals(3_600_000L, uiState.totalDurationMillis)
            assertEquals(StatsChartMetric.DISTANCE, uiState.selectedMetric)
            assertEquals(2, uiState.monthlyStats.size)
            assertEquals(1, uiState.selectedMonthIndex)
            assertEquals(YearMonth.of(2025, 11), uiState.selectedMonth?.yearMonth)
            assertTrue(uiState.hasRecordedSessions)
        }

    @Test
    fun onMetricSelectedUpdatesOnlySelectedMetric() =
        runTest {
            val repository =
                FakeStatsRunningRepository(
                    summary = HomeSummary(2_000.0, 600_000L, 3.3333333333, 0.0),
                    monthlyStats =
                        listOf(
                            MonthlyStatsPoint(YearMonth.of(2026, 2), 2_000.0, 600_000L, 3.3333333333),
                        ),
                )
            val viewModel = StatsViewModel(runningRepository = repository)
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val initialMonthlyStats = viewModel.uiState.value.monthlyStats
            val initialSelectedMonthIndex = viewModel.uiState.value.selectedMonthIndex
            viewModel.onMetricSelected(StatsChartMetric.AVERAGE_SPEED)
            advanceUntilIdle()

            val uiState = viewModel.uiState.value
            assertEquals(StatsChartMetric.AVERAGE_SPEED, uiState.selectedMetric)
            assertEquals(initialMonthlyStats, uiState.monthlyStats)
            assertEquals(initialSelectedMonthIndex, uiState.selectedMonthIndex)
        }

    @Test
    fun onMonthSelectedUpdatesSelectedMonthIndexAndMonth() =
        runTest {
            val repository =
                FakeStatsRunningRepository(
                    summary = HomeSummary(2_000.0, 600_000L, 3.3333333333, 0.0),
                    monthlyStats =
                        listOf(
                            MonthlyStatsPoint(YearMonth.of(2025, 12), 500.0, 200_000L, 2.5),
                            MonthlyStatsPoint(YearMonth.of(2026, 1), 600.0, 180_000L, 3.3333333333),
                            MonthlyStatsPoint(YearMonth.of(2026, 2), 900.0, 220_000L, 4.0909090909),
                        ),
                )
            val viewModel = StatsViewModel(runningRepository = repository)
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.onMonthSelected(0)
            advanceUntilIdle()

            val uiState = viewModel.uiState.value
            assertEquals(0, uiState.selectedMonthIndex)
            assertEquals(YearMonth.of(2025, 12), uiState.selectedMonth?.yearMonth)
        }

    @Test
    fun uiStateKeepsSixMonthWindowWhenNoRecordedDistanceOrDuration() =
        runTest {
            val viewModel =
                StatsViewModel(
                    runningRepository =
                        FakeStatsRunningRepository(
                            summary = HomeSummary(0.0, 0L, 0.0, 0.0),
                            monthlyStats =
                                listOf(
                                    MonthlyStatsPoint(YearMonth.of(2025, 10), 0.0, 0L, 0.0),
                                    MonthlyStatsPoint(YearMonth.of(2025, 11), 0.0, 0L, 0.0),
                                    MonthlyStatsPoint(YearMonth.of(2025, 12), 0.0, 0L, 0.0),
                                    MonthlyStatsPoint(YearMonth.of(2026, 1), 0.0, 0L, 0.0),
                                    MonthlyStatsPoint(YearMonth.of(2026, 2), 0.0, 0L, 0.0),
                                    MonthlyStatsPoint(YearMonth.of(2026, 3), 0.0, 0L, 0.0),
                                ),
                        ),
                )
            backgroundScope.launch { viewModel.uiState.collect {} }

            advanceUntilIdle()

            val uiState = viewModel.uiState.value
            assertFalse(uiState.hasRecordedSessions)
            assertEquals(6, uiState.monthlyStats.size)
            assertEquals(5, uiState.selectedMonthIndex)
            assertEquals(YearMonth.of(2026, 3), uiState.selectedMonth?.yearMonth)
        }
}

private class FakeStatsRunningRepository(
    summary: HomeSummary,
    monthlyStats: List<MonthlyStatsPoint>,
) : RunningRepository {
    private val summaryState = MutableStateFlow(summary)
    private val monthlyStatsState = MutableStateFlow(monthlyStats)

    override fun observeHomeSummary(): Flow<HomeSummary> = summaryState

    override fun observeMonthlyStats(): Flow<List<MonthlyStatsPoint>> = monthlyStatsState

    override fun observeSavedSessionCount(): Flow<Int> = flowOf(0)

    override fun observeSession(sessionId: Long): Flow<RunningSession?> = flowOf(null)

    override fun observeSavedSessionsPaged(): Flow<PagingData<RunningSession>> = flowOf(PagingData.empty())

    override fun observeRecentSessions(limit: Int): Flow<List<RunningSession>> = flowOf(emptyList())

    override fun observeTrackPoints(sessionId: Long): Flow<List<TrackPoint>> = flowOf(emptyList())

    override suspend fun getSession(sessionId: Long): RunningSession? = null

    override suspend fun getActiveSession(): RunningSession? = null

    override suspend fun insertSession(session: RunningSession): Long = error("unused")

    override suspend fun updateSession(session: RunningSession) = Unit

    override suspend fun insertTrackPoints(points: List<TrackPoint>) = Unit

    override suspend fun deleteSession(sessionId: Long) = Unit
}
