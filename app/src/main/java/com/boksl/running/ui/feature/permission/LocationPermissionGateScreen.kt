package com.boksl.running.ui.feature.permission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.boksl.running.ui.common.AppPrimaryButton
import com.boksl.running.ui.common.AppScreenHeader
import com.boksl.running.ui.common.AppSectionCard
import com.boksl.running.ui.common.AppTextAction
import com.boksl.running.ui.common.AppUiTokens
import com.boksl.running.ui.common.appScreenModifier

@Composable
fun locationPermissionGateScreen(
    uiState: LocationPermissionGateUiState,
    onAllowClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onLaterClick: () -> Unit,
) {
    Scaffold(containerColor = AppUiTokens.Background) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .then(appScreenModifier()),
            verticalArrangement = Arrangement.spacedBy(AppUiTokens.ScreenSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppScreenHeader(title = "위치 권한")
            AppSectionCard {
                Text(
                    text = "경로 기록을 위해 위치 권한이 필요해요",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AppUiTokens.TextPrimary,
                )
                Text(
                    text =
                        when (uiState.permissionState) {
                            LocationPermissionUiState.ShowRequest ->
                                "권한을 허용하면 홈에서 바로 달리기 준비 화면으로 이어질 수 있어요."
                            LocationPermissionUiState.Denied ->
                                "권한이 거부되었어요. 다시 허용하거나 나중에 진행할 수 있어요."
                            LocationPermissionUiState.PermanentlyDenied ->
                                "설정에서 위치 권한을 다시 켜야 경로 기록이 가능합니다."
                            LocationPermissionUiState.Granted ->
                                "권한이 허용되었습니다."
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppUiTokens.TextSecondary,
                )
                AppPrimaryButton(text = "허용", onClick = onAllowClick)
                if (uiState.permissionState == LocationPermissionUiState.PermanentlyDenied) {
                    AppTextAction(
                        text = "설정 열기",
                        onClick = onOpenSettingsClick,
                    )
                }
                AppTextAction(
                    text = "나중에",
                    onClick = onLaterClick,
                )
            }
        }
    }
}
