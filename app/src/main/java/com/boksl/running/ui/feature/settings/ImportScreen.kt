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
import com.boksl.running.domain.model.ImportProgress
import com.boksl.running.ui.common.AppPrimaryButton
import com.boksl.running.ui.common.AppScreenHeader
import com.boksl.running.ui.common.AppSectionCard
import com.boksl.running.ui.common.AppSecondaryButton
import com.boksl.running.ui.common.AppStatusCard
import com.boksl.running.ui.common.AppTextAction
import com.boksl.running.ui.common.AppUiTokens
import com.boksl.running.ui.common.appScreenModifier

@Composable
fun importScreen(
    uiState: ImportUiState,
    actions: ImportScreenActions,
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
                title = "가져오기",
                onNavigateUp = actions.onNavigateUp,
            )
            importProgressContent(progress = uiState.progress, actions = actions)
        }
    }
}

@Composable
private fun importProgressContent(
    progress: ImportProgress,
    actions: ImportScreenActions,
) {
    when (progress) {
        ImportProgress.Idle -> importIdleContent(actions = actions)
        ImportProgress.BackingUp -> importRunningContent(message = "1/2 내부 백업 중", actions = actions)
        ImportProgress.Importing -> importRunningContent(message = "2/2 데이터 병합 중", actions = actions)
        is ImportProgress.Completed -> importCompletedContent(progress = progress, actions = actions)
        is ImportProgress.Error -> importErrorContent(progress = progress, actions = actions)
    }
}

@Composable
private fun importIdleContent(actions: ImportScreenActions) {
    importInfoCard()
    AppPrimaryButton(text = "가져오기 시작", onClick = actions.onStartImport)
}

@Composable
private fun importRunningContent(
    message: String,
    actions: ImportScreenActions,
) {
    importInfoCard()
    AppStatusCard(
        title = message,
        message = "선택한 JSON 파일을 검증하고 현재 데이터와 병합하고 있습니다.",
        accentColor = AppUiTokens.Accent,
    )
    AppSecondaryButton(text = "취소", onClick = actions.onCancelImport)
}

@Composable
private fun importCompletedContent(
    progress: ImportProgress.Completed,
    actions: ImportScreenActions,
) {
    importCompletedCard(progress = progress)
    AppPrimaryButton(text = "확인", onClick = actions.onConfirm)
    AppTextAction(text = "홈으로 이동", onClick = actions.onNavigateHome)
}

@Composable
private fun importCompletedCard(progress: ImportProgress.Completed) {
    AppSectionCard {
        if (progress.result.wasDuplicateFile) {
            Text(
                text = "이미 가져온 파일입니다",
                style = MaterialTheme.typography.titleMedium,
                color = AppUiTokens.TextPrimary,
            )
        } else {
            Text(
                text = "가져오기 완료",
                style = MaterialTheme.typography.titleMedium,
                color = AppUiTokens.TextPrimary,
            )
            Text(
                text = "추가 ${progress.result.addedSessionCount}건, 중복 ${progress.result.duplicateSessionCount}건",
                style = MaterialTheme.typography.bodyMedium,
                color = AppUiTokens.TextSecondary,
            )
            if (progress.result.appliedProfile) {
                Text(
                    text = "프로필과 앱 설정도 함께 복원했습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppUiTokens.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun importErrorContent(
    progress: ImportProgress.Error,
    actions: ImportScreenActions,
) {
    importInfoCard()
    AppStatusCard(
        title = "가져오기 실패",
        message = progress.message,
        accentColor = AppUiTokens.Error,
    )
    AppPrimaryButton(text = "다시 시도", onClick = actions.onStartImport)
    AppTextAction(text = "뒤로", onClick = actions.onNavigateUp)
}

@Composable
private fun importInfoCard() {
    AppSectionCard {
        Text(
            text = "가져오기 전에 현재 데이터를 내부 백업합니다",
            style = MaterialTheme.typography.titleMedium,
            color = AppUiTokens.TextPrimary,
        )
        Text(
            text = "정책: 기존 데이터 유지 + 가져온 데이터 병합",
            style = MaterialTheme.typography.bodyMedium,
            color = AppUiTokens.TextSecondary,
        )
    }
}
