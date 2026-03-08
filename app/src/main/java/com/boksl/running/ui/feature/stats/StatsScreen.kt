@file:Suppress("LongMethod", "MagicNumber", "TooManyFunctions")

package com.boksl.running.ui.feature.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.boksl.running.domain.model.MonthlyStatsPoint
import com.boksl.running.domain.model.StatsChartMetric
import com.boksl.running.ui.formatChartDurationLabel
import com.boksl.running.ui.formatDistanceKm
import com.boksl.running.ui.formatDurationText
import com.boksl.running.ui.formatSpeedKmh
import com.boksl.running.ui.formatStatsChartValue
import com.boksl.running.ui.formatYearMonthLabel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.layout.segmented
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.compose.component.marker.markerComponent
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.layout.HorizontalLayout
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.chart.values.ChartValues
import com.patrykandpatrick.vico.core.component.marker.MarkerComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.ChartEntryModel
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.marker.Marker
import com.patrykandpatrick.vico.core.marker.MarkerLabelFormatter
import com.patrykandpatrick.vico.core.marker.MarkerVisibilityChangeListener
import com.patrykandpatrick.vico.core.scroll.InitialScroll
import java.util.Locale
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun statsScreen(
    uiState: StatsUiState,
    onMetricSelected: (StatsChartMetric) -> Unit,
    onMonthSelected: (Int) -> Unit,
    onNavigateUp: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "통계") },
                navigationIcon = {
                    TextButton(onClick = onNavigateUp) {
                        Text(text = "뒤로")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            summaryCard(uiState = uiState)
            monthlyChartCard(
                uiState = uiState,
                onMetricSelected = onMetricSelected,
                onMonthSelected = onMonthSelected,
            )
        }
    }
}

@Composable
private fun summaryCard(uiState: StatsUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "누적 요약",
                style = MaterialTheme.typography.titleMedium,
            )
            statsSummaryRow(label = "총 달린 거리", value = "${uiState.totalDistanceMeters.formatDistanceKm()} km")
            statsSummaryRow(label = "달린 시간", value = uiState.totalDurationMillis.formatDurationText())
            statsSummaryRow(label = "평균 속도", value = "${uiState.averageSpeedMps.formatSpeedKmh()} km/h")
        }
    }
}

