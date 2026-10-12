package dev.retza.mak.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateRangeSelectionTest {

    @Test
    fun pickedRangeKeepsBothDates() {
        val result = resolvePickedRange(
            pickedStart = LocalDate.of(2026, 10, 14),
            pickedEnd = LocalDate.of(2026, 10, 20),
            previousEnd = LocalDate.of(2026, 10, 14)
        )
        assertEquals(LocalDate.of(2026, 10, 14) to LocalDate.of(2026, 10, 20), result)
    }

    @Test
    fun startOnlyKeepsPreviousEndWhenNotBeforeStart() {
        assertEquals(
            LocalDate.of(2026, 10, 14) to LocalDate.of(2026, 10, 20),
            resolvePickedRange(LocalDate.of(2026, 10, 14), null, LocalDate.of(2026, 10, 20))
        )
        assertEquals(
            LocalDate.of(2026, 10, 20) to LocalDate.of(2026, 10, 20),
            resolvePickedRange(LocalDate.of(2026, 10, 20), null, LocalDate.of(2026, 10, 20))
        )
    }

    @Test
    fun startOnlyAfterPreviousEndGivesOneDay() {
        assertEquals(
            LocalDate.of(2026, 10, 22) to LocalDate.of(2026, 10, 22),
            resolvePickedRange(LocalDate.of(2026, 10, 22), null, LocalDate.of(2026, 10, 20))
        )
    }

    @Test
    fun startOnlyWithoutPreviousEndGivesOneDay() {
        assertEquals(
            LocalDate.of(2026, 10, 14) to LocalDate.of(2026, 10, 14),
            resolvePickedRange(LocalDate.of(2026, 10, 14), null, null)
        )
    }
}
