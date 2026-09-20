package dev.retza.mak.ui.setup

import dev.retza.mak.ui.FakeMakRepository
import dev.retza.mak.ui.MainDispatcherRule
import dev.retza.mak.ui.feedback.FeedbackController
import dev.retza.mak.ui.feedback.FeedbackSink
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    @Test
    fun invalidSemesterFormShowsErrorsWithoutWriting() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val viewModel = viewModel(repository)
        viewModel.update {
            it.copy(semesterName = "  ", startDate = "2026-09-01", endDate = "2026-08-01")
        }

        viewModel.next()
        advanceUntilIdle()

        assertTrue(repository.events.none { it == "saveSemester" })
        assertTrue(viewModel.setup.value.errors.isNotEmpty())
        assertEquals(SetupStep.Semester, viewModel.setup.value.step)
    }

    @Test
    fun validSemesterFormWritesAndAdvancesToCourse() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val viewModel = viewModel(repository)
        viewModel.update {
            it.copy(semesterName = "Nowy", startDate = "2026-09-01", endDate = "2026-10-01")
        }

        viewModel.next()
        advanceUntilIdle()

        assertTrue(repository.events.contains("saveSemester"))
        assertEquals(SetupStep.Course, viewModel.setup.value.step)
    }

    @Test
    fun blankCourseNameShowsErrorWithoutWritingCourse() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val viewModel = viewModel(repository)
        viewModel.update {
            it.copy(semesterName = "Nowy", startDate = "2026-09-01", endDate = "2026-10-01")
        }
        viewModel.next()
        advanceUntilIdle()

        viewModel.next()
        advanceUntilIdle()

        assertTrue(repository.events.none { it == "saveCourse" })
        assertTrue(viewModel.setup.value.errors.containsKey(SetupField.CourseName))
        assertEquals(SetupStep.Course, viewModel.setup.value.step)
    }

    @Test
    fun validCourseFormWritesAndAdvancesToClasses() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val viewModel = viewModel(repository)
        viewModel.update {
            it.copy(semesterName = "Nowy", startDate = "2026-09-01", endDate = "2026-10-01")
        }
        viewModel.next()
        advanceUntilIdle()
        viewModel.update { it.copy(courseName = "Informatyka") }

        viewModel.next()
        advanceUntilIdle()

        assertTrue(repository.events.contains("saveCourse"))
        assertEquals(SetupStep.Classes, viewModel.setup.value.step)
    }

    @Test
    fun startResetsTheWizard() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val viewModel = viewModel(repository)
        viewModel.update {
            it.copy(semesterName = "Nowy", startDate = "2026-09-01", endDate = "2026-10-01")
        }
        viewModel.next()
        advanceUntilIdle()

        viewModel.start()

        assertEquals(SetupWizardUiState(), viewModel.setup.value)
    }

    @Test
    fun backMovesThroughStepsInOrder() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val viewModel = viewModel(repository)
        viewModel.update {
            it.copy(semesterName = "Nowy", startDate = "2026-09-01", endDate = "2026-10-01")
        }
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
