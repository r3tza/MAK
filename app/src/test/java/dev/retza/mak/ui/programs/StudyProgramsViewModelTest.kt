package dev.retza.mak.ui.programs

import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.ui.FakeRepository
import dev.retza.mak.ui.FakeSemesterRepository
import dev.retza.mak.ui.FormErrorTexts
import dev.retza.mak.ui.MainDispatcherRule
import dev.retza.mak.ui.components.suggestedCourseColor
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import dev.retza.mak.settings.InMemorySettingsPreferences
import dev.retza.mak.sync.PlanEditTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StudyProgramsViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(mainDispatcher)

    private class RecordingFeedbackSink : FeedbackSink {
        val published = mutableListOf<UiFeedback>()
        override fun publish(feedback: UiFeedback) {
            published += feedback
        }
    }

    private val preferences = InMemorySettingsPreferences()

    private fun viewModel(repository: FakeRepository, sink: FeedbackSink = RecordingFeedbackSink()) =
        StudyProgramsViewModel(FakeSemesterRepository(repository), sink, preferences, PlanEditTracker(CoroutineScope(UnconfinedTestDispatcher())))

    @Test
    fun listsGlobalPrograms() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        assertEquals(repository.studyPrograms.map { it.id }, viewModel.programs.value.programs.map { it.id })
    }

    @Test
    fun savingEditedProgramUpdatesNameAndColorAndClosesEditor() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = viewModel(repository, sink)
        val effects = mutableListOf<StudyProgramsEffect>()
        backgroundScope.launch { viewModel.effects.collect { effects += it } }
        val program = repository.studyPrograms.first()
        advanceUntilIdle()

        viewModel.openOnce(program.id)
        advanceUntilIdle()
        viewModel.updateName("  Informatyka stosowana ")
        viewModel.updateColor("#334FCE")
        viewModel.save()
        advanceUntilIdle()

        val saved = repository.studyPrograms.first { it.id == program.id }
        assertEquals("Informatyka stosowana", saved.name)
        assertEquals("#334FCE", saved.color)
        assertEquals(listOf(UiFeedback("Zapisano kierunek", UiFeedbackKind.Success)), sink.published)
        assertEquals(listOf(StudyProgramsEffect.CloseEditor), effects)
        assertNull(viewModel.programs.value.editor.id)
    }

    @Test
    fun blankNameShowsErrorWithoutSaving() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.openOnce(repository.studyPrograms.first().id)
        advanceUntilIdle()

        viewModel.updateName("   ")
        viewModel.save()
        advanceUntilIdle()

        assertEquals("Podaj nazwę kierunku.", viewModel.programs.value.editor.nameError)
        assertFalse("saveStudyProgram" in repository.events)
    }

    @Test
    fun doubleSaveWritesOnce() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.openOnce(repository.studyPrograms.first().id)
        advanceUntilIdle()

        repository.saveGate = CompletableDeferred()
        viewModel.save()
        viewModel.save()
        repository.saveGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.events.count { it == "saveStudyProgram" })
    }

    @Test
    fun secondOpenOnceKeepsDraft() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        val id = repository.studyPrograms.first().id
        advanceUntilIdle()
        viewModel.openOnce(id)
        advanceUntilIdle()
        viewModel.updateName("Szkic")

        viewModel.openOnce(id)
        advanceUntilIdle()

        assertEquals("Szkic", viewModel.programs.value.editor.name)
    }

    @Test
    fun openOnceForMissingProgramReportsMissing() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.openOnce(999L)
        advanceUntilIdle()

        assertEquals(StudyProgramEditOpenState.Missing, viewModel.opened.value)
        assertNull(viewModel.programs.value.editor.id)
    }

    @Test
    fun unusedProgramIsDeletedOnlyAfterConfirmation() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = viewModel(repository, sink)
        val effects = mutableListOf<StudyProgramsEffect>()
        backgroundScope.launch { viewModel.effects.collect { effects += it } }
        val unusedId = repository.saveStudyProgram(StudyProgramEntity(name = "Ekonomia", color = "#A65724"))
        viewModel.openOnce(unusedId)
        advanceUntilIdle()

        viewModel.requestDelete()
        assertFalse("deleteStudyProgram" in repository.events)
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertFalse(repository.studyPrograms.any { it.id == unusedId })
        assertEquals(listOf(StudyProgramsEffect.CloseEditor), effects)
        assertEquals(UiFeedback("Usunięto kierunek", UiFeedbackKind.Success), sink.published.last())
    }

    @Test
    fun assignedProgramCannotBeDeletedAndNamesItsSemesters() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        val assigned = repository.studyPrograms.first()
        viewModel.openOnce(assigned.id)
        advanceUntilIdle()

        viewModel.requestDelete()

        val editor = viewModel.programs.value.editor
        assertEquals(listOf(repository.semester.name), editor.usedInSemesters)
        assertFalse(editor.canDelete)
        assertFalse(editor.showDeleteConfirmation)
        assertFalse("deleteStudyProgram" in repository.events)
    }

    @Test
    fun newProgramIsAssignedToSharedCalendarOfActiveSemester() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = viewModel(repository, sink)
        val effects = mutableListOf<StudyProgramsEffect>()
        backgroundScope.launch { viewModel.effects.collect { effects += it } }
        // The fake gives the new calendar a higher id than the existing one, so the lowest id is the shared one.
        repository.saveCalendar(
            AcademicCalendarEntity(
                semesterId = repository.semester.id,
                startDate = LocalDate.of(2026, 11, 1),
                endDate = LocalDate.of(2027, 1, 31),
                firstWeekType = WeekType.B
            )
        )
        advanceUntilIdle()
        viewModel.openNewOnce()
        advanceUntilIdle()

        viewModel.updateName("Fizyka")
        viewModel.save()
        advanceUntilIdle()

        val program = repository.studyPrograms.first { it.name == "Fizyka" }
        val assignment = repository.semesterPrograms.single { it.studyProgramId == program.id }
        assertEquals(repository.semester.id, assignment.semesterId)
        assertEquals(repository.calendar.id, assignment.academicCalendarId)
        assertEquals(
            listOf(UiFeedback("Dodano kierunek do semestru ${repository.semester.name}", UiFeedbackKind.Success)),
            sink.published
        )
        assertEquals(listOf(StudyProgramsEffect.CloseEditor), effects)
        assertNull(viewModel.programs.value.editor.id)
    }

    @Test
    fun newProgramWithoutActiveSemesterIsOnlyCreated() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val sink = RecordingFeedbackSink()
        val viewModel = viewModel(repository, sink)
        advanceUntilIdle()
        repository.clearSemesters()
        viewModel.openNewOnce()
        advanceUntilIdle()

        viewModel.updateName("Fizyka")
        viewModel.save()
        advanceUntilIdle()

        val program = repository.studyPrograms.first { it.name == "Fizyka" }
        assertFalse(repository.semesterPrograms.any { it.studyProgramId == program.id })
        assertFalse("saveStudyProgramAssignment" in repository.events)
        assertEquals(listOf(UiFeedback("Dodano kierunek", UiFeedbackKind.Success)), sink.published)
    }

    @Test
    fun newProgramRejectsBlankName() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.openNewOnce()
        advanceUntilIdle()

        viewModel.updateName("   ")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(FormErrorTexts.ProgramNameRequired, viewModel.programs.value.editor.nameError)
        assertFalse("saveStudyProgram" in repository.events)
        assertFalse("saveStudyProgramAssignment" in repository.events)
    }

    @Test
    fun newProgramSuggestsColorAwayFromExistingPrograms() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.openNewOnce()
        advanceUntilIdle()

        val editor = viewModel.programs.value.editor
        val existingColor = repository.studyPrograms.first().color
        assertEquals(suggestedCourseColor(repository.studyPrograms.map { it.color }), editor.color)
        assertNotEquals(existingColor, editor.color)
        assertEquals(repository.semester.name, editor.activeSemesterName)
        assertNull(editor.id)
    }
}
