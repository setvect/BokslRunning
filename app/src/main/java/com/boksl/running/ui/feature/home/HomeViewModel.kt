package com.boksl.running.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boksl.running.domain.repository.ProfileRepository
import com.boksl.running.domain.repository.RunningRepository
import com.boksl.running.ui.feature.permission.LocationPermissionUiState
import com.boksl.running.ui.feature.permission.PermissionReturnAction
import com.boksl.running.ui.feature.permission.resolveRunStartPermissionDialogState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
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
        private val permissionReturnAction = MutableStateFlow(PermissionReturnAction.None)
        private val eventChannel = Channel<HomeEvent>(capacity = Channel.BUFFERED)
        val event = eventChannel.receiveAsFlow()

        val uiState: StateFlow<HomeUiState> =
            combine(
                runningRepository.observeHomeSummary(),
                profileRepository.observeProfile(),
                permissionDialogState,
                permissionReturnAction,
            ) { summary, profile, dialogState, pendingAction ->
                HomeUiState(
                    totalDistanceMeters = summary.totalDistanceMeters,
                    totalDurationMillis = summary.totalDurationMillis,
                    averageSpeedMps = summary.averageSpeedMps,
                    totalCaloriesKcal = summary.totalCaloriesKcal,
                    hasProfile = profile != null,
                    permissionDialogState = dialogState,
                    permissionReturnAction = pendingAction,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MILLIS),
                initialValue = HomeUiState(),
            )

        fun onRunStartRequested(shouldShowRationale: Boolean) {
            viewModelScope.launch {
                permissionDialogState.value =
                    resolveRunStartPermissionDialogState(
                        profileRepository = profileRepository,
                        shouldShowRationale = shouldShowRationale,
                    )
            }
        }

        fun dismissPermissionDialog() {
            permissionDialogState.value = null
        }

        fun onOpenAppSettingsRequested() {
            permissionDialogState.value = null
            permissionReturnAction.value = PermissionReturnAction.StartRun
        }

        fun onPermissionSettingsResult(hasPermission: Boolean) {
            if (permissionReturnAction.value != PermissionReturnAction.StartRun) return
            permissionReturnAction.value = PermissionReturnAction.None
            if (hasPermission) {
                eventChannel.trySend(HomeEvent.NavigateToRunReady)
            }
        }
    }

data class HomeUiState(
    val totalDistanceMeters: Double = 0.0,
    val totalDurationMillis: Long = 0L,
    val averageSpeedMps: Double = 0.0,
    val totalCaloriesKcal: Double = 0.0,
    val hasProfile: Boolean = false,
    val permissionDialogState: LocationPermissionUiState? = null,
    val permissionReturnAction: PermissionReturnAction = PermissionReturnAction.None,
)

sealed interface HomeEvent {
    data object NavigateToRunReady : HomeEvent
}

private const val STATE_TIMEOUT_MILLIS = 5_000L
