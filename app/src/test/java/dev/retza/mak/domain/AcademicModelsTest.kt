package dev.retza.mak.domain

import java.time.LocalDate
import org.junit.Assert.assertThrows
import org.junit.Test

class AcademicModelsTest {
    @Test
    fun academicCalendarRejectsReversedDates() {
        assertThrows(IllegalArgumentException::class.java) {
            AcademicCalendar(
                id = "calendar-bad",
                startDate = LocalDate.of(2027, 2, 1),
                endDate = LocalDate.of(2027, 1, 1),
                firstWeekType = WeekType.A
            )
        }
    }
}
