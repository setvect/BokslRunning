package com.boksl.running.ui.feature.settings

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun settingsScreen(
    uiState: SettingsUiState,
    onNavigateUp: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenImport: () -> Unit,
    onNavigateHome: () -> Unit,
    onGenerateSeedDataClick: () -> Unit,
    onDeleteSeedDataClick: () -> Unit,
    onConfirmPendingAction: () -> Unit,
    onDismissPendingAction: () -> Unit,
    onClearStatusMessage: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "설정") },
                navigationIcon = {
                    TextButton(onClick = onNavigateUp) {
                        Text(text = "뒤로")
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
            Button(onClick = onEditProfile, modifier = Modifier.fillMaxWidth()) {
                Text(text = "프로필 정보 수정")
            }
            Button(
                onClick = onOpenExport,
                enabled = !uiState.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "전체 내보내기")
            }
            Button(
                onClick = onOpenImport,
                enabled = !uiState.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "가져오기")
            }
            Button(onClick = onNavigateHome, modifier = Modifier.fillMaxWidth()) {
                Text(text = "홈")
            }
            if (uiState.isDebugToolsVisible) {
                debugToolsSection(
                    uiState = uiState,
                    onGenerateSeedDataClick = onGenerateSeedDataClick,
                    onDeleteSeedDataClick = onDeleteSeedDataClick,
                    onClearStatusMessage = onClearStatusMessage,
                )
            }
        }
    }

    uiState.pendingAction?.let { pendingAction ->
        debugActionConfirmationDialog(
            pendingAction = pendingAction,
            onConfirm = onConfirmPendingAction,
            onDismiss = onDismissPendingAction,
        )
    }
}

@Composable
private fun debugToolsSection(
    uiState: SettingsUiState,
    onGenerateSeedDataClick: () -> Unit,
    onDeleteSeedDataClick: () -> Unit,
    onClearStatusMessage: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "개발 도구",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "debug 빌드에서만 테스트 데이터를 생성하거나 정리할 수 있습니다.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onGenerateSeedDataClick,
                enabled = !uiState.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "최근 12개월 테스트 데이터 생성")
            }
            Button(
                onClick = onDeleteSeedDataClick,
                enabled = !uiState.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "생성한 테스트 데이터 삭제")
            }
            uiState.busyMessage?.let { busyMessage ->
                Text(
                    text = busyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            uiState.statusMessage?.let { statusMessage ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                when (uiState.statusKind) {
                                    SettingsStatusKind.SUCCESS -> MaterialTheme.colorScheme.primary
                                    SettingsStatusKind.ERROR -> MaterialTheme.colorScheme.error
                                    SettingsStatusKind.IDLE -> MaterialTheme.colorScheme.onSurface
                                },
                        )
                        TextButton(onClick = onClearStatusMessage, modifier = Modifier.fillMaxWidth()) {
                            Text(text = "메시지 닫기")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun debugActionConfirmationDialog(
    pendingAction: SettingsPendingAction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val (title, message) =
        when (pendingAction) {
            SettingsPendingAction.GENERATE ->
                "테스트 데이터 생성" to
                    "기존에 생성한 테스트 데이터를 지우고 최근 12개월 테스트 데이터를 다시 만듭니다. 실제 사용자 기록은 유지됩니다."
            SettingsPendingAction.DELETE ->
                "테스트 데이터 삭제" to
                    "debug 생성기로 만든 테스트 데이터만 삭제합니다. 실제 사용자 기록은 유지됩니다."
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = "확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "취소")
            }
        },
    )
}
