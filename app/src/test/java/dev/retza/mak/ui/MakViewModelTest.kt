package dev.retza.mak.ui

import dev.retza.mak.data.database.ClassWithDetails
import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.CourseEntity
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.Recurrence
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.TeacherEntity
import dev.retza.mak.data.entity.WeekOverrideEntity
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.data.repository.MakRepository
import dev.retza.mak.ui.feedback.FeedbackController
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MakViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()
    private val clock = Clock.fixed(
        Instant.parse("2026-09-21T08:00:00Z"),
        ZoneId.of("Europe/Warsaw")
    )

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(mainDispatcher)

    @Test
    fun sharedNoteSaveKeepsEditTypedDuringSave() = runTest(mainDispatcher) {
        val repository = NoteFakeRepository()
        val viewModel = createViewModel(repository)
        viewModel.openOccurrence("1:2026-09-21")
        advanceUntilIdle()
        viewModel.updateSharedNoteDraft("Pierwsza")
        advanceUntilIdle()

        repository.saveGate = CompletableDeferred()
        viewModel.saveSharedNote()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.occurrence.isSavingSharedNote)

        viewModel.updateSharedNoteDraft("Druga")
        advanceUntilIdle()
        repository.saveGate?.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.uiState.value.occurrence
        assertEquals("Pierwsza", state.sharedNote)
        assertEquals("Druga", state.sharedNoteDraft)
        assertFalse(state.isSavingSharedNote)
        assertEquals("Pierwsza", repository.classes.single().classNote)
    }

    @Test
    fun sharedNoteSaveErrorKeepsDraftAndClearsSaving() = runTest(mainDispatcher) {
        val repository = NoteFakeRepository()
        val viewModel = createViewModel(repository)
        viewModel.openOccurrence("1:2026-09-21")
        advanceUntilIdle()
        viewModel.updateSharedNoteDraft("Nowa")
        advanceUntilIdle()

        repository.failSaves = true
        viewModel.saveSharedNote()
        advanceUntilIdle()

        val state = viewModel.uiState.value.occurrence
        assertEquals("Nowa", state.sharedNoteDraft)
        assertEquals("Wspólna", state.sharedNote)
        assertFalse(state.isSavingSharedNote)
        assertNotNull(state.sharedNoteError)
    }

    @Test
    fun occurrenceNoteSaveDeletesOnEmptyAndStaysOnDetails() = runTest(mainDispatcher) {
        val repository = NoteFakeRepository()
        val viewModel = createViewModel(repository)
        viewModel.openOccurrence("1:2026-09-21")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.occurrence.canSaveOccurrenceNote)

        viewModel.updateOccurrenceNoteDraft("Notatka daty")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.occurrence.canSaveOccurrenceNote)
        viewModel.saveOccurrenceNote()
        advanceUntilIdle()
        assertEquals("Notatka daty", viewModel.uiState.value.occurrence.occurrenceNote)
        assertEquals(MakDestination.OccurrenceDetails, viewModel.uiState.value.destination)
        assertEquals(1, repository.occurrenceNotes.size)

        viewModel.updateOccurrenceNoteDraft("")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.occurrence.canSaveOccurrenceNote)
        viewModel.saveOccurrenceNote()
        advanceUntilIdle()

        val state = viewModel.uiState.value.occurrence
        assertEquals(null, state.occurrenceNote)
        assertEquals("", state.occurrenceNoteDraft)
        assertTrue(repository.occurrenceNotes.isEmpty())
        assertEquals(MakDestination.OccurrenceDetails, viewModel.uiState.value.destination)
    }

    @Test
    fun occurrenceNoteSaveKeepsEditTypedDuringSave() = runTest(mainDispatcher) {
        val repository = NoteFakeRepository()
        val viewModel = createViewModel(repository)
        viewModel.openOccurrence("1:2026-09-21")
        advanceUntilIdle()
        viewModel.updateOccurrenceNoteDraft("Pierwsza")
        advanceUntilIdle()

        repository.saveGate = CompletableDeferred()
        viewModel.saveOccurrenceNote()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.occurrence.isSavingOccurrenceNote)

        viewModel.updateOccurrenceNoteDraft("Druga")
        advanceUntilIdle()
        repository.saveGate?.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.uiState.value.occurrence
        assertEquals("Pierwsza", state.occurrenceNote)
        assertEquals("Druga", state.occurrenceNoteDraft)
        assertFalse(state.isSavingOccurrenceNote)
    }

    @Test
    fun feedbackFlowEmitsPublishedMessagesOnce() = runTest(mainDispatcher) {
        val controller = FeedbackController()
        val viewModel = createViewModel(NoteFakeRepository(), controller)
        val received = mutableListOf<UiFeedback>()
        backgroundScope.launch(mainDispatcher) {
            controller.feedback.collect { received += it }
        }
        advanceUntilIdle()

        viewModel.publishFeedback(UiFeedbackKind.Info, "Test")
        viewModel.publishFeedback(UiFeedbackKind.Error, "Błąd")
        advanceUntilIdle()

        assertEquals(listOf("Test", "Błąd"), received.map { it.message })
    }

    private fun TestScope.createViewModel(
        repository: MakRepository,
        feedbackController: FeedbackController = FeedbackController()
    ): MakViewModel {
        val viewModel = MakViewModel(repository, feedbackController, clock)
        backgroundScope.launch(mainDispatcher) {
            viewModel.uiState.collect {}
        }
        return viewModel
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class NoteFakeRepository : MakRepository {
    val semester = SemesterEntity(
        id = 1L,
        name = "Semestr",
        startDate = LocalDate.of(2026, 9, 1),
        endDate = LocalDate.of(2026, 10, 31),
        firstWeekType = WeekType.A,
        isActive = true
    )
    val courses = mutableListOf(
        CourseEntity(id = 1L, semesterId = 1L, name = "Informatyka", color = "#137B71")
    )
    val classes = mutableListOf(
        ClassEntity(
            id = 1L,
            semesterId = 1L,
            name = "Programowanie",
            type = "Wykład",
            courseId = 1L,
            teacherId = null,
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 30),
            room = "L204",
            building = null,
            group = null,
            recurrence = Recurrence.EVERY_WEEK,
            date = null,
            classNote = "Wspólna"
        )
    )
    val occurrenceNotes = mutableListOf<OccurrenceNoteEntity>()
    val occurrenceChanges = mutableListOf<OccurrenceChangeEntity>()

    var saveGate: CompletableDeferred<Unit>? = null
    var failSaves = false

    override fun observeSemesters(): Flow<List<SemesterEntity>> = flowOf(listOf(semester))
    override fun observeActiveSemester(): Flow<SemesterEntity?> = flowOf(semester)
    override fun observeSemester(id: Long): Flow<SemesterEntity?> = flowOf(semester)
    override fun observeSemesterData(id: Long): Flow<SemesterWithData?> = flowOf(
        SemesterWithData(
            semester = semester,
            courses = courses,
            teachers = emptyList(),
            classes = classes,
            weekOverrides = emptyList(),
            occurrenceNotes = occurrenceNotes,
            occurrenceChanges = occurrenceChanges
        )
    )

    override fun observeCourses(semesterId: Long): Flow<List<CourseEntity>> = flowOf(courses)
    override fun observeTeachers(semesterId: Long): Flow<List<TeacherEntity>> = flowOf(emptyList())
    override fun observeClasses(semesterId: Long): Flow<List<ClassEntity>> = flowOf(classes)
    override fun observeClassesWithDetails(semesterId: Long): Flow<List<ClassWithDetails>> = flowOf(emptyList())
    override fun observeWeekOverrides(semesterId: Long): Flow<List<WeekOverrideEntity>> = flowOf(emptyList())
    override fun observeOccurrenceNotes(semesterId: Long): Flow<List<OccurrenceNoteEntity>> = flowOf(occurrenceNotes)
    override fun observeOccurrenceChanges(semesterId: Long): Flow<List<OccurrenceChangeEntity>> = flowOf(occurrenceChanges)
    override fun observeOccurrenceNotesForClass(classId: Long): Flow<List<OccurrenceNoteEntity>> = flowOf(occurrenceNotes)
    override fun observeOccurrenceChangesForClass(classId: Long): Flow<List<OccurrenceChangeEntity>> = flowOf(occurrenceChanges)
    override suspend fun getAllSemesterData(): List<SemesterWithData> = emptyList()

    override suspend fun saveSemester(entity: SemesterEntity): Long = 1L
    override suspend fun setActiveSemester(id: Long) = Unit
    override suspend fun clearActiveSemester() = Unit
    override suspend fun deleteSemester(id: Long) = Unit
    override suspend fun saveCourse(entity: CourseEntity): Long = 1L
    override suspend fun deleteCourse(id: Long) = Unit
    override suspend fun saveTeacher(entity: TeacherEntity): Long = 1L
    override suspend fun deleteTeacher(id: Long) = Unit

    override suspend fun saveClass(entity: ClassEntity): Long {
        awaitSave()
        val index = classes.indexOfFirst { it.id == entity.id }
        if (index >= 0) classes[index] = entity else classes += entity.copy(id = classes.size + 1L)
        return entity.id
    }

    override suspend fun deleteClass(id: Long) = Unit
    override suspend fun saveWeekOverride(entity: WeekOverrideEntity): Long = 1L
    override suspend fun deleteWeekOverride(id: Long) = Unit

    override suspend fun saveOccurrenceNote(entity: OccurrenceNoteEntity): Long {
        awaitSave()
        val index = occurrenceNotes.indexOfFirst {
            it.classId == entity.classId && it.occurrenceDate == entity.occurrenceDate
        }
        if (index >= 0) {
            occurrenceNotes[index] = entity.copy(id = occurrenceNotes[index].id)
        } else {
            occurrenceNotes += entity.copy(id = (occurrenceNotes.size + 1).toLong())
        }
        return entity.id
    }

    override suspend fun deleteOccurrenceNote(id: Long) {
        awaitSave()
        occurrenceNotes.removeAll { it.id == id }
    }

    override suspend fun saveOccurrenceChange(entity: OccurrenceChangeEntity): Long = 1L
    override suspend fun deleteOccurrenceChange(id: Long) = Unit

    private suspend fun awaitSave() {
        if (failSaves) throw IllegalStateException("save failed")
        saveGate?.await()
    }
}
