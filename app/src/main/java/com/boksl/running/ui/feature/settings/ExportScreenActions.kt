package com.boksl.running.ui.feature.settings

data class ExportScreenActions(
    val onNavigateUp: () -> Unit,
    val onStartExport: () -> Unit,
    val onCancelExport: () -> Unit,
    val onShareExport: () -> Unit,
    val onSaveToDevice: () -> Unit,
    val onNavigateHome: () -> Unit,
)
