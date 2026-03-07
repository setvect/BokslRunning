package com.boksl.running.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.paging.compose.collectAsLazyPagingItems
import com.boksl.running.ui.feature.history.HistoryDetailViewModel
import com.boksl.running.ui.feature.history.HistoryListEvent
import com.boksl.running.ui.feature.history.HistoryListViewModel
import com.boksl.running.ui.feature.history.HistoryScreenActions
import com.boksl.running.ui.feature.history.historyDetailScreen
import com.boksl.running.ui.feature.history.historyScreen
import com.boksl.running.ui.feature.onboarding.ProfileSetupEntryPoint
import com.boksl.running.ui.feature.permission.findActivity
import com.boksl.running.ui.feature.permission.hasLocationPermission
import com.boksl.running.ui.feature.permission.openAppSettings
import com.boksl.running.ui.feature.permission.shouldShowLocationPermissionRationale
import com.boksl.running.ui.feature.settings.ExportEvent
import com.boksl.running.ui.feature.settings.ExportViewModel
import com.boksl.running.ui.feature.settings.ImportEvent
import com.boksl.running.ui.feature.settings.ImportViewModel
import com.boksl.running.ui.feature.settings.SettingsViewModel
import com.boksl.running.ui.feature.settings.buildExportShareIntent
import com.boksl.running.ui.feature.settings.exportScreen
import com.boksl.running.ui.feature.settings.importScreen
import com.boksl.running.ui.feature.settings.saveExportFileToUri
import com.boksl.running.ui.feature.settings.settingsScreen
import com.boksl.running.ui.feature.stats.StatsViewModel
import com.boksl.running.ui.feature.stats.statsScreen

internal fun NavGraphBuilder.addHistoryRoutes(navController: NavHostController) {
    addHistoryListRoute(navController = navController)
    addHistoryDetailRoute(navController = navController)
}

private fun NavGraphBuilder.addHistoryListRoute(navController: NavHostController) {
    composable(route = AppRoute.History.route) {
        val context = LocalContext.current
        val activity = context.findActivity()
        val viewModel: HistoryListViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()
        val pagedItems = viewModel.pagedItems.collectAsLazyPagingItems()
        observeOnResume {
            viewModel.onPermissionSettingsResult(hasPermission = hasLocationPermission(context))
        }

        LaunchedEffect(Unit) {
            viewModel.event.collect { event ->
                when (event) {
                    HistoryListEvent.NavigateToRunReady -> navController.navigate(AppRoute.RunReady.route)
                }
            }
        }

        historyScreen(
            uiState = uiState,
            pagedItems = pagedItems,
            actions =
                HistoryScreenActions(
                    onNavigateUp = { navController.navigateUp() },
                    onOpenSession = { sessionId ->
                        navController.navigate(AppRoute.HistoryDetail.createRoute(sessionId))
                    },
                    onStartRun = {
                        if (hasLocationPermission(context)) {
                            navController.navigate(AppRoute.RunReady.route)
                        } else {
                            viewModel.onRunStartRequested(
                                shouldShowRationale =
                                    activity?.let(::shouldShowLocationPermissionRationale) ?: false,
                            )
                        }
                    },
                    onDismissPermissionDialog = viewModel::dismissPermissionDialog,
                    onOpenAppSettings = {
                        viewModel.onOpenAppSettingsRequested()
                        openAppSettings(context)
                    },
                ),
        )
    }
}

private fun NavGraphBuilder.addHistoryDetailRoute(navController: NavHostController) {
    composable(
        route = AppRoute.HistoryDetail.route,
        arguments =
            listOf(
                navArgument(AppRoute.HistoryDetail.SESSION_ID_ARG) {
                    type = NavType.LongType
                },
            ),
    ) {
        val viewModel: HistoryDetailViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()

        historyDetailScreen(
            uiState = uiState,
            onNavigateUp = { navController.navigateUp() },
        )
    }
}

