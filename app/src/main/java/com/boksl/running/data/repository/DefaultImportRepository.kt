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
import java.io.IOException
import java.security.MessageDigest
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultImportRepository internal constructor(
    private val contentResolver: ContentResolver,
    private val dependencies: Dependencies,
    private val backupDirectoryProvider: () -> File,
) : ImportRepository {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        database: AppDatabase,
        profilePreferencesDataSource: ProfilePreferencesDataSource,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        clock: Clock,
        json: Json,
    ) : this(
        contentResolver = context.contentResolver,
        dependencies =
            Dependencies(
                database = database,
                profilePreferencesDataSource = profilePreferencesDataSource,
                ioDispatcher = ioDispatcher,
                clock = clock,
                json = json,
            ),
        backupDirectoryProvider = { File(context.filesDir, BACKUP_DIRECTORY_NAME) },
    )

    override fun importAllData(uri: Uri): Flow<ImportProgress> =
        flow {
            val jsonBytes =
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.readBytes()
                } ?: throw IllegalStateException(FILE_OPEN_FAILURE_MESSAGE)

            importAllData(jsonBytes).collect { progress -> emit(progress) }
        }.flowOn(dependencies.ioDispatcher)

    internal fun importAllData(jsonBytes: ByteArray): Flow<ImportProgress> =
        flow {
            try {
                importAllDataInternal(jsonBytes).collect { progress -> emit(progress) }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (exception: IOException) {
                emit(ImportProgress.Error(message = exception.message ?: IMPORT_FAILURE_MESSAGE))
            } catch (exception: IllegalStateException) {
                emit(ImportProgress.Error(message = exception.message ?: IMPORT_FAILURE_MESSAGE))
            } catch (exception: SecurityException) {
                emit(ImportProgress.Error(message = exception.message ?: IMPORT_FAILURE_MESSAGE))
            }
        }.flowOn(dependencies.ioDispatcher)

    private fun importAllDataInternal(jsonBytes: ByteArray): Flow<ImportProgress> =
        flow {
            val fileHash = jsonBytes.sha256()
            if (dependencies.profilePreferencesDataSource.getImportedFileHashes().contains(fileHash)) {
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

            val importBundle = dependencies.json.decodeImportBundle(jsonBytes)

            emit(ImportProgress.BackingUp)
            createBackupFile()
            currentCoroutineContext().ensureActive()

            emit(ImportProgress.Importing)
            val currentProfile = dependencies.profilePreferencesDataSource.observeProfile().first()
            val mergeResult = mergeImportBundle(importBundle)
            currentCoroutineContext().ensureActive()

            val appliedProfile = currentProfile == null && importBundle.profile != null
            if (currentProfile == null) {
                importBundle.profile?.let { dependencies.profilePreferencesDataSource.saveProfile(it.toDomain()) }
                dependencies.profilePreferencesDataSource.saveAppPreferences(importBundle.appPreferences.toDomain())
            }
            dependencies.profilePreferencesDataSource.addImportedFileHash(fileHash)

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
        }

    private suspend fun createBackupFile() {
        val runningSessionDao = dependencies.database.runningSessionDao()
        val trackPointDao = dependencies.database.trackPointDao()
        val backupDirectory = resolveBackupDirectory()
        val backupFile = File(backupDirectory, "bokslrunning_backup_${dependencies.clock.millis()}.json")
        val backupBundle =
            buildExportBundle(
                runningSessionDao = runningSessionDao,
                trackPointDao = trackPointDao,
                profilePreferencesDataSource = dependencies.profilePreferencesDataSource,
                clock = dependencies.clock,
            )

        runCatching {
            backupFile.writeText(
                text = dependencies.json.encodeToString(backupBundle),
                charset = Charsets.UTF_8,
            )
        }.getOrElse { exception ->
            throw IllegalStateException(exception.message ?: BACKUP_FAILURE_MESSAGE, exception)
        }
    }

    private suspend fun mergeImportBundle(importBundle: ExportBundleDto): ImportMergeResult =
        dependencies.database.withTransaction {
            val runningSessionDao = dependencies.database.runningSessionDao()
            val trackPointDao = dependencies.database.trackPointDao()
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

    private fun resolveBackupDirectory(): File =
        backupDirectoryProvider().apply {
            if (exists()) {
                check(isDirectory) { BACKUP_FAILURE_MESSAGE }
            } else {
                check(mkdirs()) { BACKUP_FAILURE_MESSAGE }
            }
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

    internal data class Dependencies(
        val database: AppDatabase,
        val profilePreferencesDataSource: ProfilePreferencesDataSource,
        val ioDispatcher: CoroutineDispatcher,
        val clock: Clock,
        val json: Json,
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
