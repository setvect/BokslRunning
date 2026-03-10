package com.boksl.running.ui.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.boksl.running.R
import com.boksl.running.ui.common.AppHintRow
import com.boksl.running.ui.common.AppIconActionButton
import com.boksl.running.ui.common.AppMetricLine
import com.boksl.running.ui.common.AppPrimaryButton
import com.boksl.running.ui.common.AppScreenHeader
import com.boksl.running.ui.common.AppSectionCard
import com.boksl.running.ui.common.AppSecondaryButton
import com.boksl.running.ui.common.AppUiTokens
import com.boksl.running.ui.common.appScreenModifier
import com.boksl.running.ui.feature.permission.locationPermissionDialog
import com.boksl.running.ui.formatDistanceKm
import com.boksl.running.ui.formatGroupedIntegerText
import com.boksl.running.ui.formatHourMinuteKoreanText
import com.boksl.running.ui.formatSpeedKmh

@Composable
fun homeScreen(
    uiState: HomeUiState,
    actions: HomeScreenActions,
) {
    Scaffold(
        containerColor = AppUiTokens.Background,
    ) { innerPadding ->
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
                title = "복슬달리기",
                endContent = {
                    AppIconActionButton(
                        iconRes = R.drawable.ic_settings_gear,
                        contentDescription = "설정",
                        onClick = actions.onOpenSettings,
                    )
                },
            )
            AppSectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${uiState.totalDistanceMeters.formatDistanceKm()} km",
                        style = MaterialTheme.typography.headlineLarge,
                        color = AppUiTokens.TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "누적 거리",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppUiTokens.TextSecondary,
                    )
                }
                AppMetricLine(
                    value = uiState.totalDurationMillis.formatHourMinuteKoreanText(),
                    label = "달린 시간",
                    accent = AppUiTokens.Accent,
                )
                AppMetricLine(
                    value = "${uiState.averageSpeedMps.formatSpeedKmh()} km/h",
                    label = "평균 속도",
                    accent = AppUiTokens.AccentSecondary,
                )
                AppMetricLine(
                    value =
                        if (uiState.hasProfile) {
                            "${uiState.totalCaloriesKcal.formatGroupedIntegerText()} kcal"
                        } else {
                            "프로필 필요"
                        },
                    label = "소모 칼로리",
                    accent = AppUiTokens.AccentMuted,
                )
            }
            AppSectionCard(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                AppPrimaryButton(
                    text = "달리기 시작",
                    onClick = actions.onStartRun,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppSecondaryButton(
                        text = "기록",
                        onClick = actions.onOpenHistory,
                        modifier = Modifier.weight(1f),
                    )
                    AppSecondaryButton(
                        text = "통계",
                        onClick = actions.onOpenStats,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (!uiState.hasProfile) {
                AppSectionCard {
                    AppHintRow(
                        title = "프로필을 입력하면 칼로리 계산 정확도가 올라갑니다",
                        message = "설정에서 몸무게, 성별, 나이를 추가할 수 있어요",
                    )
                }
            }
        }
    }

    uiState.permissionDialogState?.let { dialogState ->
        locationPermissionDialog(
            uiState = dialogState,
            onDismiss = actions.onDismissPermissionDialog,
            onOpenAppSettings = actions.onOpenAppSettings,
        )
    }
}
