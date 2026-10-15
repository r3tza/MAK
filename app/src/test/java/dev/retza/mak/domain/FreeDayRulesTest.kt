package dev.retza.mak.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FreeDayRulesTest {
    // 2026-10-12 is a Monday.

    @Test
    fun firstMondayAfterSundayIsNextDay() {
        assertEquals(LocalDate.of(2026, 10, 19), firstMondayAfter(LocalDate.of(2026, 10, 18)))
    }

    @Test
    fun firstMondayAfterFridayIsThreeDaysLater() {
        assertEquals(LocalDate.of(2026, 10, 19), firstMondayAfter(LocalDate.of(2026, 10, 16)))
    }

    @Test
    fun firstMondayAfterMondayIsSevenDaysLater() {
        assertEquals(LocalDate.of(2026, 10, 19), firstMondayAfter(LocalDate.of(2026, 10, 12)))
    }

    @Test
    fun inclusiveDaysCountsBothEnds() {
        assertEquals(1, inclusiveDays(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 12)))
        assertEquals(10, inclusiveDays(LocalDate.of(2026, 12, 24), LocalDate.of(2027, 1, 2)))
    }
}
