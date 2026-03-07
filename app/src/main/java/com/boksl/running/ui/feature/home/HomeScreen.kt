package com.boksl.running.ui.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.boksl.running.ui.feature.permission.LocationPermissionUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun homeScreen(
    uiState: HomeUiState,
    onStartRun: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissPermissionDialog: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onDismissRunPlaceholder: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "홈") },
                actions = {
                    TextButton(onClick = onOpenSettings) {
                        Text(text = "설정")
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
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "누적 요약",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    summaryRow(label = "총 달린 거리", value = "${uiState.totalDistanceMeters.toKilometersText()} km")
                    summaryRow(label = "달린 시간", value = uiState.totalDurationMillis.toDurationText())
                    summaryRow(label = "평균 속도", value = "${uiState.averageSpeedMps.toKmhText()} km/h")
                    summaryRow(
                        label = "소모 칼로리",
                        value =
                            if (uiState.hasProfile) {
                                "${uiState.totalCaloriesKcal.toCaloriesText()} kcal"
                            } else {
                                "프로필 입력 시 계산 가능"
                            },
                    )
                }
            }
            Button(onClick = onStartRun, modifier = Modifier.fillMaxWidth()) {
                Text(text = "러닝 시작")
            }
            Button(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) {
                Text(text = "기록")
            }
            Button(onClick = onOpenStats, modifier = Modifier.fillMaxWidth()) {
                Text(text = "통계")
            }
        }
    }

    uiState.permissionDialogState?.let { dialogState ->
        locationPermissionDialog(
            uiState = dialogState,
            onDismiss = onDismissPermissionDialog,
            onOpenAppSettings = onOpenAppSettings,
        )
    }

    if (uiState.showRunPlaceholder) {
        AlertDialog(
            onDismissRequest = onDismissRunPlaceholder,
            title = { Text(text = "러닝 준비 중") },
            text = { Text(text = "Ready 화면과 기록 엔진은 Phase 3에서 구현됩니다.") },
            confirmButton = {
                TextButton(onClick = onDismissRunPlaceholder) {
                    Text(text = "확인")
                }
            },
        )
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

@Composable
private fun locationPermissionDialog(
    uiState: LocationPermissionUiState,
    onDismiss: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    val message =
        when (uiState) {
            LocationPermissionUiState.Granted -> "권한이 이미 허용되어 있습니다."
            LocationPermissionUiState.Denied,
            LocationPermissionUiState.ShowRequest,
            ->
                "위치 권한이 없으면 경로를 기록할 수 없어요"
            LocationPermissionUiState.PermanentlyDenied ->
                "위치 권한이 차단되어 있어요. 설정에서 권한을 허용해 주세요."
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "권한 안내") },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onOpenAppSettings) {
                Text(text = "설정 열기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "취소")
            }
        },
    )
}

private fun Double.toKilometersText(): String = String.format(Locale.US, "%.2f", this / 1_000.0)

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

private fun Double.toKmhText(): String = String.format(Locale.US, "%.2f", this * 3.6)

private fun Double.toCaloriesText(): String = String.format(Locale.US, "%.0f", this)
