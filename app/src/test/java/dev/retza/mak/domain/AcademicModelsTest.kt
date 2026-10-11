package dev.retza.mak.domain

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AcademicModelsTest {
    @Test
    fun academicCalendarContainsBothEndsAndNothingOutside() {
        val calendar = AcademicCalendar(
            id = 1L,
            startDate = LocalDate.of(2027, 2, 1),
            endDate = LocalDate.of(2027, 6, 30),
            firstWeekType = WeekType.A
        )

        assertTrue(LocalDate.of(2027, 2, 1) in calendar)
        assertTrue(LocalDate.of(2027, 6, 30) in calendar)
        assertFalse(LocalDate.of(2027, 1, 31) in calendar)
        assertFalse(LocalDate.of(2027, 7, 1) in calendar)
    }

    @Test
    fun academicCalendarRejectsReversedDates() {
        assertThrows(IllegalArgumentException::class.java) {
            AcademicCalendar(
                id = 2L,
                startDate = LocalDate.of(2027, 2, 1),
                endDate = LocalDate.of(2027, 1, 1),
                firstWeekType = WeekType.A
            )
        }
    }
}
