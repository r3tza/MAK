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

    @Test
    fun addCourseRejectsBlankName() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        viewModel.updateCourseName("   ")
        viewModel.addCourse()
        advanceUntilIdle()

        assertNotNull(viewModel.semester.value.courseNameError)
        assertTrue(sink.published.isEmpty())
        assertTrue(repository.events.none { it == "saveCourse" })
    }

    @Test
    fun addCourseUsesDefaultColorAndPublishesSuccess() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        viewModel.updateCourseName("Fizyka")
        viewModel.updateCourseColor("")
        viewModel.addCourse()
        advanceUntilIdle()

        assertEquals("#137b71", repository.courses.first { it.name == "Fizyka" }.color)
        assertEquals("", viewModel.semester.value.courseNameDraft)
        assertEquals(
            listOf(UiFeedback("Dodano kierunek", UiFeedbackKind.Success)),
            sink.published
        )
    }

    @Test
    fun addCourseRunsOnce() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        viewModel.updateCourseName("Fizyka")
        repository.saveGate = CompletableDeferred()
        viewModel.addCourse()
        viewModel.addCourse()
        advanceUntilIdle()
        repository.saveGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.events.count { it == "saveCourse" })
        assertEquals(1, sink.published.size)
        assertEquals(2, repository.courses.size)
    }

    @Test
    fun addCourseErrorKeepsDraftAndPublishesError() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        viewModel.updateCourseName("Fizyka")
        repository.failSaves = true
        viewModel.addCourse()
        advanceUntilIdle()

        assertEquals("Fizyka", viewModel.semester.value.courseNameDraft)
        assertFalse(viewModel.semester.value.isAddingCourse)
        assertEquals(1, sink.published.size)
        assertEquals(UiFeedbackKind.Error, sink.published.single().kind)
        assertEquals("Nie udało się dodać kierunku.", sink.published.single().message)
    }

    @Test
    fun deleteCourseRunsOnceAndPublishesSuccess() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        repository.saveGate = CompletableDeferred()
        viewModel.deleteCourse("1")
        viewModel.deleteCourse("1")
        advanceUntilIdle()
        repository.saveGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.events.count { it == "deleteCourse" })
        assertTrue(repository.courses.isEmpty())
        assertEquals(
            listOf(UiFeedback("Usunięto kierunek", UiFeedbackKind.Success)),
            sink.published
        )
        assertFalse(viewModel.semester.value.isDeletingCourse)
    }

    @Test
    fun deleteCourseErrorKeepsItemAndPublishesError() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        repository.failSaves = true
        viewModel.deleteCourse("1")
        advanceUntilIdle()

        assertEquals(1, repository.courses.size)
        assertEquals(1, sink.published.size)
        assertEquals(UiFeedbackKind.Error, sink.published.single().kind)
        assertEquals("Nie udało się usunąć kierunku.", sink.published.single().message)
        assertFalse(viewModel.semester.value.isDeletingCourse)
    }

    @Test
    fun addCourseCancellationDoesNotPublish() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        viewModel.updateCourseName("Fizyka")
        repository.cancelSaves = true
        viewModel.addCourse()
        advanceUntilIdle()

        assertTrue(sink.published.isEmpty())
        assertFalse(viewModel.semester.value.isAddingCourse)
    }

    @Test
    fun saveOverrideRejectsNonMonday() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        viewModel.newWeekOverride()
        viewModel.update { it.copy(overrideForm = it.overrideForm.copy(weekStartDate = "2026-10-06")) }
        viewModel.saveWeekOverride()
        advanceUntilIdle()

        assertNotNull(viewModel.semester.value.overrideForm.weekStartDateError)
        assertTrue(sink.published.isEmpty())
        assertTrue(repository.events.none { it == "saveWeekOverride" })
    }

    @Test
    fun saveNewOverridePublishesAddedMessage() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        viewModel.newWeekOverride()
        viewModel.update {
            it.copy(
                overrideForm = it.overrideForm.copy(
                    weekStartDate = "2026-10-05",
                    weekType = WeekTypeUi.B,
                    scope = WeekOverrideScopeUi.FROM_WEEK
                )
            )
        }
        viewModel.saveWeekOverride()
        advanceUntilIdle()

        assertEquals(1, repository.weekOverrides.size)
        assertEquals(WeekType.B, repository.weekOverrides.single().weekType)
        assertFalse(viewModel.semester.value.overrideForm.isOpen)
        assertEquals(
            listOf(UiFeedback("Dodano korektę tygodnia", UiFeedbackKind.Success)),
            sink.published
        )
    }

    @Test
    fun saveEditedOverridePublishesSavedMessage() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        repository.weekOverrides += WeekOverrideEntity(
            id = 5L,
            semesterId = 1L,
            weekStartDate = LocalDate.of(2026, 10, 5),
            weekType = WeekType.A,
            scope = WeekOverrideScope.ONE_WEEK
        )
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        viewModel.editWeekOverride("5")
        advanceUntilIdle()
        assertEquals("5", viewModel.semester.value.overrideForm.id)

        viewModel.update { it.copy(overrideForm = it.overrideForm.copy(weekType = WeekTypeUi.B)) }
        viewModel.saveWeekOverride()
        advanceUntilIdle()

        assertEquals(
            listOf(UiFeedback("Zapisano korektę tygodnia", UiFeedbackKind.Success)),
            sink.published
        )
        assertEquals(WeekType.B, repository.weekOverrides.single().weekType)
    }

    @Test
    fun saveOverrideRunsOnce() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        viewModel.newWeekOverride()
        viewModel.update { it.copy(overrideForm = it.overrideForm.copy(weekStartDate = "2026-10-05")) }
        repository.saveGate = CompletableDeferred()
        viewModel.saveWeekOverride()
        viewModel.saveWeekOverride()
        advanceUntilIdle()
        repository.saveGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.events.count { it == "saveWeekOverride" })
        assertEquals(1, sink.published.size)
    }

    @Test
    fun saveOverrideErrorKeepsFormOpen() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        viewModel.newWeekOverride()
        viewModel.update { it.copy(overrideForm = it.overrideForm.copy(weekStartDate = "2026-10-05")) }
        repository.failSaves = true
        viewModel.saveWeekOverride()
        advanceUntilIdle()

        assertTrue(viewModel.semester.value.overrideForm.isOpen)
        assertEquals("2026-10-05", viewModel.semester.value.overrideForm.weekStartDate)
        assertFalse(viewModel.semester.value.overrideForm.isSaving)
        assertEquals(1, sink.published.size)
        assertEquals(UiFeedbackKind.Error, sink.published.single().kind)
    }

    @Test
    fun deleteOverrideRunsOnceAndPublishesSuccess() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        repository.weekOverrides += WeekOverrideEntity(
            id = 5L,
            semesterId = 1L,
            weekStartDate = LocalDate.of(2026, 10, 5),
            weekType = WeekType.A,
            scope = WeekOverrideScope.ONE_WEEK
        )
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        repository.saveGate = CompletableDeferred()
        viewModel.deleteWeekOverride("5")
        viewModel.deleteWeekOverride("5")
        advanceUntilIdle()
        repository.saveGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.events.count { it == "deleteWeekOverride" })
        assertTrue(repository.weekOverrides.isEmpty())
        assertEquals(
            listOf(UiFeedback("Usunięto korektę tygodnia", UiFeedbackKind.Success)),
            sink.published
        )
        assertFalse(viewModel.semester.value.isDeletingOverride)
    }

    @Test
    fun deleteOverrideCancellationDoesNotPublish() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        repository.weekOverrides += WeekOverrideEntity(
            id = 5L,
            semesterId = 1L,
            weekStartDate = LocalDate.of(2026, 10, 5),
            weekType = WeekType.A,
            scope = WeekOverrideScope.ONE_WEEK
        )
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        repository.cancelSaves = true
        viewModel.deleteWeekOverride("5")
        advanceUntilIdle()

        assertTrue(sink.published.isEmpty())
        assertFalse(viewModel.semester.value.isDeletingOverride)
    }
}

private class RecordingFeedbackSink : FeedbackSink {
    val published = mutableListOf<UiFeedback>()

    override fun publish(feedback: UiFeedback) {
        published += feedback
    }
}
