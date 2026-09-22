package dev.retza.mak.ui

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(mainDispatcher)

    @Test
    fun selectsFirstSemesterWhenNoneActive() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        repository.clearActiveSemester()
        AppViewModel(FakeSemesterRepository(repository), repository)
        advanceUntilIdle()

        assertEquals(1L, repository.activeSemesterId)
    }

    @Test
    fun emptyDatabaseRequiresSetup() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        repository.clearSemesters()
        val viewModel = AppViewModel(FakeSemesterRepository(repository), repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasLoadedData)
        assertTrue(state.requiresSetup)
        assertNull(state.setupResume)
    }

    @Test
    fun doesNotRequireSetupBeforeDataLoads() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        repository.occurrenceDataGate = CompletableDeferred()
        val viewModel = AppViewModel(FakeSemesterRepository(repository), repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasLoadedData)
        assertFalse(viewModel.uiState.value.requiresSetup)
        assertNull(viewModel.uiState.value.setupResume)

        repository.occurrenceDataGate?.complete(Unit)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasLoadedData)
        assertFalse(viewModel.uiState.value.requiresSetup)
    }

    @Test
    fun resumesSemesterWithoutCourses() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        repository.setActiveSemester(2)
        val viewModel = AppViewModel(FakeSemesterRepository(repository), repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.requiresSetup)
        assertEquals(2L, state.setupResume?.semesterId)
    }
}
