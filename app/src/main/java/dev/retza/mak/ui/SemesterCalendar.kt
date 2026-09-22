package dev.retza.mak.ui

import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.AcademicCalendarEntity
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

internal fun SemesterWithData.sharedCalendar(): AcademicCalendarEntity? =
    academicCalendars.minByOrNull { it.id }

internal fun SemesterWithData.calendarForAssignment(assignmentId: Long?): AcademicCalendarEntity? {
    if (assignmentId == null) return sharedCalendar()
    val calendarId = semesterPrograms.firstOrNull { it.id == assignmentId }?.academicCalendarId
        ?: return sharedCalendar()
    return academicCalendars.firstOrNull { it.id == calendarId } ?: sharedCalendar()
}

internal fun SemesterWithData.calendarsForAssignment(assignmentId: Long?): List<AcademicCalendarEntity> {
    if (assignmentId == null) return academicCalendars
    val calendar = calendarForAssignment(assignmentId) ?: return emptyList()
    return listOf(calendar)
}
