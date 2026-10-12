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
    fun weekAfterBreakShownForOneDay() = runTest(mainDispatcher) {
        addSecondProgramWithCalendar()
        val viewModel = viewModel()
        viewModel.startNew(semesterId = 1L, date = LocalDate.of(2026, 10, 5))
        advanceUntilIdle()

        viewModel.updateRange(LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 12))
        viewModel.updateWeekAfterBreak(WeekType.A)
        val state = viewModel.editor.value
        assertTrue(state.showsWeekAfterBreak)
        assertEquals(
            "Od 16 lis 2026 tydzień A, potem na zmianę. Powstanie korekta „Od tego tygodnia”.",
            state.weekAfterBreakDescription
        )
    }

    @Test
    fun saveSendsWeekAfterBreak() = runTest(mainDispatcher) {
        addSecondProgramWithCalendar()
        val viewModel = viewModel()
        viewModel.startNew(semesterId = 1L, date = LocalDate.of(2026, 12, 21))
        advanceUntilIdle()
        viewModel.updateRange(LocalDate.of(2026, 12, 21), LocalDate.of(2027, 1, 3))
        viewModel.updateWeekAfterBreak(WeekType.B)

        viewModel.save()
        advanceUntilIdle()

        assertEquals(WeekType.B, semesters.freeDayGroupSaves.last().weekAfterBreak)
    }

    @Test
    fun saveKeepsWeekAfterBreakForShortRange() = runTest(mainDispatcher) {
        addSecondProgramWithCalendar()
        val viewModel = viewModel()
        viewModel.startNew(semesterId = 1L, date = LocalDate.of(2026, 12, 21))
        advanceUntilIdle()
        viewModel.updateRange(LocalDate.of(2026, 12, 21), LocalDate.of(2027, 1, 3))
        viewModel.updateWeekAfterBreak(WeekType.B)
        viewModel.updateRange(LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 8))

        viewModel.save()
        advanceUntilIdle()

        assertEquals(WeekType.B, semesters.freeDayGroupSaves.last().weekAfterBreak)
    }

    @Test
    fun descriptionCountsDaysInclusively() {
        assertEquals("1 dzień", descriptionFor(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 1)))
        assertEquals("12 dni", descriptionFor(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 12)))
        assertEquals("2 dni", descriptionFor(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2)))
        assertEquals("5 dni", descriptionFor(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5)))
        assertEquals("22 dni", descriptionFor(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 22)))
    }

    private fun descriptionFor(start: LocalDate, end: LocalDate): String? =
        FreeDayEditorUiState(startDate = start, endDate = end).description
}
