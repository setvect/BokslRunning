package com.boksl.running.ui.feature.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boksl.running.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationPermissionGateViewModel
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(LocationPermissionGateUiState())
        val uiState: StateFlow<LocationPermissionGateUiState> = _uiState.asStateFlow()
        private val eventChannel = Channel<LocationPermissionGateEvent>(capacity = Channel.BUFFERED)
        val event = eventChannel.receiveAsFlow()

        fun onPermissionResult(
            granted: Boolean,
            shouldShowRationale: Boolean,
        ) {
            val permissionState =
                if (granted) {
                    LocationPermissionUiState.Granted
                } else if (shouldShowRationale) {
                    LocationPermissionUiState.Denied
                } else {
                    LocationPermissionUiState.PermanentlyDenied
                }
            _uiState.value = _uiState.value.copy(permissionState = permissionState)
        }

        fun completeOnboarding(onCompleted: () -> Unit) {
            viewModelScope.launch {
                profileRepository.setOnboardingCompleted(completed = true)
                onCompleted()
            }
        }

        fun onOpenSettingsRequested() {
            _uiState.value =
                _uiState.value.copy(permissionReturnAction = PermissionReturnAction.CompleteOnboarding)
        }

        fun onPermissionSettingsResult(hasPermission: Boolean) {
            val pendingAction = _uiState.value.permissionReturnAction
            if (pendingAction != PermissionReturnAction.CompleteOnboarding) return

            _uiState.value =
                _uiState.value.copy(permissionReturnAction = PermissionReturnAction.None)

            if (!hasPermission) return

            _uiState.value =
                _uiState.value.copy(permissionState = LocationPermissionUiState.Granted)
            viewModelScope.launch {
                profileRepository.setOnboardingCompleted(completed = true)
                eventChannel.send(LocationPermissionGateEvent.NavigateHome)
            }
        }
    }

data class LocationPermissionGateUiState(
    val permissionState: LocationPermissionUiState = LocationPermissionUiState.ShowRequest,
    val permissionReturnAction: PermissionReturnAction = PermissionReturnAction.None,
)

sealed interface LocationPermissionGateEvent {
    data object NavigateHome : LocationPermissionGateEvent
}
