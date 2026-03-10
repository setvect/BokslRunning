package com.boksl.running.ui.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.boksl.running.domain.model.ExportProgress
import com.boksl.running.ui.common.AppPrimaryButton
import com.boksl.running.ui.common.AppScreenHeader
import com.boksl.running.ui.common.AppSectionCard
import com.boksl.running.ui.common.AppSecondaryButton
import com.boksl.running.ui.common.AppStatusCard
import com.boksl.running.ui.common.AppTextAction
import com.boksl.running.ui.common.AppUiTokens
import com.boksl.running.ui.common.appScreenModifier

@Composable
fun exportScreen(
    uiState: ExportUiState,
    actions: ExportScreenActions,
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
                title = "전체 내보내기",
                onNavigateUp = actions.onNavigateUp,
            )
            exportProgressContent(progress = uiState.progress, actions = actions)
        }
    }
}

@Composable
private fun exportProgressContent(
    progress: ExportProgress,
    actions: ExportScreenActions,
) {
    when (progress) {
        ExportProgress.Idle -> exportIdleContent(actions = actions)
        is ExportProgress.Running -> exportRunningContent(progress = progress, actions = actions)
        is ExportProgress.Completed -> exportCompletedContent(actions = actions)
        is ExportProgress.Error -> exportErrorContent(progress = progress, actions = actions)
    }
}

@Composable
private fun exportIdleContent(actions: ExportScreenActions) {
    exportInfoCard()
    AppPrimaryButton(text = "내보내기 시작", onClick = actions.onStartExport)
}

@Composable
private fun exportRunningContent(
    progress: ExportProgress.Running,
    actions: ExportScreenActions,
) {
    exportInfoCard()
    AppStatusCard(
        title = "${progress.totalSessions}개 중 ${progress.completedSessions}개 생성 중",
        message = "모든 러닝 기록, 프로필, 앱 설정을 JSON 파일로 만들고 있습니다.",
        accentColor = AppUiTokens.Accent,
    )
    AppSecondaryButton(text = "취소", onClick = actions.onCancelExport)
}

@Composable
private fun exportCompletedContent(actions: ExportScreenActions) {
    AppStatusCard(
        title = "내보내기 완료",
        message = "JSON 파일이 준비되었습니다. 공유 시트나 파일 앱으로 저장할 수 있습니다.",
        accentColor = AppUiTokens.TextPrimary,
    )
    AppPrimaryButton(text = "파일 공유/저장", onClick = actions.onShareExport)
    AppSecondaryButton(text = "디바이스에 저장", onClick = actions.onSaveToDevice)
    AppTextAction(text = "다시 내보내기", onClick = actions.onStartExport)
}

@Composable
private fun exportErrorContent(
    progress: ExportProgress.Error,
    actions: ExportScreenActions,
) {
    exportInfoCard()
    AppStatusCard(
        title = "내보내기 실패",
        message = progress.message,
        accentColor = AppUiTokens.Error,
    )
    AppPrimaryButton(text = "다시 시도", onClick = actions.onStartExport)
    AppTextAction(text = "뒤로", onClick = actions.onNavigateUp)
}

@Composable
private fun exportInfoCard() {
    AppSectionCard {
        androidx.compose.material3.Text(
            text = "모든 러닝 기록을 파일로 생성합니다",
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            color = AppUiTokens.TextPrimary,
        )
        androidx.compose.material3.Text(
            text = "형식: bokslrunning_export_v1.json",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = AppUiTokens.TextSecondary,
        )
    }
}
