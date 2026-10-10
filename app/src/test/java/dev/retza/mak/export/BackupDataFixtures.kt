package dev.retza.mak.export

import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.data.repository.SemesterBackup
import dev.retza.mak.data.repository.toRecord

/** Builds the export snapshot through the production [BackupData] path from Room relations. */
internal fun exportSnapshotOf(
    semesters: Collection<SemesterWithData>,
    studyPrograms: Collection<StudyProgramEntity>
): ExportSnapshot = ExportSnapshot.from(
    BackupData(
        studyPrograms = studyPrograms.map { it.toRecord() },
        semesters = semesters.map { data ->
            SemesterBackup(
                semester = data.semester.toRecord(),
                calendars = data.academicCalendars.map { it.toRecord() },
                programs = data.semesterPrograms.map { it.toRecord() },
                classes = data.classes.map { it.toRecord() },
                weekOverrides = data.weekOverrides.map { it.toRecord() },
                occurrenceNotes = data.occurrenceNotes.map { it.toRecord() },
                occurrenceChanges = data.occurrenceChanges.map { it.toRecord() }
            )
        }
    )
)
