package dev.retza.mak.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** True when the range holds a whole Monday to Friday week (`DOMAIN.md`, „Dni wolne”). */
fun coversWorkWeek(start: LocalDate, end: LocalDate): Boolean =
    !start.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)).plusDays(4).isAfter(end)

/** The Monday on which the rhythm restarts after a break ending on [end]. */
fun firstMondayAfter(end: LocalDate): LocalDate = end.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
