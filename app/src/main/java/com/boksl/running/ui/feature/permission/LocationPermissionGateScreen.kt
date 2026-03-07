package com.boksl.running.ui.feature.permission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun locationPermissionGateScreen(
    uiState: LocationPermissionGateUiState,
    onAllowClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onLaterClick: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "경로 기록을 위해 위치 권한이 필요해요",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text =
                    when (uiState.permissionState) {
                        LocationPermissionUiState.ShowRequest ->
                            "권한을 허용하면 홈에서 바로 러닝 준비 화면으로 이어질 수 있습니다."
                        LocationPermissionUiState.Denied ->
                            "권한이 거부되었어요. 다시 허용하거나 나중에 진행할 수 있어요."
                        LocationPermissionUiState.PermanentlyDenied ->
                            "설정에서 위치 권한을 다시 켜야 경로 기록이 가능합니다."
                        LocationPermissionUiState.Granted ->
                            "권한이 허용되었습니다."
                    },
                modifier = Modifier.padding(top = 12.dp),
            )
            Button(
                onClick = onAllowClick,
                modifier = Modifier.padding(top = 24.dp),
            ) {
                Text(text = "허용")
            }
            if (uiState.permissionState == LocationPermissionUiState.PermanentlyDenied) {
                TextButton(
                    onClick = onOpenSettingsClick,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(text = "설정 열기")
                }
            }
            TextButton(
                onClick = onLaterClick,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(text = "나중에")
            }
        }
    }
}
