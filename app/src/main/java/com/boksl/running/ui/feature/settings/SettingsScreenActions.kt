package com.boksl.running.ui.feature.settings

data class SettingsScreenActions(
    val onNavigateUp: () -> Unit,
    val onEditProfile: () -> Unit,
    val onOpenExport: () -> Unit,
    val onOpenImport: () -> Unit,
    val onNavigateHome: () -> Unit,
    val onGenerateSeedDataClick: () -> Unit,
    val onDeleteSeedDataClick: () -> Unit,
    val onConfirmPendingAction: () -> Unit,
    val onDismissPendingAction: () -> Unit,
    val onClearStatusMessage: () -> Unit,
)
