package com.boksl.running.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.boksl.running.core.di.IoDispatcher
import com.boksl.running.data.export.EXPORT_SCHEMA_VERSION
import com.boksl.running.data.export.ExportBundleDto
import com.boksl.running.data.export.buildExportBundle
import com.boksl.running.data.import.toDomain
import com.boksl.running.data.import.toEntity
import com.boksl.running.data.local.db.AppDatabase
import com.boksl.running.data.local.db.dao.RunningSessionDao
import com.boksl.running.data.local.db.dao.TrackPointDao
import com.boksl.running.data.local.preferences.ProfilePreferencesDataSource
import com.boksl.running.domain.model.ImportProgress
import com.boksl.running.domain.model.ImportResult
import com.boksl.running.domain.model.SessionStatus
import com.boksl.running.domain.repository.ImportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultImportRepository internal constructor(
    @ApplicationContext private val context: Context,
    private val contentResolver: ContentResolver,
    private val database: AppDatabase,
    private val runningSessionDao: RunningSessionDao,
    private val trackPointDao: TrackPointDao,
    private val profilePreferencesDataSource: ProfilePreferencesDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val clock: Clock,
    private val json: Json,
    private val backupDirectoryProvider: () -> File,
) : ImportRepository {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        database: AppDatabase,
        runningSessionDao: RunningSessionDao,
        trackPointDao: TrackPointDao,
        profilePreferencesDataSource: ProfilePreferencesDataSource,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        clock: Clock,
        json: Json,
    ) : this(
        context = context,
        contentResolver = context.contentResolver,
        database = database,
        runningSessionDao = runningSessionDao,
        trackPointDao = trackPointDao,
        profilePreferencesDataSource = profilePreferencesDataSource,
        ioDispatcher = ioDispatcher,
        clock = clock,
        json = json,
        backupDirectoryProvider = { File(context.filesDir, BACKUP_DIRECTORY_NAME) },
    )

    override fun importAllData(uri: Uri): Flow<ImportProgress> =
        flow {
            val jsonBytes =
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.readBytes()
                } ?: throw IllegalStateException(FILE_OPEN_FAILURE_MESSAGE)

            importAllData(jsonBytes).collect { progress -> emit(progress) }
        }.flowOn(ioDispatcher)

    internal fun importAllData(jsonBytes: ByteArray): Flow<ImportProgress> =
        flow {
            try {
                val fileHash = jsonBytes.sha256()
                if (profilePreferencesDataSource.getImportedFileHashes().contains(fileHash)) {
                    emit(
                        ImportProgress.Completed(
                            ImportResult(
                                addedSessionCount = 0,
                                duplicateSessionCount = 0,
                                appliedProfile = false,
                                wasDuplicateFile = true,
                            ),
                        ),
                    )
                    return@flow
                }

                val importBundle = json.decodeImportBundle(jsonBytes)

                emit(ImportProgress.BackingUp)
                createBackupFile()
                currentCoroutineContext().ensureActive()

                emit(ImportProgress.Importing)
                val currentProfile = profilePreferencesDataSource.observeProfile().first()
                val mergeResult = mergeImportBundle(importBundle)
                currentCoroutineContext().ensureActive()

                val appliedProfile = currentProfile == null && importBundle.profile != null
                if (currentProfile == null) {
                    importBundle.profile?.let { profilePreferencesDataSource.saveProfile(it.toDomain()) }
                    profilePreferencesDataSource.saveAppPreferences(importBundle.appPreferences.toDomain())
                }
                profilePreferencesDataSource.addImportedFileHash(fileHash)

                emit(
                    ImportProgress.Completed(
                        ImportResult(
                            addedSessionCount = mergeResult.addedSessionCount,
                            duplicateSessionCount = mergeResult.duplicateSessionCount,
                            appliedProfile = appliedProfile,
                            wasDuplicateFile = false,
                        ),
                    ),
                )
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (throwable: Throwable) {
                emit(ImportProgress.Error(message = throwable.message ?: IMPORT_FAILURE_MESSAGE))
            }
        }.flowOn(ioDispatcher)

    private suspend fun createBackupFile() {
        val backupDirectory =
            backupDirectoryProvider().apply {
                if (!exists() && !mkdirs()) {
                    throw IllegalStateException(BACKUP_FAILURE_MESSAGE)
                }
                if (!isDirectory) {
                    throw IllegalStateException(BACKUP_FAILURE_MESSAGE)
                }
            }
        val backupFile = File(backupDirectory, "bokslrunning_backup_${clock.millis()}.json")
        val backupBundle =
            buildExportBundle(
                runningSessionDao = runningSessionDao,
                trackPointDao = trackPointDao,
                profilePreferencesDataSource = profilePreferencesDataSource,
                clock = clock,
            )

        runCatching {
            backupFile.writeText(
                text = json.encodeToString(backupBundle),
                charset = Charsets.UTF_8,
            )
        }.getOrElse { throwable ->
            throw IllegalStateException(throwable.message ?: BACKUP_FAILURE_MESSAGE, throwable)
        }
    }

    private suspend fun mergeImportBundle(importBundle: ExportBundleDto): ImportMergeResult =
        database.withTransaction {
            var addedSessionCount = 0
            var duplicateSessionCount = 0

            importBundle.sessions.forEach { session ->
                currentCoroutineContext().ensureActive()
                val status =
                    runCatching { SessionStatus.valueOf(session.status) }.getOrElse {
                        throw IllegalStateException(UNSUPPORTED_STATUS_MESSAGE)
                    }
                if (status != SessionStatus.SAVED) return@forEach

                val existingSession = runningSessionDao.getByExternalId(session.externalId)
                if (existingSession != null) {
                    duplicateSessionCount += 1
                    return@forEach
                }

                val insertedSessionId = runningSessionDao.insert(session.toEntity())
                val trackPointEntities =
                    session.trackPoints
                        .sortedBy { trackPoint -> trackPoint.sequence }
                        .map { trackPoint -> trackPoint.toEntity(insertedSessionId) }
                if (trackPointEntities.isNotEmpty()) {
                    trackPointDao.insertAll(trackPointEntities)
                }
                addedSessionCount += 1
            }

            ImportMergeResult(
                addedSessionCount = addedSessionCount,
                duplicateSessionCount = duplicateSessionCount,
            )
        }

    private fun Json.decodeImportBundle(jsonBytes: ByteArray): ExportBundleDto =
        runCatching {
            decodeFromString<ExportBundleDto>(jsonBytes.toString(Charsets.UTF_8))
        }.getOrElse { throwable ->
            when (throwable) {
                is SerializationException,
                is IllegalArgumentException,
                -> throw IllegalStateException(INVALID_JSON_MESSAGE, throwable)

                else -> throw throwable
            }
        }.also { bundle ->
            check(bundle.schemaVersion == EXPORT_SCHEMA_VERSION) { UNSUPPORTED_SCHEMA_MESSAGE }
        }

    private fun ByteArray.sha256(): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(this)
            .joinToString(separator = "") { byte -> "%02x".format(byte) }

    private data class ImportMergeResult(
        val addedSessionCount: Int,
        val duplicateSessionCount: Int,
    )

    private companion object {
        const val BACKUP_DIRECTORY_NAME = "import-backups"
        const val FILE_OPEN_FAILURE_MESSAGE = "가져오기 파일을 열 수 없습니다."
        const val INVALID_JSON_MESSAGE = "가져오기 파일 형식이 올바르지 않습니다."
        const val UNSUPPORTED_SCHEMA_MESSAGE = "지원하지 않는 schema_version 입니다."
        const val UNSUPPORTED_STATUS_MESSAGE = "지원하지 않는 세션 상태가 포함되어 있습니다."
        const val BACKUP_FAILURE_MESSAGE = "내부 백업 생성에 실패했습니다."
        const val IMPORT_FAILURE_MESSAGE = "가져오기에 실패했습니다."
    }
}
