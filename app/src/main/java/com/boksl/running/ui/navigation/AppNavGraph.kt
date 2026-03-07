package com.boksl.running.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.boksl.running.ui.feature.onboarding.ProfileSetupEntryPoint

@Composable
fun appNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AppRoute.Launch.route,
        modifier = modifier,
    ) {
        addLaunchRoute(navController = navController)
        addOnboardingRoute(navController = navController)
        addProfileSetupRoute(navController = navController)
        addLocationPermissionGateRoute(navController = navController)
        addHomeRoute(navController = navController)
        addRunReadyRoute(navController = navController)
        addRunRecoveryRoute(navController = navController)
        addRunLiveRoute(navController = navController)
        addRunSummaryRoute(navController = navController)
        addHistoryRoutes(navController = navController)
        addStatsRoute(navController = navController)
        addSettingsRoute(navController = navController)
        addExportRoute(navController = navController)
        addImportRoute(navController = navController)
    }
}

@Composable
internal fun observeOnResume(onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnResume = rememberUpdatedState(onResume)

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    currentOnResume.value()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

internal fun AppLaunchUiState.toStartDestination(): String? =
    when (this) {
        AppLaunchUiState.Loading -> null
        AppLaunchUiState.NeedsOnboarding -> AppRoute.Onboarding.route
        AppLaunchUiState.NeedsProfile ->
            AppRoute.ProfileSetup.createRoute(ProfileSetupEntryPoint.Onboarding.routeValue)
        AppLaunchUiState.NeedsLocationPermission -> AppRoute.LocationPermissionGate.route
        AppLaunchUiState.NeedsRunRecovery -> AppRoute.RunRecovery.route
        AppLaunchUiState.Ready -> AppRoute.Home.route
    }

internal fun NavHostController.navigateToHome(popUpRoute: String) {
    navigate(AppRoute.Home.route) {
        popUpTo(popUpRoute) { inclusive = true }
    }
}

internal fun NavHostController.navigateToHistoryFromSummary() {
    navigate(AppRoute.History.route) {
        popUpTo(AppRoute.Home.route) { inclusive = false }
        launchSingleTop = true
    }
}
