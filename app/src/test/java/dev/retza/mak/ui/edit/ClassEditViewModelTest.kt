package dev.retza.mak.ui.edit

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
class ClassEditViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(mainDispatcher)

    private fun viewModel(repository: FakeMakRepository) =
        ClassEditViewModel(repository, FeedbackController())

    private fun recordingViewModel(repository: FakeMakRepository, sink: RecordingFeedbackSink) =
        ClassEditViewModel(repository, sink)

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
        assertEquals(listOf("Informatyka"), editor.courseOptions.map { it.label })
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
                semesterProgramId = "1",
                type = "Wykład",
                dayLabel = "Poniedziałek",
                startTime = "11:00",
                endTime = "10:30"
            )
        }
        advanceUntilIdle()

        val effects = mutableListOf<ClassEditEffect>()
        backgroundScope.launch(mainDispatcher) { viewModel.effects.collect { effects += it } }
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertNotNull(viewModel.editor.value.errors[ClassEditField.EndTime])
        assertEquals(1, repository.classes.size)
        assertTrue(effects.isEmpty())
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
                semesterProgramId = "1",
                type = "Wykład",
                dayLabel = "Wtorek",
                startTime = "12:00",
                endTime = "13:30"
            )
        }
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

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
                semesterProgramId = "1",
                type = "Wykład",
                dayLabel = "Poniedziałek",
                startTime = "12:00",
                endTime = "13:30"
            )
        }
        advanceUntilIdle()

        val effects = mutableListOf<ClassEditEffect>()
        backgroundScope.launch(mainDispatcher) { viewModel.effects.collect { effects += it } }
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertEquals(2, repository.classes.size)
        assertEquals("Analiza", repository.classes.last().name)
        assertEquals("Dodaj zajęcia", viewModel.editor.value.title)
        assertEquals("", viewModel.editor.value.name)
        assertEquals(listOf(ClassEditEffect.CloseEditor), effects)
    }

    @Test
    fun saveRunsOnceAndEmitsCloseEffectOnce() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.openNew()
        viewModel.update {
            it.copy(
                name = "Analiza",
                semesterProgramId = "1",
                type = "Wykład",
                dayLabel = "Poniedziałek",
                startTime = "12:00",
                endTime = "13:30"
            )
        }
        advanceUntilIdle()

        val effects = mutableListOf<ClassEditEffect>()
        backgroundScope.launch(mainDispatcher) { viewModel.effects.collect { effects += it } }
        advanceUntilIdle()

        repository.saveGate = CompletableDeferred()
        viewModel.save()
        viewModel.save()
        advanceUntilIdle()
        repository.saveGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.events.count { it == "saveClass" })
        assertEquals(listOf(ClassEditEffect.CloseEditor), effects)
        assertFalse(viewModel.editor.value.isSaving)
    }

    @Test
    fun saveNewClassPublishesAddedMessage() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.openNew()
        viewModel.update {
            it.copy(
                name = "Analiza",
                semesterProgramId = "1",
                type = "Wykład",
                dayLabel = "Poniedziałek",
                startTime = "12:00",
                endTime = "13:30"
            )
        }
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertEquals(
            listOf(UiFeedback("Dodano zajęcia", UiFeedbackKind.Success)),
            sink.published
        )
    }

    @Test
    fun saveEditPublishesUpdatedMessage() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.openEdit("1:2026-09-21")
        advanceUntilIdle()
        viewModel.update { it.copy(name = "Programowanie zaawansowane") }
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertEquals(
            listOf(UiFeedback("Zapisano zmiany zajęć", UiFeedbackKind.Success)),
            sink.published
        )
        assertEquals("Programowanie zaawansowane", repository.classes.single().name)
    }

    @Test
    fun saveKeepsSelectedAssignmentWhenProgramNamesMatch() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val secondProgramId = repository.saveStudyProgram(
            dev.retza.mak.data.entity.StudyProgramEntity(name = "Informatyka", color = "#222222")
        )
        val secondAssignmentId = repository.saveSemesterProgram(
            dev.retza.mak.data.entity.SemesterProgramEntity(
                semesterId = 1L,
                studyProgramId = secondProgramId,
                academicCalendarId = 1L
            )
        )
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.openNew()
        viewModel.update {
            it.copy(
                name = "Analiza",
                semesterProgramId = secondAssignmentId.toString(),
                type = "Wykład",
                dayLabel = "Poniedziałek",
                startTime = "12:00",
                endTime = "13:30"
            )
        }
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertEquals(secondAssignmentId, repository.classes.last().semesterProgramId)
    }

    @Test
    fun saveErrorPublishesOneErrorAndKeepsForm() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.openNew()
        viewModel.update {
            it.copy(
                name = "Analiza",
                semesterProgramId = "1",
                type = "Wykład",
                dayLabel = "Poniedziałek",
                startTime = "12:00",
                endTime = "13:30"
            )
        }
        advanceUntilIdle()

        repository.failSaves = true
        viewModel.save()
        advanceUntilIdle()

        assertEquals(
            listOf(UiFeedback("Nie udało się zapisać zajęć.", UiFeedbackKind.Error)),
            sink.published
        )
        assertEquals("Analiza", viewModel.editor.value.name)
        assertFalse(viewModel.editor.value.isSaving)
    }

    @Test
    fun validationDoesNotPublishFeedback() = runTest(mainDispatcher) {
        val repository = FakeMakRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = recordingViewModel(repository, sink)
        advanceUntilIdle()
        viewModel.openNew()
        viewModel.update {
            it.copy(
                name = "Analiza",
                semesterProgramId = "1",
                type = "Wykład",
                dayLabel = "Poniedziałek",
                startTime = "11:00",
                endTime = "10:30"
            )
        }
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertNotNull(viewModel.editor.value.errors[ClassEditField.EndTime])
        assertTrue(sink.published.isEmpty())
    }
}

private class RecordingFeedbackSink : FeedbackSink {
    val published = mutableListOf<UiFeedback>()

    override fun publish(feedback: UiFeedback) {
        published += feedback
    }
}
