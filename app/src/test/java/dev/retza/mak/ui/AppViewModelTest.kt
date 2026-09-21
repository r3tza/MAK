package dev.retza.mak.ui

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
    fun exposesActiveSemesterData() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val viewModel = AppViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(1L, viewModel.uiState.value.activeSemesterData?.semester?.id)
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
