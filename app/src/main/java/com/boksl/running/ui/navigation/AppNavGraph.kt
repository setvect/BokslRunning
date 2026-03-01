package com.boksl.running.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.boksl.running.ui.feature.history.historyScreen
import com.boksl.running.ui.feature.home.homeScreen
import com.boksl.running.ui.feature.settings.settingsScreen
import com.boksl.running.ui.feature.stats.statsScreen

@Composable
fun appNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AppRoute.Home.route,
        modifier = modifier,
    ) {
        composable(route = AppRoute.Home.route) {
            homeScreen(
                onOpenHistory = { navController.navigate(AppRoute.History.route) },
                onOpenStats = { navController.navigate(AppRoute.Stats.route) },
                onOpenSettings = { navController.navigate(AppRoute.Settings.route) },
            )
        }
        composable(route = AppRoute.History.route) {
            historyScreen(onNavigateUp = navController::navigateUp)
        }
        composable(route = AppRoute.Stats.route) {
            statsScreen(onNavigateUp = navController::navigateUp)
        }
        composable(route = AppRoute.Settings.route) {
            settingsScreen(onNavigateUp = navController::navigateUp)
        }
    }
}
