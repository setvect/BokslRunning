@file:Suppress("TooManyFunctions")

package com.boksl.running.ui.feature.run

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boksl.running.domain.model.RunSnapshot
import com.boksl.running.domain.model.RunningSession
import com.boksl.running.ui.common.AppDialog
import com.boksl.running.ui.common.AppPrimaryButton
import com.boksl.running.ui.common.AppScreenHeader
import com.boksl.running.ui.common.AppSectionCard
import com.boksl.running.ui.common.AppSecondaryButton
import com.boksl.running.ui.common.AppStatusCard
import com.boksl.running.ui.common.AppTextAction
import com.boksl.running.ui.common.AppUiTokens
import com.boksl.running.ui.common.appScreenModifier
import com.boksl.running.ui.formatCaloriesText
import com.boksl.running.ui.formatDistanceKm
import com.boksl.running.ui.formatDurationText
import com.boksl.running.ui.formatHourMinuteKoreanText
import com.boksl.running.ui.formatPaceText
import com.boksl.running.ui.formatSpeedKmh
import com.google.android.gms.maps.model.LatLng
import java.util.Locale

@Composable
fun runReadyScreen(
    uiState: RunSessionUiState,
    onStartRun: () -> Unit,
    onCancel: () -> Unit,
) {
    val currentLocation = uiState.snapshot?.latestLocation?.let { LatLng(it.latitude, it.longitude) }
    val readyMessage =
        when {
            uiState.isStarting -> "기록을 시작하는 중입니다"
            uiState.isCountingDown ->
                "${uiState.countdownRemainingSeconds ?: 0}초 후 기록을 시작합니다"
            uiState.canStart -> "버튼을 누르면 3초 카운트다운 후 기록을 시작합니다"
            else -> "위치를 준비하는 중입니다"
        }
    val startButtonText =
        when {
            uiState.isStarting -> "시작 중..."
            uiState.isCountingDown -> "${uiState.countdownRemainingSeconds ?: 0}"
            else -> "달리기 시작"
        }

    runScreenLayout(title = "달리기 준비") {
        if (uiState.isOffline) {
            runOfflineBanner()
        }
        runMapSection(
            currentLocation = currentLocation,
            routePoints = emptyList(),
            modifier = Modifier.fillMaxWidth(),
            maxZoom = 18f,
        )
        AppSectionCard {
            Text(
                text = "현재 상태",
                style = MaterialTheme.typography.titleMedium,
                color = AppUiTokens.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            runSummaryLine(
                label = "현재 위치",
                value =
                    currentLocation?.let {
                        "${"%.5f".format(Locale.US, it.latitude)}, ${"%.5f".format(Locale.US, it.longitude)}"
                    } ?: "위치 확인 중",
            )
            runSummaryLine(
                label = "안내",
                value = readyMessage,
            )
        }
        AppPrimaryButton(
            text = startButtonText,
            onClick = onStartRun,
            enabled = uiState.canStart,
        )
        AppTextAction(
            text = "취소",
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun runLiveScreen(
    uiState: RunSessionUiState,
    onRequestStop: () -> Unit,
    onConfirmSave: () -> Unit,
    onCancelStop: () -> Unit,
) {
    val routePoints = uiState.trackPoints.map { it.toLatLng() }
    val currentLocation = uiState.snapshot?.latestLocation?.let { LatLng(it.latitude, it.longitude) }

    runScreenLayout(title = "달리는 중") {
        if (uiState.isOffline) {
            runOfflineBanner()
        }
        runMapSection(
            currentLocation = currentLocation,
            routePoints = routePoints,
            modifier = Modifier.fillMaxWidth(),
            maxZoom = 16f,
        )
        metricsGrid(snapshot = uiState.snapshot, savedSession = uiState.savedSession)
        AppPrimaryButton(
            text = "기록 종료",
            onClick = onRequestStop,
            containerColor = AppUiTokens.AccentMuted,
            contentColor = AppUiTokens.Background,
        )
    }

    if (uiState.showStopConfirm) {
        AppDialog(
            title = "달리기를 종료하고 저장할까요?",
            message = "저장하면 기록 목록과 통계에 바로 반영됩니다.",
            onDismiss = onCancelStop,
            confirmText = "저장",
            onConfirm = onConfirmSave,
            confirmColor = AppUiTokens.Accent,
        )
    }
}

@Composable
fun runSummaryScreen(
    uiState: RunSessionUiState,
    onComplete: () -> Unit,
) {
    val routePoints = uiState.trackPoints.map { it.toLatLng() }

    runScreenLayout(title = "결과 요약") {
        if (uiState.isOffline) {
            runOfflineBanner()
        }
        runMapSection(
            currentLocation = routePoints.lastOrNull(),
            routePoints = routePoints,
            modifier = Modifier.fillMaxWidth(),
            maxZoom = 17f,
        )
        summaryCard(summarySession = uiState.savedSession, snapshot = uiState.snapshot)
        AppPrimaryButton(text = "완료", onClick = onComplete)
    }
}

@Composable
fun runRecoveryScreen(
    uiState: RunRecoveryUiState,
    onContinue: () -> Unit,
    onDiscard: () -> Unit,
) {
    val session = uiState.activeSession

    runScreenLayout(title = "러닝 복구") {
        if (uiState.isOffline) {
            runOfflineBanner()
        }
        AppStatusCard(
            title = "진행 중이던 기록을 찾았습니다",
            message = "마지막 저장 지점부터 이어서 기록할 수 있어요. 앱이 꺼져 있던 구간은 포함되지 않을 수 있습니다.",
            accentColor = AppUiTokens.TextPrimary,
        )
        if (session != null) {
            AppSectionCard {
                Text(
                    text = "복구 대상",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppUiTokens.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                runSummaryLine(
                    label = "누적 거리",
                    value = "${session.stats.distanceMeters.formatDistanceKm()} km",
                )
                runSummaryLine(
                    label = "누적 시간",
                    value = session.stats.durationMillis.formatHourMinuteKoreanText(),
                )
                runSummaryLine(
                    label = "평균 페이스",
                    value = session.stats.averagePaceSecPerKm.formatPaceText(),
                )
            }
        }
        AppPrimaryButton(
            text = "이어하기",
            onClick = onContinue,
            enabled = session != null,
        )
        AppSecondaryButton(
            text = "폐기",
            onClick = onDiscard,
            enabled = session != null,
        )
    }
}

@Composable
private fun runScreenLayout(
    title: String,
    body: @Composable ColumnScope.() -> Unit,
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
            AppScreenHeader(title = title)
            body()
        }
    }
}

@Composable
fun runOfflineBanner() {
    AppStatusCard(
        title = "오프라인 상태",
        message = "기록은 저장되고, 지도 배경은 제한될 수 있어요.",
        accentColor = AppUiTokens.AccentSecondary,
    )
}

@Composable
private fun metricsGrid(
    snapshot: RunSnapshot?,
    savedSession: RunningSession?,
) {
    AppSectionCard {
        Text(
            text = "실시간 지표",
            style = MaterialTheme.typography.titleMedium,
            color = AppUiTokens.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            metricTile(
                label = "시간",
                value = snapshot?.durationMillis?.formatDurationText() ?: "--:--",
                modifier = Modifier.weight(1f),
                accent = AppUiTokens.Accent,
            )
            metricTile(
                label = "거리",
                value = "${snapshot?.totalDistanceMeters?.formatDistanceKm() ?: "0.00"} km",
                modifier = Modifier.weight(1f),
                accent = AppUiTokens.AccentSecondary,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            metricTile(
                label = "현재 페이스",
                value = snapshot?.currentPaceSecPerKm.formatPaceText(),
                modifier = Modifier.weight(1f),
                accent = AppUiTokens.AccentMuted,
            )
            metricTile(
                label = "평균 페이스",
                value = snapshot?.averagePaceSecPerKm.formatPaceText(),
                modifier = Modifier.weight(1f),
                accent = AppUiTokens.Accent,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            metricTile(
                label = "최고 속도",
                value = "${snapshot?.maxSpeedMps?.formatSpeedKmh() ?: "0.00"} km/h",
                modifier = Modifier.weight(1f),
                accent = AppUiTokens.AccentSecondary,
            )
            metricTile(
                label = "칼로리",
                value = (snapshot?.calorieKcal ?: savedSession?.stats?.calorieKcal).formatCaloriesText(),
                modifier = Modifier.weight(1f),
                accent = AppUiTokens.AccentMuted,
            )
        }
    }
}

@Composable
private fun summaryCard(
    summarySession: RunningSession?,
    snapshot: RunSnapshot?,
) {
    val stats = summarySession?.stats
    AppSectionCard {
        Text(
            text = "저장된 결과",
            style = MaterialTheme.typography.titleMedium,
            color = AppUiTokens.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        runSummaryLine(
            label = "총 거리",
            value = "${(stats?.distanceMeters ?: snapshot?.totalDistanceMeters ?: 0.0).formatDistanceKm()} km",
        )
        runSummaryLine(
            label = "총 시간",
            value = (stats?.durationMillis ?: snapshot?.durationMillis ?: 0L).formatHourMinuteKoreanText(),
        )
        runSummaryLine(
            label = "평균 페이스",
            value = (stats?.averagePaceSecPerKm ?: snapshot?.averagePaceSecPerKm).formatPaceText(),
        )
        runSummaryLine(
            label = "최고 속도",
            value = "${(stats?.maxSpeedMps ?: snapshot?.maxSpeedMps ?: 0.0).formatSpeedKmh()} km/h",
        )
        runSummaryLine(
            label = "칼로리",
            value = (stats?.calorieKcal ?: snapshot?.calorieKcal).formatCaloriesText(),
        )
    }
}

@Composable
private fun metricTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(AppUiTokens.SecondaryButtonRadius),
        colors = CardDefaults.cardColors(containerColor = AppUiTokens.SurfaceMuted),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = accent,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = AppUiTokens.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun runSummaryLine(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AppUiTokens.TextSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = AppUiTokens.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
