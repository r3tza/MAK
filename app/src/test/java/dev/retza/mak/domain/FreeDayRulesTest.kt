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
}
