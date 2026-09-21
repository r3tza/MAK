package dev.retza.mak.data.repository

import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.entity.WeekOverrideEntity

data class BackupData(
    val studyPrograms: List<StudyProgramEntity>,
    val semesters: List<SemesterBackup>
) {
    val activeSemesterId: Long?
        get() = semesters.firstOrNull { it.semester.isActive }?.semester?.id
}

data class SemesterBackup(
    val semester: SemesterEntity,
    val calendars: List<AcademicCalendarEntity>,
    val programs: List<SemesterProgramEntity>,
    val classes: List<ClassEntity>,
    val weekOverrides: List<WeekOverrideEntity>,
    val occurrenceNotes: List<OccurrenceNoteEntity>,
    val occurrenceChanges: List<OccurrenceChangeEntity>
)
