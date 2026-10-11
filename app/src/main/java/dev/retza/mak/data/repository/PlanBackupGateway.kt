package dev.retza.mak.data.repository

import androidx.room.withTransaction
import dev.retza.mak.data.database.AppDatabase
import java.time.Clock
import java.time.LocalDate

interface PlanBackupGateway {
    suspend fun snapshot(): BackupData

    suspend fun replaceAll(data: BackupData): Long?

    /**
     * Reads the plan and replaces it in one transaction. [prepare] gets the current plan and returns the plan
     * to write, or null to leave the database unchanged. It runs inside the transaction, so it must not suspend.
     * True when the plan was replaced.
     */
    suspend fun replaceIfCurrent(prepare: (current: BackupData) -> BackupData?): Boolean
}

@org.koin.core.annotation.Single(binds = [PlanBackupGateway::class])
class RoomPlanBackupGateway(
    private val database: AppDatabase,
    private val clock: Clock
) : PlanBackupGateway {
    private val semesters = database.semesterDao()
    private val studyPrograms = database.studyProgramDao()
    private val calendars = database.academicCalendarDao()
    private val semesterPrograms = database.semesterProgramDao()
    private val classes = database.classDao()
    private val weekOverrides = database.weekOverrideDao()
    private val freeDays = database.freeDayDao()
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
                    freeDays = data.freeDays.map { it.toRecord() },
                    occurrenceNotes = data.occurrenceNotes.map { it.toRecord() },
                    occurrenceChanges = data.occurrenceChanges.map { it.toRecord() }
                )
            }
        )
    }

    override suspend fun replaceIfCurrent(prepare: (current: BackupData) -> BackupData?): Boolean =
        database.withTransaction {
            val next = prepare(snapshot()) ?: return@withTransaction false
            replaceAll(next)
            true
        }

    override suspend fun replaceAll(data: BackupData): Long? = database.withTransaction {
        // A plan without an active semester (for example from a file made without one) gets the fallback one.
        val plan = if (data.activeSemesterId == null) {
            data.withActiveSemester(data.semesterCovering(LocalDate.now(clock)))
        } else {
            data
        }
        occurrenceChanges.deleteAll()
        occurrenceNotes.deleteAll()
        weekOverrides.deleteAll()
        freeDays.deleteAll()
        classes.deleteAll()
        semesterPrograms.deleteAll()
        calendars.deleteAll()
        studyPrograms.deleteAll()
        semesters.deleteAll()

        plan.studyPrograms.forEach { studyPrograms.insert(it.toEntity()) }
        plan.semesters.forEach { backup ->
            semesters.insert(backup.semester.toEntity())
            backup.calendars.forEach { calendars.insert(it.toEntity()) }
            backup.programs.forEach { semesterPrograms.insert(it.toEntity()) }
            backup.classes.forEach { classes.insert(it.toEntity()) }
            backup.weekOverrides.forEach { weekOverrides.insert(it.toEntity()) }
            backup.freeDays.forEach { freeDays.insert(it.toEntity()) }
            backup.occurrenceNotes.forEach { occurrenceNotes.insert(it.toEntity()) }
            backup.occurrenceChanges.forEach { occurrenceChanges.insert(it.toEntity()) }
        }
        plan.activeSemesterId
    }
}
