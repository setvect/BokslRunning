package com.boksl.running.ui

import com.boksl.running.domain.model.StatsChartMetric
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class UiFormattersTest {
    @Test
    fun formatYearMonthLabelUsesTwoDigitYearAndMonth() {
        assertEquals("26.03", YearMonth.of(2026, 3).formatYearMonthLabel())
    }

    @Test
    fun formatChartDurationLabelUsesHoursAndMinutes() {
        assertEquals("01:30", 5_400_000L.formatChartDurationLabel())
    }

    @Test
    fun formatStatsChartValueFormatsDistanceAndSpeedDisplayUnits() {
        assertEquals("2.5 km", 2.5.formatStatsChartValue(StatsChartMetric.DISTANCE))
        assertEquals("12.3 km/h", 12.34.formatStatsChartValue(StatsChartMetric.AVERAGE_SPEED))
    }
}
