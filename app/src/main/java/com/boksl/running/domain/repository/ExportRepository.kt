package com.boksl.running.domain.repository

import com.boksl.running.domain.model.ExportProgress
import kotlinx.coroutines.flow.Flow

interface ExportRepository {
    fun exportAllData(): Flow<ExportProgress>
}
