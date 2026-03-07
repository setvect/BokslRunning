package com.boksl.running.data.local.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.boksl.running.data.local.db.AppDatabase
import com.boksl.running.data.local.db.entity.RunningSessionEntity
import com.boksl.running.data.local.db.entity.TrackPointEntity
import com.boksl.running.domain.model.SessionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrackPointDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var runningSessionDao: RunningSessionDao
    private lateinit var trackPointDao: TrackPointDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        runningSessionDao = database.runningSessionDao()
        trackPointDao = database.trackPointDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertIgnoreDuplicateSequenceAndReadInOrder() =
        runTest {
            val sessionId =
                runningSessionDao.insert(
                    RunningSessionEntity(
                        externalId = "session-1",
                        status = SessionStatus.IN_PROGRESS,
                        startedAtEpochMillis = 1_000L,
                        endedAtEpochMillis = null,
                        durationMillis = 0L,
                        distanceMeters = 0.0,
                        averagePaceSecPerKm = null,
                        maxSpeedMps = 0.0,
                        calorieKcal = null,
                        createdAtEpochMillis = 1_000L,
                        updatedAtEpochMillis = 1_000L,
                    ),
                )

            val insertResults =
                trackPointDao.insertAll(
                    listOf(
                        trackPointEntity(
                            externalId = "tp-1",
                            sessionId = sessionId,
                            sequence = 0,
                            recordedAtEpochMillis = 1_000L,
                        ),
                        trackPointEntity(
                            externalId = "tp-duplicate-sequence",
                            sessionId = sessionId,
                            sequence = 0,
                            recordedAtEpochMillis = 1_500L,
                        ),
                        trackPointEntity(
                            externalId = "tp-2",
                            sessionId = sessionId,
                            sequence = 1,
                            recordedAtEpochMillis = 2_000L,
                        ),
                    ),
                )

            assertEquals(-1L, insertResults[1])

            val trackPoints = trackPointDao.observeBySessionId(sessionId).first()
            assertEquals(listOf(0, 1), trackPoints.map { it.sequence })
            assertEquals(listOf("tp-1", "tp-2"), trackPoints.map { it.externalId })
        }

    @Test
    fun deletingSessionCascadesTrackPoints() =
        runTest {
            val sessionId =
                runningSessionDao.insert(
                    RunningSessionEntity(
                        externalId = "session-2",
                        status = SessionStatus.IN_PROGRESS,
                        startedAtEpochMillis = 1_000L,
                        endedAtEpochMillis = null,
                        durationMillis = 0L,
                        distanceMeters = 0.0,
                        averagePaceSecPerKm = null,
                        maxSpeedMps = 0.0,
                        calorieKcal = null,
                        createdAtEpochMillis = 1_000L,
                        updatedAtEpochMillis = 1_000L,
                    ),
                )

            trackPointDao.insertAll(
                listOf(
                    trackPointEntity(
                        externalId = "tp-3",
                        sessionId = sessionId,
                        sequence = 0,
                        recordedAtEpochMillis = 1_000L,
                    ),
                ),
            )

            runningSessionDao.deleteById(sessionId)
            val remaining = trackPointDao.getBySessionId(sessionId)

            assertTrue(remaining.isEmpty())
        }

    private fun trackPointEntity(
        externalId: String,
        sessionId: Long,
        sequence: Int,
        recordedAtEpochMillis: Long,
    ): TrackPointEntity =
        TrackPointEntity(
            externalId = externalId,
            sessionId = sessionId,
            sequence = sequence,
            latitude = 37.12,
            longitude = 127.12,
            altitudeMeters = 15.0,
            accuracyMeters = 5.0f,
            speedMps = 3.2f,
            recordedAtEpochMillis = recordedAtEpochMillis,
        )
}
