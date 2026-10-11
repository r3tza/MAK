package dev.retza.mak.ui.freedays

import dev.retza.mak.data.repository.AcademicCalendarRecord
import dev.retza.mak.data.repository.SemesterProgramRecord
import dev.retza.mak.data.repository.StudyProgramRecord
import dev.retza.mak.domain.WeekType
import dev.retza.mak.sync.PlanEditTracker
import dev.retza.mak.ui.FakeRepository
import dev.retza.mak.ui.FakeSemesterRepository
import dev.retza.mak.ui.MainDispatcherRule
import dev.retza.mak.ui.feedback.FeedbackController
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FreeDayEditorViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(mainDispatcher)

    private val repository = FakeRepository()
    private val semesters = FakeSemesterRepository(repository)

    private fun viewModel() = FreeDayEditorViewModel(
        semesters,
        FeedbackController(),
        PlanEditTracker(CoroutineScope(mainDispatcher))
    )

    /** Semester 1 gets a second calendar, used by a second study program, next to the seeded one. */
    private suspend fun addSecondProgramWithCalendar() {
        val programId = semesters.saveStudyProgram(StudyProgramRecord(name = "Matematyka", color = "#3B5BDB"))
        val calendarId = semesters.saveCalendar(
            AcademicCalendarRecord(
                semesterId = 1L,
                startDate = LocalDate.of(2026, 9, 1),
                endDate = LocalDate.of(2026, 12, 20),
                firstWeekType = WeekType.B
            )
        )
        semesters.saveSemesterProgram(
            SemesterProgramRecord(semesterId = 1L, studyProgramId = programId, academicCalendarId = calendarId)
        )
    }

    @Test
    fun startNewChecksEveryCalendarAndUsesTheGivenDay() = runTest(mainDispatcher) {
        addSecondProgramWithCalendar()
        val viewModel = viewModel()

        viewModel.startNew(semesterId = 1L, date = LocalDate.of(2026, 10, 5))
        advanceUntilIdle()

        val state = viewModel.editor.value
        assertTrue(state.isOpen)
        assertEquals(listOf("Informatyka", "Matematyka"), state.calendars.map { it.label })
        assertEquals(listOf(true, true), state.calendars.map { it.checked })
        assertEquals(LocalDate.of(2026, 10, 5), state.startDate)
        assertEquals(LocalDate.of(2026, 10, 5), state.endDate)
    }

    @Test
    fun saveWithoutCheckedCalendarShowsErrorAndDoesNotSave() = runTest(mainDispatcher) {
        addSecondProgramWithCalendar()
        val viewModel = viewModel()
        viewModel.startNew(semesterId = 1L, date = LocalDate.of(2026, 10, 5))
        advanceUntilIdle()
        viewModel.editor.value.calendars.forEach { viewModel.toggleCalendar(it.id) }

        viewModel.save()
        advanceUntilIdle()

        assertEquals("Wybierz co najmniej jeden kalendarz.", viewModel.editor.value.calendarError)
        assertTrue(semesters.freeDayGroupSaves.isEmpty())
    }

    @Test
    fun saveCalledTwiceQuicklySavesOnce() = runTest(mainDispatcher) {
        addSecondProgramWithCalendar()
        val viewModel = viewModel()
        viewModel.startNew(semesterId = 1L, date = LocalDate.of(2026, 10, 5))
        advanceUntilIdle()

        viewModel.save()
        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, semesters.freeDayGroupSaves.size)
    }

    @Test
    fun startEditLoadsGroupAndSavePassesItsIdsAsReplaced() = runTest(mainDispatcher) {
        addSecondProgramWithCalendar()
        semesters.saveFreeDayGroup(
            semesterId = 1L,
            replacedIds = emptyList(),
            startDate = LocalDate.of(2026, 10, 1),
            endDate = LocalDate.of(2026, 10, 3),
            name = "Wyjazd",
            calendarIds = listOf(1L)
        )
        val groupIds = semesters.observeFreeDays(1L).first().map { it.id }
        val viewModel = viewModel()

        viewModel.startEdit(semesterId = 1L, freeDayIds = groupIds)
        advanceUntilIdle()

        val state = viewModel.editor.value
        assertEquals(LocalDate.of(2026, 10, 1), state.startDate)
        assertEquals(LocalDate.of(2026, 10, 3), state.endDate)
        assertEquals("Wyjazd", state.name)
        assertEquals(listOf(true, false), state.calendars.map { it.checked })

        viewModel.save()
        advanceUntilIdle()

        val save = semesters.freeDayGroupSaves.last()
        assertEquals(groupIds, save.replacedIds)
        assertEquals(listOf(1L), save.calendarIds)
    }

    @Test
    fun descriptionCountsDaysInclusively() {
        val oneDay = FreeDayEditorUiState(startDate = LocalDate.of(2026, 10, 1), endDate = LocalDate.of(2026, 10, 1))
        assertTrue(oneDay.description!!.startsWith("1 dzień. "))

        val twelveDays = FreeDayEditorUiState(startDate = LocalDate.of(2026, 10, 1), endDate = LocalDate.of(2026, 10, 12))
        assertTrue(twelveDays.description!!.startsWith("12 dni. "))
    }
}
