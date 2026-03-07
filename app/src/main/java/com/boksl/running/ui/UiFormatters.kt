package com.boksl.running.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun Double.formatDistanceKm(): String = String.format(Locale.US, "%.2f", this / metersPerKilometer)

fun Long.formatDurationText(): String {
    val totalSeconds = this / millisPerSecond
    val hours = totalSeconds / secondsPerHour
    val minutes = (totalSeconds % secondsPerHour) / secondsPerMinute
    val seconds = totalSeconds % secondsPerMinute
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun Double?.formatPaceText(): String {
    val pace = this ?: return "계산 불가"
    val totalSeconds = pace.toInt()
    val minutes = totalSeconds / secondsPerMinute.toInt()
    val seconds = totalSeconds % secondsPerMinute.toInt()
    return String.format(Locale.US, "%d:%02d /km", minutes, seconds)
}

fun Double.formatSpeedKmh(): String = String.format(Locale.US, "%.2f", this * kilometersPerHourPerMetersPerSecond)

fun Double.formatCaloriesValue(): String = String.format(Locale.US, "%.0f", this)

fun Double?.formatCaloriesText(): String =
    this?.let { "${it.formatCaloriesValue()} kcal" } ?: "프로필 입력 시 계산 가능"

fun Long.formatSessionDateTimeText(): String =
    Instant
        .ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(sessionDateTimeFormatter)

private val sessionDateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.getDefault())

private const val metersPerKilometer = 1_000.0
private const val millisPerSecond = 1_000L
private const val secondsPerMinute = 60L
private const val secondsPerHour = 3_600L
private const val kilometersPerHourPerMetersPerSecond = 3.6
