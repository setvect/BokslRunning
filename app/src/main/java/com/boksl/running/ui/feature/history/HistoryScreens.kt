@file:Suppress("LongMethod", "TooManyFunctions")

package com.boksl.running.ui.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.boksl.running.ui.common.AppDialog
import com.boksl.running.ui.common.AppPrimaryButton
import com.boksl.running.ui.common.AppScreenHeader
import com.boksl.running.ui.common.AppSectionCard
import com.boksl.running.ui.common.AppSecondaryButton
import com.boksl.running.ui.common.AppStatusCard
import com.boksl.running.ui.common.AppTextAction
import com.boksl.running.ui.common.AppUiTokens
import com.boksl.running.ui.common.appScreenModifier
import com.boksl.running.ui.feature.permission.locationPermissionDialog
import com.boksl.running.ui.feature.run.runMapSection
import com.boksl.running.ui.feature.run.toLatLng
import com.boksl.running.ui.formatCaloriesText
import com.boksl.running.ui.formatDistanceKm
import com.boksl.running.ui.formatGroupedIntegerText
import com.boksl.running.ui.formatHourMinuteSecondKoreanText
import com.boksl.running.ui.formatPaceText
import com.boksl.running.ui.formatSessionDateTimeText
import com.boksl.running.ui.formatSpeedKmh

@Composable
fun historyScreen(
    uiState: HistoryListUiState,
    pagedItems: LazyPagingItems<HistoryListItemUiState>,
    actions: HistoryScreenActions,
) {
    Scaffold(containerColor = AppUiTokens.Background) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .then(appScreenModifier()),
            verticalArrangement = Arrangement.spacedBy(AppUiTokens.ScreenSpacing),
        ) {
            AppScreenHeader(
                title = "기록",
                onNavigateUp = actions.onNavigateUp,
            )
            Text(
                text = "전체 ${uiState.totalCount.toDouble().formatGroupedIntegerText()}건",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = AppUiTokens.TextSecondary,
                textAlign = TextAlign.End,
            )
            historyListContent(
                pagedItems = pagedItems,
                onOpenSession = actions.onOpenSession,
                onStartRun = actions.onStartRun,
            )
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
fun historyDetailScreen(
    uiState: HistoryDetailUiState,
    onNavigateUp: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismissDeleteConfirmation: () -> Unit,
    onConfirmDelete: () -> Unit,
    onClearDeleteError: () -> Unit,
) {
    val canDelete = uiState.session != null && !uiState.isDeleting && !uiState.isNotFound

    Scaffold(containerColor = AppUiTokens.Background) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .then(appScreenModifier()),
            verticalArrangement = Arrangement.spacedBy(AppUiTokens.ScreenSpacing),
        ) {
            AppScreenHeader(
                title = "기록 상세",
                onNavigateUp = onNavigateUp,
                endContent = {
                    if (uiState.session != null) {
                        AppTextAction(
                            text = "삭제",
                            onClick = onDeleteClick,
                            enabled = canDelete,
                            color = AppUiTokens.Error,
                        )
                    }
                },
            )
            historyDetailContent(
                uiState = uiState,
                onNavigateUp = onNavigateUp,
                onClearDeleteError = onClearDeleteError,
            )
        }
    }

    if (uiState.showDeleteConfirmation) {
        AppDialog(
            title = "기록을 삭제할까요?",
            message = "삭제한 기록은 복구할 수 없어요.",
            onDismiss = onDismissDeleteConfirmation,
            confirmText = "삭제",
            onConfirm = onConfirmDelete,
            confirmColor = AppUiTokens.Error,
        )
    }
}

@Composable
private fun historyListContent(
    pagedItems: LazyPagingItems<HistoryListItemUiState>,
    onOpenSession: (Long) -> Unit,
    onStartRun: () -> Unit,
) {
    val refreshState = pagedItems.loadState.refresh
    val appendState = pagedItems.loadState.append

    when {
        refreshState is LoadState.Loading && pagedItems.itemCount == 0 -> historyLoadingState()
        refreshState is LoadState.Error && pagedItems.itemCount == 0 ->
            historyErrorState(
                message = "기록을 불러오지 못했습니다",
                actionLabel = "다시 시도",
                onAction = pagedItems::retry,
            )
        refreshState is LoadState.NotLoading && pagedItems.itemCount == 0 ->
            historyEmptyState(onStartRun = onStartRun)
        else ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    count = pagedItems.itemCount,
                    key = pagedItems.itemKey { item -> item.sessionId },
                ) { index ->
                    val item = pagedItems[index] ?: return@items
                    historyListItemCard(
                        item = item,
                        onClick = { onOpenSession(item.sessionId) },
                    )
                }
                when (appendState) {
                    is LoadState.Loading -> {
                        item(key = "append_loading") {
                            historyAppendLoadingItem()
                        }
                    }
                    is LoadState.Error -> {
                        item(key = "append_error") {
                            historyErrorState(
                                message = "추가 기록을 불러오지 못했습니다",
                                actionLabel = "다시 시도",
                                onAction = pagedItems::retry,
                            )
                        }
                    }
                    is LoadState.NotLoading -> Unit
                }
            }
    }
}

