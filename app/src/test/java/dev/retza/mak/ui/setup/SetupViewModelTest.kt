package dev.retza.mak.ui.setup

import dev.retza.mak.ui.FakeMakRepository
import dev.retza.mak.ui.MainDispatcherRule
import dev.retza.mak.ui.feedback.FeedbackController
import dev.retza.mak.ui.feedback.FeedbackSink
import kotlinx.coroutines.CompletableDeferred
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
    ) = SetupViewModel(repository, sink)

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
        val course = repository.lastSetupCourse
        assertEquals(1, repository.events.count { it == "saveSetupConfiguration" })
        assertEquals("Nowy", semester?.name)
        assertTrue(semester?.isActive == true)
        assertEquals("Informatyka", course?.name)
        assertEquals(semester?.id, course?.semesterId)
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
        assertNull(repository.lastSetupCourse)
        assertEquals(SetupStep.Course, viewModel.setup.value.step)
        assertEquals("Informatyka", viewModel.setup.value.courseName)
        assertFalse(viewModel.setup.value.isSaving)
    }

    @Test
    fun doubleClickRunsOneTransaction() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        repository.saveGate = CompletableDeferred()
        val viewModel = viewModel(repository)
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
        val firstCourse = repository.lastSetupCourse?.id

        viewModel.back()
        viewModel.next()
        advanceUntilIdle()

        assertEquals(2, repository.events.count { it == "saveSetupConfiguration" })
        assertEquals(firstSemester, repository.lastSetupSemester?.id)
        assertEquals(firstCourse, repository.lastSetupCourse?.id)
        assertEquals(SetupStep.Classes, viewModel.setup.value.step)
    }

    @Test
    fun startDuringSaveResetsTheWizardWithoutPartialState() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        repository.saveGate = CompletableDeferred()
        val viewModel = viewModel(repository)
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
