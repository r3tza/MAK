package dev.retza.mak.ui.edit

import dev.retza.mak.ui.FakeMakRepository
import dev.retza.mak.ui.MainDispatcherRule
import dev.retza.mak.ui.feedback.FeedbackController
import java.time.LocalDate
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
class ClassEditViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(mainDispatcher)

    private fun viewModel(repository: FakeMakRepository) =
        ClassEditViewModel(repository, FeedbackController())

    @Test
    fun openNewUsesWeeklyRecurrence() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.openNew()

        val editor = viewModel.editor.value
        assertEquals("Dodaj zajęcia", editor.title)
        assertEquals("every_week", editor.recurrenceId)
        assertEquals("", editor.occurrenceDate)
        assertEquals(listOf("Informatyka"), editor.courseOptions)
        assertEquals("2026-09-01", editor.semesterStartDate)
        assertEquals("2026-10-31", editor.semesterEndDate)
    }

    @Test
    fun openNewOneOffSetsDateAndRecurrence() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.openNew(LocalDate.of(2026, 9, 25))

        val editor = viewModel.editor.value
        assertEquals("once", editor.recurrenceId)
        assertEquals("2026-09-25", editor.occurrenceDate)
    }

    @Test
    fun openEditLoadsBaseClass() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.openEdit("1:2026-09-21")

        val editor = viewModel.editor.value
        assertEquals("Edytuj zajęcia", editor.title)
        assertEquals("Programowanie", editor.name)
        assertEquals("Informatyka", editor.courseName)
        assertEquals("Poniedziałek", editor.dayLabel)
        assertEquals("09:00", editor.startTime)
        assertEquals("10:30", editor.endTime)
        assertEquals("L204", editor.room)
        assertEquals("Wspólna", editor.note)
    }

    @Test
    fun openEditWaitsForFirstSemesterDataEmission() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        repository.occurrenceDataGate = kotlinx.coroutines.CompletableDeferred()
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.openEdit("1:2026-09-21")
        advanceUntilIdle()
        assertEquals("", viewModel.editor.value.name)

        repository.occurrenceDataGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals("Edytuj zajęcia", viewModel.editor.value.title)
        assertEquals("Programowanie", viewModel.editor.value.name)
    }

    @Test
    fun lastOpenEditWinsWhenDataArrivesLate() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        repository.classes += repository.classes.single().copy(id = 2L, name = "Matematyka")
        repository.occurrenceDataGate = kotlinx.coroutines.CompletableDeferred()
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.openEdit("1:2026-09-21")
        viewModel.openEdit("2:2026-09-21")
        advanceUntilIdle()
        assertEquals("", viewModel.editor.value.name)

        repository.occurrenceDataGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals("Matematyka", viewModel.editor.value.name)
    }

    @Test
    fun saveRejectsInvalidTime() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.openNew()
        viewModel.update {
            it.copy(
                name = "Analiza",
                courseName = "Informatyka",
                type = "Wykład",
                dayLabel = "Poniedziałek",
                startTime = "11:00",
                endTime = "10:30"
            )
        }
        advanceUntilIdle()

        val saved = viewModel.save()

        assertFalse(saved)
        assertNotNull(viewModel.editor.value.errors[ClassEditField.EndTime])
        assertEquals(1, repository.classes.size)
    }

    @Test
    fun saveRejectsOneOffDateOutsideSemester() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.openNew(LocalDate.of(2026, 12, 1))
        viewModel.update {
            it.copy(
                name = "Analiza",
                courseName = "Informatyka",
                type = "Wykład",
                dayLabel = "Wtorek",
                startTime = "12:00",
                endTime = "13:30"
            )
        }
        advanceUntilIdle()

        val saved = viewModel.save()

        assertFalse(saved)
        assertNotNull(viewModel.editor.value.errors[ClassEditField.Date])
        assertEquals(1, repository.classes.size)
    }

    @Test
    fun savePersistsNewClassAndResetsEditor() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.openNew()
        viewModel.update {
            it.copy(
                name = "Analiza",
                courseName = "Informatyka",
                type = "Wykład",
                dayLabel = "Poniedziałek",
                startTime = "12:00",
                endTime = "13:30"
            )
        }
        advanceUntilIdle()

        val saved = viewModel.save()
        advanceUntilIdle()

        assertTrue(saved)
        assertEquals(2, repository.classes.size)
        assertEquals("Analiza", repository.classes.last().name)
        assertEquals("Dodaj zajęcia", viewModel.editor.value.title)
        assertEquals("", viewModel.editor.value.name)
    }
}
