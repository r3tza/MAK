package dev.retza.mak.domain

import java.time.LocalDate

data class Holiday(val date: LocalDate, val name: String)

/** Polish public holidays (ustawa o dniach wolnych od pracy), movable dates computed locally. */
object PolishHolidays {
    /** Holidays from [start] to [end], both inclusive, sorted by date; empty when [end] is before [start]. */
    fun between(start: LocalDate, end: LocalDate): List<Holiday> {
        if (end.isBefore(start)) return emptyList()
        return (start.year..end.year)
            .flatMap { year -> holidaysOf(year) }
            .filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
            .sortedBy { it.date }
    }

    private fun holidaysOf(year: Int): List<Holiday> {
        val easter = easterSunday(year)
        return buildList {
            add(Holiday(LocalDate.of(year, 1, 1), "Nowy Rok"))
            add(Holiday(LocalDate.of(year, 1, 6), "Trzech Króli"))
            add(Holiday(easter, "Wielkanoc"))
            add(Holiday(easter.plusDays(1), "Poniedziałek Wielkanocny"))
            add(Holiday(LocalDate.of(year, 5, 1), "Święto Pracy"))
            add(Holiday(LocalDate.of(year, 5, 3), "Święto Konstytucji 3 Maja"))
            add(Holiday(easter.plusDays(49), "Zielone Świątki"))
            add(Holiday(easter.plusDays(60), "Boże Ciało"))
            add(Holiday(LocalDate.of(year, 8, 15), "Wniebowzięcie Najświętszej Maryi Panny"))
            add(Holiday(LocalDate.of(year, 11, 1), "Wszystkich Świętych"))
            add(Holiday(LocalDate.of(year, 11, 11), "Święto Niepodległości"))
            if (year >= 2025) add(Holiday(LocalDate.of(year, 12, 24), "Wigilia Bożego Narodzenia"))
            add(Holiday(LocalDate.of(year, 12, 25), "Boże Narodzenie"))
            add(Holiday(LocalDate.of(year, 12, 26), "Drugi dzień Bożego Narodzenia"))
        }
    }

    /** Easter Sunday in the Gregorian calendar (anonymous algorithm, Meeus/Jones/Butcher). */
    internal fun easterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }
}
