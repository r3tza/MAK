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

    override suspend fun snapshot(): BackupData = database.withTransaction {
        val all = semesters.getAllWithData()
        BackupData(
            studyPrograms = studyPrograms.getAll().map { it.toRecord() },
            semesters = all.map { data ->
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

        data.studyPrograms.forEach { studyPrograms.insert(it.toEntity()) }
        data.semesters.forEach { backup ->
            semesters.insert(backup.semester.toEntity())
            backup.calendars.forEach { calendars.insert(it.toEntity()) }
            backup.programs.forEach { semesterPrograms.insert(it.toEntity()) }
            backup.classes.forEach { classes.insert(it.toEntity()) }
            backup.weekOverrides.forEach { weekOverrides.insert(it.toEntity()) }
            backup.occurrenceNotes.forEach { occurrenceNotes.insert(it.toEntity()) }
            backup.occurrenceChanges.forEach { occurrenceChanges.insert(it.toEntity()) }
        }
        data.activeSemesterId
    }
}
