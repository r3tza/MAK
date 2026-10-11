package dev.retza.mak.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PolishHolidaysTest {
    @Test
    fun easterSundayMatchesKnownYears() {
        assertEquals(LocalDate.of(2024, 3, 31), PolishHolidays.easterSunday(2024))
        assertEquals(LocalDate.of(2025, 4, 20), PolishHolidays.easterSunday(2025))
        assertEquals(LocalDate.of(2026, 4, 5), PolishHolidays.easterSunday(2026))
        assertEquals(LocalDate.of(2027, 3, 28), PolishHolidays.easterSunday(2027))
    }

    @Test
    fun corpusChristiFollowsEaster() {
        assertEquals(LocalDate.of(2025, 6, 19), holidayNamed(2025, "Boże Ciało").date)
        assertEquals(LocalDate.of(2026, 6, 4), holidayNamed(2026, "Boże Ciało").date)
    }

    @Test
    fun christmasEveIsHolidayOnlyFromYear2025() {
        val in2024 = PolishHolidays.between(LocalDate.of(2024, 12, 1), LocalDate.of(2024, 12, 31))
        assertTrue(in2024.none { it.name == "Wigilia Bożego Narodzenia" })

        val in2025 = PolishHolidays.between(LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 31))
        assertTrue(in2025.any { it.name == "Wigilia Bożego Narodzenia" })
    }

    @Test
    fun holidaysAcrossNewYearAreSortedAndComplete() {
        val holidays = PolishHolidays.between(LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 10))
        assertEquals(
            listOf(
                "Wigilia Bożego Narodzenia",
                "Boże Narodzenie",
                "Drugi dzień Bożego Narodzenia",
                "Nowy Rok",
                "Trzech Króli"
            ),
            holidays.map { it.name }
        )
    }

    @Test
    fun endBeforeStartGivesNoHolidays() {
        assertTrue(PolishHolidays.between(LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 1)).isEmpty())
    }

    private fun holidayNamed(year: Int, name: String): Holiday =
        PolishHolidays.between(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
            .single { it.name == name }
}
