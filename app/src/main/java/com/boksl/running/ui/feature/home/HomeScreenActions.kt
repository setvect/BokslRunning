package com.boksl.running.ui.feature.home

import com.boksl.running.domain.model.HomeStatsPeriod

data class HomeScreenActions(
    val onStartRun: () -> Unit,
    val onOpenHistory: () -> Unit,
    val onOpenStats: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onSelectStatsPeriod: (HomeStatsPeriod) -> Unit,
    val onDismissPermissionDialog: () -> Unit,
    val onOpenAppSettings: () -> Unit,
)
