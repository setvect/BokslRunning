package com.boksl.running.data.local.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.boksl.running.data.local.db.AppDatabase
import com.boksl.running.data.local.db.entity.RunningSessionEntity
import com.boksl.running.domain.model.SessionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RunningSessionDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var runningSessionDao: RunningSessionDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        runningSessionDao = database.runningSessionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun sessionCrudWorks() =
        runTest {
            val insertedId =
                runningSessionDao.insert(
                    runningSessionEntity(
                        externalId = "session-1",
                        status = SessionStatus.IN_PROGRESS,
                        startedAtEpochMillis = 1_000L,
                    ),
                )

            val observed = runningSessionDao.observeById(insertedId).first()
            assertNotNull(observed)

            val inserted = runningSessionDao.getById(insertedId)
            assertNotNull(inserted)
            assertEquals("session-1", inserted?.externalId)

            runningSessionDao.update(
                inserted!!.copy(
                    status = SessionStatus.SAVED,
                    endedAtEpochMillis = 3_000L,
                    durationMillis = 2_000L,
                    distanceMeters = 1_500.0,
                    averagePaceSecPerKm = 300.0,
                    maxSpeedMps = 5.0,
                    calorieKcal = 120.0,
                    updatedAtEpochMillis = 4_000L,
                ),
            )

            val updated = runningSessionDao.getById(insertedId)
            assertEquals(SessionStatus.SAVED, updated?.status)
            assertEquals(2_000L, updated?.durationMillis)

            runningSessionDao.deleteById(insertedId)
            assertNull(runningSessionDao.getById(insertedId))
        }

    @Test
    fun getLatestInProgressSessionReturnsMostRecent() =
        runTest {
            runningSessionDao.insert(
                runningSessionEntity(
                    externalId = "saved-1",
                    status = SessionStatus.SAVED,
                    startedAtEpochMillis = 1_000L,
                ),
            )
            runningSessionDao.insert(
                runningSessionEntity(
                    externalId = "running-old",
                    status = SessionStatus.IN_PROGRESS,
                    startedAtEpochMillis = 2_000L,
                ),
            )
            runningSessionDao.insert(
                runningSessionEntity(
                    externalId = "running-new",
                    status = SessionStatus.IN_PROGRESS,
                    startedAtEpochMillis = 3_000L,
                ),
            )

            val activeSession = runningSessionDao.getLatestByStatus(SessionStatus.IN_PROGRESS)
            assertEquals("running-new", activeSession?.externalId)
        }

    @Test
    fun observeRecentReturnsStartedAtDescending() =
        runTest {
            runningSessionDao.insert(
                runningSessionEntity(
                    externalId = "session-a",
                    status = SessionStatus.SAVED,
                    startedAtEpochMillis = 1_000L,
                ),
            )
            runningSessionDao.insert(
                runningSessionEntity(
                    externalId = "session-b",
                    status = SessionStatus.SAVED,
                    startedAtEpochMillis = 5_000L,
                ),
            )
            runningSessionDao.insert(
                runningSessionEntity(
                    externalId = "session-c",
                    status = SessionStatus.SAVED,
                    startedAtEpochMillis = 3_000L,
                ),
            )

            val recent = runningSessionDao.observeRecent(limit = 2).first()
            assertEquals(listOf("session-b", "session-c"), recent.map { it.externalId })
        }

    private fun runningSessionEntity(
        externalId: String,
        status: SessionStatus,
        startedAtEpochMillis: Long,
    ): RunningSessionEntity =
        RunningSessionEntity(
            externalId = externalId,
            status = status,
            startedAtEpochMillis = startedAtEpochMillis,
            endedAtEpochMillis = null,
            durationMillis = 0L,
            distanceMeters = 0.0,
            averagePaceSecPerKm = null,
            maxSpeedMps = 0.0,
            calorieKcal = null,
            createdAtEpochMillis = startedAtEpochMillis,
            updatedAtEpochMillis = startedAtEpochMillis,
        )
}
