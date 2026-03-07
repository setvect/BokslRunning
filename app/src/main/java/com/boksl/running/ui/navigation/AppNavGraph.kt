package com.boksl.running.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.paging.compose.collectAsLazyPagingItems
import com.boksl.running.ui.feature.history.HistoryDetailViewModel
import com.boksl.running.ui.feature.history.HistoryListViewModel
import com.boksl.running.ui.feature.history.HistoryScreenActions
import com.boksl.running.ui.feature.history.historyDetailScreen
import com.boksl.running.ui.feature.history.historyScreen
import com.boksl.running.ui.feature.home.HomeViewModel
import com.boksl.running.ui.feature.home.homeScreen
import com.boksl.running.ui.feature.onboarding.ProfileFormNavigationEvent
import com.boksl.running.ui.feature.onboarding.ProfileFormViewModel
import com.boksl.running.ui.feature.onboarding.ProfileSetupEntryPoint
import com.boksl.running.ui.feature.onboarding.onboardingScreen
import com.boksl.running.ui.feature.onboarding.profileFormScreen
import com.boksl.running.ui.feature.permission.LocationPermissionGateViewModel
import com.boksl.running.ui.feature.permission.findActivity
import com.boksl.running.ui.feature.permission.hasLocationPermission
import com.boksl.running.ui.feature.permission.locationPermissionGateScreen
import com.boksl.running.ui.feature.permission.locationPermissions
import com.boksl.running.ui.feature.permission.openAppSettings
import com.boksl.running.ui.feature.permission.shouldShowLocationPermissionRationale
import com.boksl.running.ui.feature.run.RunSessionViewModel
import com.boksl.running.ui.feature.run.runLiveScreen
import com.boksl.running.ui.feature.run.runReadyScreen
import com.boksl.running.ui.feature.run.runSummaryScreen
import com.boksl.running.ui.feature.settings.ExportEvent
import com.boksl.running.ui.feature.settings.ExportViewModel
import com.boksl.running.ui.feature.settings.buildExportShareIntent
import com.boksl.running.ui.feature.settings.exportScreen
import com.boksl.running.ui.feature.settings.saveExportFileToUri
import com.boksl.running.ui.feature.settings.ImportEvent
import com.boksl.running.ui.feature.settings.ImportViewModel
import com.boksl.running.ui.feature.settings.importScreen
import com.boksl.running.ui.feature.settings.SettingsViewModel
import com.boksl.running.ui.feature.settings.settingsScreen
import com.boksl.running.ui.feature.stats.statsScreen
import com.boksl.running.ui.feature.stats.StatsViewModel

