package dev.retza.mak.ui.semester

import dev.retza.mak.data.entity.WeekOverrideEntity
import dev.retza.mak.data.entity.WeekOverrideScope
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.ui.FakeMakRepository
import dev.retza.mak.ui.MainDispatcherRule
import dev.retza.mak.ui.feedback.FeedbackController
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SemesterViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(mainDispatcher)

    private fun viewModel(repository: FakeMakRepository) =
        SemesterViewModel(repository, FeedbackController())

    private fun recordingViewModel(repository: FakeMakRepository, sink: RecordingFeedbackSink) =
        SemesterViewModel(repository, sink)

    @Test
    fun openMapsFormCoursesAndOverrides() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        repository.weekOverrides += WeekOverrideEntity(
            id = 5L,
            semesterId = 1L,
            weekStartDate = LocalDate.of(2026, 10, 5),
            weekType = WeekType.B,
            scope = WeekOverrideScope.ONE_WEEK
        )
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.open("1")
        advanceUntilIdle()

        val state = viewModel.semester.value
        assertEquals("Semestr", state.semester.name)
        assertEquals("2026-09-01", state.semester.startDate)
        assertEquals("2026-10-31", state.semester.endDate)
        assertEquals(WeekTypeUi.A, state.semester.firstWeek)
        assertEquals(listOf("1" to "Informatyka"), state.courses)
        assertEquals(1, state.overrides.size)
        assertEquals("2026-10-05", state.overrides.single().weekStartDate)
        assertEquals(WeekTypeUi.B, state.overrides.single().weekType)
        assertEquals(WeekOverrideScopeUi.ONE_WEEK, state.overrides.single().scope)
        assertEquals(1L, viewModel.semesterId.value)
    }

    @Test
    fun openWaitsForFirstDataEmission() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        repository.occurrenceDataGate = CompletableDeferred()
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.open("1")
        advanceUntilIdle()
        assertEquals("", viewModel.semester.value.semester.name)
        assertEquals(null, viewModel.semesterId.value)

        repository.occurrenceDataGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals("Semestr", viewModel.semester.value.semester.name)
        assertEquals(1L, viewModel.semesterId.value)
    }

    @Test
    fun lastOpenWinsWhenDataArrivesLate() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        repository.occurrenceDataGate = CompletableDeferred()
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.open("1")
        viewModel.open("2")
        advanceUntilIdle()
        assertEquals("", viewModel.semester.value.semester.name)

        repository.occurrenceDataGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals("Semestr drugi", viewModel.semester.value.semester.name)
        assertEquals(2L, viewModel.semesterId.value)
    }

    @Test
    fun invalidIdDoesNotKeepPreviousSemester() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()
        assertEquals("Semestr", viewModel.semester.value.semester.name)

        viewModel.open("99")
        advanceUntilIdle()

        assertEquals("", viewModel.semester.value.semester.name)
        assertEquals(null, viewModel.semesterId.value)
    }

    @Test
    fun saveRejectsBlankNameAndReversedDates() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        viewModel.update { it.copy(semester = it.semester.copy(name = "")) }
        viewModel.saveSemester()
        advanceUntilIdle()
        assertNotNull(viewModel.semester.value.semester.nameError)

        viewModel.update {
            it.copy(
                semester = it.semester.copy(
                    name = "Semestr",
                    startDate = "2026-10-01",
                    endDate = "2026-09-01"
                )
            )
        }
        viewModel.saveSemester()
        advanceUntilIdle()
        assertNotNull(viewModel.semester.value.semester.dateRangeError)

        assertTrue(sink.published.isEmpty())
        assertTrue(repository.events.none { it == "saveSemester" })
    }

    @Test
    fun saveRunsOnceAndPublishesSuccessWithOneEffect() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        val effects = mutableListOf<SemesterEffect>()
        backgroundScope.launch(mainDispatcher) { viewModel.effects.collect { effects += it } }
        advanceUntilIdle()

        repository.saveGate = CompletableDeferred()
        viewModel.saveSemester()
        viewModel.saveSemester()
        advanceUntilIdle()
        repository.saveGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.events.count { it == "saveSemester" })
        assertEquals(listOf(UiFeedback("Zapisano semestr", UiFeedbackKind.Success)), sink.published)
        assertEquals(listOf(SemesterEffect.CloseConfiguration), effects)
        assertFalse(viewModel.semester.value.semester.isSaving)
    }

    @Test
    fun saveErrorKeepsFormAndPublishesError() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        val effects = mutableListOf<SemesterEffect>()
        backgroundScope.launch(mainDispatcher) { viewModel.effects.collect { effects += it } }
        advanceUntilIdle()

        repository.failSaves = true
        viewModel.saveSemester()
        advanceUntilIdle()

        assertEquals("Semestr", viewModel.semester.value.semester.name)
        assertFalse(viewModel.semester.value.semester.isSaving)
        assertNotNull(viewModel.semester.value.semester.dateRangeError)
        assertEquals(1, sink.published.size)
        assertEquals(UiFeedbackKind.Error, sink.published.single().kind)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun cancellationDoesNotPublishOrKeepSaving() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        repository.cancelSaves = true
        viewModel.saveSemester()
        advanceUntilIdle()

        assertTrue(sink.published.isEmpty())
        assertFalse(viewModel.semester.value.semester.isSaving)
    }
}

private class RecordingFeedbackSink : FeedbackSink {
    val published = mutableListOf<UiFeedback>()

    override fun publish(feedback: UiFeedback) {
        published += feedback
    }
}
