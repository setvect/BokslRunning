package com.boksl.running.ui.feature.settings

import com.boksl.running.MainDispatcherRule
import com.boksl.running.data.debug.DebugSeedManager
import com.boksl.running.data.debug.DebugSeedOperationResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun debugToolsVisibilityDependsOnInjectedBuildFlag() =
        runTest {
            val debugViewModel = SettingsViewModel(debugSeedManager = FakeDebugSeedManager(), isDebugBuild = true)
            val releaseViewModel = SettingsViewModel(debugSeedManager = FakeDebugSeedManager(), isDebugBuild = false)

            assertTrue(debugViewModel.uiState.value.isDebugToolsVisible)
            assertFalse(releaseViewModel.uiState.value.isDebugToolsVisible)
        }

    @Test
    fun generateAndDeleteClicksOpenMatchingPendingActionInDebugBuild() =
        runTest {
            val viewModel = SettingsViewModel(debugSeedManager = FakeDebugSeedManager(), isDebugBuild = true)

            viewModel.onGenerateSeedDataClick()
            assertEquals(SettingsPendingAction.GENERATE, viewModel.uiState.value.pendingAction)

            viewModel.dismissPendingAction()
            viewModel.onDeleteSeedDataClick()
            assertEquals(SettingsPendingAction.DELETE, viewModel.uiState.value.pendingAction)
        }

    @Test
    fun confirmPendingActionPublishesSuccessMessage() =
        runTest {
            val viewModel =
                SettingsViewModel(
                    debugSeedManager =
                        FakeDebugSeedManager(
                            generateResult = DebugSeedOperationResult(4, 42, 1_420),
                        ),
                    isDebugBuild = true,
                )
            backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.onGenerateSeedDataClick()
            viewModel.confirmPendingAction()
            advanceUntilIdle()

            val uiState = viewModel.uiState.value
            assertFalse(uiState.isBusy)
            assertNull(uiState.pendingAction)
            assertEquals(SettingsStatusKind.SUCCESS, uiState.statusKind)
            assertTrue(uiState.statusMessage?.contains("42건") == true)
            assertTrue(uiState.statusMessage?.contains("1420건") == true)
        }

    @Test
    fun confirmPendingActionPublishesErrorMessageWhenGeneratorFails() =
        runTest {
            val viewModel =
                SettingsViewModel(
                    debugSeedManager =
                        FakeDebugSeedManager(
                            generateFailure = IllegalStateException("seed failed"),
                        ),
                    isDebugBuild = true,
                )
            backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.onGenerateSeedDataClick()
            viewModel.confirmPendingAction()
            advanceUntilIdle()

            val uiState = viewModel.uiState.value
            assertFalse(uiState.isBusy)
            assertEquals(SettingsStatusKind.ERROR, uiState.statusKind)
            assertEquals("seed failed", uiState.statusMessage)
        }

    @Test
    fun busyStateBlocksAdditionalDebugActionsUntilCurrentWorkFinishes() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val viewModel =
                SettingsViewModel(
                    debugSeedManager =
                        FakeDebugSeedManager(
                            generateGate = gate,
                        ),
                    isDebugBuild = true,
                )
            backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.onGenerateSeedDataClick()
            viewModel.confirmPendingAction()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isBusy)
            assertEquals("최근 12개월 테스트 데이터를 생성하는 중입니다.", viewModel.uiState.value.busyMessage)

            viewModel.onDeleteSeedDataClick()
            assertNull(viewModel.uiState.value.pendingAction)

            gate.complete(Unit)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isBusy)
            assertEquals(SettingsStatusKind.SUCCESS, viewModel.uiState.value.statusKind)
        }
}

private class FakeDebugSeedManager(
    private val generateResult: DebugSeedOperationResult = DebugSeedOperationResult(0, 10, 250),
    private val deleteResult: DebugSeedOperationResult = DebugSeedOperationResult(10, 0, 0),
    private val generateFailure: Throwable? = null,
    private val deleteFailure: Throwable? = null,
    private val generateGate: CompletableDeferred<Unit>? = null,
    private val deleteGate: CompletableDeferred<Unit>? = null,
) : DebugSeedManager {
    override suspend fun regenerateLastYear(): DebugSeedOperationResult {
        generateGate?.await()
        generateFailure?.let { throw it }
        return generateResult
    }

    override suspend fun deleteSeedData(): DebugSeedOperationResult {
        deleteGate?.await()
        deleteFailure?.let { throw it }
        return deleteResult
    }
}
