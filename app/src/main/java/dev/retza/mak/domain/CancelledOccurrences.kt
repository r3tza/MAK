package dev.retza.mak.domain

import java.time.LocalDate

data class CancelledOccurrence(
    val classItem: ClassItem,
    val studyProgram: StudyProgram?,
    val date: LocalDate,
    val occurrenceNote: OccurrenceNote?
)

/**
 * Cancelled terms of the active plan on [date]. A cancellation counts only while its class still
 * has a term on that date; after a class edit it stays stored but is not shown (`DOMAIN.md`).
 */
fun cancelledOccurrences(
    data: ActivePlanData,
    date: LocalDate,
    weekCalculator: WeekCalculator = WeekCalculator()
): List<CancelledOccurrence> {
    val classesById = data.classes.filter { it.semesterId == data.semester.id }.associateBy { it.id }
    val assignmentsById = data.semesterPrograms.associateBy { it.id }
    val calendarsById = data.calendars.associateBy { it.id }
    val programsById = data.courses.associateBy { it.id }
    return data.occurrenceChanges
        .filter { it.kind == OccurrenceChangeKind.CANCELLED && it.originalDate == date }
        .mapNotNull { change ->
            val classItem = classesById[change.classId] ?: return@mapNotNull null
            // The resolver ignores occurrence changes of one-time classes.
            if (classItem.recurrence == Recurrence.ONCE) return@mapNotNull null
            val assignment = assignmentsById[classItem.semesterProgramId] ?: return@mapNotNull null
            val calendar = calendarsById[assignment.academicCalendarId] ?: return@mapNotNull null
            if (!classItem.hasBaseOccurrenceOn(date, calendar, data.weekOverrides, weekCalculator)) {
                return@mapNotNull null
            }
            CancelledOccurrence(
                classItem = classItem,
                studyProgram = programsById[assignment.studyProgramId],
                date = date,
                occurrenceNote = data.occurrenceNotes.lastOrNull {
                    it.classId == classItem.id && it.occurrenceDate == date
                }
            )
        }
        .sortedWith(compareBy<CancelledOccurrence> { it.classItem.startTime }.thenBy { it.classItem.name })
}
