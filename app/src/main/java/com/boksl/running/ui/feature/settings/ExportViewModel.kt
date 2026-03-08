package com.boksl.running.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boksl.running.domain.model.ExportProgress
import com.boksl.running.domain.repository.ExportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExportViewModel
    @Inject
    constructor(
        private val exportRepository: ExportRepository,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(ExportUiState())
        val uiState: StateFlow<ExportUiState> = mutableUiState.asStateFlow()

        private val eventChannel = Channel<ExportEvent>(capacity = Channel.BUFFERED)
        val event = eventChannel.receiveAsFlow()

        private var exportJob: Job? = null

        fun startExport() {
            if (exportJob?.isActive == true) return

            exportJob =
                viewModelScope.launch {
                    try {
                        exportRepository.exportAllData().collect { progress ->
                            mutableUiState.update { uiState -> uiState.copy(progress = progress) }
                        }
                    } catch (cancellationException: CancellationException) {
                        throw cancellationException
                    } catch (exception: IllegalStateException) {
                        mutableUiState.update { uiState ->
                            uiState.copy(
                                progress =
                                    ExportProgress.Error(
                                        message = exception.message ?: EXPORT_FAILURE_MESSAGE,
                                    ),
                            )
                        }
                    } finally {
                        exportJob = null
                    }
                }
        }

        fun cancelExport() {
            exportJob?.cancel()
            exportJob = null
            mutableUiState.value = ExportUiState()
        }

        fun shareExportFile() {
            val completed = mutableUiState.value.progress as? ExportProgress.Completed ?: return
            eventChannel.trySend(ExportEvent.ShareFile(completed.filePath))
        }

        fun saveExportFileToDevice() {
            val completed = mutableUiState.value.progress as? ExportProgress.Completed ?: return
            eventChannel.trySend(
                ExportEvent.SaveFileToDevice(
                    filePath = completed.filePath,
                    fileName = DEVICE_EXPORT_FILE_NAME,
                ),
            )
        }

        fun showShareError(message: String) {
            mutableUiState.update { uiState ->
                uiState.copy(progress = ExportProgress.Error(message = message))
            }
        }

        private companion object {
            const val EXPORT_FAILURE_MESSAGE = "내보내기에 실패했습니다."
            const val DEVICE_EXPORT_FILE_NAME = "bokslrunning_export_v1.json"
        }
    }

data class ExportUiState(
    val progress: ExportProgress = ExportProgress.Idle,
)

sealed interface ExportEvent {
    data class ShareFile(
        val filePath: String,
    ) : ExportEvent

    data class SaveFileToDevice(
        val filePath: String,
        val fileName: String,
    ) : ExportEvent
}
