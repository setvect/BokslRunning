package com.boksl.running.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boksl.running.R
import com.boksl.running.ui.feature.permission.locationPermissionDialog
import com.boksl.running.ui.formatDistanceKm
import com.boksl.running.ui.formatGroupedIntegerText
import com.boksl.running.ui.formatHourMinuteKoreanText
import com.boksl.running.ui.formatSpeedKmh

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun homeScreen(
    uiState: HomeUiState,
    actions: HomeScreenActions,
) {
    Scaffold(
        containerColor = HomePalette.Background,
    ) { innerPadding ->
        homeContent(
            uiState = uiState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 18.dp),
            actions = actions,
        )
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
private fun homeContent(
    uiState: HomeUiState,
    modifier: Modifier,
    actions: HomeScreenActions,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        HomeHeader(onOpenSettings = actions.onOpenSettings)
        HomeHeroCard(uiState = uiState)
        HomeExerciseCard(actions = actions)
        if (!uiState.hasProfile) {
            HomeProfileHint()
        }
    }
}

@Composable
private fun HomeHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "복슬달리기",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                color = HomePalette.TextPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        IconButton(
            onClick = onOpenSettings,
            modifier =
                Modifier
                    .padding(top = 2.dp, end = 4.dp)
                    .size(32.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_settings_gear),
                contentDescription = "설정",
                tint = HomePalette.TextPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun HomeHeroCard(uiState: HomeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = HomePalette.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${uiState.totalDistanceMeters.formatDistanceKm()} km",
                    style = MaterialTheme.typography.headlineLarge,
                    color = HomePalette.TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "누적 거리",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HomePalette.TextSecondary,
                )
            }
            MetricRow(
                primary = uiState.totalDurationMillis.formatHourMinuteKoreanText(),
                secondary = "달린 시간",
                accent = HomePalette.Accent,
            )
            MetricRow(
                primary = "${uiState.averageSpeedMps.formatSpeedKmh()} km/h",
                secondary = "평균 속도",
                accent = HomePalette.AccentSecondary,
            )
            MetricRow(
                primary =
                    if (uiState.hasProfile) {
                        "${uiState.totalCaloriesKcal.formatGroupedIntegerText()} kcal"
                    } else {
                        "프로필 필요"
                    },
                secondary = "소모 칼로리",
                accent = HomePalette.AccentMuted,
            )
        }
    }
}

@Composable
private fun MetricRow(
    primary: String,
    secondary: String,
    accent: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = primary,
                style = MaterialTheme.typography.titleLarge,
                color = HomePalette.TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(accent),
                )
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = HomePalette.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun HomeExerciseCard(actions: HomeScreenActions) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = HomePalette.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Button(
                onClick = actions.onStartRun,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                shape = RoundedCornerShape(20.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = HomePalette.Accent,
                        contentColor = HomePalette.Background,
                    ),
            ) {
                Text(
                    text = "달리기 시작",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SecondaryPillButton(
                    modifier = Modifier.weight(1f),
                    label = "기록",
                    onClick = actions.onOpenHistory,
                )
                SecondaryPillButton(
                    modifier = Modifier.weight(1f),
                    label = "통계",
                    onClick = actions.onOpenStats,
                )
            }
        }
    }
}

@Composable
private fun SecondaryPillButton(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier =
            modifier
                .height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = HomePalette.Pill,
                contentColor = HomePalette.TextPrimary,
            ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun HomeProfileHint() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = HomePalette.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(HomePalette.AccentSecondary),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "프로필을 입력하면 칼로리 계산 정확도가 올라갑니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HomePalette.TextPrimary,
                )
                Text(
                    text = "설정에서 몸무게, 성별, 나이를 추가할 수 있어요",
                    style = MaterialTheme.typography.bodySmall,
                    color = HomePalette.TextSecondary,
                )
            }
        }
    }
}

private object HomePalette {
    val Background = Color(0xFF000000)
    val Card = Color(0xFF1B1B1E)
    val Pill = Color(0xFF38383C)
    val TextPrimary = Color(0xFFF5F5F5)
    val TextSecondary = Color(0xFFA2A2A7)
    val Accent = Color(0xFF8ED548)
    val AccentSecondary = Color(0xFF66A1FF)
    val AccentMuted = Color(0xFFFF8A5B)
}
