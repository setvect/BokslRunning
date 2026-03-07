package com.boksl.running.ui.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.map
import com.boksl.running.domain.model.RunningSession
import com.boksl.running.domain.model.SessionStatus
import com.boksl.running.domain.model.TrackPoint
import com.boksl.running.domain.repository.ProfileRepository
import com.boksl.running.domain.repository.RunningRepository
import com.boksl.running.ui.feature.permission.LocationPermissionUiState
import com.boksl.running.ui.feature.permission.PermissionReturnAction
import com.boksl.running.ui.feature.permission.resolveRunStartPermissionDialogState
import com.boksl.running.ui.navigation.AppRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryListViewModel
    @Inject
    constructor(
        private val runningRepository: RunningRepository,
        private val profileRepository: ProfileRepository,
    ) : ViewModel() {
        private val permissionDialogState = MutableStateFlow<LocationPermissionUiState?>(null)
        private val permissionReturnAction = MutableStateFlow(PermissionReturnAction.None)
        private val eventChannel = Channel<HistoryListEvent>(capacity = Channel.BUFFERED)
        val event = eventChannel.receiveAsFlow()

        val pagedItems: Flow<PagingData<HistoryListItemUiState>> =
            runningRepository
                .observeSavedSessionsPaged()
                .map { pagingData ->
                    pagingData.map { session -> session.toHistoryListItem() }
                }

        val uiState: StateFlow<HistoryListUiState> =
            combine(permissionDialogState, permissionReturnAction) { dialogState, pendingAction ->
                HistoryListUiState(
                    permissionDialogState = dialogState,
                    permissionReturnAction = pendingAction,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MILLIS),
                initialValue = HistoryListUiState(),
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
                eventChannel.trySend(HistoryListEvent.NavigateToRunReady)
            }
        }
    }

@HiltViewModel
class HistoryDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        runningRepository: RunningRepository,
    ) : ViewModel() {
        private val sessionId: Long = savedStateHandle[AppRoute.HistoryDetail.SESSION_ID_ARG] ?: INVALID_SESSION_ID

        val uiState: StateFlow<HistoryDetailUiState> =
            combine(
                runningRepository.observeSession(sessionId),
                runningRepository.observeTrackPoints(sessionId),
            ) { session, trackPoints ->
                val savedSession = session?.takeIf { it.status == SessionStatus.SAVED }
                HistoryDetailUiState(
                    isLoading = false,
                    session = savedSession,
                    trackPoints = if (savedSession != null) trackPoints else emptyList(),
                    isNotFound = savedSession == null,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MILLIS),
                initialValue = HistoryDetailUiState(isLoading = true),
            )
    }

data class HistoryListUiState(
    val permissionDialogState: LocationPermissionUiState? = null,
    val permissionReturnAction: PermissionReturnAction = PermissionReturnAction.None,
)

sealed interface HistoryListEvent {
    data object NavigateToRunReady : HistoryListEvent
}

data class HistoryListItemUiState(
    val sessionId: Long,
    val startedAtEpochMillis: Long,
    val distanceMeters: Double,
    val durationMillis: Long,
    val averagePaceSecPerKm: Double?,
)

data class HistoryDetailUiState(
    val isLoading: Boolean = false,
    val session: RunningSession? = null,
    val trackPoints: List<TrackPoint> = emptyList(),
    val isNotFound: Boolean = false,
)

private fun RunningSession.toHistoryListItem(): HistoryListItemUiState =
    HistoryListItemUiState(
        sessionId = id,
        startedAtEpochMillis = startedAtEpochMillis,
        distanceMeters = stats.distanceMeters,
        durationMillis = stats.durationMillis,
        averagePaceSecPerKm = stats.averagePaceSecPerKm,
    )

private const val INVALID_SESSION_ID = -1L
private const val STATE_TIMEOUT_MILLIS = 5_000L
