package com.boksl.running.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.boksl.running.domain.model.RunEngineState
import com.boksl.running.ui.feature.home.HomeEvent
import com.boksl.running.ui.feature.home.HomeScreenActions
import com.boksl.running.ui.feature.home.HomeViewModel
import com.boksl.running.ui.feature.home.homeScreen
import com.boksl.running.ui.feature.onboarding.ProfileFormActions
import com.boksl.running.ui.feature.onboarding.ProfileFormNavigationEvent
import com.boksl.running.ui.feature.onboarding.ProfileFormViewModel
import com.boksl.running.ui.feature.onboarding.ProfileSetupEntryPoint
import com.boksl.running.ui.feature.onboarding.onboardingScreen
import com.boksl.running.ui.feature.onboarding.profileFormScreen
import com.boksl.running.ui.feature.permission.LocationPermissionGateEvent
import com.boksl.running.ui.feature.permission.LocationPermissionGateViewModel
import com.boksl.running.ui.feature.permission.findActivity
import com.boksl.running.ui.feature.permission.hasLocationPermission
import com.boksl.running.ui.feature.permission.locationPermissionGateScreen
import com.boksl.running.ui.feature.permission.locationPermissions
import com.boksl.running.ui.feature.permission.openAppSettings
import com.boksl.running.ui.feature.permission.shouldShowLocationPermissionRationale
import com.boksl.running.ui.feature.run.RunRecoveryEvent
import com.boksl.running.ui.feature.run.RunRecoveryViewModel
import com.boksl.running.ui.feature.run.RunSessionViewModel
import com.boksl.running.ui.feature.run.runLiveScreen
import com.boksl.running.ui.feature.run.runReadyScreen
import com.boksl.running.ui.feature.run.runRecoveryScreen
import com.boksl.running.ui.feature.run.runSummaryScreen

internal fun NavGraphBuilder.addLaunchRoute(navController: NavHostController) {
    composable(route = AppRoute.Launch.route) {
        val viewModel: AppLaunchViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()

        LaunchedEffect(uiState) {
            val destination = uiState.toStartDestination()
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
}

internal fun NavGraphBuilder.addOnboardingRoute(navController: NavHostController) {
    composable(route = AppRoute.Onboarding.route) {
        onboardingScreen(
            onStartClick = {
                navController.navigate(
                    AppRoute.ProfileSetup.createRoute(ProfileSetupEntryPoint.Onboarding.routeValue),
                )
            },
        )
    }
}

internal fun NavGraphBuilder.addProfileSetupRoute(navController: NavHostController) {
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
            actions =
                ProfileFormActions(
                    onWeightChanged = viewModel::onWeightChanged,
                    onGenderChanged = viewModel::onGenderChanged,
                    onAgeChanged = viewModel::onAgeChanged,
                    onSaveClick = viewModel::saveProfile,
                ),
            onNavigateUp =
                if (uiState.entryPoint == ProfileSetupEntryPoint.Settings) {
                    { navController.navigateUp() }
                } else {
                    null
                },
        )
    }
}

internal fun NavGraphBuilder.addLocationPermissionGateRoute(navController: NavHostController) {
    composable(route = AppRoute.LocationPermissionGate.route) {
        val context = LocalContext.current
        val activity = context.findActivity()
        val viewModel: LocationPermissionGateViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()
        observeOnResume {
            viewModel.onPermissionSettingsResult(hasPermission = hasLocationPermission(context))
        }
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
                        navController.navigateToHome(popUpRoute = AppRoute.LocationPermissionGate.route)
                    }
                }
            }

        LaunchedEffect(Unit) {
            viewModel.event.collect { event ->
                when (event) {
                    LocationPermissionGateEvent.NavigateHome ->
                        navController.navigateToHome(popUpRoute = AppRoute.LocationPermissionGate.route)
                }
            }
        }

        locationPermissionGateScreen(
            uiState = uiState,
            onAllowClick = { permissionLauncher.launch(locationPermissions) },
            onOpenSettingsClick = {
                viewModel.onOpenSettingsRequested()
                openAppSettings(context)
            },
            onLaterClick = {
                viewModel.completeOnboarding {
                    navController.navigateToHome(popUpRoute = AppRoute.LocationPermissionGate.route)
                }
            },
        )
    }
}

internal fun NavGraphBuilder.addHomeRoute(navController: NavHostController) {
    composable(route = AppRoute.Home.route) {
        val context = LocalContext.current
        val activity = context.findActivity()
        val viewModel: HomeViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()
        observeOnResume {
            viewModel.onPermissionSettingsResult(hasPermission = hasLocationPermission(context))
        }

        LaunchedEffect(Unit) {
            viewModel.event.collect { event ->
                when (event) {
                    HomeEvent.NavigateToRunReady -> navController.navigate(AppRoute.RunReady.route)
                }
            }
        }

        homeScreen(
            uiState = uiState,
            actions =
                HomeScreenActions(
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
                        viewModel.onOpenAppSettingsRequested()
                        openAppSettings(context)
                    },
                ),
        )
    }
}

internal fun NavGraphBuilder.addRunReadyRoute(navController: NavHostController) {
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
}

internal fun NavGraphBuilder.addRunRecoveryRoute(navController: NavHostController) {
    composable(route = AppRoute.RunRecovery.route) {
        val viewModel: RunRecoveryViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()

        LaunchedEffect(Unit) {
            viewModel.loadRecoveryState()
            viewModel.event.collect { event ->
                when (event) {
                    RunRecoveryEvent.NavigateHome ->
                        navController.navigateToHome(popUpRoute = AppRoute.RunRecovery.route)
                    RunRecoveryEvent.NavigateToHistory ->
                        navController.navigate(AppRoute.History.route) {
                            popUpTo(AppRoute.RunRecovery.route) { inclusive = true }
                        }
                    RunRecoveryEvent.NavigateToLive ->
                        navController.navigate(AppRoute.RunLive.route) {
                            popUpTo(AppRoute.RunRecovery.route) { inclusive = true }
                        }
                }
            }
        }

        runRecoveryScreen(
            uiState = uiState,
            onContinue = viewModel::continueRun,
            onDiscard = viewModel::discardRun,
        )
    }
}

internal fun NavGraphBuilder.addRunLiveRoute(navController: NavHostController) {
    composable(route = AppRoute.RunLive.route) {
        val viewModel: RunSessionViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()

        LaunchedEffect(Unit) {
            viewModel.resumeActiveRun()
        }

        LaunchedEffect(uiState.snapshot?.state) {
            if (uiState.snapshot?.state == RunEngineState.SAVED) {
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
}

internal fun NavGraphBuilder.addRunSummaryRoute(navController: NavHostController) {
    composable(route = AppRoute.RunSummary.route) {
        val viewModel: RunSessionViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()

        runSummaryScreen(
            uiState = uiState,
            onComplete = { navController.navigateToHistoryFromSummary() },
        )
    }
}
