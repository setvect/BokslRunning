package com.boksl.running.ui.navigation

sealed class AppRoute(val route: String) {
    data object Home : AppRoute("home")

    data object Settings : AppRoute("settings")

    data object History : AppRoute("history")

    data object Stats : AppRoute("stats")
}
