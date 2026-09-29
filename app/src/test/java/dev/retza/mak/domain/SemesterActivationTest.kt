package dev.retza.mak.domain

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemesterActivationTest {
    private val start = LocalDate.of(2027, 2, 22)
    private val end = LocalDate.of(2027, 6, 30)

    @Test
    fun activatesFromFirstToLastDayOfCalendar() {
        listOf(start, LocalDate.of(2027, 4, 1), end).forEach { today ->
            assertTrue("$today", shouldActivateNewSemester(today, start, end, hasActiveSemester = true))
        }
    }

    @Test
    fun keepsCurrentSemesterOutsideCalendar() {
        listOf(start.minusDays(1), end.plusDays(1)).forEach { today ->
            assertFalse("$today", shouldActivateNewSemester(today, start, end, hasActiveSemester = true))
        }
    }

    @Test
    fun activatesAnyDatesWhenNoSemesterIsActive() {
        assertTrue(shouldActivateNewSemester(start.minusMonths(3), start, end, hasActiveSemester = false))
        assertTrue(shouldActivateNewSemester(end.plusMonths(3), start, end, hasActiveSemester = false))
    }
}
