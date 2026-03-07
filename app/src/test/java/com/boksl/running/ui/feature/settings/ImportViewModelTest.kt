package com.boksl.running.ui.feature.settings

import android.net.Uri
import com.boksl.running.MainDispatcherRule
import com.boksl.running.domain.model.ImportProgress
import com.boksl.running.domain.model.ImportResult
import com.boksl.running.domain.repository.ImportRepository
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ImportViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun startImportRequestsDocumentPicker() =
        runTest {
            val viewModel = ImportViewModel(importRepository = FakeImportRepository())

            viewModel.startImport()
            val event = viewModel.event.first()

            assertEquals(ImportEvent.OpenDocumentPicker, event)
        }

    @Test
    fun onImportFileSelectedUpdatesStateToCompleted() =
        runTest {
            val viewModel =
                ImportViewModel(
                    importRepository =
                        FakeImportRepository(
                            flowFactory = {
                                flowOf(
                                    ImportProgress.BackingUp,
                                    ImportProgress.Importing,
                                    ImportProgress.Completed(
                                        ImportResult(
                                            addedSessionCount = 2,
                                            duplicateSessionCount = 1,
                                            appliedProfile = true,
                                            wasDuplicateFile = false,
                                        ),
                                    ),
                                )
                            },
                        ),
                )

            viewModel.onImportFileSelected(testUri())
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.progress is ImportProgress.Completed)
        }

    @Test
    fun nullSelectionKeepsIdleState() =
        runTest {
            val viewModel = ImportViewModel(importRepository = FakeImportRepository())

            viewModel.onImportFileSelected(null)

            assertEquals(ImportProgress.Idle, viewModel.uiState.value.progress)
        }

    @Test
    fun duplicateInFlightSelectionIsIgnored() =
        runTest {
            val progressFlow = MutableSharedFlow<ImportProgress>()
            val repository = FakeImportRepository(flowFactory = { progressFlow })
            val viewModel = ImportViewModel(importRepository = repository)

            viewModel.onImportFileSelected(testUri())
            advanceUntilIdle()
            viewModel.onImportFileSelected(testUri())

            assertEquals(1, repository.callCount)
        }

    @Test
    fun cancelImportResetsStateToIdle() =
        runTest {
            val progressFlow = MutableSharedFlow<ImportProgress>(replay = 1)
            val viewModel =
                ImportViewModel(
                    importRepository = FakeImportRepository(flowFactory = { progressFlow }),
                )

            viewModel.onImportFileSelected(testUri())
            progressFlow.emit(ImportProgress.Importing)
            advanceUntilIdle()

            viewModel.cancelImport()

            assertEquals(ImportProgress.Idle, viewModel.uiState.value.progress)
        }

    @Test
    fun duplicateFileCompletionIsPreserved() =
        runTest {
            val viewModel =
                ImportViewModel(
                    importRepository =
                        FakeImportRepository(
                            flowFactory = {
                                flowOf(
                                    ImportProgress.Completed(
                                        ImportResult(
                                            addedSessionCount = 0,
                                            duplicateSessionCount = 0,
                                            appliedProfile = false,
                                            wasDuplicateFile = true,
                                        ),
                                    ),
                                )
                            },
                        ),
                )

            viewModel.onImportFileSelected(testUri())
            advanceUntilIdle()

            val progress = viewModel.uiState.value.progress as ImportProgress.Completed
            assertTrue(progress.result.wasDuplicateFile)
        }

    @Test
    fun repositoryFailureShowsErrorMessage() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val viewModel =
                ImportViewModel(
                    importRepository =
                        FakeImportRepository(
                            flowFactory = {
                                flow {
                                    gate.await()
                                    throw IllegalStateException("import failed")
                                }
                            },
                        ),
                )

            viewModel.onImportFileSelected(testUri())
            gate.complete(Unit)
            advanceUntilIdle()

            val progress = viewModel.uiState.value.progress as ImportProgress.Error
            assertEquals("import failed", progress.message)
        }

    private fun testUri(): Uri = Uri.parse("content://boksl/import.json")
}

private class FakeImportRepository(
    private val flowFactory: () -> Flow<ImportProgress> = {
        flowOf(
            ImportProgress.Completed(
                ImportResult(
                    addedSessionCount = 0,
                    duplicateSessionCount = 0,
                    appliedProfile = false,
                    wasDuplicateFile = false,
                ),
            ),
        )
    },
) : ImportRepository {
    var callCount: Int = 0
        private set

    override fun importAllData(uri: Uri): Flow<ImportProgress> {
        callCount += 1
        return flowFactory()
    }
}
