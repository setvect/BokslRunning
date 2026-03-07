package com.boksl.running.domain.model

sealed interface ImportProgress {
    data object Idle : ImportProgress

    data object BackingUp : ImportProgress

    data object Importing : ImportProgress

    data class Completed(
        val result: ImportResult,
    ) : ImportProgress

    data class Error(
        val message: String,
    ) : ImportProgress
}
