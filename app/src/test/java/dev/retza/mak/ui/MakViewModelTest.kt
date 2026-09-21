package dev.retza.mak.ui

import dev.retza.mak.ui.feedback.FeedbackController
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import dev.retza.mak.ui.edit.ClassEditViewModel
import dev.retza.mak.ui.settings.InMemorySettingsPreferences
import dev.retza.mak.ui.settings.SettingsViewModel
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MakViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()
    private val clock = Clock.fixed(
        Instant.parse("2026-09-21T08:00:00Z"),
        ZoneId.of("Europe/Warsaw")
    )

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(mainDispatcher)

    @Test
    fun feedbackFlowEmitsPublishedMessagesOnce() = runTest(mainDispatcher) {
        val controller = FeedbackController()
        val repository = FakeMakRepository()
        val classEditViewModel = ClassEditViewModel(repository, controller)
        val settingsViewModel = SettingsViewModel(repository, InMemorySettingsPreferences(), controller)
        val viewModel = MakViewModel(repository, controller, classEditViewModel, settingsViewModel, clock)
        val received = mutableListOf<UiFeedback>()
        backgroundScope.launch(mainDispatcher) {
            controller.feedback.collect { received += it }
        }
        advanceUntilIdle()

        viewModel.publishFeedback(UiFeedbackKind.Info, "Test")
        viewModel.publishFeedback(UiFeedbackKind.Error, "Błąd")
        advanceUntilIdle()

        assertEquals(listOf("Test", "Błąd"), received.map { it.message })
    }

    @Test
    fun changingSemesterDropsFilterThatNoLongerExists() = runTest(mainDispatcher) {
        val controller = FeedbackController()
        val repository = FakeMakRepository()
        val classEditViewModel = ClassEditViewModel(repository, controller)
        val settingsViewModel = SettingsViewModel(repository, InMemorySettingsPreferences(), controller)
        val viewModel = MakViewModel(repository, controller, classEditViewModel, settingsViewModel, clock)
        backgroundScope.launch(mainDispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.selectCourseFilter("1")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.schedule.filters.any { it.id == "1" && it.isSelected })

        settingsViewModel.selectSemester("2")
        advanceUntilIdle()

        val filters = viewModel.uiState.value.schedule.filters
        assertTrue(filters.any { it.id == "all" && it.isSelected })
        assertTrue(filters.none { it.id == "1" })
    }
}
