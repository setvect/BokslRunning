package com.boksl.running.ui.feature.run

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boksl.running.domain.model.RunSnapshot
import com.boksl.running.domain.repository.RunEngineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RunSessionViewModel
    @Inject
    constructor(
        private val runEngineRepository: RunEngineRepository,
    ) : ViewModel() {
        val snapshot: StateFlow<RunSnapshot?> =
            runEngineRepository.observeActiveRun().stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = null,
            )

        fun prepareRun() {
            viewModelScope.launch {
                runEngineRepository.prepareRun()
            }
        }

        fun startRun() {
            viewModelScope.launch {
                runEngineRepository.startRun()
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
