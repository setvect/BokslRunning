package com.boksl.running.ui.feature.settings

data class ImportScreenActions(
    val onNavigateUp: () -> Unit,
    val onStartImport: () -> Unit,
    val onCancelImport: () -> Unit,
    val onConfirm: () -> Unit,
    val onNavigateHome: () -> Unit,
)
