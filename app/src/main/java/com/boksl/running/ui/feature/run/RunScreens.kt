@file:Suppress("TooManyFunctions")

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
import com.boksl.running.ui.formatCaloriesText
import com.boksl.running.ui.formatDistanceKm
import com.boksl.running.ui.formatDurationText
import com.boksl.running.ui.formatPaceText
import com.boksl.running.ui.formatSpeedKmh
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
                runOfflineBanner()
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
                runOfflineBanner()
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
                runOfflineBanner()
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

@Composable
fun runRecoveryScreen(
    uiState: RunRecoveryUiState,
    onContinue: () -> Unit,
    onDiscard: () -> Unit,
) {
    val session = uiState.activeSession

    runScaffold(
        title = "러닝 복구",
        body = {
            if (uiState.isOffline) {
                runOfflineBanner()
            }
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = "진행 중이던 러닝이 있어요. 마지막 저장 지점부터 이어서 기록할까요?")
                    Text(
                        text = "앱이 꺼져 있던 구간은 기록되지 않을 수 있어요",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (session != null) {
                metricCard(
                    title = "복구 대상",
                    entries =
                        listOf(
                            "누적 거리" to "${session.stats.distanceMeters.formatDistanceKm()} km",
                            "누적 시간" to session.stats.durationMillis.formatDurationText(),
                            "평균 페이스" to session.stats.averagePaceSecPerKm.formatPaceText(),
                        ),
                )
            }
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth(), enabled = session != null) {
                Text(text = "이어하기")
            }
            TextButton(onClick = onDiscard, modifier = Modifier.fillMaxWidth(), enabled = session != null) {
                Text(text = "폐기")
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
fun runOfflineBanner() {
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
                    value = snapshot?.durationMillis?.formatDurationText() ?: "--:--",
                    modifier = Modifier.weight(1f),
                )
                metricTile(
                    label = "거리",
                    value = "${snapshot?.totalDistanceMeters?.formatDistanceKm() ?: "0.00"} km",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                metricTile(
                    label = "현재 페이스",
                    value = snapshot?.currentPaceSecPerKm.formatPaceText(),
                    modifier = Modifier.weight(1f),
                )
                metricTile(
                    label = "평균 페이스",
                    value = snapshot?.averagePaceSecPerKm.formatPaceText(),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                metricTile(
                    label = "최고 속도",
                    value = "${snapshot?.maxSpeedMps?.formatSpeedKmh() ?: "0.00"} km/h",
                    modifier = Modifier.weight(1f),
                )
                metricTile(
                    label = "칼로리",
                    value = (snapshot?.calorieKcal ?: savedSession?.stats?.calorieKcal).formatCaloriesText(),
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
                value = "${(stats?.distanceMeters ?: snapshot?.totalDistanceMeters ?: 0.0).formatDistanceKm()} km",
            )
            summaryRow(
                label = "총 시간",
                value = (stats?.durationMillis ?: snapshot?.durationMillis ?: 0L).formatDurationText(),
            )
            summaryRow(
                label = "평균 페이스",
                value = (stats?.averagePaceSecPerKm ?: snapshot?.averagePaceSecPerKm).formatPaceText(),
            )
            summaryRow(
                label = "최고 속도",
                value = "${(stats?.maxSpeedMps ?: snapshot?.maxSpeedMps ?: 0.0).formatSpeedKmh()} km/h",
            )
            summaryRow(
                label = "칼로리",
                value = (stats?.calorieKcal ?: snapshot?.calorieKcal).formatCaloriesText(),
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