internal fun NavGraphBuilder.addStatsRoute(navController: NavHostController) {
    composable(route = AppRoute.Stats.route) {
        val viewModel: StatsViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()

        statsScreen(
            uiState = uiState,
            onMetricSelected = viewModel::onMetricSelected,
            onMonthSelected = viewModel::onMonthSelected,
            onNavigateUp = { navController.navigateUp() },
        )
    }
}

internal fun NavGraphBuilder.addSettingsRoute(navController: NavHostController) {
    composable(route = AppRoute.Settings.route) {
        val viewModel: SettingsViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()

        settingsScreen(
            uiState = uiState,
            onNavigateUp = { navController.navigateUp() },
            onEditProfile = {
                navController.navigate(
                    AppRoute.ProfileSetup.createRoute(ProfileSetupEntryPoint.Settings.routeValue),
                )
            },
            onOpenExport = { navController.navigate(AppRoute.Export.route) },
            onOpenImport = { navController.navigate(AppRoute.Import.route) },
            onNavigateHome = { navController.navigateHomeSingleTop() },
            onGenerateSeedDataClick = viewModel::onGenerateSeedDataClick,
            onDeleteSeedDataClick = viewModel::onDeleteSeedDataClick,
            onConfirmPendingAction = viewModel::confirmPendingAction,
            onDismissPendingAction = viewModel::dismissPendingAction,
            onClearStatusMessage = viewModel::clearStatusMessage,
        )
    }
}

internal fun NavGraphBuilder.addExportRoute(navController: NavHostController) {
    composable(route = AppRoute.Export.route) {
        val context = LocalContext.current
        val viewModel: ExportViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()
        val pendingSaveFilePath = remember { mutableStateOf<String?>(null) }
        val saveDocumentLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
                val filePath = pendingSaveFilePath.value
                if (uri != null && filePath != null) {
                    runCatching {
                        saveExportFileToUri(
                            context = context,
                            sourceFilePath = filePath,
                            targetUri = uri,
                        )
                    }.onFailure { throwable ->
                        viewModel.showShareError(throwable.message ?: "디바이스 저장에 실패했습니다.")
                    }
                }
                pendingSaveFilePath.value = null
            }

        LaunchedEffect(Unit) {
            viewModel.event.collect { event ->
                when (event) {
                    is ExportEvent.ShareFile ->
                        runCatching {
                            context.startActivity(buildExportShareIntent(context, event.filePath))
                        }.onFailure { throwable ->
                            viewModel.showShareError(throwable.message ?: "파일 공유를 시작하지 못했습니다.")
                        }
                    is ExportEvent.SaveFileToDevice -> {
                        pendingSaveFilePath.value = event.filePath
                        saveDocumentLauncher.launch(event.fileName)
                    }
                }
            }
        }

        exportScreen(
            uiState = uiState,
            onNavigateUp = { navController.navigateUp() },
            onStartExport = viewModel::startExport,
            onCancelExport = viewModel::cancelExport,
            onShareExport = viewModel::shareExportFile,
            onSaveToDevice = viewModel::saveExportFileToDevice,
            onNavigateHome = { navController.navigateHomeSingleTop() },
        )
    }
}

internal fun NavGraphBuilder.addImportRoute(navController: NavHostController) {
    composable(route = AppRoute.Import.route) {
        val viewModel: ImportViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()
        val documentLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                viewModel.onImportFileSelected(uri)
            }

        LaunchedEffect(Unit) {
            viewModel.event.collect { event ->
                when (event) {
                    ImportEvent.OpenDocumentPicker -> documentLauncher.launch(arrayOf("application/json"))
                }
            }
        }

        importScreen(
            uiState = uiState,
            onNavigateUp = { navController.navigateUp() },
            onStartImport = viewModel::startImport,
            onCancelImport = viewModel::cancelImport,
            onConfirm = { navController.navigateUp() },
            onNavigateHome = { navController.navigateHomeSingleTop() },
        )
    }
}

private fun NavHostController.navigateHomeSingleTop() {
    navigate(AppRoute.Home.route) {
        popUpTo(AppRoute.Home.route) { inclusive = false }
        launchSingleTop = true
    }
}
