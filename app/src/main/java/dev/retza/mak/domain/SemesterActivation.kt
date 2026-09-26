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
