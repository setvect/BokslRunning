package com.boksl.running.ui.navigation

sealed class AppRoute(val route: String) {
    data object Launch : AppRoute("launch")

    data object Onboarding : AppRoute("onboarding")

    data object LocationPermissionGate : AppRoute("location_permission_gate")

    data object ProfileSetup : AppRoute("profile_setup/{entryPoint}") {
        const val entryPointArg = "entryPoint"

        fun createRoute(entryPoint: String): String = "profile_setup/$entryPoint"
    }

    data object RunReady : AppRoute("run_ready")

    data object RunLive : AppRoute("run_live")

    data object RunSummary : AppRoute("run_summary")

    data object Home : AppRoute("home")

    data object Settings : AppRoute("settings")

    data object History : AppRoute("history")

    data object HistoryDetail : AppRoute("history_detail/{sessionId}") {
        const val sessionIdArg = "sessionId"

        fun createRoute(sessionId: Long): String = "history_detail/$sessionId"
    }

    data object Stats : AppRoute("stats")
}
