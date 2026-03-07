package com.boksl.running.ui.feature.run

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.boksl.running.domain.model.RunSnapshot
import com.boksl.running.domain.model.RunningSession
import com.google.android.gms.maps.model.LatLng
import java.util.Locale

@Composable
fun runReadyScreen(
    uiState: RunSessionUiState,
    onStartRun: () -> Unit,
    onCancel: () -> Unit,
) {
    val currentLocation = uiState.snapshot?.latestLocation?.let { LatLng(it.latitude, it.longitude) }

    runScaffold(
        title = "러닝 준비",
        body = {
            if (uiState.isOffline) {
                offlineBanner()
            }
            runMapSection(
                currentLocation = currentLocation,
                routePoints = emptyList(),
                modifier = Modifier.fillMaxWidth(),
                maxZoom = 18f,
            )
            metricCard(
                title = "현재 상태",
                entries =
                    listOf(
                        "현재 위치" to
                            (
                                currentLocation?.let {
                                    "${"%.5f".format(Locale.US, it.latitude)}, " +
                                        "${"%.5f".format(Locale.US, it.longitude)}"
                                }
                                    ?: "위치 확인 중"
                            ),
                        "안내" to if (uiState.canStart) "시작 버튼을 누르면 기록을 시작합니다." else "위치를 준비하는 중입니다.",
                    ),
            )
            Button(
                onClick = onStartRun,
                enabled = uiState.canStart,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = if (uiState.isStarting) "시작 중..." else "시작")
            }
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text(text = "취소")
            }
        },
    )
}

@Composable
fun runLiveScreen(
    uiState: RunSessionUiState,
    onRequestStop: () -> Unit,
    onConfirmSave: () -> Unit,
    onCancelStop: () -> Unit,
) {
    val routePoints = uiState.trackPoints.map { it.toLatLng() }
    val currentLocation = uiState.snapshot?.latestLocation?.let { LatLng(it.latitude, it.longitude) }

    runScaffold(
        title = "러닝 중",
        body = {
            if (uiState.isOffline) {
                offlineBanner()
            }
            runMapSection(
                currentLocation = currentLocation,
                routePoints = routePoints,
                modifier = Modifier.fillMaxWidth(),
                maxZoom = 16f,
            )
            metricsGrid(snapshot = uiState.snapshot, savedSession = uiState.savedSession)
            Button(onClick = onRequestStop, modifier = Modifier.fillMaxWidth()) {
                Text(text = "종료")
            }
        },
    )

    if (uiState.showStopConfirm) {
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
    uiState: RunSessionUiState,
    onComplete: () -> Unit,
) {
    val routePoints = uiState.trackPoints.map { it.toLatLng() }
    val summarySession = uiState.savedSession

    runScaffold(
        title = "결과 요약",
        body = {
            if (uiState.isOffline) {
                offlineBanner()
            }
            runMapSection(
                currentLocation = routePoints.lastOrNull(),
                routePoints = routePoints,
                modifier = Modifier.fillMaxWidth(),
                maxZoom = 17f,
            )
            summaryCard(summarySession = summarySession, snapshot = uiState.snapshot)
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = body,
        )
    }
}

@Composable
private fun offlineBanner() {
    Card {
        Text(
            text = "기록은 저장되고, 지도 배경은 제한될 수 있어요.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun metricsGrid(
    snapshot: RunSnapshot?,
    savedSession: RunningSession?,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "실시간 지표", style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                metricTile(
                    label = "시간",
                    value = snapshot?.durationMillis?.toDurationText() ?: "--:--",
                    modifier = Modifier.weight(1f),
                )
                metricTile(
                    label = "거리",
                    value = "${snapshot?.totalDistanceMeters?.toKilometersText() ?: "0.00"} km",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                metricTile(
                    label = "현재 페이스",
                    value = snapshot?.currentPaceSecPerKm.toPaceText(),
                    modifier = Modifier.weight(1f),
                )
                metricTile(
                    label = "평균 페이스",
                    value = snapshot?.averagePaceSecPerKm.toPaceText(),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                metricTile(
                    label = "최고 속도",
                    value = "${snapshot?.maxSpeedMps?.toSpeedText() ?: "0.00"} km/h",
                    modifier = Modifier.weight(1f),
                )
                metricTile(
                    label = "칼로리",
                    value = (snapshot?.calorieKcal ?: savedSession?.stats?.calorieKcal).toCaloriesText(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun summaryCard(
    summarySession: RunningSession?,
    snapshot: RunSnapshot?,
) {
    val stats = summarySession?.stats
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "저장된 결과", style = MaterialTheme.typography.titleMedium)
            summaryRow(
                label = "총 거리",
                value = "${(stats?.distanceMeters ?: snapshot?.totalDistanceMeters ?: 0.0).toKilometersText()} km",
            )
            summaryRow(
                label = "총 시간",
                value = (stats?.durationMillis ?: snapshot?.durationMillis ?: 0L).toDurationText(),
            )
            summaryRow(
                label = "평균 페이스",
                value = (stats?.averagePaceSecPerKm ?: snapshot?.averagePaceSecPerKm).toPaceText(),
            )
            summaryRow(
                label = "최고 속도",
                value = "${(stats?.maxSpeedMps ?: snapshot?.maxSpeedMps ?: 0.0).toSpeedText()} km/h",
            )
            summaryRow(
                label = "칼로리",
                value = (stats?.calorieKcal ?: snapshot?.calorieKcal).toCaloriesText(),
            )
        }
    }
}

@Composable
private fun metricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun metricCard(
    title: String,
    entries: List<Pair<String, String>>,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            entries.forEach { (label, value) -> summaryRow(label, value) }
        }
    }
}

@Composable
private fun summaryRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.titleSmall)
    }
}

private fun Long.toDurationText(): String {
    val totalSeconds = this / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

private fun Double.toKilometersText(): String = String.format(Locale.US, "%.2f", this / 1_000.0)

private fun Double?.toPaceText(): String {
    val pace = this ?: return "계산 불가"
    val totalSeconds = pace.toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d /km", minutes, seconds)
}

private fun Double.toSpeedText(): String = String.format(Locale.US, "%.2f", this * 3.6)

private fun Double?.toCaloriesText(): String =
    this?.let { String.format(Locale.US, "%.0f kcal", it) } ?: "프로필 입력 시 계산 가능"
