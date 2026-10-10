package dev.retza.mak.data.repository

data class BackupData(
    val studyPrograms: List<StudyProgramRecord>,
    val semesters: List<SemesterBackup>
) {
    val activeSemesterId: Long?
        get() = semesters.firstOrNull { it.semester.isActive }?.semester?.id
}

data class SemesterBackup(
    val semester: SemesterRecord,
    val calendars: List<AcademicCalendarRecord>,
    val programs: List<SemesterProgramRecord>,
    val classes: List<ClassRecord>,
    val weekOverrides: List<WeekOverrideRecord>,
    val occurrenceNotes: List<OccurrenceNoteRecord>,
    val occurrenceChanges: List<OccurrenceChangeRecord>
)
