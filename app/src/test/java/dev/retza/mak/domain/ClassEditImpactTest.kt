package dev.retza.mak.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassEditImpactTest {
    // January 2026 starts on Thursday, so Mondays 5 and 19 are week B, 12 and 26 are week A.
    private val calendar = AcademicCalendar(
        id = "calendar-1",
        startDate = LocalDate.of(2026, 1, 1),
        endDate = LocalDate.of(2026, 1, 31),
        firstWeekType = WeekType.A
    )
    private val weekB = LocalDate.of(2026, 1, 5)
    private val weekA = LocalDate.of(2026, 1, 12)
    private val laterWeekB = LocalDate.of(2026, 1, 19)

    private val weekly = ClassItem(
        id = "class-1",
        semesterId = "semester-1",
        semesterProgramId = "assignment-1",
        name = "Programming",
        type = "lecture",
        dayOfWeek = DayOfWeek.MONDAY,
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(11, 0)
    )

    private fun change(date: LocalDate, classId: String = weekly.id) = OccurrenceChange(
        id = "change-$date",
        classId = classId,
        originalDate = date,
        kind = OccurrenceChangeKind.CANCELLED
    )

    private fun note(date: LocalDate, classId: String = weekly.id) =
        OccurrenceNote(id = "note-$date", classId = classId, occurrenceDate = date, body = "Kolokwium")

    private fun impact(
        before: ClassItem,
        after: ClassItem,
        changes: List<OccurrenceChange> = emptyList(),
        notes: List<OccurrenceNote> = emptyList(),
        afterCalendar: AcademicCalendar = calendar
    ) = hiddenByClassEdit(before, after, calendar, afterCalendar, emptyList(), changes, notes)

    @Test
    fun changingOnlyTimeHidesNothing() {
        val result = impact(
            weekly,
            weekly.copy(startTime = LocalTime.of(12, 0), endTime = LocalTime.of(13, 0)),
            changes = listOf(change(weekA)),
            notes = listOf(note(weekB))
        )

        assertTrue(result.isEmpty)
    }

    @Test
    fun weeklyToWeekAHidesOnlyWeekBData() {
        val result = impact(
            weekly,
            weekly.copy(recurrence = Recurrence.A_WEEK),
            changes = listOf(change(weekB), change(weekA)),
            notes = listOf(note(laterWeekB), note(weekA))
        )

        assertEquals(ClassEditImpact(changeCount = 1, noteCount = 1), result)
    }

    @Test
    fun changingDayHidesAllData() {
        val result = impact(
            weekly,
            weekly.copy(dayOfWeek = DayOfWeek.TUESDAY),
            changes = listOf(change(weekB), change(weekA)),
            notes = listOf(note(laterWeekB))
        )

        assertEquals(ClassEditImpact(changeCount = 2, noteCount = 1), result)
    }

    @Test
    fun movingOneOffDateHidesItsNote() {
        val once = weekly.copy(recurrence = Recurrence.ONCE, dayOfWeek = DayOfWeek.WEDNESDAY, date = LocalDate.of(2026, 1, 14))

        val result = impact(
            once,
            once.copy(dayOfWeek = DayOfWeek.THURSDAY, date = LocalDate.of(2026, 1, 15)),
            notes = listOf(note(LocalDate.of(2026, 1, 14)))
        )

        assertEquals(ClassEditImpact(changeCount = 0, noteCount = 1), result)
    }

    @Test
    fun calendarWithDifferentRhythmHidesShiftedWeeks() {
        val weekAClass = weekly.copy(recurrence = Recurrence.A_WEEK)
        val otherCalendar = calendar.copy(id = "calendar-2", firstWeekType = WeekType.B)

        val result = impact(
            weekAClass,
            weekAClass.copy(semesterProgramId = "assignment-2"),
            changes = listOf(change(weekA)),
            afterCalendar = otherCalendar
        )

        assertEquals(ClassEditImpact(changeCount = 1, noteCount = 0), result)
    }

    @Test
    fun dataOutsideCurrentTermsAndOfOtherClassesIsNotCounted() {
        val outsideCalendar = LocalDate.of(2026, 2, 2)

        val result = impact(
            weekly,
            weekly.copy(dayOfWeek = DayOfWeek.TUESDAY),
            changes = listOf(change(outsideCalendar), change(weekA, classId = "class-2")),
            notes = listOf(note(LocalDate.of(2026, 1, 6)), note(weekB, classId = "class-2"))
        )

        assertTrue(result.isEmpty)
    }

    @Test
    fun baseOccurrenceRuleFollowsDayCycleAndCalendar() {
        val weekAClass = weekly.copy(recurrence = Recurrence.A_WEEK)

        assertTrue(weekAClass.hasBaseOccurrenceOn(weekA, calendar))
        assertFalse(weekAClass.hasBaseOccurrenceOn(weekB, calendar))
        assertFalse(weekly.hasBaseOccurrenceOn(LocalDate.of(2026, 1, 13), calendar))
        assertFalse(weekly.hasBaseOccurrenceOn(LocalDate.of(2026, 2, 2), calendar))
    }
}