@Composable
private fun historyDetailContent(
    uiState: HistoryDetailUiState,
    onNavigateUp: () -> Unit,
    onClearDeleteError: () -> Unit,
) {
    when {
        uiState.isLoading -> historyLoadingState()
        uiState.isNotFound ->
            historyNotFoundState(
                onNavigateUp = onNavigateUp,
            )
        else ->
            historyDetailBody(
                uiState = uiState,
                onClearDeleteError = onClearDeleteError,
            )
    }
}

@Composable
private fun historyDetailBody(
    uiState: HistoryDetailUiState,
    onClearDeleteError: () -> Unit,
) {
    val session = checkNotNull(uiState.session)
    val routePoints = uiState.trackPoints.map { it.toLatLng() }
    var isMapTouchActive by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    state = rememberScrollState(),
                    enabled = !isMapTouchActive,
                ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        uiState.deleteErrorMessage?.let { message ->
            AppStatusCard(
                title = "삭제 실패",
                message = message,
                accentColor = AppUiTokens.Error,
            )
            AppTextAction(
                text = "닫기",
                onClick = onClearDeleteError,
            )
        }
        AppSectionCard(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = session.startedAtEpochMillis.formatSessionDateTimeText(),
                style = MaterialTheme.typography.titleLarge,
                color = AppUiTokens.TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            runMapSection(
                currentLocation = routePoints.lastOrNull(),
                routePoints = routePoints,
                modifier = Modifier.fillMaxWidth(),
                maxZoom = 17f,
                onTouchActiveChanged = { isMapTouchActive = it },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                historyDetailMetric(
                    label = "거리",
                    value = "${session.stats.distanceMeters.formatDistanceKm()} km",
                    modifier = Modifier.weight(1f),
                )
                historyDetailMetric(
                    label = "시간",
                    value = session.stats.durationMillis.formatHourMinuteSecondKoreanText(),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                historyDetailMetric(
                    label = "평균 페이스",
                    value = session.stats.averagePaceSecPerKm.formatPaceText(),
                    modifier = Modifier.weight(1f),
                )
                historyDetailMetric(
                    label = "최고 속도",
                    value = "${session.stats.maxSpeedMps.formatSpeedKmh()} km/h",
                    modifier = Modifier.weight(1f),
                )
            }
            historyDetailMetric(
                label = "칼로리",
                value = session.stats.calorieKcal.formatCaloriesText(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun historyLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = AppUiTokens.Accent)
    }
}

@Composable
private fun historyAppendLoadingItem() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = AppUiTokens.Accent)
    }
}

@Composable
private fun historyListItemCard(
    item: HistoryListItemUiState,
    onClick: () -> Unit,
) {
    AppSectionCard(
        modifier =
            Modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = item.startedAtEpochMillis.formatSessionDateTimeText(),
            style = MaterialTheme.typography.bodySmall,
            color = AppUiTokens.TextSecondary,
        )
        Text(
            text = "${item.distanceMeters.formatDistanceKm()} km",
            style = MaterialTheme.typography.headlineSmall,
            color = AppUiTokens.TextPrimary,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            historyInlineMetric(
                label = "시간",
                value = item.durationMillis.formatHourMinuteSecondKoreanText(),
                modifier = Modifier.weight(1f),
            )
            historyInlineMetric(
                label = "평균 페이스",
                value = item.averagePaceSecPerKm.formatPaceText(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun historyEmptyState(
    onStartRun: () -> Unit,
) {
    AppSectionCard {
        Text(
            text = "아직 저장된 기록이 없어요",
            style = MaterialTheme.typography.titleMedium,
            color = AppUiTokens.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "첫 달리기를 시작하면 여기에서 다시 볼 수 있어요.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppUiTokens.TextSecondary,
        )
        AppPrimaryButton(
            text = "달리기 시작",
            onClick = onStartRun,
        )
    }
}

@Composable
private fun historyNotFoundState(
    onNavigateUp: () -> Unit,
) {
    AppSectionCard {
        Text(
            text = "기록을 찾을 수 없습니다",
            style = MaterialTheme.typography.titleMedium,
            color = AppUiTokens.TextPrimary,
        )
        AppSecondaryButton(
            text = "이전으로",
            onClick = onNavigateUp,
        )
    }
}

@Composable
private fun historyErrorState(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    AppSectionCard {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = AppUiTokens.TextPrimary,
        )
        AppSecondaryButton(
            text = actionLabel,
            onClick = onAction,
        )
    }
}

@Composable
private fun historySummaryRow(
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

@Composable
private fun historyDetailMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
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

@Composable
private fun historyInlineMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AppUiTokens.TextSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = AppUiTokens.TextPrimary,
        )
    }
}
