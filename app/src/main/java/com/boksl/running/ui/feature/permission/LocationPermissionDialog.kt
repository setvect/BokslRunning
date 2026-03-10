package com.boksl.running.ui.feature.permission

import androidx.compose.runtime.Composable
import com.boksl.running.ui.common.AppDialog
import com.boksl.running.ui.common.AppUiTokens

@Composable
fun locationPermissionDialog(
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

    AppDialog(
        title = "권한 안내",
        message = message,
        onDismiss = onDismiss,
        confirmText = "설정 열기",
        onConfirm = onOpenAppSettings,
        confirmColor = AppUiTokens.Accent,
    )
}
