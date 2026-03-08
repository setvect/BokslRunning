@file:Suppress("LongMethod")

package com.boksl.running.data.repository

import androidx.paging.PagingData
import com.boksl.running.data.location.LocationClient
import com.boksl.running.data.location.LocationRequestConfig
import com.boksl.running.domain.model.AppPreferences
import com.boksl.running.domain.model.Gender
import com.boksl.running.domain.model.HomeSummary
import com.boksl.running.domain.model.LocationSample
import com.boksl.running.domain.model.MonthlyStatsPoint
import com.boksl.running.domain.model.Profile
import com.boksl.running.domain.model.RunEngineState
import com.boksl.running.domain.model.RunStats
import com.boksl.running.domain.model.RunningSession
import com.boksl.running.domain.model.SessionStatus
import com.boksl.running.domain.model.TrackPoint
import com.boksl.running.domain.repository.ProfileRepository
import com.boksl.running.domain.repository.RunningRepository
import com.boksl.running.ui.feature.run.RunTrackingServiceController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultRunEngineRepositoryTest {
    @Test
    fun startStopAndSaveRunUpdatesStateAndPersistsTrackPoints() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val runningRepository = FakeRunningRepository()
            val profileRepository = FakeProfileRepository()
            val locationClient = FakeLocationClient()
            val serviceController = FakeRunTrackingServiceController()
            val repository =
                DefaultRunEngineRepository(
                    runningRepository = runningRepository,
                    profileRepository = profileRepository,
                    locationClient = locationClient,
                    serviceController = serviceController,
                    ioDispatcher = dispatcher,
                )

            repository.prepareRun()
            repository.startRun()
            repository.onLocationSample(locationSample(37.0, 127.0, 1_000L))
            repository.onLocationSample(locationSample(37.0009, 127.0, 7_000L))
            repository.requestStop()
            repository.confirmSave()
            advanceUntilIdle()

            val snapshot = repository.observeActiveRun().first()
            assertEquals(RunEngineState.SAVED, snapshot?.state)
            assertEquals(3, runningRepository.savedTrackPoints.values.first().size)
            assertEquals(SessionStatus.SAVED, runningRepository.sessions.values.first().status)
            assertTrue(serviceController.startCount >= 1)
            assertTrue(serviceController.stopCount >= 1)
            repository.shutdown()
        }

    @Test
    fun resumeActiveRunRecoversStoredSession() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val runningRepository =
                FakeRunningRepository(
                    initialSessions =
                        mutableMapOf(
                            11L to
                                RunningSession(
                                    id = 11L,
                                    externalId = "active-session",
                                    status = SessionStatus.IN_PROGRESS,
                                    startedAtEpochMillis = 1_000L,
                                    endedAtEpochMillis = null,
                                    stats =
                                        RunStats(
                                            durationMillis = 10_000L,
                                            distanceMeters = 120.0,
                                            averagePaceSecPerKm = 300.0,
                                            maxSpeedMps = 4.0,
                                            calorieKcal = 14.0,
                                        ),
                                    createdAtEpochMillis = 1_000L,
                                    updatedAtEpochMillis = 11_000L,
                                ),
                        ),
                    initialTrackPoints =
                        mutableMapOf(
                            11L to
                                mutableListOf(
                                    TrackPoint(
                                        externalId = "tp-1",
                                        sessionId = 11L,
                                        sequence = 0,
                                        latitude = 37.0,
                                        longitude = 127.0,
                                        altitudeMeters = null,
                                        accuracyMeters = 4f,
                                        speedMps = 3f,
                                        recordedAtEpochMillis = 1_000L,
                                    ),
                                ),
                        ),
                )
            val repository =
                DefaultRunEngineRepository(
                    runningRepository = runningRepository,
                    profileRepository = FakeProfileRepository(),
                    locationClient = FakeLocationClient(),
                    serviceController = FakeRunTrackingServiceController(),
                    ioDispatcher = dispatcher,
                )

            repository.resumeActiveRun()
            runCurrent()

            val snapshot = repository.observeActiveRun().first()
            assertNotNull(snapshot)
            assertEquals(RunEngineState.RUNNING, snapshot?.state)
            assertEquals(11L, snapshot?.sessionId)
            assertEquals(1, snapshot?.pointCount)
            repository.shutdown()
        }

    private fun locationSample(
        latitude: Double,
        longitude: Double,
        time: Long,
    ): LocationSample =
        LocationSample(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = 5f,
            speedMps = 3f,
            altitudeMeters = null,
            recordedAtEpochMillis = time,
        )
}

