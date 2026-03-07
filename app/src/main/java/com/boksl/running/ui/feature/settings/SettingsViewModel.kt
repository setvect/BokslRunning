package com.boksl.running.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boksl.running.core.di.DebugBuild
import com.boksl.running.data.debug.DebugSeedManager
import com.boksl.running.data.debug.DebugSeedOperationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val debugSeedManager: DebugSeedManager,
        @DebugBuild private val isDebugBuild: Boolean,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(SettingsUiState(isDebugToolsVisible = isDebugBuild))

        val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

        fun onGenerateSeedDataClick() {
            if (!canStartDebugAction()) return
            mutableUiState.update { uiState -> uiState.copy(pendingAction = SettingsPendingAction.GENERATE) }
        }

        fun onDeleteSeedDataClick() {
            if (!canStartDebugAction()) return
            mutableUiState.update { uiState -> uiState.copy(pendingAction = SettingsPendingAction.DELETE) }
        }

        fun confirmPendingAction() {
            val pendingAction = mutableUiState.value.pendingAction ?: return
            if (mutableUiState.value.isBusy) return

            mutableUiState.update { uiState ->
                uiState.copy(
                    isBusy = true,
                    pendingAction = null,
                    busyMessage = pendingAction.toBusyMessage(),
                    statusMessage = null,
                    statusKind = SettingsStatusKind.IDLE,
                )
            }

            viewModelScope.launch {
                runCatching {
                    when (pendingAction) {
                        SettingsPendingAction.GENERATE -> debugSeedManager.regenerateLastYear()
                        SettingsPendingAction.DELETE -> debugSeedManager.deleteSeedData()
                    }
                }.onSuccess { result ->
                    mutableUiState.update { uiState ->
                        uiState.copy(
                            isBusy = false,
                            busyMessage = null,
                            statusMessage = pendingAction.toSuccessMessage(result),
                            statusKind = SettingsStatusKind.SUCCESS,
                        )
                    }
                }.onFailure { throwable ->
                    mutableUiState.update { uiState ->
                        uiState.copy(
                            isBusy = false,
                            busyMessage = null,
                            statusMessage = throwable.message ?: pendingAction.toFallbackErrorMessage(),
                            statusKind = SettingsStatusKind.ERROR,
                        )
                    }
                }
            }
        }

        fun dismissPendingAction() {
            mutableUiState.update { uiState -> uiState.copy(pendingAction = null) }
        }

        fun clearStatusMessage() {
            mutableUiState.update { uiState ->
                uiState.copy(
                    statusMessage = null,
                    statusKind = SettingsStatusKind.IDLE,
                )
            }
        }

        private fun canStartDebugAction(): Boolean = isDebugBuild && !mutableUiState.value.isBusy
    }

data class SettingsUiState(
    val isDebugToolsVisible: Boolean = false,
    val isBusy: Boolean = false,
    val pendingAction: SettingsPendingAction? = null,
    val statusMessage: String? = null,
    val statusKind: SettingsStatusKind = SettingsStatusKind.IDLE,
    val busyMessage: String? = null,
)

enum class SettingsPendingAction {
    GENERATE,
    DELETE,
}

enum class SettingsStatusKind {
    IDLE,
    SUCCESS,
    ERROR,
}

private fun SettingsPendingAction.toBusyMessage(): String =
    when (this) {
        SettingsPendingAction.GENERATE -> "최근 12개월 테스트 데이터를 생성하는 중입니다."
        SettingsPendingAction.DELETE -> "생성한 테스트 데이터를 삭제하는 중입니다."
    }

private fun SettingsPendingAction.toSuccessMessage(result: DebugSeedOperationResult): String =
    when (this) {
        SettingsPendingAction.GENERATE ->
            "테스트 데이터 ${result.generatedSessionCount}건과 트랙포인트 ${result.generatedTrackPointCount}건을 생성했습니다. 기존 시드 ${result.deletedSessionCount}건은 정리했습니다."
        SettingsPendingAction.DELETE ->
            "생성한 테스트 데이터 ${result.deletedSessionCount}건을 삭제했습니다."
    }

private fun SettingsPendingAction.toFallbackErrorMessage(): String =
    when (this) {
        SettingsPendingAction.GENERATE -> "테스트 데이터 생성에 실패했습니다."
        SettingsPendingAction.DELETE -> "테스트 데이터 삭제에 실패했습니다."
    }
