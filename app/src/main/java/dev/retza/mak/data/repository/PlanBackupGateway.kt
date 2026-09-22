package dev.retza.mak.data.repository

import androidx.room.withTransaction
import dev.retza.mak.data.database.AppDatabase

interface PlanBackupGateway {
    suspend fun snapshot(): BackupData

    suspend fun replaceAll(data: BackupData): Long?
}

@org.koin.core.annotation.Single(binds = [PlanBackupGateway::class])
class RoomPlanBackupGateway(
    private val database: AppDatabase
) : PlanBackupGateway {
    private val semesters = database.semesterDao()
    private val studyPrograms = database.studyProgramDao()
    private val calendars = database.academicCalendarDao()
    private val semesterPrograms = database.semesterProgramDao()
    private val classes = database.classDao()
    private val weekOverrides = database.weekOverrideDao()
    private val occurrenceNotes = database.occurrenceNoteDao()
    private val occurrenceChanges = database.occurrenceChangeDao()

    override suspend fun snapshot(): BackupData {
        val all = semesters.getAllWithData()
        return BackupData(
            studyPrograms = studyPrograms.getAll(),
            semesters = all.map { data ->
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
    }

    override suspend fun replaceAll(data: BackupData): Long? = database.withTransaction {
        occurrenceChanges.deleteAll()
        occurrenceNotes.deleteAll()
        weekOverrides.deleteAll()
        classes.deleteAll()
        semesterPrograms.deleteAll()
        calendars.deleteAll()
        studyPrograms.deleteAll()
        semesters.deleteAll()

        data.studyPrograms.forEach { studyPrograms.insert(it) }
        data.semesters.forEach { backup ->
            semesters.insert(backup.semester)
            backup.calendars.forEach { calendars.insert(it) }
            backup.programs.forEach { semesterPrograms.insert(it) }
            backup.classes.forEach { classes.insert(it) }
            backup.weekOverrides.forEach { weekOverrides.insert(it) }
            backup.occurrenceNotes.forEach { occurrenceNotes.insert(it) }
            backup.occurrenceChanges.forEach { occurrenceChanges.insert(it) }
        }
        data.activeSemesterId
    }
}
