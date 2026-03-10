package com.boksl.running.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import java.util.Locale

fun Double.formatDistanceKm(): String = String.format(Locale.US, "%.2f", this / METERS_PER_KILOMETER)

fun Long.formatDurationText(): String {
    val totalSeconds = this / MILLIS_PER_SECOND
    val hours = totalSeconds / SECONDS_PER_HOUR
    val minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun Double?.formatPaceText(): String {
    val pace = this ?: return "계산 불가"
    val totalSeconds = pace.toInt()
    val minutes = totalSeconds / SECONDS_PER_MINUTE.toInt()
    val seconds = totalSeconds % SECONDS_PER_MINUTE.toInt()
    return String.format(Locale.US, "%d:%02d /km", minutes, seconds)
}

fun Double.formatSpeedKmh(): String = String.format(Locale.US, "%.2f", this * KILOMETERS_PER_HOUR_PER_METERS_PER_SECOND)

fun Double.formatCaloriesValue(): String = String.format(Locale.US, "%.0f", this)

fun Double.formatGroupedIntegerText(): String = NumberFormat.getIntegerInstance(Locale.KOREA).format(this.toLong())

fun Long.formatHourMinuteKoreanText(): String {
    val totalMinutes = this / MILLIS_PER_SECOND / SECONDS_PER_MINUTE
    val hours = totalMinutes / MINUTES_PER_HOUR
    val minutes = totalMinutes % MINUTES_PER_HOUR
    return String.format(Locale.KOREA, "%03d시간 %02d분", hours, minutes)
}

fun Long.formatHourMinuteSecondKoreanText(): String {
    val totalSeconds = this / MILLIS_PER_SECOND
    val hours = totalSeconds / SECONDS_PER_HOUR
    val minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return if (hours > 0L) {
        String.format(Locale.KOREA, "%02d시간 %02d분 %02d초", hours, minutes, seconds)
    } else {
        String.format(Locale.KOREA, "%02d분 %02d초", minutes, seconds)
    }
}

fun Double?.formatCaloriesText(): String = this?.let { "${it.formatCaloriesValue()} kcal" } ?: "프로필 입력 시 계산 가능"

fun Long.formatSessionDateTimeText(): String =
    Instant
        .ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(sessionDateTimeFormatter)

private val sessionDateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.getDefault())

private const val METERS_PER_KILOMETER = 1_000.0
private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
private const val MINUTES_PER_HOUR = 60L
private const val KILOMETERS_PER_HOUR_PER_METERS_PER_SECOND = 3.6
