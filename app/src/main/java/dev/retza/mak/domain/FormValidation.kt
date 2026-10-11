package dev.retza.mak.domain

import java.time.LocalDate

enum class DateRangeError {
    START_REQUIRED,
    END_REQUIRED,
    END_BEFORE_START
}

/** An end on the same day as the start is valid. */
fun validateDateRange(start: LocalDate?, end: LocalDate?): Set<DateRangeError> {
    val errors = linkedSetOf<DateRangeError>()
    if (start == null) errors += DateRangeError.START_REQUIRED
    if (end == null) errors += DateRangeError.END_REQUIRED
    if (start != null && end != null && end.isBefore(start)) errors += DateRangeError.END_BEFORE_START
    return errors
}

/** Without a calendar there is nothing to check the date against, so it is accepted. */
fun dateInCalendar(date: LocalDate, calendar: AcademicCalendar?): Boolean =
    calendar == null || date in calendar
