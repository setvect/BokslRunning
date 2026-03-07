package com.boksl.running.ui.feature.run

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.boksl.running.domain.model.RunEngineState
import com.boksl.running.domain.model.RunSnapshot
import java.util.Locale

@Composable
fun runReadyScreen(
    snapshot: RunSnapshot?,
    onStartRun: () -> Unit,
    onCancel: () -> Unit,
) {
    runScaffold(
        title = "러닝 준비",
        body = {
            metricColumn(
                headline = "Ready 화면 placeholder",
                lines =
                    listOf(
                        "현재 위치: ${snapshot?.latestLocation?.latitude ?: "-"}, ${snapshot?.latestLocation?.longitude ?: "-"}",
                        "오프라인에서도 기록은 저장됩니다.",
                    ),
            )
            Button(onClick = onStartRun, modifier = Modifier.fillMaxWidth()) {
                Text(text = "시작")
            }
            Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text(text = "취소")
            }
        },
    )
}

@Composable
fun runLiveScreen(
    snapshot: RunSnapshot?,
    onRequestStop: () -> Unit,
    onConfirmSave: () -> Unit,
    onCancelStop: () -> Unit,
) {
    runScaffold(
        title = "러닝 중",
        body = {
            metricColumn(
                headline = "러닝 라이브 placeholder",
                lines =
                    listOf(
                        "시간: ${snapshot?.durationMillis?.toDurationText() ?: "--:--"}",
                        "거리: ${snapshot?.totalDistanceMeters?.toKilometersText() ?: "0.00"} km",
                        "현재 pace: ${snapshot?.currentPaceSecPerKm?.toPaceText() ?: "계산 불가"}",
                        "평균 pace: ${snapshot?.averagePaceSecPerKm?.toPaceText() ?: "계산 불가"}",
                        "최고 속도: ${snapshot?.maxSpeedMps?.toSpeedText() ?: "0.00"} km/h",
                        "칼로리: ${snapshot?.calorieKcal?.toCaloriesText() ?: "프로필 입력 시 계산 가능"}",
                    ),
            )
            Button(onClick = onRequestStop, modifier = Modifier.fillMaxWidth()) {
                Text(text = "종료")
            }
        },
    )

    if (snapshot?.state == RunEngineState.STOP_CONFIRM) {
        AlertDialog(
            onDismissRequest = onCancelStop,
            title = { Text(text = "러닝을 종료하고 저장할까요?") },
            confirmButton = {
                TextButton(onClick = onConfirmSave) {
                    Text(text = "저장")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelStop) {
                    Text(text = "취소")
                }
            },
        )
    }
}

@Composable
fun runSummaryScreen(
    snapshot: RunSnapshot?,
    onComplete: () -> Unit,
) {
    runScaffold(
        title = "결과 요약",
        body = {
            metricColumn(
                headline = "결과 요약 placeholder",
                lines =
                    listOf(
                        "총 거리: ${snapshot?.totalDistanceMeters?.toKilometersText() ?: "0.00"} km",
                        "총 시간: ${snapshot?.durationMillis?.toDurationText() ?: "--:--"}",
                        "평균 pace: ${snapshot?.averagePaceSecPerKm?.toPaceText() ?: "계산 불가"}",
                        "최고 속도: ${snapshot?.maxSpeedMps?.toSpeedText() ?: "0.00"} km/h",
                        "칼로리: ${snapshot?.calorieKcal?.toCaloriesText() ?: "프로필 입력 시 계산 가능"}",
                    ),
            )
            Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
                Text(text = "완료")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun runScaffold(
    title: String,
    body: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(text = title) }) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = body,
        )
    }
}

@Composable
private fun metricColumn(
    headline: String,
    lines: List<String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = headline, style = MaterialTheme.typography.titleMedium)
        lines.forEach { line ->
            Text(text = line)
        }
    }
}

private fun Long.toDurationText(): String {
    val totalSeconds = this / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private fun Double.toKilometersText(): String = String.format(Locale.US, "%.2f", this / 1_000.0)

private fun Double.toPaceText(): String {
    val totalSeconds = toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d /km", minutes, seconds)
}

private fun Double.toSpeedText(): String = String.format(Locale.US, "%.2f", this * 3.6)

private fun Double.toCaloriesText(): String = String.format(Locale.US, "%.0f kcal", this)
