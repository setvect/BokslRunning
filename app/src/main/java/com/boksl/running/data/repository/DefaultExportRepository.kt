package com.boksl.running.data.repository

import android.content.Context
import com.boksl.running.core.di.IoDispatcher
import com.boksl.running.data.export.buildExportBundle
import com.boksl.running.data.local.db.dao.RunningSessionDao
import com.boksl.running.data.local.db.dao.TrackPointDao
import com.boksl.running.data.local.preferences.ProfilePreferencesDataSource
import com.boksl.running.domain.model.ExportProgress
import com.boksl.running.domain.model.SessionStatus
import com.boksl.running.domain.repository.ExportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultExportRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val runningSessionDao: RunningSessionDao,
        private val trackPointDao: TrackPointDao,
        private val profilePreferencesDataSource: ProfilePreferencesDataSource,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val clock: Clock,
        private val json: Json,
    ) : ExportRepository {
        override fun exportAllData(): Flow<ExportProgress> =
            flow {
                val exportDirectory =
                    File(context.cacheDir, EXPORT_DIRECTORY_NAME).apply {
                        mkdirs()
                    }
                val outputFile = File(exportDirectory, EXPORT_FILE_NAME)
                val temporaryFile = File(exportDirectory, TEMP_FILE_NAME)

                outputFile.delete()
                temporaryFile.delete()

                try {
                    val sessionEntities = runningSessionDao.getByStatus(SessionStatus.SAVED)
                    val totalSessions = sessionEntities.size

                    emit(ExportProgress.Running(totalSessions = totalSessions, completedSessions = 0))

                    sessionEntities.forEachIndexed { index, session ->
                        currentCoroutineContext().ensureActive()
                        emit(ExportProgress.Running(totalSessions = totalSessions, completedSessions = index + 1))
                    }

                    currentCoroutineContext().ensureActive()

                    val exportBundle =
                        buildExportBundle(
                            runningSessionDao = runningSessionDao,
                            trackPointDao = trackPointDao,
                            profilePreferencesDataSource = profilePreferencesDataSource,
                            clock = clock,
                        )

                    temporaryFile.writeText(
                        text = json.encodeToString(exportBundle),
                        charset = Charsets.UTF_8,
                    )

                    currentCoroutineContext().ensureActive()

                    if (!temporaryFile.renameTo(outputFile)) {
                        temporaryFile.copyTo(target = outputFile, overwrite = true)
                        temporaryFile.delete()
                    }

                    emit(ExportProgress.Completed(filePath = outputFile.absolutePath))
                } catch (cancellationException: CancellationException) {
                    outputFile.delete()
                    temporaryFile.delete()
                    throw cancellationException
                } catch (throwable: Throwable) {
                    outputFile.delete()
                    temporaryFile.delete()
                    emit(ExportProgress.Error(message = throwable.message ?: EXPORT_FAILURE_MESSAGE))
                }
            }.flowOn(ioDispatcher)

        private companion object {
            const val EXPORT_DIRECTORY_NAME = "exports"
            const val EXPORT_FILE_NAME = "bokslrunning_export_v1.json"
            const val TEMP_FILE_NAME = "$EXPORT_FILE_NAME.tmp"
            const val EXPORT_FAILURE_MESSAGE = "내보내기에 실패했습니다."
        }
    }
