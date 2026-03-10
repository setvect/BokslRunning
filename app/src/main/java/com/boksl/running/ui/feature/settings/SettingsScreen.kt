package com.boksl.running.ui.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boksl.running.ui.common.AppDialog
import com.boksl.running.ui.common.AppPrimaryButton
import com.boksl.running.ui.common.AppScreenHeader
import com.boksl.running.ui.common.AppSectionCard
import com.boksl.running.ui.common.AppSecondaryButton
import com.boksl.running.ui.common.AppStatusCard
import com.boksl.running.ui.common.AppTextAction
import com.boksl.running.ui.common.AppUiTokens
import com.boksl.running.ui.common.appScreenModifier

@Composable
fun settingsScreen(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
) {
    Scaffold(containerColor = AppUiTokens.Background) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .then(appScreenModifier())
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppUiTokens.ScreenSpacing),
        ) {
            AppScreenHeader(
                title = "설정",
                onNavigateUp = actions.onNavigateUp,
            )
            settingsPrimaryActions(uiState = uiState, actions = actions)
            if (uiState.isDebugToolsVisible) {
                debugToolsSection(
                    uiState = uiState,
                    actions = actions,
                )
            }
        }
    }

    uiState.pendingAction?.let { pendingAction ->
        debugActionConfirmationDialog(
            pendingAction = pendingAction,
            onConfirm = actions.onConfirmPendingAction,
            onDismiss = actions.onDismissPendingAction,
        )
    }
}

@Composable
private fun settingsPrimaryActions(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
) {
    AppSectionCard {
        Text(
            text = "앱 설정",
            style = MaterialTheme.typography.titleMedium,
            color = AppUiTokens.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        AppPrimaryButton(
            text = "프로필 정보 수정",
            onClick = actions.onEditProfile,
        )
        AppSecondaryButton(
            text = "전체 내보내기",
            onClick = actions.onOpenExport,
            enabled = !uiState.isBusy,
        )
        AppSecondaryButton(
            text = "가져오기",
            onClick = actions.onOpenImport,
            enabled = !uiState.isBusy,
        )
        AppTextAction(
            text = "홈으로 이동",
            onClick = actions.onNavigateHome,
        )
    }
}

@Composable
private fun debugToolsSection(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
) {
    AppSectionCard {
        Text(
            text = "개발 도구",
            style = MaterialTheme.typography.titleMedium,
            color = AppUiTokens.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "debug 빌드에서만 테스트 데이터를 생성하거나 정리할 수 있습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppUiTokens.TextSecondary,
        )
        AppSecondaryButton(
            text = "최근 12개월 테스트 데이터 생성",
            onClick = actions.onGenerateSeedDataClick,
            enabled = !uiState.isBusy,
        )
        AppSecondaryButton(
            text = "생성한 테스트 데이터 삭제",
            onClick = actions.onDeleteSeedDataClick,
            enabled = !uiState.isBusy,
        )
        uiState.busyMessage?.let { busyMessage ->
            AppStatusCard(
                title = "작업 중",
                message = busyMessage,
                accentColor = AppUiTokens.Accent,
            )
        }
        uiState.statusMessage?.let { statusMessage ->
            settingsStatusCard(
                message = statusMessage,
                statusKind = uiState.statusKind,
                onClearStatusMessage = actions.onClearStatusMessage,
            )
        }
    }
}

@Composable
private fun settingsStatusCard(
    message: String,
    statusKind: SettingsStatusKind,
    onClearStatusMessage: () -> Unit,
) {
    AppSectionCard(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color =
                when (statusKind) {
                    SettingsStatusKind.SUCCESS -> AppUiTokens.Accent
                    SettingsStatusKind.ERROR -> AppUiTokens.Error
                    SettingsStatusKind.IDLE -> AppUiTokens.TextPrimary
                },
        )
        AppTextAction(
            text = "메시지 닫기",
            onClick = onClearStatusMessage,
        )
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
                "테스트 데이터를 생성할까요?" to
                    "기존에 생성한 테스트 데이터를 지우고 최근 12개월 테스트 데이터를 다시 만듭니다. 실제 사용자 기록은 유지됩니다."
            SettingsPendingAction.DELETE ->
                "테스트 데이터를 삭제할까요?" to
                    "debug 생성기로 만든 테스트 데이터만 삭제합니다. 실제 사용자 기록은 유지됩니다."
        }

    AppDialog(
        title = title,
        message = message,
        onDismiss = onDismiss,
        confirmText = "확인",
        onConfirm = onConfirm,
        confirmColor = AppUiTokens.Accent,
    )
}
