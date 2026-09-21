package dev.retza.mak.ui

import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.AcademicCalendarEntity

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
