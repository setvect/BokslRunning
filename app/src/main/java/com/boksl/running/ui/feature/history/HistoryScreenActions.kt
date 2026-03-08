package com.boksl.running.ui.feature.history

data class HistoryScreenActions(
    val onNavigateUp: () -> Unit,
    val onOpenSession: (Long) -> Unit,
    val onStartRun: () -> Unit,
    val onDismissPermissionDialog: () -> Unit,
    val onOpenAppSettings: () -> Unit,
)
