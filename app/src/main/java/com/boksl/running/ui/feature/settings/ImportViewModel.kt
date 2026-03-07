package com.boksl.running.ui.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boksl.running.domain.model.ImportProgress
import com.boksl.running.domain.repository.ImportRepository
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
class ImportViewModel
    @Inject
    constructor(
        private val importRepository: ImportRepository,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(ImportUiState())
        val uiState: StateFlow<ImportUiState> = mutableUiState.asStateFlow()

        private val eventChannel = Channel<ImportEvent>(capacity = Channel.BUFFERED)
        val event = eventChannel.receiveAsFlow()

        private var importJob: Job? = null

        fun startImport() {
            if (importJob?.isActive == true) return
            eventChannel.trySend(ImportEvent.OpenDocumentPicker)
        }

        fun onImportFileSelected(uri: Uri?) {
            if (uri == null || importJob?.isActive == true) return

            importJob =
                viewModelScope.launch {
                    try {
                        importRepository.importAllData(uri).collect { progress ->
                            mutableUiState.update { uiState -> uiState.copy(progress = progress) }
                        }
                    } catch (cancellationException: CancellationException) {
                        throw cancellationException
                    } catch (throwable: Throwable) {
                        mutableUiState.update { uiState ->
                            uiState.copy(
                                progress =
                                    ImportProgress.Error(
                                        message = throwable.message ?: IMPORT_FAILURE_MESSAGE,
                                    ),
                            )
                        }
                    } finally {
                        importJob = null
                    }
                }
        }

        fun cancelImport() {
            importJob?.cancel()
            importJob = null
            mutableUiState.value = ImportUiState()
        }

        private companion object {
            const val IMPORT_FAILURE_MESSAGE = "가져오기에 실패했습니다."
        }
    }

data class ImportUiState(
    val progress: ImportProgress = ImportProgress.Idle,
)

sealed interface ImportEvent {
    data object OpenDocumentPicker : ImportEvent
}
