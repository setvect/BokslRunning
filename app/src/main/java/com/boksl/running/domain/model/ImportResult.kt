package com.boksl.running.domain.model

data class ImportResult(
    val addedSessionCount: Int,
    val duplicateSessionCount: Int,
    val appliedProfile: Boolean,
    val wasDuplicateFile: Boolean,
)
