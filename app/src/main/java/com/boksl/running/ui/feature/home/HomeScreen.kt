package com.boksl.running.ui.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.boksl.running.ui.feature.permission.locationPermissionDialog
import com.boksl.running.ui.formatCaloriesValue
import com.boksl.running.ui.formatDistanceKm
import com.boksl.running.ui.formatDurationText
import com.boksl.running.ui.formatSpeedKmh

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
                    summaryRow(label = "총 달린 거리", value = "${uiState.totalDistanceMeters.formatDistanceKm()} km")
                    summaryRow(label = "달린 시간", value = uiState.totalDurationMillis.formatDurationText())
                    summaryRow(label = "평균 속도", value = "${uiState.averageSpeedMps.formatSpeedKmh()} km/h")
                    summaryRow(
                        label = "소모 칼로리",
                        value =
                            if (uiState.hasProfile) {
                                "${uiState.totalCaloriesKcal.formatCaloriesValue()} kcal"
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
