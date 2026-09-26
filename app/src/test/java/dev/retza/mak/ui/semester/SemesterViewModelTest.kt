package dev.retza.mak.ui.semester

import dev.retza.mak.data.entity.WeekOverrideEntity
import dev.retza.mak.data.entity.WeekOverrideScope
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.ui.FakeRepository
import dev.retza.mak.ui.FakeSemesterRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SemesterViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(mainDispatcher)

    private fun viewModel(repository: FakeRepository) =
        SemesterViewModel(FakeSemesterRepository(repository), FeedbackController())

    private fun recordingViewModel(repository: FakeRepository, sink: RecordingFeedbackSink) =
        SemesterViewModel(FakeSemesterRepository(repository), sink)

    @Test
    fun openMapsFormCoursesAndOverrides() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        repository.weekOverrides += WeekOverrideEntity(
            id = 5L,
            semesterId = 1L,
            academicCalendarId = 1L,
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
        assertEquals(listOf("1" to "Informatyka"), state.courseItems.map { it.assignmentId to it.name })
        assertEquals(1, state.overrides.size)
        assertEquals("2026-10-05", state.overrides.single().weekStartDate)
        assertEquals(WeekTypeUi.B, state.overrides.single().weekType)
        assertEquals(WeekOverrideScopeUi.ONE_WEEK, state.overrides.single().scope)
        assertEquals(1L, viewModel.semesterId.value)
    }

    @Test
    fun openWaitsForFirstDataEmission() = runTest(mainDispatcher) {
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
        assertTrue(repository.events.none { it == "updateSemester" })
    }

    @Test
    fun saveRunsOnceAndPublishesSuccessWithOneEffect() = runTest(mainDispatcher) {
        val repository = FakeRepository()
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

        assertEquals(1, repository.events.count { it == "updateSemesterWithCalendar" })
        assertEquals(listOf(UiFeedback("Zapisano semestr", UiFeedbackKind.Success)), sink.published)
        assertEquals(listOf(SemesterEffect.CloseConfiguration), effects)
        assertFalse(viewModel.semester.value.semester.isSaving)
    }

    @Test
    fun saveErrorKeepsFormAndPublishesError() = runTest(mainDispatcher) {
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
        assertTrue(repository.events.none { it == "saveStudyProgramAssignment" })
    }

    @Test
    fun addCourseUsesDefaultColorAndPublishesSuccess() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        viewModel.updateCourseName("Fizyka")
        viewModel.updateCourseColor("")
        viewModel.addCourse()
        advanceUntilIdle()

        assertEquals("#137b71", repository.studyPrograms.first { it.name == "Fizyka" }.color)
        assertEquals("", viewModel.semester.value.courseNameDraft)
        assertEquals(
            listOf(UiFeedback("Dodano kierunek", UiFeedbackKind.Success)),
            sink.published
        )
    }

    @Test
    fun addCourseRunsOnce() = runTest(mainDispatcher) {
        val repository = FakeRepository()
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

        assertEquals(1, repository.events.count { it == "saveStudyProgramAssignment" })
        assertEquals(1, sink.published.size)
        assertEquals(2, repository.studyPrograms.size)
    }

    @Test
    fun addCourseErrorKeepsDraftAndPublishesError() = runTest(mainDispatcher) {
        val repository = FakeRepository()
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
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        viewModel.requestCourseDeletion("1")
        advanceUntilIdle()
        repository.saveGate = CompletableDeferred()
        viewModel.confirmCourseDeletion()
        viewModel.confirmCourseDeletion()
        advanceUntilIdle()
        repository.saveGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.events.count { it == "deleteSemesterProgram" })
        assertTrue(repository.semesterPrograms.isEmpty())
        assertEquals(
            listOf(UiFeedback("Usunięto kierunek", UiFeedbackKind.Success)),
            sink.published
        )
        assertFalse(viewModel.semester.value.isDeletingCourse)
        assertNull(viewModel.semester.value.pendingCourseDeletion)
    }

    @Test
    fun requestCourseDeletionAsksForConfirmationWithClassCount() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = recordingViewModel(repository, RecordingFeedbackSink())
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        viewModel.requestCourseDeletion("1")
        advanceUntilIdle()

        val pending = viewModel.semester.value.pendingCourseDeletion
        assertEquals("1", pending?.assignmentId)
        assertEquals(repository.classes.count { it.semesterProgramId == 1L }, pending?.classCount)
        assertFalse("deleteSemesterProgram" in repository.events)
        assertEquals(1, repository.semesterPrograms.size)
    }

    @Test
    fun cancelCourseDeletionKeepsCourse() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = recordingViewModel(repository, RecordingFeedbackSink())
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        viewModel.requestCourseDeletion("1")
        advanceUntilIdle()
        viewModel.cancelCourseDeletion()
        viewModel.confirmCourseDeletion()
        advanceUntilIdle()

        assertNull(viewModel.semester.value.pendingCourseDeletion)
        assertFalse("deleteSemesterProgram" in repository.events)
        assertEquals(1, repository.semesterPrograms.size)
    }

    @Test
    fun deleteCourseErrorKeepsItemAndPublishesError() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        viewModel.requestCourseDeletion("1")
        advanceUntilIdle()
        repository.failSaves = true
        viewModel.confirmCourseDeletion()
        advanceUntilIdle()

        assertEquals(1, repository.studyPrograms.size)
        assertEquals(1, sink.published.size)
        assertEquals(UiFeedbackKind.Error, sink.published.single().kind)
        assertEquals("Nie udało się usunąć kierunku.", sink.published.single().message)
        assertFalse(viewModel.semester.value.isDeletingCourse)
    }

    @Test
    fun addCourseCancellationDoesNotPublish() = runTest(mainDispatcher) {
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
        val repository = FakeRepository()
        repository.weekOverrides += WeekOverrideEntity(
            id = 5L,
            semesterId = 1L,
            academicCalendarId = 1L,
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
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
        val repository = FakeRepository()
        repository.weekOverrides += WeekOverrideEntity(
            id = 5L,
            semesterId = 1L,
            academicCalendarId = 1L,
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
        val repository = FakeRepository()
        repository.weekOverrides += WeekOverrideEntity(
            id = 5L,
            semesterId = 1L,
            academicCalendarId = 1L,
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

    @Test
    fun addCourseWithSeparateCalendarCreatesCopy() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()
        val sourceCalendarId = viewModel.semester.value.selectedCalendarId

        viewModel.updateCourseName("Fizyka")
        viewModel.setCourseCalendarMode(CourseCalendarModeUi.SEPARATE)
        viewModel.addCourse()
        advanceUntilIdle()

        assertEquals(1, repository.events.count { it == "addSeparatedSemesterProgram" })
        val newProgramId = repository.studyPrograms.first { it.name == "Fizyka" }.id
        val assignment = repository.semesterPrograms.first { it.studyProgramId == newProgramId }
        assertTrue(assignment.academicCalendarId.toString() != sourceCalendarId)
        assertEquals(2, viewModel.semester.value.calendars.size)
        assertEquals(
            listOf(UiFeedback("Dodano kierunek", UiFeedbackKind.Success)),
            sink.published
        )
    }

    @Test
    fun addCourseSharedModeUsesSelectedCalendar() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        viewModel.updateCourseName("Fizyka")
        viewModel.addCourse()
        advanceUntilIdle()

        assertEquals(1, repository.events.count { it == "saveStudyProgramAssignment" })
        assertEquals(1, viewModel.semester.value.calendars.size)
        assertEquals(2, viewModel.semester.value.courseItems.size)
    }

    @Test
    fun selectCourseProgramPrefillsNameAndColor() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        viewModel.selectCourseProgram("1")

        assertEquals("Informatyka", viewModel.semester.value.courseNameDraft)
        assertEquals("#137B71", viewModel.semester.value.courseColorDraft)
        assertEquals("1", viewModel.semester.value.courseProgramId)
    }

    @Test
    fun addCourseRejectsProgramAlreadyAssigned() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        viewModel.selectCourseProgram("1")
        viewModel.addCourse()
        advanceUntilIdle()

        assertNotNull(viewModel.semester.value.courseNameError)
        assertTrue(repository.events.none { it == "saveStudyProgramAssignment" })
        assertTrue(sink.published.isEmpty())
    }

    @Test
    fun refreshDropsCourseCalendarThatWasRemoved() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        viewModel.update { it.copy(courseCalendarId = "999") }
        viewModel.selectCalendar(repository.calendar.id.toString())
        advanceUntilIdle()

        assertEquals(repository.calendar.id.toString(), viewModel.semester.value.courseCalendarId)
    }

    @Test
    fun selectingOverrideCalendarShowsOnlyItsOverrides() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        viewModel.updateCourseName("Fizyka")
        viewModel.setCourseCalendarMode(CourseCalendarModeUi.SEPARATE)
        viewModel.addCourse()
        advanceUntilIdle()
        val newProgramId = repository.studyPrograms.first { it.name == "Fizyka" }.id
        val separatedCalendarId = repository.semesterPrograms
            .first { it.studyProgramId == newProgramId }
            .academicCalendarId
        repository.weekOverrides += WeekOverrideEntity(
            id = 6L,
            semesterId = 1L,
            academicCalendarId = repository.calendar.id,
            weekStartDate = LocalDate.of(2026, 9, 28),
            weekType = WeekType.B,
            scope = WeekOverrideScope.ONE_WEEK
        )
        repository.weekOverrides += WeekOverrideEntity(
            id = 7L,
            semesterId = 1L,
            academicCalendarId = separatedCalendarId,
            weekStartDate = LocalDate.of(2026, 10, 5),
            weekType = WeekType.A,
            scope = WeekOverrideScope.ONE_WEEK
        )

        viewModel.selectCalendar(repository.calendar.id.toString())
        advanceUntilIdle()
        assertEquals(1, viewModel.semester.value.overrides.size)
        assertEquals(2, viewModel.semester.value.overrideCount)

        viewModel.selectCalendar(separatedCalendarId.toString())
        advanceUntilIdle()
        assertEquals(1, viewModel.semester.value.overrides.size)
        assertEquals(2, viewModel.semester.value.overrideCount)
    }

    @Test
    fun reconnectMarksUnusedSourceAndMovesAssignment() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        viewModel.updateCourseName("Fizyka")
        viewModel.setCourseCalendarMode(CourseCalendarModeUi.SEPARATE)
        viewModel.addCourse()
        advanceUntilIdle()
        val newProgramId = repository.studyPrograms.first { it.name == "Fizyka" }.id
        val separatedCalendarId = repository.semesterPrograms
            .first { it.studyProgramId == newProgramId }
            .academicCalendarId
        val informatykaId = viewModel.semester.value.courseItems
            .first { it.name == "Informatyka" }
            .assignmentId

        viewModel.requestReconnect(informatykaId, separatedCalendarId.toString())
        advanceUntilIdle()
        assertEquals(true, viewModel.semester.value.pendingReconnect?.sourceBecomesUnused)
        assertEquals("Informatyka", viewModel.semester.value.pendingReconnect?.programName)

        viewModel.confirmReconnect()
        advanceUntilIdle()

        assertEquals(1, repository.events.count { it == "reconnectSemesterProgram" })
        assertEquals(null, viewModel.semester.value.pendingReconnect)
        assertTrue(repository.calendars.none { it.id == repository.calendar.id })
    }

    @Test
    fun separateCourseCalendarPublishesSuccess() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()

        val assignmentId = viewModel.semester.value.courseItems.first().assignmentId
        viewModel.separateCourseCalendar(assignmentId)
        advanceUntilIdle()

        assertEquals(1, repository.events.count { it == "separateSemesterProgramCalendar" })
        assertEquals(2, viewModel.semester.value.calendars.size)
        assertEquals(
            listOf(UiFeedback("Rozdzielono kalendarz kierunku", UiFeedbackKind.Success)),
            sink.published
        )
    }

    @Test
    fun unparsableIdClearsPreviousSemester() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.open("1")
        advanceUntilIdle()
        assertEquals("Semestr", viewModel.semester.value.semester.name)

        viewModel.open("abc")
        advanceUntilIdle()

        assertEquals("", viewModel.semester.value.semester.name)
        assertEquals(null, viewModel.semesterId.value)
    }

    @Test
    fun lateSaveFromPreviousSessionDoesNotCloseOrModifyCurrentSemester() = runTest(mainDispatcher) {
        val repository = FakeRepository()
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
        advanceUntilIdle()

        viewModel.open("2")
        advanceUntilIdle()
        assertEquals("Semestr drugi", viewModel.semester.value.semester.name)

        repository.saveGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals("Semestr drugi", viewModel.semester.value.semester.name)
        assertEquals(2L, viewModel.semesterId.value)
        assertEquals(2L, repository.activeSemesterId)
        assertTrue(effects.isEmpty())
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
