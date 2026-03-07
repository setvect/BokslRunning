package com.boksl.running.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boksl.running.domain.model.Profile
import com.boksl.running.domain.repository.ProfileRepository
import com.boksl.running.domain.repository.RunningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppLaunchViewModel
    @Inject
    constructor(
        profileRepository: ProfileRepository,
        runningRepository: RunningRepository,
    ) : ViewModel() {
        val uiState: StateFlow<AppLaunchUiState> =
            combine(
                profileRepository.observeProfile(),
                profileRepository.observeAppPreferences(),
                flow { emit(runningRepository.getActiveSession() != null) },
            ) { profile, preferences, hasActiveRun ->
                resolveAppLaunchUiState(
                    profile = profile,
                    onboardingCompleted = preferences.onboardingCompleted,
                    hasActiveRun = hasActiveRun,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = AppLaunchUiState.Loading,
            )
    }

sealed interface AppLaunchUiState {
    data object Loading : AppLaunchUiState

    data object NeedsOnboarding : AppLaunchUiState

    data object NeedsProfile : AppLaunchUiState

    data object NeedsLocationPermission : AppLaunchUiState

    data object HasActiveRun : AppLaunchUiState

    data object Ready : AppLaunchUiState
}

internal fun resolveAppLaunchUiState(
    profile: Profile?,
    onboardingCompleted: Boolean,
    hasActiveRun: Boolean,
): AppLaunchUiState =
    when {
        hasActiveRun -> AppLaunchUiState.HasActiveRun
        onboardingCompleted -> AppLaunchUiState.Ready
        profile == null -> AppLaunchUiState.NeedsOnboarding
        else -> AppLaunchUiState.NeedsLocationPermission
    }
