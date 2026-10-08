package dev.retza.mak.ui.setup

import dev.retza.mak.ui.FakeRepository
import dev.retza.mak.ui.FakeSemesterRepository
import dev.retza.mak.ui.MainDispatcherRule
import dev.retza.mak.ui.feedback.FeedbackController
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
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
class SetupViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(mainDispatcher)

    // Inside the calendar used by fillValidSemester, so a new semester is activated by default.
    private val insideSemester = Clock.fixed(Instant.parse("2026-09-15T10:00:00Z"), ZoneOffset.UTC)

    private fun viewModel(
        repository: FakeRepository,
        sink: FeedbackSink = FeedbackController(),
        clock: Clock = insideSemester
    ) = SetupViewModel(FakeSemesterRepository(repository), sink, clock)

    private fun SetupViewModel.fillValidSemester() {
        update { it.copy(semesterName = "Nowy", startDate = "2026-09-01", endDate = "2026-10-01") }
    }

    @Test
    fun invalidSemesterFormShowsErrorsWithoutAdvancing() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        viewModel.update {
            it.copy(semesterName = "  ", startDate = "2026-09-01", endDate = "2026-08-01")
        }

        viewModel.next()
        advanceUntilIdle()

        assertTrue(repository.events.isEmpty())
        assertTrue(viewModel.setup.value.errors.isNotEmpty())
        assertEquals(SetupStep.Semester, viewModel.setup.value.step)
    }

    @Test
    fun semesterStepOnlyValidatesAndAdvances() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        viewModel.fillValidSemester()

        viewModel.next()
        advanceUntilIdle()

        assertTrue(repository.events.isEmpty())
        assertEquals(SetupStep.Course, viewModel.setup.value.step)
    }

    @Test
    fun blankCourseNameShowsErrorWithoutWriting() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        viewModel.fillValidSemester()
        viewModel.next()
        advanceUntilIdle()

        viewModel.next()
        advanceUntilIdle()

        assertTrue(repository.events.isEmpty())
        assertTrue(viewModel.setup.value.errors.containsKey(SetupField.CourseName))
        assertEquals(SetupStep.Course, viewModel.setup.value.step)
    }

    @Test
    fun saveConfigurationWritesSemesterAndCourseTogether() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        viewModel.fillValidSemester()
        viewModel.next()
        advanceUntilIdle()
        viewModel.update { it.copy(courseName = "Informatyka", courseColor = "#123456") }

        viewModel.next()
        advanceUntilIdle()

        val semester = repository.lastSetupSemester
        val course = repository.lastSetupStudyProgram
        assertEquals(1, repository.events.count { it == "saveSetupConfiguration" })
        assertEquals("Nowy", semester?.name)
        assertTrue(semester?.isActive == true)
        assertEquals("Informatyka", course?.name)
        assertEquals(semester?.id, repository.lastSetupCalendar?.semesterId)
        assertEquals(SetupStep.Classes, viewModel.setup.value.step)
    }

    @Test
    fun doubleClickRunsOneTransactionAndOneFeedback() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        repository.saveGate = CompletableDeferred()
        val sink = RecordingFeedbackSink()
        val viewModel = viewModel(repository, sink)
        viewModel.fillValidSemester()
        viewModel.next()
        advanceUntilIdle()
        viewModel.update { it.copy(courseName = "Informatyka") }

        viewModel.next()
        viewModel.next()
        advanceUntilIdle()
        assertTrue(viewModel.setup.value.isSaving)

        repository.saveGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.events.count { it == "saveSetupConfiguration" })
        assertEquals(1, sink.published.size)
        assertFalse(viewModel.setup.value.isSaving)
    }

    @Test
    fun backAndResaveUpdatesExistingRecordsWithoutDuplicates() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        viewModel.fillValidSemester()
        viewModel.next()
        advanceUntilIdle()
        viewModel.update { it.copy(courseName = "Informatyka") }
        viewModel.next()
        advanceUntilIdle()
        val firstSemester = repository.lastSetupSemester?.id
        val firstCourse = repository.lastSetupStudyProgram?.id

        viewModel.back()
        viewModel.next()
        advanceUntilIdle()

        assertEquals(2, repository.events.count { it == "saveSetupConfiguration" })
        assertEquals(firstSemester, repository.lastSetupSemester?.id)
        assertEquals(firstCourse, repository.lastSetupStudyProgram?.id)
        assertEquals(SetupStep.Classes, viewModel.setup.value.step)
    }

    @Test
    fun existingProgramIsAssignedWithoutCreatingNewRecord() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val existing = repository.studyPrograms.first()
        val viewModel = viewModel(repository)
        viewModel.fillValidSemester()
        viewModel.next()
        advanceUntilIdle()

        viewModel.selectProgramMode(SetupProgramMode.Existing)
        viewModel.selectProgram(existing.id)
        viewModel.next()
        advanceUntilIdle()

        assertEquals(existing.id, repository.lastSetupStudyProgram?.id)
        assertEquals(existing.name, repository.lastSetupStudyProgram?.name)
        assertEquals(existing.color, repository.lastSetupStudyProgram?.color)
        assertEquals(SetupStep.Classes, viewModel.setup.value.step)
    }

    @Test
    fun existingModeWithoutSelectionShowsErrorWithoutWriting() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        viewModel.fillValidSemester()
        viewModel.next()
        advanceUntilIdle()

        viewModel.selectProgramMode(SetupProgramMode.Existing)
        viewModel.next()
        advanceUntilIdle()

        assertTrue(repository.events.isEmpty())
        assertTrue(viewModel.setup.value.errors.containsKey(SetupField.CourseProgram))
        assertEquals(SetupStep.Course, viewModel.setup.value.step)
    }

    @Test
    fun programChoiceIsLockedAfterSaveSoBackDoesNotRenameSharedProgram() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val existing = repository.studyPrograms.first()
        val viewModel = viewModel(repository)
        viewModel.fillValidSemester()
        viewModel.next()
        advanceUntilIdle()
        viewModel.selectProgramMode(SetupProgramMode.Existing)
        viewModel.selectProgram(existing.id)
        viewModel.next()
        advanceUntilIdle()

        viewModel.back()
        viewModel.selectProgramMode(SetupProgramMode.New)
        viewModel.update { it.copy(courseName = "Inna nazwa") }
        viewModel.next()
        advanceUntilIdle()

        assertTrue(viewModel.setup.value.isProgramChoiceLocked)
        assertEquals(SetupProgramMode.Existing, viewModel.setup.value.programMode)
        assertEquals(existing.id, repository.lastSetupStudyProgram?.id)
        assertEquals(existing.name, repository.lastSetupStudyProgram?.name)
    }

    @Test
    fun wizardListsExistingProgramsAndStartsInNewMode() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        val state = viewModel.setup.value
        assertEquals(repository.studyPrograms.map { it.id }, state.programOptions.map { it.id })
        assertEquals(SetupProgramMode.New, state.programMode)
    }

    @Test
    fun startDuringSaveResetsTheWizardWithoutFeedback() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        repository.saveGate = CompletableDeferred()
        val sink = RecordingFeedbackSink()
        val viewModel = viewModel(repository, sink)
        viewModel.fillValidSemester()
        viewModel.next()
        advanceUntilIdle()
        viewModel.update { it.copy(courseName = "Informatyka") }
        viewModel.next()
        advanceUntilIdle()
        assertTrue(viewModel.setup.value.isSaving)

        viewModel.start()
        advanceUntilIdle()

        assertEquals(SetupWizardUiState(), viewModel.setup.value.copy(programOptions = emptyList()))
        assertFalse(viewModel.setup.value.isSaving)
        assertTrue(sink.published.isEmpty())
    }

    @Test
    fun validationDoesNotPublishFeedback() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = viewModel(repository, sink)

        viewModel.next()
        advanceUntilIdle()
        viewModel.fillValidSemester()
        viewModel.next()
        advanceUntilIdle()
        viewModel.next()
        advanceUntilIdle()

        assertTrue(sink.published.isEmpty())
    }

    @Test
    fun successfulSavePublishesSingleMessage() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = viewModel(repository, sink)
        viewModel.fillValidSemester()
        viewModel.next()
        advanceUntilIdle()
        viewModel.update { it.copy(courseName = "Informatyka") }

        viewModel.next()
        advanceUntilIdle()

        assertEquals(listOf("Utworzono semestr i kierunek"), sink.published.map { it.message })
    }

    @Test
    fun semesterStartingLaterIsSavedInactiveWhenAnotherIsActive() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val previousActive = repository.activeSemesterId
        val sink = RecordingFeedbackSink()
        val before = Clock.fixed(Instant.parse("2026-08-20T10:00:00Z"), ZoneOffset.UTC)
        val viewModel = viewModel(repository, sink, before)
        viewModel.fillValidSemester()
        viewModel.next()
        advanceUntilIdle()
        viewModel.update { it.copy(courseName = "Informatyka") }

        viewModel.next()
        advanceUntilIdle()

        assertEquals(false, repository.lastSetupActivate)
        assertEquals(previousActive, repository.activeSemesterId)
        assertFalse(viewModel.setup.value.isSemesterActive)
        assertEquals(SetupStep.Classes, viewModel.setup.value.step)
        assertEquals(
            listOf("Utworzono semestr i kierunek. Aktywny semestr się nie zmienił."),
            sink.published.map { it.message }
        )
    }

    @Test
    fun semesterCoveringTodayIsActivated() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        viewModel.fillValidSemester()
        viewModel.next()
        advanceUntilIdle()
        viewModel.update { it.copy(courseName = "Informatyka") }

        viewModel.next()
        advanceUntilIdle()

        assertEquals(true, repository.lastSetupActivate)
        assertEquals(repository.lastSetupSemester?.id, repository.activeSemesterId)
        assertTrue(viewModel.setup.value.isSemesterActive)
    }

    @Test
    fun activateAndAddClassActivatesSavedSemesterThenOpensEditor() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val before = Clock.fixed(Instant.parse("2026-08-20T10:00:00Z"), ZoneOffset.UTC)
        val viewModel = viewModel(repository, clock = before)
        val effects = mutableListOf<SetupEffect>()
        backgroundScope.launch { viewModel.effects.collect { effects += it } }
        viewModel.fillValidSemester()
        viewModel.next()
        advanceUntilIdle()
        viewModel.update { it.copy(courseName = "Informatyka") }
        viewModel.next()
        advanceUntilIdle()
        val activations = repository.setActiveCount

        viewModel.activateAndAddClass()
        advanceUntilIdle()

        assertEquals(activations + 1, repository.setActiveCount)
        assertTrue(viewModel.setup.value.isSemesterActive)
        assertEquals(listOf(SetupEffect.OpenNewClassEditor), effects)
    }

    @Test
    fun finishAndReturnToSettingsEmitOneEffect() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        val effects = mutableListOf<SetupEffect>()
        backgroundScope.launch { viewModel.effects.collect { effects += it } }
        advanceUntilIdle()

        viewModel.finish()
        advanceUntilIdle()
        viewModel.addClass()
        advanceUntilIdle()
        viewModel.returnToSettings()
        advanceUntilIdle()

        assertEquals(
            listOf(
                SetupEffect.FinishToToday,
                SetupEffect.OpenNewClassEditor,
                SetupEffect.ReturnToSettings
            ),
            effects
        )
        assertEquals(SetupWizardUiState(), viewModel.setup.value.copy(programOptions = emptyList()))
    }

    @Test
    fun startResumesExistingSemesterAtCourseStep() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)

        viewModel.start(
            SetupSemesterResume(
                semesterId = repository.semester.id,
                calendarId = repository.calendar.id,
                name = repository.semester.name,
                startDate = repository.calendar.startDate.toString(),
                endDate = repository.calendar.endDate.toString(),
                firstWeekLabel = repository.calendar.firstWeekType.name
            )
        )

        val state = viewModel.setup.value
        assertEquals(SetupStep.Course, state.step)
        assertEquals("Semestr", state.semesterName)
        assertEquals("2026-09-01", state.startDate)
        assertEquals("2026-10-31", state.endDate)
        assertEquals("A", state.firstWeekLabel)

        viewModel.update { it.copy(courseName = "Nowy kierunek") }
        viewModel.next()
        advanceUntilIdle()

        assertEquals(repository.semester.id, repository.lastSetupSemester?.id)
        assertEquals(1, repository.events.count { it == "saveSetupConfiguration" })
        assertEquals(SetupStep.Classes, viewModel.setup.value.step)
    }

    @Test
    fun addingSemesterDoesNotResumeAnActiveIncompleteSemester() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val snapshot = repository.snapshot()
        repository.replaceAll(
            snapshot.copy(
                semesters = snapshot.semesters.map { semester ->
                    if (semester.semester.id == repository.semester.id) {
                        semester.copy(programs = emptyList(), classes = emptyList())
                    } else {
                        semester
                    }
                }
            )
        )
        repository.events.clear()
        val viewModel = viewModel(repository)

        viewModel.startFromRoom(resumeExisting = false)

        val state = viewModel.setup.value
        assertEquals(SetupStep.Semester, state.step)
        assertEquals("", state.semesterName)
        assertEquals("", state.startDate)
        assertEquals("", state.endDate)
        assertTrue(repository.events.isEmpty())
    }

    @Test
    fun startFromRoomRefreshesProgramOptionsAfterReplacement() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        val snapshot = repository.snapshot()
        repository.replaceAll(
            snapshot.copy(
                studyPrograms = snapshot.studyPrograms.map { it.copy(name = "Zdalny kierunek") }
            )
        )

        viewModel.startFromRoom(resumeExisting = false)

        assertEquals(listOf("Zdalny kierunek"), viewModel.setup.value.programOptions.map { it.name })
    }

    @Test
    fun backMovesThroughStepsInOrder() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        viewModel.fillValidSemester()
        viewModel.next()
        advanceUntilIdle()
        viewModel.update { it.copy(courseName = "Informatyka") }
        viewModel.next()
        advanceUntilIdle()
        assertEquals(SetupStep.Classes, viewModel.setup.value.step)

        viewModel.back()
        assertEquals(SetupStep.Course, viewModel.setup.value.step)
        viewModel.back()
        assertEquals(SetupStep.Semester, viewModel.setup.value.step)
    }

    private suspend fun kotlinx.coroutines.test.TestScope.savedFirstProgram(
        repository: FakeRepository,
        name: String = "Matematyka"
    ): SetupViewModel {
        val viewModel = viewModel(repository)
        viewModel.fillValidSemester()
        viewModel.next()
        advanceUntilIdle()
        viewModel.update { it.copy(courseName = name) }
        viewModel.next()
        advanceUntilIdle()
        repository.events.clear()
        return viewModel
    }

    @Test
    fun anotherProgramSharesTheFirstProgramCalendarByDefault() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = savedFirstProgram(repository)
        val firstCalendarId = repository.lastSetupConfiguration!!.academicCalendarId

        viewModel.startAnotherProgram()
        assertTrue(viewModel.setup.value.isAddingAnotherProgram)
        assertEquals("", viewModel.setup.value.courseName)
        assertTrue(viewModel.setup.value.courseColor != repository.lastSetupStudyProgram?.color)
        viewModel.update { it.copy(courseName = "Fizyka") }
        viewModel.next()
        advanceUntilIdle()

        assertEquals(listOf("saveStudyProgramAssignment"), repository.events)
        val added = repository.semesterPrograms.last()
        assertEquals(firstCalendarId, added.academicCalendarId)
        val state = viewModel.setup.value
        assertEquals(SetupStep.Classes, state.step)
        assertFalse(state.isAddingAnotherProgram)
        assertEquals(listOf("Matematyka", "Fizyka"), state.semesterProgramNames)
        assertEquals("Matematyka", state.courseName)
    }

    @Test
    fun separateWeeksCreateTheirOwnCalendar() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = savedFirstProgram(repository)
        val firstCalendarId = repository.lastSetupConfiguration!!.academicCalendarId

        viewModel.startAnotherProgram()
        viewModel.update { it.copy(courseName = "Fizyka") }
        viewModel.selectCalendarMode(SetupCalendarMode.Separate)
        viewModel.next()
        advanceUntilIdle()

        assertEquals(listOf("addSeparatedSemesterProgram"), repository.events)
        assertTrue(repository.semesterPrograms.last().academicCalendarId != firstCalendarId)
        assertEquals(SetupStep.Classes, viewModel.setup.value.step)
    }

    @Test
    fun backFromAnotherProgramRestoresTheFirstWithoutWriting() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = savedFirstProgram(repository)

        viewModel.startAnotherProgram()
        viewModel.update { it.copy(courseName = "Porzucony") }
        viewModel.back()

        assertTrue(repository.events.isEmpty())
        val state = viewModel.setup.value
        assertEquals(SetupStep.Classes, state.step)
        assertEquals("Matematyka", state.courseName)
        assertEquals(listOf("Matematyka"), state.semesterProgramNames)
    }

    @Test
    fun blankAnotherProgramShowsErrorWithoutWriting() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = savedFirstProgram(repository)

        viewModel.startAnotherProgram()
        viewModel.next()
        advanceUntilIdle()

        assertTrue(repository.events.isEmpty())
        assertTrue(viewModel.setup.value.errors.containsKey(SetupField.CourseName))
        assertEquals(SetupStep.Course, viewModel.setup.value.step)
    }

    @Test
    fun programAlreadyInTheSemesterIsNotOfferedAgain() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val existing = repository.studyPrograms.first()
        val viewModel = viewModel(repository)
        viewModel.fillValidSemester()
        viewModel.next()
        advanceUntilIdle()
        viewModel.selectProgramMode(SetupProgramMode.Existing)
        viewModel.selectProgram(existing.id)
        viewModel.next()
        advanceUntilIdle()

        viewModel.startAnotherProgram()

        assertTrue(viewModel.setup.value.availableProgramOptions.none { it.id == existing.id })
    }
}

private class RecordingFeedbackSink : FeedbackSink {
    val published = mutableListOf<UiFeedback>()

    override fun publish(feedback: UiFeedback) {
        published += feedback
    }
}
