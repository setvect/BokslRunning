package com.boksl.running.ui.feature.home

data class HomeScreenActions(
    val onStartRun: () -> Unit,
    val onOpenHistory: () -> Unit,
    val onOpenStats: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onDismissPermissionDialog: () -> Unit,
    val onOpenAppSettings: () -> Unit,
)