@Composable
fun appNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AppRoute.Launch.route,
        modifier = modifier,
    ) {
        composable(route = AppRoute.Launch.route) {
            val viewModel: AppLaunchViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(uiState) {
                val destination =
                    when (uiState) {
                        AppLaunchUiState.Loading -> null
                        AppLaunchUiState.NeedsOnboarding -> AppRoute.Onboarding.route
                        AppLaunchUiState.NeedsProfile ->
                            AppRoute.ProfileSetup.createRoute(ProfileSetupEntryPoint.Onboarding.routeValue)
                        AppLaunchUiState.NeedsLocationPermission -> AppRoute.LocationPermissionGate.route
                        AppLaunchUiState.HasActiveRun -> AppRoute.RunLive.route
                        AppLaunchUiState.Ready -> AppRoute.Home.route
                    }

                if (destination != null) {
                    navController.navigate(destination) {
                        popUpTo(AppRoute.Launch.route) { inclusive = true }
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        composable(route = AppRoute.Onboarding.route) {
            onboardingScreen(
                onStartClick = {
                    navController.navigate(
                        AppRoute.ProfileSetup.createRoute(ProfileSetupEntryPoint.Onboarding.routeValue),
                    )
                },
            )
        }
        composable(
            route = AppRoute.ProfileSetup.route,
            arguments =
                listOf(
                    navArgument(AppRoute.ProfileSetup.ENTRY_POINT_ARG) {
                        type = NavType.StringType
                    },
                ),
        ) {
            val viewModel: ProfileFormViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                viewModel.navigationEvent.collect { event ->
                    when (event) {
                        ProfileFormNavigationEvent.NavigateToPermissionGate ->
                            navController.navigate(AppRoute.LocationPermissionGate.route) {
                                popUpTo(AppRoute.Onboarding.route) { inclusive = true }
                            }
                        ProfileFormNavigationEvent.NavigateBackToSettings -> navController.navigateUp()
                    }
                }
            }

            profileFormScreen(
                uiState = uiState,
                onWeightChanged = viewModel::onWeightChanged,
                onGenderChanged = viewModel::onGenderChanged,
                onAgeChanged = viewModel::onAgeChanged,
                onSaveClick = viewModel::saveProfile,
                onNavigateUp =
                    if (uiState.entryPoint == ProfileSetupEntryPoint.Settings) {
                        { navController.navigateUp() }
                    } else {
                        null
                    },
            )
        }
        composable(route = AppRoute.LocationPermissionGate.route) {
            val context = LocalContext.current
            val activity = context.findActivity()
            val viewModel: LocationPermissionGateViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            val permissionLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                    val granted = permissions.values.any { it }
                    val shouldShowRationale = activity?.let(::shouldShowLocationPermissionRationale) ?: false
                    viewModel.onPermissionResult(
                        granted = granted,
                        shouldShowRationale = shouldShowRationale,
                    )
                    if (granted) {
                        viewModel.completeOnboarding {
                            navController.navigate(AppRoute.Home.route) {
                                popUpTo(AppRoute.LocationPermissionGate.route) { inclusive = true }
                            }
                        }
                    }
                }

            locationPermissionGateScreen(
                uiState = uiState,
                onAllowClick = { permissionLauncher.launch(locationPermissions) },
                onOpenSettingsClick = { openAppSettings(context) },
                onLaterClick = {
                    viewModel.completeOnboarding {
                        navController.navigate(AppRoute.Home.route) {
                            popUpTo(AppRoute.LocationPermissionGate.route) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(route = AppRoute.Home.route) {
            val context = LocalContext.current
            val activity = context.findActivity()
            val viewModel: HomeViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            homeScreen(
                uiState = uiState,
                onStartRun = {
                    if (hasLocationPermission(context)) {
                        navController.navigate(AppRoute.RunReady.route)
                    } else {
                        viewModel.onRunStartRequested(
                            shouldShowRationale = activity?.let(::shouldShowLocationPermissionRationale) ?: false,
                        )
                    }
                },
                onOpenHistory = { navController.navigate(AppRoute.History.route) },
                onOpenStats = { navController.navigate(AppRoute.Stats.route) },
                onOpenSettings = { navController.navigate(AppRoute.Settings.route) },
                onDismissPermissionDialog = viewModel::dismissPermissionDialog,
                onOpenAppSettings = {
                    openAppSettings(context)
                    viewModel.dismissPermissionDialog()
                },
            )
        }
        composable(route = AppRoute.RunReady.route) {
            val viewModel: RunSessionViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                viewModel.prepareRun()
            }

            LaunchedEffect(uiState.shouldNavigateToLive) {
                if (uiState.shouldNavigateToLive) {
                    navController.navigate(AppRoute.RunLive.route) {
                        popUpTo(AppRoute.RunReady.route) { inclusive = true }
                    }
                }
            }

            runReadyScreen(
                uiState = uiState,
                onStartRun = viewModel::startRun,
                onCancel = {
                    viewModel.discardRun()
                    navController.navigateUp()
                },
            )
        }
        composable(route = AppRoute.RunLive.route) {
            val viewModel: RunSessionViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                viewModel.resumeActiveRun()
            }

            LaunchedEffect(uiState.snapshot?.state) {
                if (uiState.snapshot?.state == com.boksl.running.domain.model.RunEngineState.SAVED) {
                    navController.navigate(AppRoute.RunSummary.route) {
                        popUpTo(AppRoute.RunLive.route) { inclusive = true }
                    }
                }
            }

            runLiveScreen(
                uiState = uiState,
                onRequestStop = viewModel::requestStop,
                onConfirmSave = viewModel::confirmSave,
                onCancelStop = viewModel::cancelStop,
            )
        }
        composable(route = AppRoute.RunSummary.route) {
            val viewModel: RunSessionViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            runSummaryScreen(
                uiState = uiState,
                onComplete = {
                    navController.navigate(AppRoute.History.route) {
                        popUpTo(AppRoute.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(route = AppRoute.History.route) {
            val context = LocalContext.current
            val activity = context.findActivity()
            val viewModel: HistoryListViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            val pagedItems = viewModel.pagedItems.collectAsLazyPagingItems()

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
                            openAppSettings(context)
                            viewModel.dismissPermissionDialog()
                        },
                    ),
            )
        }
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
                onNavigateHome = {
                    navController.navigate(AppRoute.Home.route) {
                        popUpTo(AppRoute.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onGenerateSeedDataClick = viewModel::onGenerateSeedDataClick,
                onDeleteSeedDataClick = viewModel::onDeleteSeedDataClick,
                onConfirmPendingAction = viewModel::confirmPendingAction,
                onDismissPendingAction = viewModel::dismissPendingAction,
                onClearStatusMessage = viewModel::clearStatusMessage,
            )
        }
        composable(route = AppRoute.Export.route) {
            val context = LocalContext.current
            val viewModel: ExportViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            val pendingSaveFilePath =
                androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf<String?>(null)
                }
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
                onNavigateHome = {
                    navController.navigate(AppRoute.Home.route) {
                        popUpTo(AppRoute.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }
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
                onNavigateHome = {
                    navController.navigate(AppRoute.Home.route) {
                        popUpTo(AppRoute.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
