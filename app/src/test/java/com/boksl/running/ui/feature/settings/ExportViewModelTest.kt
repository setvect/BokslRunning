package com.boksl.running.ui.feature.settings

import com.boksl.running.MainDispatcherRule
import com.boksl.running.domain.model.ExportProgress
import com.boksl.running.domain.repository.ExportRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExportViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun startExportUpdatesStateToCompleted() =
        runTest {
            val viewModel =
                ExportViewModel(
                    exportRepository =
                        FakeExportRepository(
                            flowFactory = {
                                flowOf(
                                    ExportProgress.Running(totalSessions = 2, completedSessions = 0),
                                    ExportProgress.Running(totalSessions = 2, completedSessions = 2),
                                    ExportProgress.Completed(filePath = "/tmp/export.json"),
                                )
                            },
                        ),
                )

            viewModel.startExport()
            advanceUntilIdle()

            assertEquals(
                ExportProgress.Completed(filePath = "/tmp/export.json"),
                viewModel.uiState.value.progress,
            )
        }

    @Test
    fun startExportIgnoresDuplicateRequestWhileCurrentExportIsActive() =
        runTest {
            val progressFlow = MutableSharedFlow<ExportProgress>()
            val repository = FakeExportRepository(flowFactory = { progressFlow })
            val viewModel = ExportViewModel(exportRepository = repository)

            viewModel.startExport()
            advanceUntilIdle()
            viewModel.startExport()

            assertEquals(1, repository.callCount)
        }

    @Test
    fun cancelExportResetsStateToIdle() =
        runTest {
            val progressFlow = MutableSharedFlow<ExportProgress>(replay = 1)
            val repository = FakeExportRepository(flowFactory = { progressFlow })
            val viewModel = ExportViewModel(exportRepository = repository)

            viewModel.startExport()
            progressFlow.emit(ExportProgress.Running(totalSessions = 3, completedSessions = 1))
            advanceUntilIdle()

            viewModel.cancelExport()

            assertEquals(ExportProgress.Idle, viewModel.uiState.value.progress)
        }

    @Test
    fun shareExportFileEmitsOneShotEventAfterCompletion() =
        runTest {
            val viewModel =
                ExportViewModel(
                    exportRepository =
                        FakeExportRepository(
                            flowFactory = { flowOf(ExportProgress.Completed(filePath = "/tmp/export.json")) },
                        ),
                )

            viewModel.startExport()
            advanceUntilIdle()
            viewModel.shareExportFile()
            val emittedEvent = viewModel.event.first()

            assertEquals(
                ExportEvent.ShareFile(filePath = "/tmp/export.json"),
                emittedEvent,
            )
        }

    @Test
    fun saveExportFileToDeviceEmitsOneShotEventAfterCompletion() =
        runTest {
            val viewModel =
                ExportViewModel(
                    exportRepository =
                        FakeExportRepository(
                            flowFactory = { flowOf(ExportProgress.Completed(filePath = "/tmp/export.json")) },
                        ),
                )

            viewModel.startExport()
            advanceUntilIdle()
            viewModel.saveExportFileToDevice()
            val emittedEvent = viewModel.event.first()

            assertEquals(
                ExportEvent.SaveFileToDevice(
                    filePath = "/tmp/export.json",
                    fileName = "bokslrunning_export_v1.json",
                ),
                emittedEvent,
            )
        }

    @Test
    fun startExportShowsErrorWhenRepositoryThrows() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val viewModel =
                ExportViewModel(
                    exportRepository =
                        FakeExportRepository(
                            flowFactory = {
                                flow {
                                    gate.await()
                                    error("export failed")
                                }
                            },
                        ),
                )

            viewModel.startExport()
            gate.complete(Unit)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.progress is ExportProgress.Error)
            assertEquals(
                "export failed",
                (viewModel.uiState.value.progress as ExportProgress.Error).message,
            )
        }
}

private class FakeExportRepository(
    private val flowFactory: () -> Flow<ExportProgress>,
) : ExportRepository {
    var callCount: Int = 0
        private set

    override fun exportAllData(): Flow<ExportProgress> {
        callCount += 1
        return flowFactory()
    }
}
