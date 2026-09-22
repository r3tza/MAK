package dev.retza.mak.data.repository

data class SetupConfigurationIds(
    val semesterId: Long,
    val studyProgramId: Long,
    val academicCalendarId: Long,
    val semesterProgramId: Long
)

data class SemesterDeletionResult(val activeSemesterId: Long?)
