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
        val repository = FakeMakRepository()
        repository.clearActiveSemester()
        AppViewModel(repository)
        advanceUntilIdle()

        assertEquals(1L, repository.activeSemesterId)
    }

    @Test
    fun emptyDatabaseStaysOnToday() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        repository.clearSemesters()
        val viewModel = AppViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(MakDestination.Today, state.destination)
        assertTrue(state.hasLoadedData)
        assertTrue(state.requiresSetup)
        assertNull(state.setupResume)
    }

    @Test
    fun doesNotRequireSetupBeforeDataLoads() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        repository.occurrenceDataGate = CompletableDeferred()
        val viewModel = AppViewModel(repository)
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
        val repository = FakeMakRepository()
        repository.setActiveSemester(2)
        val viewModel = AppViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.requiresSetup)
        assertEquals(2L, state.setupResume?.semesterId)
    }

    @Test
    fun navigateUpdatesDestination() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val viewModel = AppViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.navigate(MakDestination.Settings)
        advanceUntilIdle()

        assertEquals(MakDestination.Settings, viewModel.uiState.value.destination)
    }
}
