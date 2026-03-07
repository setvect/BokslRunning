package com.boksl.running.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boksl.running.domain.model.HomeSummary
import com.boksl.running.domain.repository.ProfileRepository
import com.boksl.running.domain.repository.RunningRepository
import com.boksl.running.ui.feature.permission.LocationPermissionUiState
import com.boksl.running.ui.feature.permission.resolveLocationPermissionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val runningRepository: RunningRepository,
        private val profileRepository: ProfileRepository,
    ) : ViewModel() {
        private val permissionDialogState = MutableStateFlow<LocationPermissionUiState?>(null)
        private val showRunPlaceholder = MutableStateFlow(false)

        val uiState: StateFlow<HomeUiState> =
            combine(
                runningRepository.observeHomeSummary(),
                profileRepository.observeProfile(),
                permissionDialogState,
                showRunPlaceholder,
            ) { summary, profile, dialogState, runPlaceholder ->
                HomeUiState(
                    totalDistanceMeters = summary.totalDistanceMeters,
                    totalDurationMillis = summary.totalDurationMillis,
                    averageSpeedMps = summary.averageSpeedMps,
                    totalCaloriesKcal = summary.totalCaloriesKcal,
                    hasProfile = profile != null,
                    permissionDialogState = dialogState,
                    showRunPlaceholder = runPlaceholder,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = HomeUiState(),
            )

        fun onRunStartRequested(
            hasLocationPermission: Boolean,
            shouldShowRationale: Boolean,
        ) {
            viewModelScope.launch {
                if (hasLocationPermission) {
                    showRunPlaceholder.value = true
                    return@launch
                }

                val preferences = profileRepository.observeAppPreferences().first()
                permissionDialogState.value =
                    resolveLocationPermissionUiState(
                        hasPermission = false,
                        shouldShowRationale = shouldShowRationale,
                        rationaleAlreadyShown = preferences.locationRationaleShown,
                    )
                if (!preferences.locationRationaleShown) {
                    profileRepository.setLocationRationaleShown(shown = true)
                }
            }
        }

        fun dismissPermissionDialog() {
            permissionDialogState.value = null
        }

        fun dismissRunPlaceholder() {
            showRunPlaceholder.value = false
        }
    }

data class HomeUiState(
    val totalDistanceMeters: Double = 0.0,
    val totalDurationMillis: Long = 0L,
    val averageSpeedMps: Double = 0.0,
    val totalCaloriesKcal: Double = 0.0,
    val hasProfile: Boolean = false,
    val permissionDialogState: LocationPermissionUiState? = null,
    val showRunPlaceholder: Boolean = false,
)
