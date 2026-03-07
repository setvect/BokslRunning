package com.boksl.running.ui.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boksl.running.domain.model.MonthlyStatsPoint
import com.boksl.running.domain.model.StatsChartMetric
import com.boksl.running.domain.repository.RunningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StatsViewModel
    @Inject
    constructor(
        runningRepository: RunningRepository,
    ) : ViewModel() {
        private val selectedMetric = MutableStateFlow(StatsChartMetric.DISTANCE)
        private val selectedMonthIndex = MutableStateFlow<Int?>(null)

        val uiState: StateFlow<StatsUiState> =
            combine(
                runningRepository.observeHomeSummary(),
                runningRepository.observeMonthlyStats(),
                selectedMetric,
                selectedMonthIndex,
            ) { summary, monthlyStats, metric, requestedMonthIndex ->
                val resolvedMonthIndex =
                    when {
                        monthlyStats.isEmpty() -> NO_SELECTED_MONTH
                        requestedMonthIndex == null -> monthlyStats.lastIndex
                        requestedMonthIndex in monthlyStats.indices -> requestedMonthIndex
                        else -> monthlyStats.lastIndex
                    }
                StatsUiState(
                    totalDistanceMeters = summary.totalDistanceMeters,
                    totalDurationMillis = summary.totalDurationMillis,
                    averageSpeedMps = summary.averageSpeedMps,
                    selectedMetric = metric,
                    monthlyStats = monthlyStats,
                    selectedMonthIndex = resolvedMonthIndex,
                    selectedMonth = monthlyStats.getOrNull(resolvedMonthIndex),
                    hasRecordedSessions = summary.totalDistanceMeters > 0.0 || summary.totalDurationMillis > 0L,
                    initialVisibleMonthCount = INITIAL_VISIBLE_MONTH_COUNT,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MILLIS),
                initialValue = StatsUiState(),
            )

        fun onMetricSelected(metric: StatsChartMetric) {
            selectedMetric.value = metric
        }

        fun onMonthSelected(index: Int) {
            selectedMonthIndex.value = index
        }

        private companion object {
            const val INITIAL_VISIBLE_MONTH_COUNT = 6
            const val NO_SELECTED_MONTH = -1
            const val STATE_TIMEOUT_MILLIS = 5_000L
        }
    }

data class StatsUiState(
    val totalDistanceMeters: Double = 0.0,
    val totalDurationMillis: Long = 0L,
    val averageSpeedMps: Double = 0.0,
    val selectedMetric: StatsChartMetric = StatsChartMetric.DISTANCE,
    val monthlyStats: List<MonthlyStatsPoint> = emptyList(),
    val selectedMonthIndex: Int = -1,
    val selectedMonth: MonthlyStatsPoint? = null,
    val hasRecordedSessions: Boolean = false,
    val initialVisibleMonthCount: Int = 6,
)
