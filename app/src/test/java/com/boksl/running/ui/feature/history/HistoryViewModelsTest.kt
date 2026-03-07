package com.boksl.running.ui.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import com.boksl.running.MainDispatcherRule
import com.boksl.running.domain.model.AppPreferences
import com.boksl.running.domain.model.Gender
import com.boksl.running.domain.model.HomeSummary
import com.boksl.running.domain.model.Profile
import com.boksl.running.domain.model.RunStats
import com.boksl.running.domain.model.RunningSession
import com.boksl.running.domain.model.SessionStatus
import com.boksl.running.domain.model.TrackPoint
import com.boksl.running.domain.repository.ProfileRepository
import com.boksl.running.domain.repository.RunningRepository
import com.boksl.running.ui.feature.permission.LocationPermissionUiState
import com.boksl.running.ui.navigation.AppRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun historyListPagedItemsShowsSavedItemsAndMapsSummaryFields() =
        runTest {
            val savedNew =
                runningSession(
                    id = 2L,
                    externalId = "saved-new",
                    status = SessionStatus.SAVED,
                    startedAtEpochMillis = 5_000L,
                    stats = runStats(durationMillis = 200_000L, distanceMeters = 1_000.0, averagePace = 200.0),
                )
            val savedOld =
                runningSession(
                    id = 1L,
                    externalId = "saved-old",
                    status = SessionStatus.SAVED,
                    startedAtEpochMillis = 1_000L,
                    stats = runStats(durationMillis = 300_000L, distanceMeters = 2_000.0, averagePace = 150.0),
                )
            val runningRepository =
                FakeRunningRepository(
                    sessions = mutableListOf(savedOld, savedNew),
                    trackPointsBySessionId = emptyMap(),
                )
            val viewModel =
                HistoryListViewModel(
                    runningRepository = runningRepository,
                    profileRepository = FakeProfileRepository(),
                )
            val items = viewModel.pagedItems.asSnapshot()

            assertEquals(listOf(2L, 1L), items.map { it.sessionId })
            assertEquals(1_000.0, items.first().distanceMeters, 0.0)
            assertEquals(200_000L, items.first().durationMillis)
            assertEquals(200.0, items.first().averagePaceSecPerKm ?: -1.0, 0.0)
        }

    @Test
    fun historyListPagedItemsShowsEmptyStateWhenNoSavedSessions() =
        runTest {
            val viewModel =
                HistoryListViewModel(
                    runningRepository = FakeRunningRepository(),
                    profileRepository = FakeProfileRepository(),
                )
            val items = viewModel.pagedItems.asSnapshot()

            assertTrue(items.isEmpty())
        }

    @Test
    fun historyListPermissionDialogStateTransitionsAndDismisses() =
        runTest {
            val profileRepository = FakeProfileRepository()
            val viewModel =
                HistoryListViewModel(
                    runningRepository = FakeRunningRepository(),
                    profileRepository = profileRepository,
                )
            backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.onRunStartRequested(shouldShowRationale = false)
            advanceUntilIdle()
            assertEquals(LocationPermissionUiState.ShowRequest, viewModel.uiState.value.permissionDialogState)
            assertTrue(profileRepository.preferences.value.locationRationaleShown)

            viewModel.dismissPermissionDialog()
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.permissionDialogState)

            viewModel.onRunStartRequested(shouldShowRationale = false)
            advanceUntilIdle()
            assertEquals(LocationPermissionUiState.PermanentlyDenied, viewModel.uiState.value.permissionDialogState)
        }

    @Test
    fun historyDetailUiStateLoadsSavedSessionAndTrackPoints() =
        runTest {
            val session =
                runningSession(
                    id = 9L,
                    externalId = "saved-9",
                    status = SessionStatus.SAVED,
                    startedAtEpochMillis = 2_000L,
                    stats = runStats(durationMillis = 120_000L, distanceMeters = 500.0, averagePace = 240.0),
                )
            val trackPoints =
                listOf(
                    TrackPoint(
                        externalId = "tp-1",
                        sessionId = 9L,
                        sequence = 0,
                        latitude = 37.0,
                        longitude = 127.0,
                        altitudeMeters = null,
                        accuracyMeters = 5f,
                        speedMps = 3f,
                        recordedAtEpochMillis = 2_000L,
                    ),
                    TrackPoint(
                        externalId = "tp-2",
                        sessionId = 9L,
                        sequence = 1,
                        latitude = 37.1,
                        longitude = 127.1,
                        altitudeMeters = null,
                        accuracyMeters = 5f,
                        speedMps = 3f,
                        recordedAtEpochMillis = 3_000L,
                    ),
                )
            val viewModel =
                HistoryDetailViewModel(
                    savedStateHandle = SavedStateHandle(mapOf(AppRoute.HistoryDetail.SESSION_ID_ARG to 9L)),
                    runningRepository =
                        FakeRunningRepository(
                            sessions = mutableListOf(session),
                            trackPointsBySessionId = mapOf(9L to trackPoints),
                        ),
                )
            backgroundScope.launch { viewModel.uiState.collect {} }

            advanceUntilIdle()

            val uiState = viewModel.uiState.value
            assertFalse(uiState.isLoading)
            assertFalse(uiState.isNotFound)
            assertEquals(9L, uiState.session?.id)
            assertEquals(2, uiState.trackPoints.size)
        }

    @Test
    fun historyDetailUiStateTreatsMissingOrUnsavedSessionAsNotFound() =
        runTest {
            val unsavedSession =
                runningSession(
                    id = 3L,
                    externalId = "running-3",
                    status = SessionStatus.IN_PROGRESS,
                    startedAtEpochMillis = 1_000L,
                    stats = runStats(durationMillis = 0L, distanceMeters = 0.0, averagePace = null),
                )

            val unsavedViewModel =
                HistoryDetailViewModel(
                    savedStateHandle = SavedStateHandle(mapOf(AppRoute.HistoryDetail.SESSION_ID_ARG to 3L)),
                    runningRepository =
                        FakeRunningRepository(
                            sessions = mutableListOf(unsavedSession),
                            trackPointsBySessionId = emptyMap(),
                        ),
                )
            backgroundScope.launch { unsavedViewModel.uiState.collect {} }

            advanceUntilIdle()

            assertTrue(unsavedViewModel.uiState.value.isNotFound)
            assertTrue(unsavedViewModel.uiState.value.trackPoints.isEmpty())

            val missingViewModel =
                HistoryDetailViewModel(
                    savedStateHandle = SavedStateHandle(mapOf(AppRoute.HistoryDetail.SESSION_ID_ARG to 99L)),
                    runningRepository = FakeRunningRepository(),
                )
            backgroundScope.launch { missingViewModel.uiState.collect {} }

            advanceUntilIdle()

            assertTrue(missingViewModel.uiState.value.isNotFound)
        }
}

