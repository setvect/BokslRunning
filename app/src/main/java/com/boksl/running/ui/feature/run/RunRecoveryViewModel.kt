package com.boksl.running.ui.feature.run

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boksl.running.core.network.NetworkMonitor
import com.boksl.running.domain.model.RunningSession
import com.boksl.running.domain.repository.RunEngineRepository
import com.boksl.running.domain.repository.RunningRepository
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
class RunRecoveryViewModel
    @Inject
    constructor(
        private val runEngineRepository: RunEngineRepository,
        private val runningRepository: RunningRepository,
        networkMonitor: NetworkMonitor,
    ) : ViewModel() {
        private val activeSession = MutableStateFlow<RunningSession?>(null)
        private val isLoading = MutableStateFlow(true)
        private val eventChannel = Channel<RunRecoveryEvent>(capacity = Channel.BUFFERED)
        val event = eventChannel.receiveAsFlow()

        val uiState: StateFlow<RunRecoveryUiState> =
            combine(
                activeSession,
                isLoading,
                networkMonitor.observeIsOnline(),
            ) { session, loading, isOnline ->
                RunRecoveryUiState(
                    isLoading = loading,
                    activeSession = session,
                    isOffline = !isOnline,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MILLIS),
                initialValue = RunRecoveryUiState(isLoading = true),
            )

        fun loadRecoveryState() {
            viewModelScope.launch {
                activeSession.value = runningRepository.getActiveSession()
                isLoading.value = false
                if (activeSession.value == null) {
                    eventChannel.send(RunRecoveryEvent.NavigateHome)
                }
            }
        }

        fun continueRun() {
            viewModelScope.launch {
                if (activeSession.value == null) {
                    eventChannel.send(RunRecoveryEvent.NavigateHome)
                    return@launch
                }
                runEngineRepository.resumeActiveRun()
                eventChannel.send(RunRecoveryEvent.NavigateToLive)
            }
        }

        fun discardRun() {
            viewModelScope.launch {
                if (activeSession.value == null) {
                    eventChannel.send(RunRecoveryEvent.NavigateHome)
                    return@launch
                }
                runEngineRepository.discardRun()
                activeSession.value = null
                eventChannel.send(RunRecoveryEvent.NavigateToHistory)
            }
        }
    }

data class RunRecoveryUiState(
    val isLoading: Boolean = false,
    val activeSession: RunningSession? = null,
    val isOffline: Boolean = false,
)

sealed interface RunRecoveryEvent {
    data object NavigateToLive : RunRecoveryEvent

    data object NavigateToHistory : RunRecoveryEvent

    data object NavigateHome : RunRecoveryEvent
}

private const val STATE_TIMEOUT_MILLIS = 5_000L
