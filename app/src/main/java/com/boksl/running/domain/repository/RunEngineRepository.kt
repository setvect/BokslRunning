package com.boksl.running.domain.repository

import com.boksl.running.domain.model.RunSnapshot
import kotlinx.coroutines.flow.Flow

interface RunEngineRepository {
    fun observeActiveRun(): Flow<RunSnapshot?>

    suspend fun prepareRun()

    suspend fun startRun()

    suspend fun requestStop()

    suspend fun confirmSave()

    suspend fun cancelStop()

    suspend fun discardRun()

    suspend fun resumeActiveRun()
}
