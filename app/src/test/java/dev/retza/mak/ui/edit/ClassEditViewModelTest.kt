package dev.retza.mak.ui.edit

import androidx.lifecycle.SavedStateHandle
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ClassEditViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(mainDispatcher)

    private fun viewModel(repository: FakeRepository, savedState: SavedStateHandle = SavedStateHandle()) =
        ClassEditViewModel(FakeSemesterRepository(repository), repository, FeedbackController(), savedState)

    private fun recordingViewModel(repository: FakeRepository, sink: RecordingFeedbackSink) =
        ClassEditViewModel(FakeSemesterRepository(repository), repository, sink, SavedStateHandle())

    @Test
    fun newClassDraftSurvivesRecreationFromSavedState() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val savedState = SavedStateHandle()
        val first = viewModel(repository, savedState)
        advanceUntilIdle()
        first.openNew()
        first.update { it.copy(name = "Algebra", room = "C12", teacher = "dr Nowak", startTime = "08:15") }
        first.update { it.copy(recurrenceId = "a_week") }
        advanceUntilIdle()

        val restored = viewModel(repository, savedState)
        advanceUntilIdle()

        val editor = restored.editor.value
        assertEquals("Algebra", editor.name)
        assertEquals("C12", editor.room)
        assertEquals("dr Nowak", editor.teacher)
        assertEquals("08:15", editor.startTime)
        assertEquals("a_week", editor.recurrenceId)
        assertEquals("Tydzień A", editor.recurrenceLabel)
        assertEquals(listOf("Informatyka"), editor.courseOptions.map { it.label }.take(1))
    }

    @Test
    fun editDraftSurvivesRecreationAndReopeningTheRoute() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val savedState = SavedStateHandle()
        val first = viewModel(repository, savedState)
        advanceUntilIdle()
        first.openEdit("1:2026-09-21")
        advanceUntilIdle()
        first.update { it.copy(name = "Programowanie obiektowe") }
        advanceUntilIdle()

        val restored = viewModel(repository, savedState)
        advanceUntilIdle()
        restored.openEditIfNeeded("1:2026-09-21")
        advanceUntilIdle()

        assertEquals("Edytuj zajęcia", restored.editor.value.title)
        assertEquals("Programowanie obiektowe", restored.editor.value.name)
    }

    @Test
    fun openEditIfNeededLoadsAnotherClass() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.openNew()
        viewModel.update { it.copy(name = "Szkic") }

        viewModel.openEditIfNeeded("1:2026-09-21")
        advanceUntilIdle()

        assertEquals("Programowanie", viewModel.editor.value.name)
    }

    @Test
    fun openNewUsesWeeklyRecurrence() = runTest(mainDispatcher) {
        val repository = FakeRepository()
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
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.openNew(LocalDate.of(2026, 9, 25))

        val editor = viewModel.editor.value
        assertEquals("once", editor.recurrenceId)
        assertEquals("2026-09-25", editor.occurrenceDate)
    }

    @Test
    fun openEditLoadsBaseClass() = runTest(mainDispatcher) {
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
    fun selectCourseUsesAssignmentCalendarDates() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val result = repository.addSeparatedSemesterProgram(
            semesterId = 1L,
            studyProgram = dev.retza.mak.data.entity.StudyProgramEntity(
                name = "Fizyka",
                color = "#000000"
            ),
            sourceCalendarId = 1L
        )
        repository.saveCalendar(
            repository.calendars.first { it.id == result.academicCalendarId }
                .copy(endDate = LocalDate.of(2026, 12, 20))
        )
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.openNew()
        advanceUntilIdle()

        viewModel.selectCourse(result.semesterProgramId.toString())
        advanceUntilIdle()

        assertEquals("2026-12-20", viewModel.editor.value.semesterEndDate)
    }

    @Test
    fun openEditUsesAssignmentCalendarDates() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val result = repository.addSeparatedSemesterProgram(
            semesterId = 1L,
            studyProgram = dev.retza.mak.data.entity.StudyProgramEntity(
                name = "Fizyka",
                color = "#000000"
            ),
            sourceCalendarId = 1L
        )
        repository.saveCalendar(
            repository.calendars.first { it.id == result.academicCalendarId }
                .copy(endDate = LocalDate.of(2026, 12, 20))
        )
        repository.classes += repository.classes.single().copy(
            id = 2L,
            semesterProgramId = result.semesterProgramId,
            name = "Fizyka zajęcia"
        )
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.openEdit("2:2026-09-21")
        advanceUntilIdle()

        assertEquals("2026-12-20", viewModel.editor.value.semesterEndDate)
    }

    @Test
    fun saveKeepsSelectedAssignmentWhenProgramNamesMatch() = runTest(mainDispatcher) {
        val repository = FakeRepository()
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
        val secondOption = viewModel.editor.value.courseOptions.first {
            it.id == secondAssignmentId.toString()
        }
        assertEquals("Informatyka", secondOption.label)
        viewModel.selectCourse(secondOption.id)
        viewModel.update {
            it.copy(
                name = "Analiza",
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
        val repository = FakeRepository()
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
        val repository = FakeRepository()
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
