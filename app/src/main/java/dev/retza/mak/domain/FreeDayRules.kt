package dev.retza.mak.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** The Monday on which the rhythm restarts after a break ending on [end]. */
fun firstMondayAfter(end: LocalDate): LocalDate = end.with(TemporalAdjusters.next(DayOfWeek.MONDAY))

/** Days from [start] to [end], both ends included. */
fun inclusiveDays(start: LocalDate, end: LocalDate): Int = ChronoUnit.DAYS.between(start, end).toInt() + 1
