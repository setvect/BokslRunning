package com.boksl.running.ui.feature.permission

import com.boksl.running.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.first

suspend fun resolveRunStartPermissionDialogState(
    profileRepository: ProfileRepository,
    shouldShowRationale: Boolean,
): LocationPermissionUiState {
    val preferences = profileRepository.observeAppPreferences().first()
    val dialogState =
        resolveLocationPermissionUiState(
            hasPermission = false,
            shouldShowRationale = shouldShowRationale,
            rationaleAlreadyShown = preferences.locationRationaleShown,
        )
    if (!preferences.locationRationaleShown) {
        profileRepository.setLocationRationaleShown(shown = true)
    }
    return dialogState
}
