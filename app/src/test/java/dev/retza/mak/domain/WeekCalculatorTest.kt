package dev.retza.mak.domain

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeekCalculatorTest {
    private val semester = Semester(
        id = "semester",
        name = "Winter",
        startDate = LocalDate.of(2026, 1, 7),
        endDate = LocalDate.of(2026, 2, 28),
        firstWeekType = WeekType.A
    )
    private val calendar = semester.toAcademicCalendar()
    private val calculator = WeekCalculator()

    @Test
    fun semesterStartingMidweekUsesContainingMonday() {
        val result = calculator.calculate(calendar, LocalDate.of(2026, 1, 7))

        assertEquals(LocalDate.of(2026, 1, 5), result?.weekStartDate)
        assertEquals(WeekType.A, result?.weekType)
    }

    @Test
    fun weekTypesAlternateFromTheSemesterAnchor() {
        assertEquals(WeekType.A, calculator.calculateWeekType(calendar, LocalDate.of(2026, 1, 7)))
        assertEquals(WeekType.B, calculator.calculateWeekType(calendar, LocalDate.of(2026, 1, 12)))
        assertEquals(WeekType.A, calculator.calculateWeekType(calendar, LocalDate.of(2026, 1, 19)))
    }

    @Test
    fun dateOutsideSemesterHasNoWeek() {
        assertNull(calculator.calculate(calendar, LocalDate.of(2026, 1, 4)))
        assertNull(calculator.calculate(calendar, LocalDate.of(2026, 3, 1)))
    }

    @Test
    fun oneWeekOverrideDoesNotShiftFollowingWeeks() {
        val overrides = listOf(
            WeekOverride(
                id = "one",
                academicCalendarId = calendar.id,
                weekStartDate = LocalDate.of(2026, 1, 12),
                weekType = WeekType.A,
                scope = WeekOverrideScope.ONE_WEEK
            )
        )

        assertEquals(WeekType.A, calculator.calculateWeekType(calendar, LocalDate.of(2026, 1, 12), overrides))
        assertEquals(WeekType.A, calculator.calculateWeekType(calendar, LocalDate.of(2026, 1, 19), overrides))
        assertEquals(LocalDate.of(2026, 1, 12), calculator.calculate(calendar, LocalDate.of(2026, 1, 12), overrides)?.overrideSource?.weekStartDate)
    }

    @Test
    fun fromWeekOverrideStartsANewAlternatingSequence() {
        val overrides = listOf(
            WeekOverride(
                id = "from",
                academicCalendarId = calendar.id,
                weekStartDate = LocalDate.of(2026, 1, 12),
                weekType = WeekType.A,
                scope = WeekOverrideScope.FROM_WEEK
            )
        )

        assertEquals(WeekType.A, calculator.calculateWeekType(calendar, LocalDate.of(2026, 1, 12), overrides))
        assertEquals(WeekType.B, calculator.calculateWeekType(calendar, LocalDate.of(2026, 1, 19), overrides))
        assertEquals(WeekType.A, calculator.calculateWeekType(calendar, LocalDate.of(2026, 1, 26), overrides))
    }

    @Test
    fun laterFromWeekAndOneWeekOverridesHaveExpectedPrecedence() {
        val overrides = listOf(
            WeekOverride("from-1", calendar.id, LocalDate.of(2026, 1, 12), WeekType.B, WeekOverrideScope.FROM_WEEK),
            WeekOverride("from-2", calendar.id, LocalDate.of(2026, 1, 19), WeekType.A, WeekOverrideScope.FROM_WEEK),
            WeekOverride("one", calendar.id, LocalDate.of(2026, 1, 26), WeekType.A, WeekOverrideScope.ONE_WEEK)
        )

        assertEquals(WeekType.A, calculator.calculateWeekType(calendar, LocalDate.of(2026, 1, 19), overrides))
        assertEquals(WeekType.A, calculator.calculateWeekType(calendar, LocalDate.of(2026, 1, 26), overrides))
        assertEquals(WeekType.A, calculator.calculateWeekType(calendar, LocalDate.of(2026, 2, 2), overrides))
    }

    @Test
    fun weekStartAlwaysUsesMonday() {
        assertEquals(LocalDate.of(2026, 1, 5), calculator.weekStart(LocalDate.of(2026, 1, 11)))
        assertEquals(DayOfWeek.MONDAY, calculator.weekStart(LocalDate.of(2026, 1, 12)).dayOfWeek)
    }
}