private class FakeRunningRepository(
    initialSessions: MutableMap<Long, RunningSession> = mutableMapOf(),
    initialTrackPoints: MutableMap<Long, MutableList<TrackPoint>> = mutableMapOf(),
) : RunningRepository {
    val sessions = initialSessions
    val savedTrackPoints = initialTrackPoints
    private var nextSessionId = (sessions.keys.maxOrNull() ?: 0L) + 1L

    override fun observeHomeSummary(): Flow<HomeSummary> = flowOf(HomeSummary(0.0, 0L, 0.0, 0.0))

    override fun observeMonthlyStats(): Flow<List<MonthlyStatsPoint>> = flowOf(emptyList())

    override fun observeSession(sessionId: Long): Flow<RunningSession?> = flowOf(sessions[sessionId])

    override fun observeSavedSessionsPaged(): Flow<PagingData<RunningSession>> =
        flowOf(
            PagingData.from(
                sessions.values
                    .filter { it.status == SessionStatus.SAVED }
                    .sortedByDescending { it.startedAtEpochMillis },
            ),
        )

    override fun observeRecentSessions(limit: Int): Flow<List<RunningSession>> = flowOf(sessions.values.toList())

    override fun observeTrackPoints(sessionId: Long): Flow<List<TrackPoint>> =
        flowOf(savedTrackPoints[sessionId]?.toList().orEmpty())

    override suspend fun getSession(sessionId: Long): RunningSession? = sessions[sessionId]

    override suspend fun getActiveSession(): RunningSession? =
        sessions.values.firstOrNull { it.status == SessionStatus.IN_PROGRESS }

    override suspend fun insertSession(session: RunningSession): Long {
        val id = nextSessionId++
        sessions[id] = session.copy(id = id)
        return id
    }

    override suspend fun updateSession(session: RunningSession) {
        sessions[session.id] = session
    }

    override suspend fun insertTrackPoints(points: List<TrackPoint>) {
        points.groupBy { it.sessionId }.forEach { (sessionId, sessionPoints) ->
            val existing = savedTrackPoints.getOrPut(sessionId) { mutableListOf() }
            existing.addAll(sessionPoints)
        }
    }

    override suspend fun deleteSession(sessionId: Long) {
        sessions.remove(sessionId)
        savedTrackPoints.remove(sessionId)
    }
}

private class FakeProfileRepository : ProfileRepository {
    private val profile: MutableStateFlow<Profile?> =
        MutableStateFlow(
            Profile(
                weightKg = 70f,
                gender = Gender.MALE,
                age = 30,
                updatedAtEpochMillis = 1_000L,
            ),
        )
    private val preferences =
        MutableStateFlow(AppPreferences(onboardingCompleted = true, locationRationaleShown = true))

    override fun observeProfile(): Flow<Profile?> = profile

    override fun observeAppPreferences(): Flow<AppPreferences> = preferences

    override suspend fun saveProfile(profile: Profile) {
        this.profile.value = profile
    }

    override suspend fun clearProfile() {
        profile.value = null
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        preferences.value = preferences.value.copy(onboardingCompleted = completed)
    }

    override suspend fun setLocationRationaleShown(shown: Boolean) {
        preferences.value = preferences.value.copy(locationRationaleShown = shown)
    }
}

private class FakeLocationClient : LocationClient {
    private val updates = MutableSharedFlow<LocationSample>()
    var currentLocation: LocationSample? =
        LocationSample(
            latitude = 37.0,
            longitude = 127.0,
            accuracyMeters = 5f,
            speedMps = 3f,
            altitudeMeters = null,
            recordedAtEpochMillis = 1_000L,
        )

    override fun observeLocationUpdates(config: LocationRequestConfig): Flow<LocationSample> = updates

    override suspend fun getCurrentLocation(): LocationSample? = currentLocation
}

private class FakeRunTrackingServiceController : RunTrackingServiceController {
    var startCount = 0
    var stopCount = 0

    override fun startTracking() {
        startCount += 1
    }

    override fun stopTracking() {
        stopCount += 1
    }
}
