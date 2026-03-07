package com.boksl.running.ui.feature.permission

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

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
