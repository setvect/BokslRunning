package com.boksl.running.ui.feature.permission

import com.boksl.running.MainDispatcherRule
import com.boksl.running.domain.model.AppPreferences
import com.boksl.running.domain.model.Profile
import com.boksl.running.domain.repository.ProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationPermissionGateViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun permissionSettingsReturnCompletesOnboardingWhenPermissionGranted() =
        runTest {
            val profileRepository = FakeProfileRepository()
            val viewModel = LocationPermissionGateViewModel(profileRepository = profileRepository)
            val eventDeferred = async { viewModel.event.first() }

            viewModel.onOpenSettingsRequested()
            assertEquals(PermissionReturnAction.CompleteOnboarding, viewModel.uiState.value.permissionReturnAction)

            viewModel.onPermissionSettingsResult(hasPermission = true)
            advanceUntilIdle()

            assertEquals(LocationPermissionUiState.Granted, viewModel.uiState.value.permissionState)
            assertEquals(PermissionReturnAction.None, viewModel.uiState.value.permissionReturnAction)
            assertEquals(LocationPermissionGateEvent.NavigateHome, eventDeferred.await())
            assertEquals(true, profileRepository.preferences.value.onboardingCompleted)
        }

    @Test
    fun permissionSettingsReturnLeavesScreenWhenPermissionStillDenied() =
        runTest {
            val viewModel = LocationPermissionGateViewModel(profileRepository = FakeProfileRepository())

            viewModel.onOpenSettingsRequested()
            viewModel.onPermissionSettingsResult(hasPermission = false)
            advanceUntilIdle()

            assertEquals(PermissionReturnAction.None, viewModel.uiState.value.permissionReturnAction)
            assertEquals(LocationPermissionUiState.ShowRequest, viewModel.uiState.value.permissionState)
        }
}

private class FakeProfileRepository : ProfileRepository {
    val preferences = MutableStateFlow(AppPreferences(onboardingCompleted = false, locationRationaleShown = false))
    private val profile = MutableStateFlow<Profile?>(null)

    override fun observeProfile(): Flow<Profile?> = profile

    override fun observeAppPreferences(): Flow<AppPreferences> = preferences

    override suspend fun saveProfile(profile: Profile) = Unit

    override suspend fun clearProfile() = Unit

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        preferences.value = preferences.value.copy(onboardingCompleted = completed)
    }

    override suspend fun setLocationRationaleShown(shown: Boolean) {
        preferences.value = preferences.value.copy(locationRationaleShown = shown)
    }
}
