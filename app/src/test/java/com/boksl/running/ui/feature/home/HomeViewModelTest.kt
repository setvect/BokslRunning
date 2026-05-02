package com.boksl.running.ui.feature.home

import com.boksl.running.MainDispatcherRule
import com.boksl.running.domain.model.AppPreferences
import com.boksl.running.domain.model.Gender
import com.boksl.running.domain.model.HomeSummary
import com.boksl.running.domain.model.HomeStatsPeriod
import com.boksl.running.domain.model.MonthlyStatsPoint
import com.boksl.running.domain.model.Profile
import com.boksl.running.domain.model.RunningSession
import com.boksl.running.domain.model.TrackPoint
import com.boksl.running.domain.repository.ProfileRepository
import com.boksl.running.domain.repository.RunningRepository
import com.boksl.running.ui.feature.permission.LocationPermissionUiState
import com.boksl.running.ui.feature.permission.PermissionReturnAction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun uiStateUsesThisMonthAsDefaultStatsPeriod() =
        runTest {
            val viewModel =
                HomeViewModel(
                    runningRepository =
                        FakeRunningRepository(
                            summaries =
                                mapOf(
                                    HomeStatsPeriod.THIS_MONTH to HomeSummary(1_200.0, 240_000L, 5.0, 90.0),
                                    HomeStatsPeriod.THIS_YEAR to HomeSummary(4_500.0, 1_200_000L, 3.75, 260.0),
                                    HomeStatsPeriod.ALL_TIME to HomeSummary(8_000.0, 2_400_000L, 3.3333333333, 500.0),
                                ),
                        ),
                    profileRepository = FakeProfileRepository(),
                )
            backgroundScope.launch { viewModel.uiState.collect {} }

            advanceUntilIdle()

            val uiState = viewModel.uiState.value
            assertEquals(HomeStatsPeriod.THIS_MONTH, uiState.selectedStatsPeriod)
            assertEquals(1_200.0, uiState.totalDistanceMeters, 0.0)
            assertEquals(240_000L, uiState.totalDurationMillis)
            assertEquals(5.0, uiState.averageSpeedMps, 0.0)
            assertEquals(90.0, uiState.totalCaloriesKcal, 0.0)
        }

    @Test
    fun onStatsPeriodSelectedUpdatesDisplayedSummary() =
        runTest {
            val viewModel =
                HomeViewModel(
                    runningRepository =
                        FakeRunningRepository(
                            summaries =
                                mapOf(
                                    HomeStatsPeriod.THIS_MONTH to HomeSummary(1_200.0, 240_000L, 5.0, 90.0),
                                    HomeStatsPeriod.THIS_YEAR to HomeSummary(4_500.0, 1_200_000L, 3.75, 260.0),
                                    HomeStatsPeriod.ALL_TIME to HomeSummary(8_000.0, 2_400_000L, 3.3333333333, 500.0),
                                ),
                        ),
                    profileRepository = FakeProfileRepository(),
                )
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.onStatsPeriodSelected(HomeStatsPeriod.THIS_YEAR)
            advanceUntilIdle()

            val uiState = viewModel.uiState.value
            assertEquals(HomeStatsPeriod.THIS_YEAR, uiState.selectedStatsPeriod)
            assertEquals(4_500.0, uiState.totalDistanceMeters, 0.0)
            assertEquals(1_200_000L, uiState.totalDurationMillis)
            assertEquals(3.75, uiState.averageSpeedMps, 0.0)
            assertEquals(260.0, uiState.totalCaloriesKcal, 0.0)
        }

    @Test
    fun onRunStartRequestedShowsPermissionDialog() =
        runTest {
            val viewModel =
                HomeViewModel(
                    runningRepository = FakeRunningRepository(),
                    profileRepository = FakeProfileRepository(),
                )
            backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.onRunStartRequested(shouldShowRationale = false)
            advanceUntilIdle()

            assertEquals(LocationPermissionUiState.ShowRequest, viewModel.uiState.value.permissionDialogState)
        }

    @Test
    fun permissionSettingsReturnNavigatesToRunReadyWhenPermissionGranted() =
        runTest {
            val viewModel =
                HomeViewModel(
                    runningRepository = FakeRunningRepository(),
                    profileRepository = FakeProfileRepository(),
                )
            backgroundScope.launch { viewModel.uiState.collect {} }
            val eventDeferred = async { viewModel.event.first() }

            viewModel.onOpenAppSettingsRequested()
            advanceUntilIdle()
            assertEquals(PermissionReturnAction.StartRun, viewModel.uiState.value.permissionReturnAction)
            assertNull(viewModel.uiState.value.permissionDialogState)

            viewModel.onPermissionSettingsResult(hasPermission = true)
            advanceUntilIdle()

            assertEquals(HomeEvent.NavigateToRunReady, eventDeferred.await())
            assertEquals(PermissionReturnAction.None, viewModel.uiState.value.permissionReturnAction)
        }

    @Test
    fun permissionSettingsReturnDoesNothingWhenPermissionStillDenied() =
        runTest {
            val viewModel =
                HomeViewModel(
                    runningRepository = FakeRunningRepository(),
                    profileRepository = FakeProfileRepository(),
                )
            backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.onOpenAppSettingsRequested()
            viewModel.onPermissionSettingsResult(hasPermission = false)
            advanceUntilIdle()

            assertEquals(PermissionReturnAction.None, viewModel.uiState.value.permissionReturnAction)
        }
}

private class FakeRunningRepository(
    summaries: Map<HomeStatsPeriod, HomeSummary> =
        mapOf(
            HomeStatsPeriod.THIS_MONTH to HomeSummary(0.0, 0L, 0.0, 0.0),
            HomeStatsPeriod.THIS_YEAR to HomeSummary(0.0, 0L, 0.0, 0.0),
            HomeStatsPeriod.ALL_TIME to HomeSummary(0.0, 0L, 0.0, 0.0),
        ),
) : RunningRepository {
    private val summariesState = MutableStateFlow(summaries)

    override fun observeHomeSummary(period: HomeStatsPeriod): Flow<HomeSummary> =
        summariesState.map { summariesByPeriod ->
            summariesByPeriod[period] ?: HomeSummary(0.0, 0L, 0.0, 0.0)
        }

    override fun observeMonthlyStats(): Flow<List<MonthlyStatsPoint>> = flowOf(emptyList())

    override fun observeSavedSessionCount(): Flow<Int> = flowOf(0)

    override fun observeSession(sessionId: Long): Flow<RunningSession?> = flowOf(null)

    override fun observeSavedSessionsPaged() = flowOf(androidx.paging.PagingData.empty<RunningSession>())

    override fun observeRecentSessions(limit: Int): Flow<List<RunningSession>> = flowOf(emptyList())

    override fun observeTrackPoints(sessionId: Long): Flow<List<TrackPoint>> = flowOf(emptyList())

    override suspend fun getSession(sessionId: Long): RunningSession? = null

    override suspend fun getActiveSession(): RunningSession? = null

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
                age = 31,
                updatedAtEpochMillis = 1_000L,
            ),
        )
    private val preferences =
        MutableStateFlow(AppPreferences(onboardingCompleted = true, locationRationaleShown = false))

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
