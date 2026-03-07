package com.boksl.running.data.debug

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.boksl.running.data.local.db.AppDatabase
import com.boksl.running.data.repository.DefaultRunningRepository
import com.boksl.running.core.util.estimateCaloriesKcal
import com.boksl.running.domain.model.AppPreferences
import com.boksl.running.domain.model.Gender
import com.boksl.running.domain.model.Profile
import com.boksl.running.domain.model.RunStats
import com.boksl.running.domain.model.RunningSession
import com.boksl.running.domain.model.SessionStatus
import com.boksl.running.domain.repository.ProfileRepository
import com.boksl.running.domain.repository.RunningRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
class DebugRunSeedGeneratorTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: RunningRepository
    private lateinit var generator: DebugRunSeedGenerator
    private lateinit var profileRepository: FakeSeedProfileRepository

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
        profileRepository = FakeSeedProfileRepository(profile = DEFAULT_PROFILE)
        generator =
            DebugRunSeedGenerator(
                appDatabase = database,
                runningSessionDao = database.runningSessionDao(),
                runningRepository = repository,
                profileRepository = profileRepository,
                ioDispatcher = Dispatchers.IO,
                clock = FIXED_CLOCK,
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun regenerateLastYearCreatesSeedSessionsAndTrackPointsAcrossTwelveMonths() =
        runTest {
            val result = generator.regenerateLastYear()

            val sessions = repository.observeRecentSessions(limit = 500).first().firstSeedSessions()
            val distinctMonths = sessions.map { session -> session.startedAtEpochMillis.toYearMonth() }.distinct()

            assertEquals(result.generatedSessionCount, sessions.size)
            assertEquals(12, distinctMonths.size)
            assertTrue(result.generatedTrackPointCount >= result.generatedSessionCount * 20)
            assertTrue(sessions.all { session -> session.status == SessionStatus.SAVED })
            assertTrue(sessions.all { session -> session.externalId.startsWith(SESSION_EXTERNAL_ID_PREFIX) })

            val firstSession = sessions.first()
            val trackPoints = repository.observeTrackPoints(firstSession.id).first()
            assertTrue(trackPoints.size in 20..60)
            assertTrue(trackPoints.all { point -> point.externalId.startsWith(TRACK_POINT_EXTERNAL_ID_PREFIX) })
        }

    @Test
    fun regenerateLastYearDeletesExistingSeedDataWithoutTouchingRealSessions() =
        runTest {
            repository.insertSession(realSession(externalId = "real-session-1", startedAtEpochMillis = 1_000L))

            generator.regenerateLastYear()
            val initialSeedCount = database.runningSessionDao().getIdsByExternalIdPattern("$SESSION_EXTERNAL_ID_PREFIX-%").size

            val secondRun = generator.regenerateLastYear()
            val sessions = repository.observeRecentSessions(limit = 500).first()
            val realSessions = sessions.filterNot { session -> session.externalId.startsWith(SESSION_EXTERNAL_ID_PREFIX) }
            val seedSessions = sessions.filter { session -> session.externalId.startsWith(SESSION_EXTERNAL_ID_PREFIX) }

            assertEquals(initialSeedCount, secondRun.deletedSessionCount)
            assertEquals(1, realSessions.size)
            assertEquals("real-session-1", realSessions.single().externalId)
            assertEquals(secondRun.generatedSessionCount, seedSessions.size)
        }

    @Test
    fun deleteSeedDataRemovesOnlySeedSessions() =
        runTest {
            repository.insertSession(realSession(externalId = "real-session-2", startedAtEpochMillis = 2_000L))
            generator.regenerateLastYear()

            val deleteResult = generator.deleteSeedData()
            val sessions = repository.observeRecentSessions(limit = 500).first()

            assertTrue(deleteResult.deletedSessionCount > 0)
            assertEquals(1, sessions.size)
            assertFalse(sessions.single().externalId.startsWith(SESSION_EXTERNAL_ID_PREFIX))
        }

    @Test
    fun regenerateLastYearUsesDefaultWeightWhenProfileIsMissing() =
        runTest {
            profileRepository.profile.value = null

            generator.regenerateLastYear()

            val seededSession = repository.observeRecentSessions(limit = 500).first().firstSeedSessions().first()
            val averageSpeedMps = seededSession.stats.distanceMeters / (seededSession.stats.durationMillis / 1_000.0)
            val expectedCalories =
                estimateCaloriesKcal(
                    weightKg = 70f,
                    averageSpeedMps = averageSpeedMps,
                    durationMillis = seededSession.stats.durationMillis,
                )

            assertEquals(expectedCalories, seededSession.stats.calorieKcal ?: 0.0, 0.0001)
        }
}

private fun List<RunningSession>.firstSeedSessions(): List<RunningSession> =
    filter { session -> session.externalId.startsWith(SESSION_EXTERNAL_ID_PREFIX) }

private fun Long.toYearMonth(): YearMonth = YearMonth.from(Instant.ofEpochMilli(this).atZone(TEST_ZONE))

private fun realSession(
    externalId: String,
    startedAtEpochMillis: Long,
): RunningSession =
    RunningSession(
        externalId = externalId,
        status = SessionStatus.SAVED,
        startedAtEpochMillis = startedAtEpochMillis,
        endedAtEpochMillis = startedAtEpochMillis + 1_800_000L,
        stats =
            RunStats(
                durationMillis = 1_800_000L,
                distanceMeters = 5_000.0,
                averagePaceSecPerKm = 360.0,
                maxSpeedMps = 4.0,
                calorieKcal = 320.0,
            ),
        createdAtEpochMillis = startedAtEpochMillis,
        updatedAtEpochMillis = startedAtEpochMillis + 1_800_000L,
    )

private class FakeSeedProfileRepository(
    profile: Profile?,
) : ProfileRepository {
    val profile = MutableStateFlow(profile)

    override fun observeProfile(): Flow<Profile?> = profile

    override fun observeAppPreferences(): Flow<AppPreferences> =
        flowOf(
            AppPreferences(
                onboardingCompleted = true,
                locationRationaleShown = true,
            ),
        )

    override suspend fun saveProfile(profile: Profile) {
        this.profile.value = profile
    }

    override suspend fun clearProfile() {
        profile.value = null
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) = Unit

    override suspend fun setLocationRationaleShown(shown: Boolean) = Unit
}

private val TEST_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
private val FIXED_CLOCK: Clock = Clock.fixed(ZonedDateTime.of(2026, 3, 7, 12, 0, 0, 0, TEST_ZONE).toInstant(), TEST_ZONE)

private val DEFAULT_PROFILE =
    Profile(
        weightKg = 68f,
        gender = Gender.MALE,
        age = 31,
        updatedAtEpochMillis = 0L,
    )
