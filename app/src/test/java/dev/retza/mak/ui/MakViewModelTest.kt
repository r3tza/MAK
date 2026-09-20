package dev.retza.mak.ui

import dev.retza.mak.ui.feedback.FeedbackController
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import dev.retza.mak.ui.edit.ClassEditViewModel
import dev.retza.mak.ui.setup.SetupViewModel
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
        val setupViewModel = SetupViewModel(repository, controller)
        val viewModel = MakViewModel(repository, controller, classEditViewModel, setupViewModel, clock)
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
}
