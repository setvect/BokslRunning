package com.boksl.running.data.repository

import android.content.Context
import android.content.res.Resources
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.boksl.running.data.export.ExportAppPreferencesDto
import com.boksl.running.data.export.ExportBundleDto
import com.boksl.running.data.export.ExportProfileDto
import com.boksl.running.data.export.ExportRunStatsDto
import com.boksl.running.data.export.ExportSessionDto
import com.boksl.running.data.export.ExportTrackPointDto
import com.boksl.running.data.local.db.AppDatabase
import com.boksl.running.data.local.preferences.ProfilePreferencesDataSource
import com.boksl.running.domain.model.Gender
import com.boksl.running.domain.model.ImportProgress
import com.boksl.running.domain.model.Profile
import com.boksl.running.domain.model.RunStats
import com.boksl.running.domain.model.RunningSession
import com.boksl.running.domain.model.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class ImportRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var preferencesScope: CoroutineScope
    private lateinit var preferencesFile: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var profilePreferencesDataSource: ProfilePreferencesDataSource
    private lateinit var importRepository: DefaultImportRepository
    private lateinit var runningRepository: DefaultRunningRepository
    private lateinit var backupRootDirectory: File

    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        preferencesFile = File.createTempFile("import-preferences", ".preferences_pb")
        preferencesScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore =
            PreferenceDataStoreFactory.create(
                scope = preferencesScope,
                produceFile = { preferencesFile },
            )
        profilePreferencesDataSource = ProfilePreferencesDataSource(dataStore)
        runningRepository =
            DefaultRunningRepository(
                runningSessionDao = database.runningSessionDao(),
                trackPointDao = database.trackPointDao(),
                ioDispatcher = Dispatchers.IO,
                clock = FIXED_CLOCK,
            )
        backupRootDirectory = File(context.cacheDir, "import-backup-tests").apply { deleteRecursively() }
        importRepository =
            DefaultImportRepository(
                context = context,
                contentResolver = context.contentResolver,
                database = database,
                runningSessionDao = database.runningSessionDao(),
                trackPointDao = database.trackPointDao(),
                profilePreferencesDataSource = profilePreferencesDataSource,
                ioDispatcher = Dispatchers.IO,
                clock = FIXED_CLOCK,
                json = json,
                backupDirectoryProvider = { backupRootDirectory },
            )
    }

    @After
    fun tearDown() {
        database.close()
        preferencesScope.cancel()
        preferencesFile.delete()
        backupRootDirectory.deleteRecursively()
    }

    @Test
    fun importAllDataAddsSessionsTrackPointsAndProfileWhenCurrentProfileMissing() =
        runTest {
            val jsonBytes =
                buildImportBundle(
                    sessionExternalId = "import-session-1",
                    trackPointExternalIds = listOf("tp-2", "tp-1"),
                    trackPointSequences = listOf(1, 0),
                )

            val completed = importRepository.importAllData(jsonBytes).toList().last() as ImportProgress.Completed
            val importedSession = database.runningSessionDao().getByExternalId("import-session-1")
            val importedTrackPoints = database.trackPointDao().getBySessionId(importedSession!!.id)
            val profile = profilePreferencesDataSource.observeProfile().first()
            val preferences = profilePreferencesDataSource.observeAppPreferences().first()

            assertEquals(1, completed.result.addedSessionCount)
            assertEquals(0, completed.result.duplicateSessionCount)
            assertTrue(completed.result.appliedProfile)
            assertFalse(completed.result.wasDuplicateFile)
            assertEquals(2, importedTrackPoints.size)
            assertEquals("tp-1", importedTrackPoints.first().externalId)
            assertNotNull(profile)
            assertEquals(70.5f, profile?.weightKg)
            assertTrue(preferences.onboardingCompleted)
            assertTrue(preferences.locationRationaleShown)
            assertEquals(1, backupRootDirectory.listFiles()?.size)
        }

    @Test
    fun importAllDataCountsExistingSessionAsDuplicate() =
        runTest {
            runningRepository.insertSession(
                RunningSession(
                    externalId = "import-session-1",
                    status = SessionStatus.SAVED,
                    startedAtEpochMillis = 10_000L,
                    endedAtEpochMillis = 610_000L,
                    stats =
                        RunStats(
                            durationMillis = 600_000L,
                            distanceMeters = 2_500.0,
                            averagePaceSecPerKm = 320.0,
                            maxSpeedMps = 4.9,
                            calorieKcal = 180.0,
                        ),
                    createdAtEpochMillis = 10_000L,
                    updatedAtEpochMillis = 610_000L,
                ),
            )

            val completed =
                importRepository
                    .importAllData(buildImportBundle(sessionExternalId = "import-session-1"))
                    .toList()
                    .last() as ImportProgress.Completed

            assertEquals(0, completed.result.addedSessionCount)
            assertEquals(1, completed.result.duplicateSessionCount)
        }

    @Test
    fun importAllDataTreatsSameBytesAsDuplicateFileOnSecondImport() =
        runTest {
            val jsonBytes = buildImportBundle(sessionExternalId = "import-session-1")

            val first = importRepository.importAllData(jsonBytes).toList().last() as ImportProgress.Completed
            val second = importRepository.importAllData(jsonBytes).toList().last() as ImportProgress.Completed

            assertFalse(first.result.wasDuplicateFile)
            assertTrue(second.result.wasDuplicateFile)
            assertEquals(1, database.runningSessionDao().getByStatus(SessionStatus.SAVED).size)
        }

    @Test
    fun importAllDataFailsWhenSchemaVersionIsUnsupported() =
        runTest {
            val jsonBytes =
                json.encodeToString(
                    buildBundle(schemaVersion = 99),
                ).toByteArray()

            val progress = importRepository.importAllData(jsonBytes).toList().last()

            assertTrue(progress is ImportProgress.Error)
            assertEquals(
                "지원하지 않는 schema_version 입니다.",
                (progress as ImportProgress.Error).message,
            )
        }

    @Test
    fun importAllDataFailsWhenJsonIsMalformed() =
        runTest {
            val progress = importRepository.importAllData("not-json".toByteArray()).toList().last()

            assertTrue(progress is ImportProgress.Error)
            assertEquals(
                "가져오기 파일 형식이 올바르지 않습니다.",
                (progress as ImportProgress.Error).message,
            )
        }

    @Test
    fun importAllDataFailsWhenBackupCreationFails() =
        runTest {
            val blockingFile = File.createTempFile("backup-root", ".tmp")
            val failingRepository =
                DefaultImportRepository(
                    context = context,
                    contentResolver = context.contentResolver,
                    database = database,
                    runningSessionDao = database.runningSessionDao(),
                    trackPointDao = database.trackPointDao(),
                    profilePreferencesDataSource = profilePreferencesDataSource,
                    ioDispatcher = Dispatchers.IO,
                    clock = FIXED_CLOCK,
                    json = json,
                    backupDirectoryProvider = { blockingFile },
                )

            val progress = failingRepository.importAllData(buildImportBundle(sessionExternalId = "new-session")).toList().last()

            assertTrue(progress is ImportProgress.Error)
            assertEquals("내부 백업 생성에 실패했습니다.", (progress as ImportProgress.Error).message)
            assertEquals(0, database.runningSessionDao().getByStatus(SessionStatus.SAVED).size)
            assertTrue(profilePreferencesDataSource.getImportedFileHashes().isEmpty())
            blockingFile.delete()
        }

    @Test
    fun importAllDataKeepsExistingProfileAndPreferences() =
        runTest {
            profilePreferencesDataSource.saveProfile(
                Profile(
                    weightKg = 80.0f,
                    gender = Gender.MALE,
                    age = 40,
                    updatedAtEpochMillis = 123L,
                ),
            )
            profilePreferencesDataSource.saveAppPreferences(
                com.boksl.running.domain.model.AppPreferences(
                    onboardingCompleted = false,
                    locationRationaleShown = false,
                ),
            )

            val completed = importRepository.importAllData(buildImportBundle(sessionExternalId = "new-session")).toList().last()
            val profile = profilePreferencesDataSource.observeProfile().first()
            val preferences = profilePreferencesDataSource.observeAppPreferences().first()

            assertTrue(completed is ImportProgress.Completed)
            assertFalse((completed as ImportProgress.Completed).result.appliedProfile)
            assertEquals(80.0f, profile?.weightKg)
            assertFalse(preferences.onboardingCompleted)
            assertFalse(preferences.locationRationaleShown)
        }

    @Test
    fun importAllDataSupportsEmptySessions() =
        runTest {
            val completed =
                importRepository
                    .importAllData(json.encodeToString(buildBundle(sessions = emptyList())).toByteArray())
                    .toList()
                    .last() as ImportProgress.Completed

            assertEquals(0, completed.result.addedSessionCount)
            assertEquals(0, completed.result.duplicateSessionCount)
        }

    private fun buildImportBundle(
        sessionExternalId: String,
        trackPointExternalIds: List<String> = listOf("tp-1", "tp-2"),
        trackPointSequences: List<Int> = listOf(0, 1),
    ): ByteArray =
        json.encodeToString(
            buildBundle(
                sessions =
                    listOf(
                        ExportSessionDto(
                            externalId = sessionExternalId,
                            status = SessionStatus.SAVED.name,
                            startedAtEpochMillis = 10_000L,
                            endedAtEpochMillis = 610_000L,
                            stats =
                                ExportRunStatsDto(
                                    durationMillis = 600_000L,
                                    distanceMeters = 3_100.0,
                                    averagePaceSecPerKm = 300.0,
                                    maxSpeedMps = 5.4,
                                    calorieKcal = 205.0,
                                ),
                            createdAtEpochMillis = 10_000L,
                            updatedAtEpochMillis = 610_000L,
                            trackPoints =
                                trackPointExternalIds.zip(trackPointSequences).map { (externalId, sequence) ->
                                    ExportTrackPointDto(
                                        externalId = externalId,
                                        sequence = sequence,
                                        latitude = 37.5 + sequence,
                                        longitude = 127.0 + sequence,
                                        altitudeMeters = 10.0,
                                        accuracyMeters = 3.0f,
                                        speedMps = 2.5f,
                                        recordedAtEpochMillis = 10_000L + sequence,
                                    )
                                },
                        ),
                    ),
            ),
        ).toByteArray()

    private fun buildBundle(
        schemaVersion: Int = 1,
        sessions: List<ExportSessionDto> =
            listOf(
                ExportSessionDto(
                    externalId = "import-session-1",
                    status = SessionStatus.SAVED.name,
                    startedAtEpochMillis = 10_000L,
                    endedAtEpochMillis = 610_000L,
                    stats =
                        ExportRunStatsDto(
                            durationMillis = 600_000L,
                            distanceMeters = 3_100.0,
                            averagePaceSecPerKm = 300.0,
                            maxSpeedMps = 5.4,
                            calorieKcal = 205.0,
                        ),
                    createdAtEpochMillis = 10_000L,
                    updatedAtEpochMillis = 610_000L,
                    trackPoints = emptyList(),
                ),
            ),
    ): ExportBundleDto =
        ExportBundleDto(
            schemaVersion = schemaVersion,
            exportedAtEpochMillis = 1_234_567L,
            profile =
                ExportProfileDto(
                    weightKg = 70.5f,
                    gender = Gender.FEMALE.name,
                    age = 31,
                    updatedAtEpochMillis = 9_999L,
                ),
            appPreferences =
                ExportAppPreferencesDto(
                    onboardingCompleted = true,
                    locationRationaleShown = true,
                ),
            sessions = sessions,
        )

    private companion object {
        val FIXED_CLOCK: Clock = Clock.fixed(Instant.ofEpochMilli(1_234_567_890L), ZoneOffset.UTC)
    }
}
