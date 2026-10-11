package dev.retza.mak.data.repository

import dev.retza.mak.domain.SemesterSpan
import dev.retza.mak.domain.fallbackActiveSemester
import java.time.LocalDate

data class BackupData(
    val studyPrograms: List<StudyProgramRecord>,
    val semesters: List<SemesterBackup>
) {
    val activeSemesterId: Long?
        get() = semesters.firstOrNull { it.semester.isActive }?.semester?.id
}

fun BackupData.withActiveSemester(activeId: Long?): BackupData =
    copy(semesters = semesters.map { it.copy(semester = it.semester.copy(isActive = it.semester.id == activeId)) })

/** The semester to activate when local numbers mean nothing in this plan, see [fallbackActiveSemester]. */
fun BackupData.semesterCovering(today: LocalDate): Long? =
    fallbackActiveSemester(
        semesters.map { backup ->
            SemesterSpan(backup.semester.id, backup.calendars.map { it.startDate..it.endDate })
        },
        today
    )

data class SemesterBackup(
    val semester: SemesterRecord,
    val calendars: List<AcademicCalendarRecord>,
    val programs: List<SemesterProgramRecord>,
    val classes: List<ClassRecord>,
    val weekOverrides: List<WeekOverrideRecord>,
    val occurrenceNotes: List<OccurrenceNoteRecord>,
    val occurrenceChanges: List<OccurrenceChangeRecord>
)