@Composable
private fun monthlyChartCard(
    uiState: StatsUiState,
    onMetricSelected: (StatsChartMetric) -> Unit,
    onMonthSelected: (Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "월별 차트",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = uiState.chartSubtitle(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            metricTabs(
                selectedMetric = uiState.selectedMetric,
                onMetricSelected = onMetricSelected,
            )
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                monthlyStatsChart(
                    uiState = uiState,
                    onMonthSelected = onMonthSelected,
                )
                selectedMonthDetailCard(
                    selectedMonth = uiState.selectedMonth,
                    selectedMetric = uiState.selectedMetric,
                )
                if (!uiState.hasRecordedSessions) {
                    Text(
                        text = "저장된 기록이 없어 최근 6개월을 0값으로 표시합니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun metricTabs(
    selectedMetric: StatsChartMetric,
    onMetricSelected: (StatsChartMetric) -> Unit,
) {
    val metrics = remember { StatsChartMetric.entries }
    TabRow(selectedTabIndex = metrics.indexOf(selectedMetric)) {
        metrics.forEach { metric ->
            Tab(
                selected = metric == selectedMetric,
                onClick = { onMetricSelected(metric) },
                text = { Text(text = metric.toTabLabel()) },
            )
        }
    }
}

@Composable
private fun monthlyStatsChart(
    uiState: StatsUiState,
    onMonthSelected: (Int) -> Unit,
) {
    val monthlyStats = uiState.monthlyStats
    val selectedMetric = uiState.selectedMetric
    val modelProducer = remember { ChartEntryModelProducer(listOf(FloatEntry(0f, 0f))) }
    val chartScrollState = rememberChartScrollState()
    val chartScrollSpec =
        rememberChartScrollSpec<ChartEntryModel>(
            isScrollEnabled = monthlyStats.size > uiState.initialVisibleMonthCount,
            initialScroll = InitialScroll.End,
        )
    val marker = rememberStatsMarker(selectedMetric = selectedMetric)
    val markerVisibilityChangeListener =
        remember(onMonthSelected) {
            object : MarkerVisibilityChangeListener {
                override fun onMarkerShown(
                    marker: Marker,
                    markerEntryModels: List<Marker.EntryModel>,
                ) {
                    markerEntryModels.firstOrNull()?.index?.let(onMonthSelected)
                }

                override fun onMarkerMoved(
                    marker: Marker,
                    markerEntryModels: List<Marker.EntryModel>,
                ) {
                    markerEntryModels.firstOrNull()?.index?.let(onMonthSelected)
                }
            }
        }
    val chartValues = remember(monthlyStats, selectedMetric) { monthlyStats.map { it.chartValue(selectedMetric) } }
    val yAxisConfig = remember(chartValues, selectedMetric) { resolveYAxisConfig(chartValues, selectedMetric) }
    val xLabels = remember(monthlyStats) { monthlyStats.map { it.yearMonth.formatYearMonthLabel() } }
    val startAxisValueFormatter =
        remember(selectedMetric) {
            AxisValueFormatter<AxisPosition.Vertical.Start> { value: Float, _: ChartValues ->
                value.toDouble().formatForAxis(selectedMetric)
            }
        }
    val bottomAxisValueFormatter =
        remember(xLabels) {
            AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value: Float, _: ChartValues ->
                xLabels.getOrNull(value.toInt()).orEmpty()
            }
        }
    val chartEntries =
        remember(chartValues) {
            chartValues.mapIndexed { index, value ->
                FloatEntry(x = index.toFloat(), y = value.toFloat())
            }
        }
    val columnComponent =
        lineComponent(
            color = MaterialTheme.colorScheme.primary,
            thickness = CHART_BAR_THICKNESS,
            shape = Shapes.roundedCornerShape(allPercent = 40),
        )

    LaunchedEffect(chartEntries, modelProducer) {
        modelProducer.setEntries(chartEntries)
    }

    ProvideChartStyle(chartStyle = m3ChartStyle()) {
        Chart(
            chart =
                columnChart(
                    columns = listOf(columnComponent),
                    spacing = CHART_BAR_SPACING,
                    axisValuesOverrider = AxisValuesOverrider.fixed(minY = 0f, maxY = yAxisConfig.maxValue),
                ),
            chartModelProducer = modelProducer,
            chartScrollSpec = chartScrollSpec,
            chartScrollState = chartScrollState,
            startAxis =
                rememberStartAxis(
                    valueFormatter = startAxisValueFormatter,
                    itemPlacer =
                        remember(yAxisConfig.maxLabelCount) {
                            AxisItemPlacer.Vertical.default(maxItemCount = yAxisConfig.maxLabelCount)
                        },
                ),
            bottomAxis =
                rememberBottomAxis(
                    valueFormatter = bottomAxisValueFormatter,
                    itemPlacer =
                        remember {
                            AxisItemPlacer.Horizontal.default(
                                spacing = 1,
                                addExtremeLabelPadding = true,
                            )
                        },
                ),
            marker = marker,
            markerVisibilityChangeListener = markerVisibilityChangeListener,
            horizontalLayout = HorizontalLayout.segmented(),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(240.dp),
        )
    }
}

@Composable
private fun selectedMonthDetailCard(
    selectedMonth: MonthlyStatsPoint?,
    selectedMetric: StatsChartMetric,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "선택 월 상세",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = selectedMonth?.yearMonth?.formatYearMonthLabel() ?: "--.--",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = selectedMonth?.formatValue(selectedMetric) ?: "--",
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

@Composable
private fun statsSummaryRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun rememberStatsMarker(selectedMetric: StatsChartMetric): MarkerComponent {
    val background =
        shapeComponent(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            strokeWidth = 1.dp,
            strokeColor = MaterialTheme.colorScheme.outlineVariant,
        )
    val label =
        textComponent(
            color = MaterialTheme.colorScheme.onSurface,
            background = background,
        )
    val indicator = shapeComponent(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp))
    val guideline = lineComponent(color = MaterialTheme.colorScheme.primary, thickness = 2.dp)

    return markerComponent(
        label = label,
        indicator = indicator,
        guideline = guideline,
    ).apply {
        labelFormatter =
            MarkerLabelFormatter { markedEntries, _ ->
                markedEntries.firstOrNull()?.entry?.y?.toDouble()?.formatForMarker(selectedMetric).orEmpty()
            }
    }
}

private fun StatsUiState.chartSubtitle(): String =
    if (monthlyStats.size > initialVisibleMonthCount) {
        "최신 ${initialVisibleMonthCount}개월부터 시작 · 좌우 스와이프"
    } else {
        "최근 ${initialVisibleMonthCount}개월"
    }

private fun StatsChartMetric.toTabLabel(): String =
    when (this) {
        StatsChartMetric.DISTANCE -> "거리"
        StatsChartMetric.DURATION -> "시간"
        StatsChartMetric.AVERAGE_SPEED -> "평균 속도"
    }

private fun MonthlyStatsPoint.chartValue(metric: StatsChartMetric): Double =
    when (metric) {
        StatsChartMetric.DISTANCE -> totalDistanceMeters / 1_000.0
        StatsChartMetric.DURATION -> totalDurationMillis.toDouble()
        StatsChartMetric.AVERAGE_SPEED -> averageSpeedMps * 3.6
    }

private fun MonthlyStatsPoint.formatValue(metric: StatsChartMetric): String =
    when (metric) {
        StatsChartMetric.DISTANCE -> (totalDistanceMeters / 1_000.0).formatStatsChartValue(metric)
        StatsChartMetric.DURATION -> totalDurationMillis.formatStatsChartValue(metric)
        StatsChartMetric.AVERAGE_SPEED -> (averageSpeedMps * 3.6).formatStatsChartValue(metric)
    }

private fun Double.formatForAxis(metric: StatsChartMetric): String =
    when (metric) {
        StatsChartMetric.DISTANCE -> formatCompactAxisNumber()
        StatsChartMetric.DURATION -> toLong().formatChartDurationLabel()
        StatsChartMetric.AVERAGE_SPEED -> formatCompactAxisNumber()
    }

private fun Double.formatForMarker(metric: StatsChartMetric): String =
    when (metric) {
        StatsChartMetric.DISTANCE -> formatStatsChartValue(metric)
        StatsChartMetric.DURATION -> toLong().formatStatsChartValue(metric)
        StatsChartMetric.AVERAGE_SPEED -> formatStatsChartValue(metric)
    }

private data class YAxisConfig(
    val maxValue: Float,
    val maxLabelCount: Int,
)

private fun resolveYAxisConfig(
    chartValues: List<Double>,
    metric: StatsChartMetric,
): YAxisConfig {
    val rawMaxValue = chartValues.maxOrNull()?.takeIf { it > 0.0 } ?: 0.0
    return when (metric) {
        StatsChartMetric.DISTANCE,
        StatsChartMetric.AVERAGE_SPEED,
        -> {
            val intervalCount = DEFAULT_Y_AXIS_INTERVAL_COUNT
            val step = resolveNiceDecimalStep(rawMaxValue / intervalCount)
            val resolvedMaxValue =
                if (rawMaxValue <= 0.0) {
                    step
                } else {
                    kotlin.math.ceil(rawMaxValue / step) * step
                }
            YAxisConfig(
                maxValue = resolvedMaxValue.toFloat(),
                maxLabelCount = intervalCount + 1,
            )
        }
        StatsChartMetric.DURATION -> resolveDurationYAxisConfig(rawMaxValue)
    }
}

private fun resolveNiceDecimalStep(rawStep: Double): Double {
    if (rawStep <= 0.0) return 1.0
    val magnitude = 10.0.pow(floor(log10(rawStep)))
    val normalized = rawStep / magnitude
    val niceNormalized =
        when {
            normalized <= 1.0 -> 1.0
            normalized <= 2.0 -> 2.0
            normalized <= 2.5 -> 2.5
            normalized <= 5.0 -> 5.0
            else -> 10.0
        }
    return niceNormalized * magnitude
}

private fun Double.formatCompactAxisNumber(): String =
    if (this % 1.0 == 0.0) {
        String.format(Locale.US, "%.0f", this)
    } else {
        String.format(Locale.US, "%.1f", this)
    }

private const val DEFAULT_Y_AXIS_INTERVAL_COUNT = 5
private val CHART_BAR_THICKNESS = 24.dp
private val CHART_BAR_SPACING = 32.dp
private val DURATION_AXIS_STEP_MINUTES = listOf(30L, 60L, 90L, 120L, 180L, 240L, 360L)
private val DURATION_AXIS_INTERVAL_COUNTS = listOf(3, 4, 5)

private fun resolveDurationYAxisConfig(rawMaxValue: Double): YAxisConfig {
    if (rawMaxValue <= 0.0) {
        val defaultIntervalCount = 4
        val defaultStepMillis = 30L * 60_000.0
        return YAxisConfig(
            maxValue = (defaultIntervalCount * defaultStepMillis).toFloat(),
            maxLabelCount = defaultIntervalCount + 1,
        )
    }

    val bestCandidate =
        DURATION_AXIS_STEP_MINUTES
            .flatMap { stepMinutes ->
                DURATION_AXIS_INTERVAL_COUNTS.map { intervalCount ->
                    val stepMillis = stepMinutes * 60_000.0
                    val maxValue = stepMillis * intervalCount
                    DurationAxisCandidate(
                        maxValue = maxValue,
                        intervalCount = intervalCount,
                        score =
                            durationAxisCandidateScore(
                                rawMaxValue = rawMaxValue,
                                maxValue = maxValue,
                                intervalCount = intervalCount,
                                stepMinutes = stepMinutes,
                            ),
                    )
                }
            }.filter { candidate -> candidate.maxValue >= rawMaxValue }
            .minByOrNull(DurationAxisCandidate::score)
            ?: DurationAxisCandidate(
                maxValue = 360L * 60_000.0 * 5,
                intervalCount = 5,
                score = 0.0,
            )

    return YAxisConfig(
        maxValue = bestCandidate.maxValue.toFloat(),
        maxLabelCount = bestCandidate.intervalCount + 1,
    )
}

private fun durationAxisCandidateScore(
    rawMaxValue: Double,
    maxValue: Double,
    intervalCount: Int,
    stepMinutes: Long,
): Double {
    val overshootScore = (maxValue - rawMaxValue) / rawMaxValue
    val intervalPenalty = kotlin.math.abs(intervalCount - 4) * 0.08
    val stepPenalty =
        when (stepMinutes) {
            30L, 60L, 120L, 180L -> 0.0
            90L, 240L -> 0.03
            else -> 0.08
        }
    return overshootScore + intervalPenalty + stepPenalty
}

private data class DurationAxisCandidate(
    val maxValue: Double,
    val intervalCount: Int,
    val score: Double,
)
