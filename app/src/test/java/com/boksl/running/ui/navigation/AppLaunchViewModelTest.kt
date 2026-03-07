package com.boksl.running.ui.navigation

import com.boksl.running.domain.model.Gender
import com.boksl.running.domain.model.Profile
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLaunchViewModelTest {
    @Test
    fun resolveToReadyWhenOnboardingCompleted() {
        val profile =
            Profile(
                weightKg = 65f,
                gender = Gender.FEMALE,
                age = 29,
                updatedAtEpochMillis = 1_000L,
            )

        val state = resolveAppLaunchUiState(profile = profile, onboardingCompleted = true, hasActiveRun = false)
        assertEquals(AppLaunchUiState.Ready, state)
    }

    @Test
    fun resolveToOnboardingWhenProfileMissingAndOnboardingNotCompleted() {
        val state = resolveAppLaunchUiState(profile = null, onboardingCompleted = false, hasActiveRun = false)
        assertEquals(AppLaunchUiState.NeedsOnboarding, state)
    }

    @Test
    fun resolveToPermissionWhenProfileExistsAndOnboardingNotCompleted() {
        val state =
            resolveAppLaunchUiState(
                profile =
                    Profile(
                        weightKg = 70f,
                        gender = Gender.MALE,
                        age = 31,
                        updatedAtEpochMillis = 2_000L,
                    ),
                onboardingCompleted = false,
                hasActiveRun = false,
            )

        assertEquals(AppLaunchUiState.NeedsLocationPermission, state)
    }

    @Test
    fun resolveToRunRecoveryWhenInProgressSessionExists() {
        val state =
            resolveAppLaunchUiState(
                profile = null,
                onboardingCompleted = false,
                hasActiveRun = true,
            )

        assertEquals(AppLaunchUiState.NeedsRunRecovery, state)
    }
}
