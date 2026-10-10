package dev.retza.mak.domain

import java.time.LocalDate

/**
 * A semester created in the setup wizard becomes active only when today falls inside its
 * calendar or when there is no active semester yet, so a semester added in advance does not
 * replace the plan in use.
 */
fun shouldActivateNewSemester(
    today: LocalDate,
    calendarStart: LocalDate,
    calendarEnd: LocalDate,
    hasActiveSemester: Boolean
): Boolean = !hasActiveSemester || (!today.isBefore(calendarStart) && !today.isAfter(calendarEnd))

/** A semester with the date ranges of its calendars, enough to pick a fallback. */
data class SemesterSpan(val id: Long, val calendars: List<ClosedRange<LocalDate>>)

/**
 * The semester to make active when none is: the one whose calendar covers [today], otherwise the one
 * that starts last, otherwise the first one without calendars.
 */
fun fallbackActiveSemester(semesters: List<SemesterSpan>, today: LocalDate): Long? {
    val withCalendars = semesters.filter { it.calendars.isNotEmpty() }
    val covering = withCalendars.filter { span -> span.calendars.any { today in it } }
    return covering.ifEmpty { withCalendars }.maxByOrNull { span -> span.calendars.minOf { it.start } }?.id
        ?: semesters.firstOrNull()?.id
}
