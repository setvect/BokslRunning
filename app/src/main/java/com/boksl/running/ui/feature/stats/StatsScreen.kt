@file:Suppress("LongMethod", "MagicNumber", "TooManyFunctions")

package com.boksl.running.ui.feature.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boksl.running.domain.model.MonthlyStatsPoint
import com.boksl.running.domain.model.StatsChartMetric
import com.boksl.running.ui.common.AppScreenHeader
import com.boksl.running.ui.common.AppSectionCard
import com.boksl.running.ui.common.AppSecondaryButton
import com.boksl.running.ui.common.AppUiTokens
import com.boksl.running.ui.common.appScreenModifier
import com.boksl.running.ui.formatChartDurationLabel
import com.boksl.running.ui.formatDistanceKm
import com.boksl.running.ui.formatHourMinuteKoreanText
import com.boksl.running.ui.formatSpeedKmh
import com.boksl.running.ui.formatStatsChartValue
import com.boksl.running.ui.formatYearMonthLabel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.layout.segmented
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.compose.component.marker.markerComponent
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.compose.component.textComponent
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

@Composable
fun statsScreen(
    uiState: StatsUiState,
    onMetricSelected: (StatsChartMetric) -> Unit,
    onMonthSelected: (Int) -> Unit,
    onNavigateUp: () -> Unit,
) {
    Scaffold(containerColor = AppUiTokens.Background) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .then(appScreenModifier())
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppUiTokens.ScreenSpacing),
        ) {
            AppScreenHeader(
                title = "통계",
                onNavigateUp = onNavigateUp,
            )
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
    AppSectionCard {
        Text(
            text = "누적 요약",
            style = MaterialTheme.typography.titleMedium,
            color = AppUiTokens.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            statsSummaryTile(
                modifier = Modifier.weight(1f),
                label = "거리",
                value = "${uiState.totalDistanceMeters.formatDistanceKm()} km",
                accent = AppUiTokens.Accent,
            )
            statsSummaryTile(
                modifier = Modifier.weight(1f),
                label = "시간",
                value = uiState.totalDurationMillis.formatHourMinuteKoreanText(),
                accent = AppUiTokens.AccentSecondary,
            )
        }
        statsSummaryTile(
            modifier = Modifier.fillMaxWidth(),
            label = "평균 속도",
            value = "${uiState.averageSpeedMps.formatSpeedKmh()} km/h",
            accent = AppUiTokens.AccentMuted,
        )
    }
}

@Composable
private fun monthlyChartCard(
    uiState: StatsUiState,
    onMetricSelected: (StatsChartMetric) -> Unit,
    onMonthSelected: (Int) -> Unit,
) {
    AppSectionCard {
        Text(
            text = "월별 흐름",
            style = MaterialTheme.typography.titleMedium,
            color = AppUiTokens.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        metricTabs(
            selectedMetric = uiState.selectedMetric,
            onMetricSelected = onMetricSelected,
        )
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
                text = "저장된 기록이 없어 최근 6개월을 0값으로 표시합니다.",
                style = MaterialTheme.typography.bodySmall,
                color = AppUiTokens.TextSecondary,
            )
        }
    }
}

@Composable
private fun metricTabs(
    selectedMetric: StatsChartMetric,
    onMetricSelected: (StatsChartMetric) -> Unit,
) {
    val metrics = remember { StatsChartMetric.entries }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        metrics.forEach { metric ->
            val selected = metric == selectedMetric
            AppSecondaryButton(
                text = metric.toTabLabel(),
                onClick = { onMetricSelected(metric) },
                modifier = Modifier.weight(1f),
                containerColor = if (selected) metric.accentColor() else AppUiTokens.Pill,
                contentColor = if (selected) AppUiTokens.Background else AppUiTokens.TextPrimary,
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
    val xLabels = remember(monthlyStats) { monthlyStats.map { it.yearMonth.formatYearMonthLabel() } }
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
    val maxChartValue =
        remember(chartValues) {
            (chartValues.maxOrNull()?.takeIf { it > 0.0 }?.times(1.12) ?: 1.0).toFloat()
        }
    val columnComponent =
        lineComponent(
            color = selectedMetric.accentColor(),
            thickness = CHART_BAR_THICKNESS,
            shape = Shapes.roundedCornerShape(allPercent = 40),
        )

    LaunchedEffect(chartEntries, modelProducer) {
        modelProducer.setEntries(chartEntries)
    }

    Chart(
        chart =
            columnChart(
                columns = listOf(columnComponent),
                spacing = CHART_BAR_SPACING,
                axisValuesOverrider = AxisValuesOverrider.fixed(minY = 0f, maxY = maxChartValue),
            ),
        chartModelProducer = modelProducer,
        chartScrollSpec = chartScrollSpec,
        chartScrollState = chartScrollState,
        bottomAxis =
            rememberBottomAxis(
                valueFormatter = bottomAxisValueFormatter,
                label = textComponent(color = AppUiTokens.TextSecondary),
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
                .height(180.dp),
    )
}

@Composable
private fun selectedMonthDetailCard(
    selectedMonth: MonthlyStatsPoint?,
    selectedMetric: StatsChartMetric,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppUiTokens.SecondaryButtonRadius),
        colors = CardDefaults.cardColors(containerColor = AppUiTokens.SurfaceMuted),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = selectedMonth?.yearMonth?.formatYearMonthLabel() ?: "--.--",
                style = MaterialTheme.typography.bodySmall,
                color = AppUiTokens.TextSecondary,
            )
            Text(
                text = selectedMonth?.formatValue(selectedMetric) ?: "--",
                style = MaterialTheme.typography.headlineSmall,
                color = AppUiTokens.TextPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun statsSummaryTile(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(AppUiTokens.SecondaryButtonRadius),
        colors = CardDefaults.cardColors(containerColor = AppUiTokens.SurfaceMuted),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = accent,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = AppUiTokens.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun rememberStatsMarker(selectedMetric: StatsChartMetric): MarkerComponent {
    val background =
        shapeComponent(
            color = AppUiTokens.Card,
            shape = RoundedCornerShape(12.dp),
            strokeWidth = 1.dp,
            strokeColor = AppUiTokens.Divider,
        )
    val label =
        textComponent(
            color = AppUiTokens.TextPrimary,
            background = background,
        )
    val indicator = shapeComponent(color = selectedMetric.accentColor(), shape = RoundedCornerShape(8.dp))
    val guideline = lineComponent(color = selectedMetric.accentColor(), thickness = 2.dp)

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

private fun StatsChartMetric.toTabLabel(): String =
    when (this) {
        StatsChartMetric.DISTANCE -> "거리"
        StatsChartMetric.DURATION -> "시간"
        StatsChartMetric.AVERAGE_SPEED -> "평균 속도"
    }

private fun StatsChartMetric.accentColor() =
    when (this) {
        StatsChartMetric.DISTANCE -> AppUiTokens.Accent
        StatsChartMetric.DURATION -> AppUiTokens.AccentSecondary
        StatsChartMetric.AVERAGE_SPEED -> AppUiTokens.AccentMuted
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

private fun Double.formatForMarker(metric: StatsChartMetric): String =
    when (metric) {
        StatsChartMetric.DISTANCE -> formatStatsChartValue(metric)
        StatsChartMetric.DURATION -> toLong().formatStatsChartValue(metric)
        StatsChartMetric.AVERAGE_SPEED -> formatStatsChartValue(metric)
    }
private val CHART_BAR_THICKNESS = 18.dp
private val CHART_BAR_SPACING = 28.dp
