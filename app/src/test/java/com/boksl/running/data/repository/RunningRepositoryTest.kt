package com.boksl.running.data.repository

import android.content.Context
import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.boksl.running.data.local.db.AppDatabase
import com.boksl.running.domain.model.MonthlyStatsPoint
import com.boksl.running.domain.model.RunStats
import com.boksl.running.domain.model.RunningSession
import com.boksl.running.domain.model.SessionStatus
import com.boksl.running.domain.model.TrackPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
class RunningRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: DefaultRunningRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository =
            DefaultRunningRepository(
                runningSessionDao = database.runningSessionDao(),
                trackPointDao = database.trackPointDao(),
                ioDispatcher = Dispatchers.IO,
                clock = FIXED_CLOCK,
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndReadSessionThroughRepository() =
        runTest {
            val sessionId =
                repository.insertSession(
                    RunningSession(
                        externalId = "session-ext-1",
                        status = SessionStatus.IN_PROGRESS,
                        startedAtEpochMillis = 1_000L,
                        endedAtEpochMillis = null,
                        stats =
                            RunStats(
                                durationMillis = 0L,
                                distanceMeters = 0.0,
                                averagePaceSecPerKm = null,
                                maxSpeedMps = 0.0,
                                calorieKcal = null,
                            ),
                        createdAtEpochMillis = 1_000L,
                        updatedAtEpochMillis = 1_000L,
                    ),
                )

            val loaded = repository.getSession(sessionId)
            assertNotNull(loaded)
            assertEquals("session-ext-1", loaded?.externalId)
            assertEquals(0.0, loaded?.stats?.distanceMeters)

            repository.updateSession(
                loaded!!.copy(
                    status = SessionStatus.SAVED,
                    endedAtEpochMillis = 3_000L,
                    stats =
                        loaded.stats.copy(
                            durationMillis = 2_000L,
                            distanceMeters = 1_234.5,
                            averagePaceSecPerKm = 320.0,
                            maxSpeedMps = 5.1,
                            calorieKcal = 98.2,
                        ),
                    updatedAtEpochMillis = 3_000L,
                ),
            )

            val updated = repository.getSession(sessionId)
            assertEquals(SessionStatus.SAVED, updated?.status)
            assertEquals(1_234.5, updated?.stats?.distanceMeters)
        }

    @Test
    fun insertSessionGeneratesUuidWhenExternalIdBlank() =
        runTest {
            val sessionId =
                repository.insertSession(
                    RunningSession(
                        externalId = "",
                        status = SessionStatus.IN_PROGRESS,
                        startedAtEpochMillis = 1_000L,
                        endedAtEpochMillis = null,
                        stats =
                            RunStats(
                                durationMillis = 0L,
                                distanceMeters = 0.0,
                                averagePaceSecPerKm = null,
                                maxSpeedMps = 0.0,
                                calorieKcal = null,
                            ),
                        createdAtEpochMillis = 1_000L,
                        updatedAtEpochMillis = 1_000L,
                    ),
                )

            val loaded = repository.getSession(sessionId)
            assertTrue(loaded?.externalId?.isNotBlank() == true)
        }

    @Test
    fun trackPointCrudAndDeleteSessionWorks() =
        runTest {
            val sessionId =
                repository.insertSession(
                    RunningSession(
                        externalId = "session-ext-2",
                        status = SessionStatus.IN_PROGRESS,
                        startedAtEpochMillis = 2_000L,
                        endedAtEpochMillis = null,
                        stats =
                            RunStats(
                                durationMillis = 0L,
                                distanceMeters = 0.0,
                                averagePaceSecPerKm = null,
                                maxSpeedMps = 0.0,
                                calorieKcal = null,
                            ),
                        createdAtEpochMillis = 2_000L,
                        updatedAtEpochMillis = 2_000L,
                    ),
                )

            repository.insertTrackPoints(
                listOf(
                    TrackPoint(
                        externalId = "",
                        sessionId = sessionId,
                        sequence = 0,
                        latitude = 37.0,
                        longitude = 127.0,
                        altitudeMeters = null,
                        accuracyMeters = 3.0f,
                        speedMps = 2.1f,
                        recordedAtEpochMillis = 2_000L,
                    ),
                    TrackPoint(
                        externalId = "tp-explicit",
                        sessionId = sessionId,
                        sequence = 1,
                        latitude = 37.1,
                        longitude = 127.1,
                        altitudeMeters = 12.0,
                        accuracyMeters = 2.5f,
                        speedMps = 2.3f,
                        recordedAtEpochMillis = 2_500L,
                    ),
                ),
            )

            val points = repository.observeTrackPoints(sessionId).first()
            assertEquals(2, points.size)
            assertTrue(points[0].externalId.isNotBlank())
            assertEquals("tp-explicit", points[1].externalId)

            repository.deleteSession(sessionId)
            assertNull(repository.getSession(sessionId))
            assertTrue(repository.observeTrackPoints(sessionId).first().isEmpty())
        }

    @Test
    fun observeHomeSummaryCalculatesTotalsAndAverageSpeed() =
        runTest {
            repository.insertSession(
                RunningSession(
                    externalId = "saved-1",
                    status = SessionStatus.SAVED,
                    startedAtEpochMillis = 1_000L,
                    endedAtEpochMillis = 601_000L,
                    stats =
                        RunStats(
                            durationMillis = 600_000L,
                            distanceMeters = 2_000.0,
                            averagePaceSecPerKm = 300.0,
                            maxSpeedMps = 5.0,
                            calorieKcal = 120.0,
                        ),
                    createdAtEpochMillis = 1_000L,
                    updatedAtEpochMillis = 601_000L,
                ),
            )
            repository.insertSession(
                RunningSession(
                    externalId = "saved-2",
                    status = SessionStatus.SAVED,
                    startedAtEpochMillis = 2_000L,
                    endedAtEpochMillis = 302_000L,
                    stats =
                        RunStats(
                            durationMillis = 300_000L,
                            distanceMeters = 1_000.0,
                            averagePaceSecPerKm = 300.0,
                            maxSpeedMps = 4.0,
                            calorieKcal = null,
                        ),
                    createdAtEpochMillis = 2_000L,
                    updatedAtEpochMillis = 302_000L,
                ),
            )
            repository.insertSession(
                RunningSession(
                    externalId = "discarded",
                    status = SessionStatus.DISCARDED,
                    startedAtEpochMillis = 3_000L,
                    endedAtEpochMillis = 603_000L,
                    stats =
                        RunStats(
                            durationMillis = 600_000L,
                            distanceMeters = 5_000.0,
                            averagePaceSecPerKm = 120.0,
                            maxSpeedMps = 8.0,
                            calorieKcal = 500.0,
                        ),
                    createdAtEpochMillis = 3_000L,
                    updatedAtEpochMillis = 603_000L,
                ),
            )

            val summary = repository.observeHomeSummary().first()
            assertEquals(3_000.0, summary.totalDistanceMeters, 0.0)
            assertEquals(900_000L, summary.totalDurationMillis)
            assertEquals(3.3333333333333335, summary.averageSpeedMps, 0.000001)
            assertEquals(120.0, summary.totalCaloriesKcal, 0.0)
        }

    @Test
    fun observeSavedSessionsPagedReturnsSavedOnlyInStartedAtDescendingOrder() =
        runTest {
            repository.insertSession(
                RunningSession(
                    externalId = "saved-old",
                    status = SessionStatus.SAVED,
                    startedAtEpochMillis = 1_000L,
                    endedAtEpochMillis = 301_000L,
                    stats = RunStats(300_000L, 1_000.0, 300.0, 4.0, 90.0),
                    createdAtEpochMillis = 1_000L,
                    updatedAtEpochMillis = 301_000L,
                ),
            )
            repository.insertSession(
                RunningSession(
                    externalId = "in-progress",
                    status = SessionStatus.IN_PROGRESS,
                    startedAtEpochMillis = 8_000L,
                    endedAtEpochMillis = null,
                    stats = RunStats(0L, 0.0, null, 0.0, null),
                    createdAtEpochMillis = 8_000L,
                    updatedAtEpochMillis = 8_000L,
                ),
            )
            repository.insertSession(
                RunningSession(
                    externalId = "saved-new",
                    status = SessionStatus.SAVED,
                    startedAtEpochMillis = 5_000L,
                    endedAtEpochMillis = 205_000L,
                    stats = RunStats(200_000L, 800.0, 250.0, 4.5, 70.0),
                    createdAtEpochMillis = 5_000L,
                    updatedAtEpochMillis = 205_000L,
                ),
            )

            val savedSessions = repository.observeSavedSessionsPaged().asSnapshot()

            assertEquals(listOf("saved-new", "saved-old"), savedSessions.map { it.externalId })
        }

    @Test
    fun observeMonthlyStatsIncludesRecentMonthsAndFillsMissingBuckets() =
        runTest {
            repository.insertSession(
                savedSession(
                    externalId = "oct",
                    startedAtEpochMillis = localEpochMillis(year = 2025, month = 10, dayOfMonth = 12),
                    durationMillis = 600_000L,
                    distanceMeters = 2_400.0,
                ),
            )
            repository.insertSession(
                savedSession(
                    externalId = "jan",
                    startedAtEpochMillis = localEpochMillis(year = 2026, month = 1, dayOfMonth = 8),
                    durationMillis = 300_000L,
                    distanceMeters = 900.0,
                ),
            )
            repository.insertSession(
                savedSession(
                    externalId = "feb-a",
                    startedAtEpochMillis = localEpochMillis(year = 2026, month = 2, dayOfMonth = 5),
                    durationMillis = 600_000L,
                    distanceMeters = 1_800.0,
                ),
            )
            repository.insertSession(
                savedSession(
                    externalId = "feb-b",
                    startedAtEpochMillis = localEpochMillis(year = 2026, month = 2, dayOfMonth = 17),
                    durationMillis = 300_000L,
                    distanceMeters = 1_200.0,
                ),
            )
            repository.insertSession(
                RunningSession(
                    externalId = "discarded-feb",
                    status = SessionStatus.DISCARDED,
                    startedAtEpochMillis = localEpochMillis(year = 2026, month = 2, dayOfMonth = 19),
                    endedAtEpochMillis = localEpochMillis(year = 2026, month = 2, dayOfMonth = 19) + 600_000L,
                    stats = RunStats(600_000L, 3_000.0, 200.0, 5.0, null),
                    createdAtEpochMillis = localEpochMillis(year = 2026, month = 2, dayOfMonth = 19),
                    updatedAtEpochMillis = localEpochMillis(year = 2026, month = 2, dayOfMonth = 19) + 600_000L,
                ),
            )

            val monthlyStats = repository.observeMonthlyStats(monthCount = 6).first()

            assertEquals(
                listOf(
                    YearMonth.of(2025, 10),
                    YearMonth.of(2025, 11),
                    YearMonth.of(2025, 12),
                    YearMonth.of(2026, 1),
                    YearMonth.of(2026, 2),
                    YearMonth.of(2026, 3),
                ),
                monthlyStats.map(MonthlyStatsPoint::yearMonth),
            )
            assertEquals(0.0, monthlyStats[1].totalDistanceMeters, 0.0)
            assertEquals(0L, monthlyStats[1].totalDurationMillis)
            assertEquals(0.0, monthlyStats[1].averageSpeedMps, 0.0)
            assertEquals(3_000.0, monthlyStats[4].totalDistanceMeters, 0.0)
            assertEquals(900_000L, monthlyStats[4].totalDurationMillis)
            assertEquals(3.3333333333333335, monthlyStats[4].averageSpeedMps, 0.000001)
            assertEquals(0.0, monthlyStats[5].totalDistanceMeters, 0.0)
        }

    @Test
    fun observeMonthlyStatsRejectsNonPositiveMonthCount() =
        runTest {
            try {
                repository.observeMonthlyStats(monthCount = 0)
            } catch (exception: IllegalArgumentException) {
                assertEquals("monthCount must be > 0.", exception.message)
                return@runTest
            }

            error("Expected IllegalArgumentException")
        }

    private fun savedSession(
        externalId: String,
        startedAtEpochMillis: Long,
        durationMillis: Long,
        distanceMeters: Double,
    ): RunningSession =
        RunningSession(
            externalId = externalId,
            status = SessionStatus.SAVED,
            startedAtEpochMillis = startedAtEpochMillis,
            endedAtEpochMillis = startedAtEpochMillis + durationMillis,
            stats = RunStats(durationMillis, distanceMeters, 300.0, 4.0, null),
            createdAtEpochMillis = startedAtEpochMillis,
            updatedAtEpochMillis = startedAtEpochMillis + durationMillis,
        )

    private fun localEpochMillis(
        year: Int,
        month: Int,
        dayOfMonth: Int,
    ): Long =
        ZonedDateTime
            .of(year, month, dayOfMonth, 12, 0, 0, 0, TEST_ZONE_ID)
            .toInstant()
            .toEpochMilli()

    private companion object {
        val TEST_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        val FIXED_CLOCK: Clock = Clock.fixed(Instant.parse("2026-03-07T00:00:00Z"), ZoneOffset.UTC)
    }
}