private class FakeRunningRepository(
    sessions: MutableList<RunningSession> = mutableListOf(),
    trackPointsBySessionId: Map<Long, List<TrackPoint>> = emptyMap(),
) : RunningRepository {
    private val sessionsState = MutableStateFlow(sessions)
    private val trackPointsState = MutableStateFlow(trackPointsBySessionId)

    override fun observeHomeSummary(): Flow<HomeSummary> = flowOf(HomeSummary(0.0, 0L, 0.0, 0.0))

    override fun observeSession(sessionId: Long): Flow<RunningSession?> =
        flowOf(sessionsState.value.firstOrNull { it.id == sessionId })

    override fun observeSavedSessionsPaged(): Flow<PagingData<RunningSession>> =
        flowOf(
            PagingData.from(
                sessionsState.value
                    .filter { it.status == SessionStatus.SAVED }
                    .sortedByDescending { it.startedAtEpochMillis },
            ),
        )

    override fun observeRecentSessions(limit: Int): Flow<List<RunningSession>> =
        flowOf(sessionsState.value.sortedByDescending { it.startedAtEpochMillis }.take(limit))

    override fun observeTrackPoints(sessionId: Long): Flow<List<TrackPoint>> =
        flowOf(trackPointsState.value[sessionId].orEmpty())

    override suspend fun getSession(sessionId: Long): RunningSession? =
        sessionsState.value.firstOrNull { it.id == sessionId }

    override suspend fun getActiveSession(): RunningSession? =
        sessionsState.value.firstOrNull { it.status == SessionStatus.IN_PROGRESS }

    override suspend fun insertSession(session: RunningSession): Long = error("unused")

    override suspend fun updateSession(session: RunningSession) = Unit

    override suspend fun insertTrackPoints(points: List<TrackPoint>) = Unit

    override suspend fun deleteSession(sessionId: Long) = Unit
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
    val preferences = MutableStateFlow(AppPreferences(onboardingCompleted = true, locationRationaleShown = false))

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

private fun runningSession(
    id: Long,
    externalId: String,
    status: SessionStatus,
    startedAtEpochMillis: Long,
    stats: RunStats,
): RunningSession =
    RunningSession(
        id = id,
        externalId = externalId,
        status = status,
        startedAtEpochMillis = startedAtEpochMillis,
        endedAtEpochMillis = if (status == SessionStatus.SAVED) startedAtEpochMillis + stats.durationMillis else null,
        stats = stats,
        createdAtEpochMillis = startedAtEpochMillis,
        updatedAtEpochMillis = startedAtEpochMillis + stats.durationMillis,
    )

private fun runStats(
    durationMillis: Long,
    distanceMeters: Double,
    averagePace: Double?,
): RunStats =
    RunStats(
        durationMillis = durationMillis,
        distanceMeters = distanceMeters,
        averagePaceSecPerKm = averagePace,
        maxSpeedMps = 4.2,
        calorieKcal = 88.0,
    )
