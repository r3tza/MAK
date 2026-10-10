package dev.retza.mak.export

import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.data.repository.SemesterBackup

/** Builds the export snapshot through the production [BackupData] path from Room relations. */
internal fun exportSnapshotOf(
    semesters: Collection<SemesterWithData>,
    studyPrograms: Collection<StudyProgramEntity>
): ExportSnapshot = ExportSnapshot.from(
    BackupData(
        studyPrograms = studyPrograms.toList(),
        semesters = semesters.map { data ->
            SemesterBackup(
                semester = data.semester,
                calendars = data.academicCalendars,
                programs = data.semesterPrograms,
                classes = data.classes,
                weekOverrides = data.weekOverrides,
                occurrenceNotes = data.occurrenceNotes,
                occurrenceChanges = data.occurrenceChanges
            )
        }
    )
)
