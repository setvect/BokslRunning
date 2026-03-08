package com.boksl.running.ui

import com.boksl.running.domain.model.StatsChartMetric
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

fun YearMonth.formatYearMonthLabel(): String = this.format(yearMonthFormatter)

fun Double.formatChartDistanceLabel(): String = String.format(Locale.US, "%.1f km", this)

fun Double.formatChartSpeedLabel(): String = String.format(Locale.US, "%.1f km/h", this)

fun Long.formatChartDurationLabel(): String {
    val totalMinutes = this / MILLIS_PER_MINUTE
    val hours = totalMinutes / MINUTES_PER_HOUR
    val minutes = totalMinutes % MINUTES_PER_HOUR
    return String.format(Locale.US, "%02d:%02d", hours, minutes)
}

fun Double.formatStatsChartValue(metric: StatsChartMetric): String =
    when (metric) {
        StatsChartMetric.DISTANCE -> formatChartDistanceLabel()
        StatsChartMetric.AVERAGE_SPEED -> formatChartSpeedLabel()
        StatsChartMetric.DURATION -> error("Use Long.formatStatsChartValue for duration values.")
    }

fun Long.formatStatsChartValue(metric: StatsChartMetric): String =
    when (metric) {
        StatsChartMetric.DURATION -> formatChartDurationLabel()
        StatsChartMetric.DISTANCE -> error("Use Double.formatStatsChartValue for distance values.")
        StatsChartMetric.AVERAGE_SPEED -> error("Use Double.formatStatsChartValue for average speed values.")
    }

private val yearMonthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yy.MM", Locale.getDefault())

private const val MILLIS_PER_MINUTE = 60_000L
private const val MINUTES_PER_HOUR = 60L
