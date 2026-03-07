package com.boksl.running.domain.repository

import android.net.Uri
import com.boksl.running.domain.model.ImportProgress
import kotlinx.coroutines.flow.Flow

interface ImportRepository {
    fun importAllData(uri: Uri): Flow<ImportProgress>
}
