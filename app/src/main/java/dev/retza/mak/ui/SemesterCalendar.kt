package dev.retza.mak.ui

import dev.retza.mak.domain.AcademicCalendar
import dev.retza.mak.domain.ActivePlanData
import dev.retza.mak.domain.sharedCalendarOf
import dev.retza.mak.domain.StudyProgram

internal fun ActivePlanData.calendarForAssignment(assignmentId: Long?): AcademicCalendar? {
    val calendarId = assignmentId
        ?.let { id -> semesterPrograms.firstOrNull { it.id == id }?.academicCalendarId }
        ?: return sharedCalendar()
    return calendars.firstOrNull { it.id == calendarId } ?: sharedCalendar()
}

internal fun ActivePlanData.sharedCalendar(): AcademicCalendar? =
    sharedCalendarOf(calendars) { it.id }

internal fun ActivePlanData.programForAssignment(assignmentId: Long?): StudyProgram? {
    val studyProgramId = semesterPrograms.firstOrNull { it.id == assignmentId }?.studyProgramId ?: return null
    return courses.firstOrNull { it.id == studyProgramId }
}
