package dev.retza.mak.domain

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormValidationTest {

    @Test
    fun missingStartIsReported() {
        assertEquals(
            setOf(DateRangeError.START_REQUIRED),
            validateDateRange(start = null, end = LocalDate.of(2026, 2, 1))
        )
    }

    @Test
    fun missingEndIsReported() {
        assertEquals(
            setOf(DateRangeError.END_REQUIRED),
            validateDateRange(start = LocalDate.of(2026, 2, 1), end = null)
        )
    }

    @Test
    fun endBeforeStartIsReported() {
        assertEquals(
            setOf(DateRangeError.END_BEFORE_START),
            validateDateRange(start = LocalDate.of(2026, 2, 2), end = LocalDate.of(2026, 2, 1))
        )
    }

    @Test
    fun endOnSameDayAsStartIsValid() {
        val day = LocalDate.of(2026, 2, 1)
        assertTrue(validateDateRange(start = day, end = day).isEmpty())
    }

    @Test
    fun dateOnCalendarBoundsIsAccepted() {
        val calendar = AcademicCalendar(
            id = 1L,
            startDate = LocalDate.of(2026, 2, 1),
            endDate = LocalDate.of(2026, 6, 30),
            firstWeekType = WeekType.A
        )
        assertTrue(dateInCalendar(LocalDate.of(2026, 2, 1), calendar))
        assertTrue(dateInCalendar(LocalDate.of(2026, 6, 30), calendar))
        assertFalse(dateInCalendar(LocalDate.of(2026, 1, 31), calendar))
        assertFalse(dateInCalendar(LocalDate.of(2026, 7, 1), calendar))
    }

    @Test
    fun missingCalendarDoesNotRestrictDate() {
        assertTrue(dateInCalendar(LocalDate.of(2026, 7, 1), null))
    }

    @Test
    fun occurrenceDateOutsideCalendarIsReported() {
        val calendar = AcademicCalendar(
            id = 1L,
            startDate = LocalDate.of(2026, 2, 1),
            endDate = LocalDate.of(2026, 6, 30),
            firstWeekType = WeekType.A
        )
        val slot = slot(date = LocalDate.of(2026, 1, 31))
        assertEquals(
            setOf(OccurrenceDraftError.DATE_OUTSIDE_CALENDAR),
            validateOccurrenceDraft(slot, calendar)
        )
    }

    @Test
    fun missingCalendarDoesNotRestrictOccurrenceDate() {
        assertTrue(validateOccurrenceDraft(slot(date = LocalDate.of(2030, 1, 1)), null).isEmpty())
    }

    @Test
    fun occurrenceEndNotAfterStartIsReported() {
        val slot = slot(start = LocalTime.of(10, 0), end = LocalTime.of(10, 0))
        assertEquals(
            setOf(OccurrenceDraftError.END_NOT_AFTER_START),
            validateOccurrenceDraft(slot, null)
        )
    }

    private fun slot(
        date: LocalDate = LocalDate.of(2026, 3, 1),
        start: LocalTime = LocalTime.of(8, 0),
        end: LocalTime = LocalTime.of(9, 30)
    ) = OccurrenceSlot(date, start, end, room = null)
}
