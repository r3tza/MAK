package dev.retza.mak.domain

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemesterActivationTest {
    private val start = LocalDate.of(2027, 2, 22)
    private val end = LocalDate.of(2027, 6, 30)

    @Test
    fun activatesWhenTodayIsInsideCalendar() {
        assertTrue(shouldActivateNewSemester(LocalDate.of(2027, 4, 1), start, end, hasActiveSemester = true))
    }

    @Test
    fun activatesOnFirstAndLastDayOfCalendar() {
        assertTrue(shouldActivateNewSemester(start, start, end, hasActiveSemester = true))
        assertTrue(shouldActivateNewSemester(end, start, end, hasActiveSemester = true))
    }

    @Test
    fun keepsCurrentSemesterWhenCalendarStartsLater() {
        assertFalse(shouldActivateNewSemester(start.minusDays(1), start, end, hasActiveSemester = true))
    }

    @Test
    fun keepsCurrentSemesterWhenCalendarHasEnded() {
        assertFalse(shouldActivateNewSemester(end.plusDays(1), start, end, hasActiveSemester = true))
    }

    @Test
    fun activatesAnyDatesWhenNoSemesterIsActive() {
        assertTrue(shouldActivateNewSemester(start.minusMonths(3), start, end, hasActiveSemester = false))
        assertTrue(shouldActivateNewSemester(end.plusMonths(3), start, end, hasActiveSemester = false))
    }
}
