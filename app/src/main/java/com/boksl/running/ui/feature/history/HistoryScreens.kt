@file:Suppress("LongMethod", "TooManyFunctions")

package com.boksl.running.ui.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.boksl.running.ui.feature.permission.locationPermissionDialog
import com.boksl.running.ui.feature.run.runMapSection
import com.boksl.running.ui.feature.run.toLatLng
import com.boksl.running.ui.formatCaloriesText
import com.boksl.running.ui.formatDistanceKm
import com.boksl.running.ui.formatDurationText
import com.boksl.running.ui.formatPaceText
import com.boksl.running.ui.formatSessionDateTimeText
import com.boksl.running.ui.formatSpeedKmh

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun historyScreen(
    uiState: HistoryListUiState,
    pagedItems: LazyPagingItems<HistoryListItemUiState>,
    actions: HistoryScreenActions,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "기록") },
                navigationIcon = {
                    TextButton(onClick = actions.onNavigateUp) {
                        Text(text = "뒤로")
                    }
                },
            )
        },
    ) { innerPadding ->
        historyListContent(
            pagedItems = pagedItems,
            innerPadding = innerPadding,
            onOpenSession = actions.onOpenSession,
            onStartRun = actions.onStartRun,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun historyDetailScreen(
    uiState: HistoryDetailUiState,
    onNavigateUp: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "기록 상세") },
                navigationIcon = {
                    TextButton(onClick = onNavigateUp) {
                        Text(text = "뒤로")
                    }
                },
            )
        },
    ) { innerPadding ->
        historyDetailContent(
            uiState = uiState,
            innerPadding = innerPadding,
            onNavigateUp = onNavigateUp,
        )
    }
}

@Composable
private fun historyListContent(
    pagedItems: LazyPagingItems<HistoryListItemUiState>,
    innerPadding: PaddingValues,
    onOpenSession: (Long) -> Unit,
    onStartRun: () -> Unit,
) {
    val refreshState = pagedItems.loadState.refresh
    val appendState = pagedItems.loadState.append

    when {
        refreshState is LoadState.Loading && pagedItems.itemCount == 0 ->
            historyLoadingState(
                innerPadding = innerPadding,
            )
        refreshState is LoadState.Error && pagedItems.itemCount == 0 ->
            historyErrorState(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                message = "기록을 불러오지 못했습니다",
                actionLabel = "다시 시도",
                onAction = pagedItems::retry,
            )
        refreshState is LoadState.NotLoading && pagedItems.itemCount == 0 ->
            historyEmptyState(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                onStartRun = onStartRun,
            )
        else ->
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
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
                                modifier = Modifier.fillMaxWidth(),
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
    innerPadding: PaddingValues,
    onNavigateUp: () -> Unit,
) {
    when {
        uiState.isLoading -> historyLoadingState(innerPadding = innerPadding)
        uiState.isNotFound ->
            historyNotFoundState(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                onNavigateUp = onNavigateUp,
            )
        else -> historyDetailBody(uiState = uiState, innerPadding = innerPadding)
    }
}

@Composable
private fun historyDetailBody(
    uiState: HistoryDetailUiState,
    innerPadding: PaddingValues,
) {
    val session = checkNotNull(uiState.session)
    val routePoints = uiState.trackPoints.map { it.toLatLng() }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = session.startedAtEpochMillis.formatSessionDateTimeText(),
            style = MaterialTheme.typography.titleLarge,
        )
        runMapSection(
            currentLocation = routePoints.lastOrNull(),
            routePoints = routePoints,
            modifier = Modifier.fillMaxWidth(),
            maxZoom = 17f,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "요약", style = MaterialTheme.typography.titleMedium)
                historySummaryRow(
                    label = "거리",
                    value = "${session.stats.distanceMeters.formatDistanceKm()} km",
                )
                historySummaryRow(
                    label = "시간",
                    value = session.stats.durationMillis.formatDurationText(),
                )
                historySummaryRow(
                    label = "평균 페이스",
                    value = session.stats.averagePaceSecPerKm.formatPaceText(),
                )
                historySummaryRow(
                    label = "최고 속도",
                    value = "${session.stats.maxSpeedMps.formatSpeedKmh()} km/h",
                )
                historySummaryRow(
                    label = "칼로리",
                    value = session.stats.calorieKcal.formatCaloriesText(),
                )
            }
        }
    }
}

@Composable
private fun historyLoadingState(innerPadding: PaddingValues) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
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
        CircularProgressIndicator()
    }
}

@Composable
private fun historyListItemCard(
    item: HistoryListItemUiState,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = item.startedAtEpochMillis.formatSessionDateTimeText(),
                style = MaterialTheme.typography.titleMedium,
            )
            historySummaryRow(label = "거리", value = "${item.distanceMeters.formatDistanceKm()} km")
            historySummaryRow(label = "시간", value = item.durationMillis.formatDurationText())
            historySummaryRow(label = "평균 페이스", value = item.averagePaceSecPerKm.formatPaceText())
        }
    }
}

@Composable
private fun historyEmptyState(
    modifier: Modifier = Modifier,
    onStartRun: () -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "아직 저장된 기록이 없어요",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "첫 러닝을 시작하면 여기에서 다시 볼 수 있어요.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onStartRun) {
                Text(text = "러닝 시작")
            }
        }
    }
}

@Composable
private fun historyNotFoundState(
    modifier: Modifier = Modifier,
    onNavigateUp: () -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "기록을 찾을 수 없습니다",
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(onClick = onNavigateUp) {
                Text(text = "뒤로")
            }
        }
    }
}

@Composable
private fun historyErrorState(
    modifier: Modifier = Modifier,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(onClick = onAction) {
                Text(text = actionLabel)
            }
        }
    }
}

@Composable
private fun historySummaryRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.titleSmall)
    }
}
