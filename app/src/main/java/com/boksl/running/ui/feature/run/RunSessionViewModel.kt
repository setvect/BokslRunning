package com.boksl.running.ui.feature.run

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boksl.running.domain.model.RunEngineState
import com.boksl.running.domain.model.RunSnapshot
import com.boksl.running.domain.repository.RunEngineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RunSessionViewModel
    @Inject
    constructor(
        private val runEngineRepository: RunEngineRepository,
    ) : ViewModel() {
        private val isStarting = MutableStateFlow(false)

        val uiState: StateFlow<RunSessionUiState> =
            combine(
                runEngineRepository.observeActiveRun(),
                isStarting,
            ) { snapshot, isStarting ->
                RunSessionUiState(
                    snapshot = snapshot,
                    isStarting = isStarting,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = RunSessionUiState(),
            )

        fun prepareRun() {
            viewModelScope.launch {
                runEngineRepository.prepareRun()
            }
        }

        fun startRun() {
            viewModelScope.launch {
                if (isStarting.value) return@launch
                isStarting.value = true
                runEngineRepository.startRun()
                isStarting.value = false
            }
        }

        fun requestStop() {
            viewModelScope.launch {
                runEngineRepository.requestStop()
            }
        }

        fun confirmSave() {
            viewModelScope.launch {
                runEngineRepository.confirmSave()
            }
        }

        fun cancelStop() {
            viewModelScope.launch {
                runEngineRepository.cancelStop()
            }
        }

        fun discardRun() {
            viewModelScope.launch {
                runEngineRepository.discardRun()
            }
        }

        fun resumeActiveRun() {
            viewModelScope.launch {
                runEngineRepository.resumeActiveRun()
            }
        }
    }

data class RunSessionUiState(
    val snapshot: RunSnapshot? = null,
    val isStarting: Boolean = false,
) {
    val shouldNavigateToLive: Boolean
        get() = snapshot?.state == RunEngineState.RUNNING
}
