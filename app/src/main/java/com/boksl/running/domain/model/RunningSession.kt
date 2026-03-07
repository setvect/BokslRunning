package com.boksl.running.domain.model

data class RunningSession(
    val id: Long = 0L,
    val externalId: String,
    val status: SessionStatus,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long?,
    val stats: RunStats,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
