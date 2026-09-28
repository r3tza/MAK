package dev.retza.mak.ui.schedule

import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.WeekOverrideEntity
import dev.retza.mak.data.entity.WeekOverrideScope
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.ui.FakeRepository
import dev.retza.mak.ui.FakeSemesterRepository
import dev.retza.mak.ui.MainDispatcherRule
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import dev.retza.mak.ui.semester.WeekOverrideScopeUi
import dev.retza.mak.ui.semester.WeekTypeUi
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private val feedback = mutableListOf<UiFeedback>()

    private fun viewModel(repository: FakeRepository, clock: Clock = this.clock) = ScheduleViewModel(
        FakeSemesterRepository(repository),
        repository,
        clock,
        ActivePlanProvider(),
        feedbackSink = object : FeedbackSink {
            override fun publish(feedback: UiFeedback) {
                this@ScheduleViewModelTest.feedback += feedback
            }
        }
    )

    @Test
    fun refreshTodayMovesUnchangedSelectionToNewDay() = runTest(mainDispatcher) {
        val clock = MutableClock(Instant.parse("2026-09-27T20:00:00Z"))
        val viewModel = viewModel(FakeRepository(), clock)
        backgroundScope.launch { viewModel.schedule.collect {} }
        advanceUntilIdle()
        val sundayWeek = viewModel.schedule.value.weekRangeLabel

        clock.now = Instant.parse("2026-09-28T06:00:00Z")
        viewModel.refreshToday()
        advanceUntilIdle()

        assertTrue(viewModel.schedule.value.weekRangeLabel != sundayWeek)
        assertEquals("Bieżący tydzień", viewModel.schedule.value.weekSubtitle)
    }

    @Test
    fun refreshTodayKeepsWeekChosenByUser() = runTest(mainDispatcher) {
        val clock = MutableClock(Instant.parse("2026-09-27T20:00:00Z"))
        val viewModel = viewModel(FakeRepository(), clock)
        backgroundScope.launch { viewModel.schedule.collect {} }
        advanceUntilIdle()
        viewModel.changeWeek(2)
        advanceUntilIdle()
        val chosenWeek = viewModel.schedule.value.weekRangeLabel

        clock.now = Instant.parse("2026-09-28T06:00:00Z")
        viewModel.refreshToday()
        advanceUntilIdle()

        assertEquals(chosenWeek, viewModel.schedule.value.weekRangeLabel)
    }

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
    fun cancelledItemKeepsOccurrenceNote() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        repository.occurrenceChanges += OccurrenceChangeEntity(
            id = 1L,
            semesterId = 1L,
            classId = 1L,
            originalDate = LocalDate.of(2026, 9, 21),
            kind = dev.retza.mak.data.entity.OccurrenceChangeKind.CANCELLED,
            targetDate = null,
            newStartTime = null,
            newEndTime = null,
            newRoom = null,
            newBuilding = null,
            newTeacherName = null,
            newNote = null
        )
        repository.occurrenceNotes += OccurrenceNoteEntity(
            id = 1L,
            semesterId = 1L,
            classId = 1L,
            occurrenceDate = LocalDate.of(2026, 9, 21),
            body = "Na dziś"
        )
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.schedule.collect {} }
        advanceUntilIdle()

        viewModel.setShowCancelled(true)
        advanceUntilIdle()

        assertEquals("Na dziś", viewModel.schedule.value.items.single().occurrenceNote)
    }

    @Test
    fun cancelledItemsFollowCourseFilterAndAreNotCounted() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        val second = repository.addSeparatedSemesterProgram(
            semesterId = 1L,
            studyProgram = dev.retza.mak.data.entity.StudyProgramEntity(name = "Fizyka", color = "#000000"),
            sourceCalendarId = 1L
        )
        repository.classes += repository.classes.single().copy(
            id = 2L,
            semesterProgramId = second.semesterProgramId,
            name = "Mechanika",
            startTime = LocalTime.of(11, 0),
            endTime = LocalTime.of(12, 0)
        )
        listOf(1L, 2L).forEach { classId ->
            repository.occurrenceChanges += OccurrenceChangeEntity(
                id = classId,
                semesterId = 1L,
                classId = classId,
                originalDate = LocalDate.of(2026, 9, 21),
                kind = dev.retza.mak.data.entity.OccurrenceChangeKind.CANCELLED,
                targetDate = null,
                newStartTime = null,
                newEndTime = null,
                newRoom = null,
                newBuilding = null,
                newTeacherName = null,
                newNote = null
            )
        }
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.schedule.collect {} }
        advanceUntilIdle()

        viewModel.setShowCancelled(true)
        viewModel.selectCourseFilter(second.semesterProgramId.toString())
        advanceUntilIdle()

        val state = viewModel.schedule.value
        assertEquals(listOf("Mechanika"), state.items.map { it.name })
        assertEquals(listOf("Mechanika"), state.calendarItems.map { it.name })
        assertEquals("0 zajęć", state.selectedDayCountLabel)
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
    fun failedVisibleOverrideSaveReportsError() = runTest(mainDispatcher) {
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

        repository.failSaves = true
        viewModel.saveVisibleWeekOverride(WeekTypeUi.B, WeekOverrideScopeUi.ONE_WEEK)
        advanceUntilIdle()

        assertTrue(repository.weekOverrides.isEmpty())
        assertEquals(
            listOf(UiFeedback("Nie udało się zapisać korekty tygodnia.", UiFeedbackKind.Error)),
            feedback
        )
    }

    @Test
    fun weekCanBeCorrectedOnlyWithOneVisibleCalendar() = runTest(mainDispatcher) {
        val single = viewModel(FakeRepository())
        backgroundScope.launch { single.schedule.collect {} }
        advanceUntilIdle()
        assertTrue("one calendar, all courses", single.schedule.value.canCorrectWeek)

        val repository = FakeRepository()
        val second = repository.addSeparatedSemesterProgram(
            semesterId = 1L,
            studyProgram = dev.retza.mak.data.entity.StudyProgramEntity(name = "Fizyka", color = "#000000"),
            sourceCalendarId = 1L
        )
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.schedule.collect {} }
        advanceUntilIdle()
        assertFalse("two calendars, all courses", viewModel.schedule.value.canCorrectWeek)

        viewModel.selectCourseFilter(second.semesterProgramId.toString())
        advanceUntilIdle()
        assertTrue("two calendars, one course", viewModel.schedule.value.canCorrectWeek)
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

        assertEquals("Poniedziałek, 5 października 2026", viewModel.schedule.value.calendarSelectedDayLabel)
        // The description read by TalkBack keeps the formatter's lowercase weekday.
        val cell = viewModel.schedule.value.calendarDays.first { it.id == "2026-10-05" }
        assertTrue(cell.accessibilityLabel.startsWith("poniedziałek, 5 października 2026"))
    }

    @Test
    fun calendarShowsThreeMarkersWithoutOverflow() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        addMondayClasses(repository, 3)
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.schedule.collect {} }
        advanceUntilIdle()

        val day = viewModel.schedule.value.calendarDays.single { it.id == "2026-09-21" }
        assertEquals(3, day.markers.size)
        assertFalse(day.hasMoreMarkers)
    }

    @Test
    fun calendarShowsFiveMarkersWithoutOverflow() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        addMondayClasses(repository, 5)
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.schedule.collect {} }
        advanceUntilIdle()

        val day = viewModel.schedule.value.calendarDays.single { it.id == "2026-09-21" }
        assertEquals(5, day.markers.size)
        assertFalse(day.hasMoreMarkers)
    }

    @Test
    fun calendarShowsFourMarkersAndOverflowAtSixClasses() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        addMondayClasses(repository, 6)
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.schedule.collect {} }
        advanceUntilIdle()

        val day = viewModel.schedule.value.calendarDays.single { it.id == "2026-09-21" }
        assertEquals(4, day.markers.size)
        assertTrue(day.hasMoreMarkers)
    }

    @Test
    fun calendarMarksChangedOccurrencesAndLeavesRegularOnesFilled() = runTest(mainDispatcher) {
        val repository = FakeRepository()
        addMondayClasses(repository, 2)
        repository.occurrenceChanges += OccurrenceChangeEntity(
            id = 1L,
            semesterId = 1L,
            classId = 1L,
            originalDate = LocalDate.of(2026, 9, 21),
            kind = dev.retza.mak.data.entity.OccurrenceChangeKind.MODIFIED,
            targetDate = LocalDate.of(2026, 9, 21),
            newStartTime = LocalTime.of(10, 0),
            newEndTime = LocalTime.of(11, 30),
            newRoom = null,
            newBuilding = null,
            newTeacherName = null,
            newNote = null
        )
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.schedule.collect {} }
        advanceUntilIdle()

        val markers = viewModel.schedule.value.calendarDays
            .single { it.id == "2026-09-21" }
            .markers
        assertEquals(
            mapOf("1:2026-09-21" to true, "2:2026-09-21" to false),
            markers.associate { it.id to it.isChanged }
        )
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

private fun addMondayClasses(repository: FakeRepository, count: Int) {
    (2..count).forEach { index ->
        repository.classes += repository.classes.first().copy(
            id = index.toLong(),
            name = "Zajęcia $index",
            startTime = LocalTime.of(8 + index, 0),
            endTime = LocalTime.of(9 + index, 0)
        )
    }
}

private class MutableClock(var now: Instant) : Clock() {
    override fun getZone(): ZoneId = ZoneId.of("Europe/Warsaw")

    override fun withZone(zone: ZoneId): Clock = this

    override fun instant(): Instant = now
}
