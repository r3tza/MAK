package dev.retza.mak.ui.schedule

import dev.retza.mak.data.entity.WeekOverrideEntity
import dev.retza.mak.data.entity.WeekOverrideScope
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.ui.FakeRepository
import dev.retza.mak.ui.FakeSemesterRepository
import dev.retza.mak.ui.MainDispatcherRule
import dev.retza.mak.ui.semester.WeekOverrideScopeUi
import dev.retza.mak.ui.semester.WeekTypeUi
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ScheduleViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()
    private val clock = Clock.fixed(
        Instant.parse("2026-09-21T08:00:00Z"),
        ZoneId.of("Europe/Warsaw")
    )

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(mainDispatcher)

    private fun viewModel(repository: FakeRepository) = ScheduleViewModel(FakeSemesterRepository(repository), repository, clock, ActivePlanProvider())

    @Test
    fun initialStateMapsActiveSemesterPlan() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.schedule.collect {} }
        advanceUntilIdle()

        val state = viewModel.schedule.value
        assertEquals(ScheduleView.List, state.view)
        assertEquals(listOf("all", "1"), state.filters.map { it.id })
        assertTrue(state.filters.first { it.id == "all" }.isSelected)
        assertEquals(7, state.days.size)
        assertEquals("1 zajęcie", state.selectedDayCountLabel)
        assertEquals("Programowanie", state.items.single().name)
    }

    @Test
    fun changingSemesterDropsFilterThatNoLongerExists() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.schedule.collect {} }
        advanceUntilIdle()

        viewModel.selectCourseFilter("1")
        advanceUntilIdle()
        assertTrue(viewModel.schedule.value.filters.any { it.id == "1" && it.isSelected })

        repository.setActiveSemester(2)
        advanceUntilIdle()

        val filters = viewModel.schedule.value.filters
        assertTrue(filters.any { it.id == "all" && it.isSelected })
        assertTrue(filters.none { it.id == "1" })
    }

    @Test
    fun weekAndMonthNavigationUpdateTheState() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.schedule.collect {} }
        advanceUntilIdle()
        val initialRange = viewModel.schedule.value.weekRangeLabel
        val initialMonth = viewModel.schedule.value.calendarMonthLabel

        viewModel.changeWeek(1)
        viewModel.changeMonth(1)
        advanceUntilIdle()

        assertTrue(viewModel.schedule.value.weekRangeLabel != initialRange)
        assertTrue(viewModel.schedule.value.calendarMonthLabel != initialMonth)
    }

    @Test
    fun savingVisibleOverrideTargetsSelectedCourseCalendar() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val result = repository.addSeparatedSemesterProgram(
            semesterId = 1L,
            studyProgram = dev.retza.mak.data.entity.StudyProgramEntity(
                name = "Fizyka",
                color = "#000000"
            ),
            sourceCalendarId = 1L
        )
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.schedule.collect {} }
        advanceUntilIdle()

        viewModel.selectCourseFilter(result.semesterProgramId.toString())
        viewModel.saveVisibleWeekOverride(WeekTypeUi.B, WeekOverrideScopeUi.ONE_WEEK)
        advanceUntilIdle()

        assertEquals(1, repository.weekOverrides.size)
        assertEquals(result.academicCalendarId, repository.weekOverrides.single().academicCalendarId)
    }

    @Test
    fun savingVisibleOverrideWithAllAndMultipleCalendarsIsIgnored() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        repository.addSeparatedSemesterProgram(
            semesterId = 1L,
            studyProgram = dev.retza.mak.data.entity.StudyProgramEntity(
                name = "Fizyka",
                color = "#000000"
            ),
            sourceCalendarId = 1L
        )
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.schedule.collect {} }
        advanceUntilIdle()

        viewModel.saveVisibleWeekOverride(WeekTypeUi.B, WeekOverrideScopeUi.ONE_WEEK)
        advanceUntilIdle()

        assertTrue(repository.weekOverrides.isEmpty())
    }

    @Test
    fun selectCalendarDayUpdatesSelection() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.schedule.collect {} }
        advanceUntilIdle()

        viewModel.selectCalendarDay("2026-10-05")
        advanceUntilIdle()

        assertEquals("poniedziałek, 5 października 2026", viewModel.schedule.value.calendarSelectedDayLabel)
    }

    @Test
    fun openNewClassForSelectedCalendarDayEmitsEffectWithSelectedDate() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        val effects = mutableListOf<ScheduleEffect>()
        backgroundScope.launch { viewModel.effects.collect { effects += it } }
        advanceUntilIdle()

        viewModel.selectCalendarDay("2026-10-05")
        viewModel.openNewClassForSelectedCalendarDay()
        advanceUntilIdle()

        assertEquals(listOf(ScheduleEffect.OpenNewClassEditor(LocalDate.of(2026, 10, 5))), effects)
    }

    @Test
    fun saveVisibleWeekOverrideWritesSelectedWeek() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.schedule.collect {} }
        advanceUntilIdle()

        viewModel.saveVisibleWeekOverride(WeekTypeUi.B, WeekOverrideScopeUi.ONE_WEEK)
        advanceUntilIdle()

        val saved = repository.weekOverrides.single()
        assertEquals(LocalDate.of(2026, 9, 21), saved.weekStartDate)
        assertEquals(WeekType.B, saved.weekType)
        assertEquals(WeekOverrideScope.ONE_WEEK, saved.scope)
    }

    @Test
    fun clearVisibleWeekOverrideDeletesExistingCorrection() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        repository.weekOverrides += WeekOverrideEntity(
            id = 1L,
            semesterId = 1L,
            academicCalendarId = 1L,
            weekStartDate = LocalDate.of(2026, 9, 21),
            weekType = WeekType.B,
            scope = WeekOverrideScope.ONE_WEEK
        )
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.schedule.collect {} }
        advanceUntilIdle()

        viewModel.clearVisibleWeekOverride(WeekOverrideScopeUi.ONE_WEEK)
        advanceUntilIdle()

        assertTrue(repository.weekOverrides.isEmpty())
    }
}
