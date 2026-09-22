package dev.retza.mak.ui

import dev.retza.mak.domain.AcademicCalendar
import dev.retza.mak.domain.ActivePlanData

internal fun ActivePlanData.calendarForAssignment(assignmentId: String?): AcademicCalendar? {
    val calendarId = assignmentId
        ?.let { id -> semesterPrograms.firstOrNull { it.id == id }?.academicCalendarId }
        ?: return sharedCalendar()
    return calendars.firstOrNull { it.id == calendarId } ?: sharedCalendar()
}

private fun ActivePlanData.sharedCalendar(): AcademicCalendar? =
    calendars.minByOrNull { it.id.toLongOrNull() ?: Long.MAX_VALUE }
