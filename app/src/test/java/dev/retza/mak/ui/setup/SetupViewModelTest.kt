package dev.retza.mak.ui.setup

import dev.retza.mak.ui.FakeMakRepository
import dev.retza.mak.ui.FakeSemesterRepository
import dev.retza.mak.ui.MainDispatcherRule
import dev.retza.mak.ui.feedback.FeedbackController
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
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

    private fun viewModel(
        repository: FakeMakRepository,
        sink: FeedbackSink = FeedbackController()
    ) = SetupViewModel(FakeSemesterRepository(repository), sink)

    private fun SetupViewModel.fillValidSemester() {
        update { it.copy(semesterName = "Nowy", startDate = "2026-09-01", endDate = "2026-10-01") }
    }

    @Test
    fun invalidSemesterFormShowsErrorsWithoutAdvancing() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
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
        val repository = FakeMakRepository()
        val viewModel = viewModel(repository)
        viewModel.fillValidSemester()

        viewModel.next()
        advanceUntilIdle()

        assertTrue(repository.events.isEmpty())
        assertEquals(SetupStep.Course, viewModel.setup.value.step)
    }

    @Test
    fun blankCourseNameShowsErrorWithoutWriting() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
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
        val repository = FakeMakRepository()
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
    fun failedSaveKeepsDraftsAndStep() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        repository.failSetupConfiguration = true
        val viewModel = viewModel(repository)
        viewModel.fillValidSemester()
        viewModel.next()
        advanceUntilIdle()
        viewModel.update { it.copy(courseName = "Informatyka") }

        viewModel.next()
        advanceUntilIdle()

        assertNull(repository.lastSetupSemester)
        assertNull(repository.lastSetupStudyProgram)
        assertEquals(SetupStep.Course, viewModel.setup.value.step)
        assertEquals("Informatyka", viewModel.setup.value.courseName)
        assertFalse(viewModel.setup.value.isSaving)
    }

    @Test
    fun doubleClickRunsOneTransactionAndOneFeedback() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
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
        val repository = FakeMakRepository()
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
    fun startDuringSaveResetsTheWizardWithoutFeedback() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
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

        assertEquals(SetupWizardUiState(), viewModel.setup.value)
        assertFalse(viewModel.setup.value.isSaving)
        assertTrue(sink.published.isEmpty())
    }

    @Test
    fun validationDoesNotPublishFeedback() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
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
        val repository = FakeMakRepository()
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
    fun failedSavePublishesSingleError() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        repository.failSetupConfiguration = true
        val sink = RecordingFeedbackSink()
        val viewModel = viewModel(repository, sink)
        viewModel.fillValidSemester()
        viewModel.next()
        advanceUntilIdle()
        viewModel.update { it.copy(courseName = "Informatyka") }

        viewModel.next()
        advanceUntilIdle()

        assertEquals(listOf("Nie udało się zapisać konfiguracji."), sink.published.map { it.message })
    }

    @Test
    fun finishAndReturnToSettingsEmitOneEffect() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
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
        assertEquals(SetupWizardUiState(), viewModel.setup.value)
    }

    @Test
    fun startResumesExistingSemesterAtCourseStep() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
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
    fun backMovesThroughStepsInOrder() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
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
}

private class RecordingFeedbackSink : FeedbackSink {
    val published = mutableListOf<UiFeedback>()

    override fun publish(feedback: UiFeedback) {
        published += feedback
    }
}
