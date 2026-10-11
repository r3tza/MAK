package dev.retza.mak.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeDayRulesTest {
    // 2026-10-12 is a Monday.

    @Test
    fun mondayToFridayCoversWorkWeek() {
        assertTrue(coversWorkWeek(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 16)))
    }

    @Test
    fun tuesdayToNextMondayDoesNotCoverWorkWeek() {
        assertFalse(coversWorkWeek(LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 19)))
    }

    @Test
    fun wednesdayToFridayOfNextWeekCoversWorkWeek() {
        assertTrue(coversWorkWeek(LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 23)))
    }

    @Test
    fun weekendDoesNotCoverWorkWeek() {
        assertFalse(coversWorkWeek(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 18)))
    }

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
