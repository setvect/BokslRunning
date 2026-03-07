package com.boksl.running.ui.feature.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boksl.running.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationPermissionGateViewModel
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow<LocationPermissionUiState>(LocationPermissionUiState.ShowRequest)
        val uiState: StateFlow<LocationPermissionUiState> = _uiState.asStateFlow()

        fun onPermissionResult(
            granted: Boolean,
            shouldShowRationale: Boolean,
        ) {
            _uiState.value =
                if (granted) {
                    LocationPermissionUiState.Granted
                } else if (shouldShowRationale) {
                    LocationPermissionUiState.Denied
                } else {
                    LocationPermissionUiState.PermanentlyDenied
                }
        }

        fun completeOnboarding(onCompleted: () -> Unit) {
            viewModelScope.launch {
                profileRepository.setOnboardingCompleted(completed = true)
                onCompleted()
            }
        }
    }
