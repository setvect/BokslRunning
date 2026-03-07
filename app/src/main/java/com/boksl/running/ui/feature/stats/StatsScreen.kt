package com.boksl.running.ui.feature.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Alignment
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
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.values.ChartValues
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun statsScreen(
    uiState: StatsUiState,
    onMetricSelected: (StatsChartMetric) -> Unit,
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
                    text = "최근 6개월",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            metricTabs(
                selectedMetric = uiState.selectedMetric,
                onMetricSelected = onMetricSelected,
            )
            if (uiState.hasRecordedSessions) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    monthlyStatsChart(
                        monthlyStats = uiState.monthlyStats,
                        selectedMetric = uiState.selectedMetric,
                    )
                    monthlyChartLegend(
                        monthlyStats = uiState.monthlyStats,
                        selectedMetric = uiState.selectedMetric,
                    )
                }
            } else {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "저장된 러닝 기록이 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
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
    monthlyStats: List<MonthlyStatsPoint>,
    selectedMetric: StatsChartMetric,
) {
    val modelProducer = remember { ChartEntryModelProducer(listOf(FloatEntry(0f, 0f))) }
    val chartValues = remember(monthlyStats, selectedMetric) { monthlyStats.map { it.chartValue(selectedMetric) } }
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

    LaunchedEffect(chartValues, modelProducer) {
        modelProducer.setEntries(chartEntries)
    }

    ProvideChartStyle(chartStyle = m3ChartStyle()) {
        Chart(
            chart = columnChart(),
            chartModelProducer = modelProducer,
            startAxis = rememberStartAxis(valueFormatter = startAxisValueFormatter),
            bottomAxis =
                rememberBottomAxis(
                    valueFormatter = bottomAxisValueFormatter,
                    itemPlacer = remember { AxisItemPlacer.Horizontal.default(spacing = 1) },
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(240.dp),
        )
    }
}

@Composable
private fun monthlyChartLegend(
    monthlyStats: List<MonthlyStatsPoint>,
    selectedMetric: StatsChartMetric,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        monthlyStats.forEach { point ->
            Column(modifier = Modifier.width(44.dp)) {
                Text(
                    text = point.yearMonth.formatYearMonthLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = point.formatValue(selectedMetric),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
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
        StatsChartMetric.DISTANCE -> String.format("%.1f", this)
        StatsChartMetric.DURATION -> toLong().formatChartDurationLabel()
        StatsChartMetric.AVERAGE_SPEED -> String.format("%.1f", this)
    }
