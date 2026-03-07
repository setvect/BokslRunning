package com.boksl.running.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.boksl.running.data.local.db.AppDatabase
import com.boksl.running.data.local.preferences.ProfilePreferencesDataSource
import com.boksl.running.domain.model.ExportProgress
import com.boksl.running.domain.model.Gender
import com.boksl.running.domain.model.Profile
import com.boksl.running.domain.model.RunStats
import com.boksl.running.domain.model.RunningSession
import com.boksl.running.domain.model.SessionStatus
import com.boksl.running.domain.model.TrackPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class ExportRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var preferencesScope: CoroutineScope
    private lateinit var preferencesFile: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var profilePreferencesDataSource: ProfilePreferencesDataSource
    private lateinit var runningRepository: DefaultRunningRepository
    private lateinit var exportRepository: DefaultExportRepository

    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        preferencesFile = File.createTempFile("export-preferences", ".preferences_pb")
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
        exportRepository =
            DefaultExportRepository(
                context = context,
                runningSessionDao = database.runningSessionDao(),
                trackPointDao = database.trackPointDao(),
                profilePreferencesDataSource = profilePreferencesDataSource,
                ioDispatcher = Dispatchers.IO,
                clock = FIXED_CLOCK,
                json = json,
            )
        exportDirectory().deleteRecursively()
    }

    @After
    fun tearDown() {
        database.close()
        preferencesScope.cancel()
        preferencesFile.delete()
        exportDirectory().deleteRecursively()
    }

    @Test
    fun exportAllDataWritesSnakeCaseJsonWithoutLocalIds() =
        runTest {
            profilePreferencesDataSource.saveProfile(
                Profile(
                    weightKg = 68.5f,
                    gender = Gender.FEMALE,
                    age = 29,
                    updatedAtEpochMillis = 9_999L,
                ),
            )
            profilePreferencesDataSource.setOnboardingCompleted(completed = true)
            profilePreferencesDataSource.setLocationRationaleShown(shown = true)

            val savedSessionId =
                runningRepository.insertSession(
                    buildSession(
                        externalId = "saved-session",
                        status = SessionStatus.SAVED,
                        startedAtEpochMillis = 10_000L,
                    ),
                )
            runningRepository.insertTrackPoints(
                listOf(
                    buildTrackPoint(
                        externalId = "tp-2",
                        sessionId = savedSessionId,
                        sequence = 1,
                        recordedAtEpochMillis = 11_000L,
                    ),
                    buildTrackPoint(
                        externalId = "tp-1",
                        sessionId = savedSessionId,
                        sequence = 0,
                        recordedAtEpochMillis = 10_500L,
                    ),
                ),
            )

            val emissions = exportRepository.exportAllData().toList()

            assertTrue(emissions.last() is ExportProgress.Completed)

            val completed = emissions.last() as ExportProgress.Completed
            val root = json.parseToJsonElement(File(completed.filePath).readText()).jsonObject
            val firstSession = root.getObject("sessions").first().jsonObject
            val firstTrackPoint = firstSession.getObject("track_points").first().jsonObject

            assertEquals(1, root.getValue("schema_version").jsonPrimitive.int)
            assertTrue(root.containsKey("exported_at_epoch_millis"))
            assertTrue(root.containsKey("profile"))
            assertTrue(root.containsKey("app_preferences"))
            assertFalse(root.containsKey("schemaVersion"))
            assertFalse(firstSession.containsKey("id"))
            assertFalse(firstTrackPoint.containsKey("session_id"))
            assertEquals("saved-session", firstSession.getValue("external_id").jsonPrimitive.content)
            assertEquals("tp-1", firstTrackPoint.getValue("external_id").jsonPrimitive.content)
            assertEquals("FEMALE", root.getValue("profile").jsonObject.getValue("gender").jsonPrimitive.content)
        }

    @Test
    fun exportAllDataIncludesSavedSessionsOnly() =
        runTest {
            runningRepository.insertSession(
                buildSession(
                    externalId = "saved-session",
                    status = SessionStatus.SAVED,
                    startedAtEpochMillis = 10_000L,
                ),
            )
            runningRepository.insertSession(
                buildSession(
                    externalId = "in-progress-session",
                    status = SessionStatus.IN_PROGRESS,
                    startedAtEpochMillis = 20_000L,
                ),
            )
            runningRepository.insertSession(
                buildSession(
                    externalId = "discarded-session",
                    status = SessionStatus.DISCARDED,
                    startedAtEpochMillis = 30_000L,
                ),
            )

            val completed = exportRepository.exportAllData().toList().last() as ExportProgress.Completed
            val sessions =
                json
                    .parseToJsonElement(File(completed.filePath).readText())
                    .jsonObject
                    .getObject("sessions")

            assertEquals(1, sessions.size)
            assertEquals("saved-session", sessions.first().jsonObject.getValue("external_id").jsonPrimitive.content)
        }

    @Test
    fun exportAllDataSupportsEmptySavedSessionList() =
        runTest {
            val completed = exportRepository.exportAllData().toList().last() as ExportProgress.Completed
            val root = json.parseToJsonElement(File(completed.filePath).readText()).jsonObject

            assertTrue(root.getObject("sessions").isEmpty())
            assertTrue(File(completed.filePath).exists())
        }

    @Test
    fun exportAllDataDeletesTemporaryFilesWhenCollectorCancels() =
        runTest {
            runningRepository.insertSession(
                buildSession(
                    externalId = "saved-session",
                    status = SessionStatus.SAVED,
                    startedAtEpochMillis = 10_000L,
                ),
            )

            exportRepository.exportAllData().take(1).toList()

            assertFalse(exportFile().exists())
            assertFalse(tempFile().exists())
        }

    private fun buildSession(
        externalId: String,
        status: SessionStatus,
        startedAtEpochMillis: Long,
    ): RunningSession =
        RunningSession(
            externalId = externalId,
            status = status,
            startedAtEpochMillis = startedAtEpochMillis,
            endedAtEpochMillis = startedAtEpochMillis + 600_000L,
            stats =
                RunStats(
                    durationMillis = 600_000L,
                    distanceMeters = 3_000.0,
                    averagePaceSecPerKm = 300.0,
                    maxSpeedMps = 5.2,
                    calorieKcal = 210.0,
                ),
            createdAtEpochMillis = startedAtEpochMillis,
            updatedAtEpochMillis = startedAtEpochMillis + 600_000L,
        )

    private fun buildTrackPoint(
        externalId: String,
        sessionId: Long,
        sequence: Int,
        recordedAtEpochMillis: Long,
    ): TrackPoint =
        TrackPoint(
            externalId = externalId,
            sessionId = sessionId,
            sequence = sequence,
            latitude = 37.5 + sequence,
            longitude = 127.0 + sequence,
            altitudeMeters = 12.0,
            accuracyMeters = 3.0f,
            speedMps = 2.2f,
            recordedAtEpochMillis = recordedAtEpochMillis,
        )

    private fun exportDirectory(): File = File(context.cacheDir, "exports")

    private fun exportFile(): File = File(exportDirectory(), "bokslrunning_export_v1.json")

    private fun tempFile(): File = File(exportDirectory(), "bokslrunning_export_v1.json.tmp")

    private fun JsonObject.getObject(key: String): JsonArray = getValue(key).jsonArray

    private companion object {
        val FIXED_CLOCK: Clock = Clock.fixed(Instant.ofEpochMilli(1_234_567_890L), ZoneOffset.UTC)
    }
}
