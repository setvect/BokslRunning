package com.boksl.running.ui.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boksl.running.R
import com.boksl.running.domain.model.HomeStatsPeriod
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
                    homeStatsPeriodTabs(
                        selectedPeriod = uiState.selectedStatsPeriod,
                        onSelectPeriod = actions.onSelectStatsPeriod,
                    )
                    Text(
                        text = "${uiState.totalDistanceMeters.formatDistanceKm()} km",
                        style = MaterialTheme.typography.headlineLarge,
                        color = AppUiTokens.TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = uiState.selectedStatsPeriod.distanceLabel(),
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
                    text = "달리기 준비",
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

@Composable
private fun homeStatsPeriodTabs(
    selectedPeriod: HomeStatsPeriod,
    onSelectPeriod: (HomeStatsPeriod) -> Unit,
) {
    val periods = remember { HomeStatsPeriod.entries }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        periods.forEach { period ->
            val isSelected = period == selectedPeriod
            AppSecondaryButton(
                text = period.tabLabel(),
                onClick = { onSelectPeriod(period) },
                modifier = Modifier.weight(1f),
                containerColor = if (isSelected) AppUiTokens.Accent else AppUiTokens.Pill,
                contentColor = if (isSelected) AppUiTokens.Background else AppUiTokens.TextPrimary,
            )
        }
    }
}

private fun HomeStatsPeriod.tabLabel(): String =
    when (this) {
        HomeStatsPeriod.THIS_MONTH -> "이번달"
        HomeStatsPeriod.THIS_YEAR -> "올해"
        HomeStatsPeriod.ALL_TIME -> "전체"
    }

private fun HomeStatsPeriod.distanceLabel(): String =
    when (this) {
        HomeStatsPeriod.THIS_MONTH -> "이번달 거리"
        HomeStatsPeriod.THIS_YEAR -> "올해 거리"
        HomeStatsPeriod.ALL_TIME -> "누적 거리"
    }
