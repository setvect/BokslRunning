package com.boksl.running.ui.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import com.boksl.running.MainDispatcherRule
import com.boksl.running.domain.model.AppPreferences
import com.boksl.running.domain.model.Gender
import com.boksl.running.domain.model.Profile
import com.boksl.running.domain.repository.ProfileRepository
import com.boksl.running.ui.navigation.AppRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun validateProfileFormRejectsInvalidValues() {
        val result =
            validateProfileForm(
                ProfileFormUiState(
                    weightKg = "0",
                    age = "-1",
                ),
            )

        assertTrue(!result.isValid)
        assertEquals("몸무게를 0보다 크게 입력해 주세요.", result.weightError)
        assertEquals("나이를 0보다 크게 입력해 주세요.", result.ageError)
    }

    @Test
    fun saveProfilePersistsCurrentTimeAndEmitsOnboardingNavigation() =
        runTest {
            val repository = FakeProfileRepository()
            val viewModel =
                ProfileFormViewModel(
                    savedStateHandle =
                        SavedStateHandle(
                            mapOf(
                                AppRoute.ProfileSetup.ENTRY_POINT_ARG to ProfileSetupEntryPoint.Onboarding.routeValue,
                            ),
                        ),
                    profileRepository = repository,
                )
            val eventDeferred = async { viewModel.navigationEvent.first() }

            viewModel.onWeightChanged("72.5")
            viewModel.onGenderChanged(Gender.OTHER)
            viewModel.onAgeChanged("34")
            viewModel.saveProfile(currentTimeMillis = 55_000L)
            advanceUntilIdle()

            val saved = repository.observeProfile().first()
            assertEquals(72.5f, saved?.weightKg ?: 0f, 0f)
            assertEquals(Gender.OTHER, saved?.gender)
            assertEquals(34, saved?.age)
            assertEquals(55_000L, saved?.updatedAtEpochMillis)
            assertEquals(
                ProfileFormNavigationEvent.NavigateToPermissionGate,
                eventDeferred.await(),
            )
        }

    @Test
    fun settingsEntryPrefillsExistingProfile() =
        runTest {
            val repository =
                FakeProfileRepository(
                    initialProfile =
                        Profile(
                            weightKg = 68f,
                            gender = Gender.FEMALE,
                            age = 28,
                            updatedAtEpochMillis = 1_000L,
                        ),
                )

            val viewModel =
                ProfileFormViewModel(
                    savedStateHandle =
                        SavedStateHandle(
                            mapOf(AppRoute.ProfileSetup.ENTRY_POINT_ARG to ProfileSetupEntryPoint.Settings.routeValue),
                        ),
                    profileRepository = repository,
                )
            advanceUntilIdle()

            val uiState = viewModel.uiState.first { it.weightKg.isNotBlank() }
            assertEquals("68.0", uiState.weightKg)
            assertEquals(Gender.FEMALE, uiState.gender)
            assertEquals("28", uiState.age)
            assertNull(uiState.weightError)
        }
}

private class FakeProfileRepository(
    initialProfile: Profile? = null,
    initialPreferences: AppPreferences = AppPreferences(onboardingCompleted = false, locationRationaleShown = false),
) : ProfileRepository {
    private val profileFlow = MutableStateFlow(initialProfile)
    private val preferencesFlow = MutableStateFlow(initialPreferences)

    override fun observeProfile(): Flow<Profile?> = profileFlow

    override fun observeAppPreferences(): Flow<AppPreferences> = preferencesFlow

    override suspend fun saveProfile(profile: Profile) {
        profileFlow.value = profile
    }

    override suspend fun clearProfile() {
        profileFlow.value = null
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        preferencesFlow.value = preferencesFlow.value.copy(onboardingCompleted = completed)
    }

    override suspend fun setLocationRationaleShown(shown: Boolean) {
        preferencesFlow.value = preferencesFlow.value.copy(locationRationaleShown = shown)
    }
}
