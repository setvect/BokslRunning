package com.boksl.running.domain.model

sealed interface ExportProgress {
    data object Idle : ExportProgress

    data class Running(
        val totalSessions: Int,
        val completedSessions: Int,
    ) : ExportProgress

    data class Completed(
        val filePath: String,
    ) : ExportProgress

    data class Error(
        val message: String,
    ) : ExportProgress
}
