package dev.retza.mak.ui.freedays

import androidx.lifecycle.SavedStateHandle
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FreeDaysViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(mainDispatcher)

    private val repository = FakeRepository()
    private val semesters = FakeSemesterRepository(repository)

    private fun viewModel() = FreeDaysViewModel(
        SavedStateHandle(mapOf("semesterId" to "1")),
        semesters,
        FeedbackController(),
        PlanEditTracker(CoroutineScope(mainDispatcher))
    )

    /** Semester 1 gets a second calendar, used by a second study program, next to the seeded one. */
    private suspend fun addSecondProgramWithCalendar(): Long {
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
        return calendarId
    }

    @Test
    fun oneDayGroupShowsWeekdayDateAndCalendar() = runTest(mainDispatcher) {
        val calendarId = semesters.observeCalendars(1L).first().single().id
        val day = LocalDate.of(2026, 11, 12)
        semesters.saveFreeDayGroup(1L, emptyList(), day, day, null, listOf(calendarId))
        val viewModel = viewModel()
        advanceUntilIdle()

        val group = viewModel.freeDays.value.groups.single()
        assertEquals("Dni wolne", group.title)
        assertEquals("Czwartek, 12 lis 2026", group.dateLabel)
        assertEquals("Kalendarz: Informatyka", group.calendarsLabel)
    }

    @Test
    fun rangeInTwoCalendarsShowsDatesDaysAndCalendars() = runTest(mainDispatcher) {
        val secondCalendarId = addSecondProgramWithCalendar()
        val firstCalendarId = semesters.observeCalendars(1L).first().minBy { it.id }.id
        semesters.saveFreeDayGroup(
            1L,
            emptyList(),
            LocalDate.of(2026, 12, 23),
            LocalDate.of(2027, 1, 1),
            null,
            listOf(firstCalendarId, secondCalendarId)
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        val group = viewModel.freeDays.value.groups.single()
        assertEquals("23 gru 2026 - 1 sty 2027, 10 dni", group.dateLabel)
        assertEquals("Kalendarze: Informatyka, Matematyka", group.calendarsLabel)
    }

    @Test
    fun confirmDeletionDeletesEveryIdOfTheGroup() = runTest(mainDispatcher) {
        val secondCalendarId = addSecondProgramWithCalendar()
        val firstCalendarId = semesters.observeCalendars(1L).first().minBy { it.id }.id
        val day = LocalDate.of(2026, 11, 12)
        semesters.saveFreeDayGroup(1L, emptyList(), day, day, "Wyjazd", listOf(firstCalendarId, secondCalendarId))
        val viewModel = viewModel()
        advanceUntilIdle()
        val ids = viewModel.freeDays.value.groups.single().ids
        assertEquals(2, ids.size)

        viewModel.requestDeletion(ids)
        advanceUntilIdle()
        assertTrue(viewModel.freeDays.value.pendingDeletion != null)
        viewModel.confirmDeletion()
        advanceUntilIdle()

        assertTrue(semesters.observeFreeDays(1L).first().isEmpty())
        assertTrue(viewModel.freeDays.value.groups.isEmpty())
        assertEquals(null, viewModel.freeDays.value.pendingDeletion)
    }

    /** Stretches the seeded calendar of semester 1 over the 2026/2027 holidays and returns its id. */
    private suspend fun stretchSemesterCalendar(): Long {
        val calendarId = semesters.observeCalendars(1L).first().single().id
        semesters.saveCalendar(
            AcademicCalendarRecord(
                id = calendarId,
                semesterId = 1L,
                startDate = LocalDate.of(2026, 9, 7),
                endDate = LocalDate.of(2027, 2, 14),
                firstWeekType = WeekType.A
            )
        )
        return calendarId
    }

    @Test
    fun holidaysListsHolidaysOfTheSemesterAndSkipsCoveredOnes() = runTest(mainDispatcher) {
        val calendarId = stretchSemesterCalendar()
        semesters.saveFreeDayGroup(
            1L,
            emptyList(),
            LocalDate.of(2026, 12, 23),
            LocalDate.of(2027, 1, 1),
            "Przerwa świąteczna",
            listOf(calendarId)
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.openHolidays()
        advanceUntilIdle()

        val dialog = viewModel.freeDays.value.holidays!!
        val allSaints = dialog.options.single { it.name == "Wszystkich Świętych" }
        assertTrue(allSaints.checked && allSaints.enabled)
        val christmas = dialog.options.filter { it.name == "Boże Narodzenie" }
        assertTrue(christmas.isNotEmpty())
        christmas.forEach {
            assertFalse(it.checked)
            assertFalse(it.enabled)
            assertEquals("Już w „Przerwa świąteczna”", it.detail)
        }
        assertEquals(dialog.options.count { it.enabled }, dialog.checkedCount)
        assertTrue(dialog.confirmLabel.startsWith("Dodaj ${dialog.checkedCount} "))
    }

    @Test
    fun addHolidaysAddsCheckedOnesToAllCalendars() = runTest(mainDispatcher) {
        val calendarId = stretchSemesterCalendar()
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.openHolidays()
        advanceUntilIdle()
        viewModel.toggleHoliday(LocalDate.of(2026, 11, 1))
        val expectedDates = viewModel.freeDays.value.holidays!!.options.filter { it.checked }.map { it.date }

        viewModel.addHolidays()
        advanceUntilIdle()

        val add = semesters.holidayAdds.single()
        assertEquals(expectedDates, add.holidays.map { it.date })
        assertFalse(add.holidays.any { it.name == "Wszystkich Świętych" })
        assertEquals(listOf(calendarId), add.calendarIds)
        assertEquals(null, viewModel.freeDays.value.holidays)
    }
}
