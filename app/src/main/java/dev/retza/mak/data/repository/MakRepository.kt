package dev.retza.mak.data.repository

import androidx.room.withTransaction
import dev.retza.mak.data.database.AppDatabase
import dev.retza.mak.data.database.ClassWithDetails
import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.Recurrence
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.entity.WeekOverrideEntity
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow

data class SetupConfigurationIds(
    val semesterId: Long,
    val studyProgramId: Long,
    val academicCalendarId: Long,
    val semesterProgramId: Long
)

data class SemesterDeletionResult(val activeSemesterId: Long?)

interface MakRepository {
    fun observeSemesters(): Flow<List<SemesterEntity>>

    fun observeActiveSemester(): Flow<SemesterEntity?>

    fun observeSemester(id: Long): Flow<SemesterEntity?>

    fun observeSemesterData(id: Long): Flow<SemesterWithData?>

    fun observeStudyPrograms(): Flow<List<StudyProgramEntity>>

    fun observeSemesterPrograms(semesterId: Long): Flow<List<SemesterProgramEntity>>

    fun observeCalendars(semesterId: Long): Flow<List<AcademicCalendarEntity>>

    fun observeClasses(semesterId: Long): Flow<List<ClassEntity>>

    fun observeClassesWithDetails(semesterId: Long): Flow<List<ClassWithDetails>>

    fun observeWeekOverrides(semesterId: Long): Flow<List<WeekOverrideEntity>>

    fun observeOccurrenceNotes(semesterId: Long): Flow<List<OccurrenceNoteEntity>>

    fun observeOccurrenceChanges(semesterId: Long): Flow<List<OccurrenceChangeEntity>>

    fun observeOccurrenceNotesForClass(classId: Long): Flow<List<OccurrenceNoteEntity>>

    fun observeOccurrenceChangesForClass(classId: Long): Flow<List<OccurrenceChangeEntity>>

    suspend fun getAllSemesterData(): List<SemesterWithData>

    suspend fun saveSemester(entity: SemesterEntity): Long

    suspend fun updateSemester(entity: SemesterEntity): Long

    suspend fun setActiveSemester(id: Long)

    suspend fun clearActiveSemester()

    suspend fun deleteSemester(id: Long)

    suspend fun deleteSemesterAndSelectFallback(id: Long): SemesterDeletionResult

    suspend fun saveStudyProgram(entity: StudyProgramEntity): Long

    suspend fun deleteStudyProgram(id: Long)

    suspend fun saveCalendar(entity: AcademicCalendarEntity): Long

    suspend fun updateSemesterWithCalendar(semester: SemesterEntity, calendar: AcademicCalendarEntity)

    suspend fun deleteCalendarIfUnused(id: Long)

    suspend fun saveSemesterProgram(entity: SemesterProgramEntity): Long

    suspend fun deleteSemesterProgram(id: Long)

    suspend fun saveSetupConfiguration(
        semester: SemesterEntity,
        studyProgram: StudyProgramEntity,
        calendar: AcademicCalendarEntity
    ): SetupConfigurationIds

    suspend fun saveClass(entity: ClassEntity): Long

    suspend fun deleteClass(id: Long)

    suspend fun saveWeekOverride(entity: WeekOverrideEntity): Long

    suspend fun deleteWeekOverride(id: Long)

    suspend fun saveOccurrenceNote(entity: OccurrenceNoteEntity): Long

    suspend fun deleteOccurrenceNote(id: Long)

    suspend fun saveOccurrenceChange(entity: OccurrenceChangeEntity): Long

    suspend fun deleteOccurrenceChange(id: Long)
}

@org.koin.core.annotation.Single(binds = [MakRepository::class])
class RoomMakRepository(
    private val database: AppDatabase
) : MakRepository {
    private val semesters = database.semesterDao()
    private val studyPrograms = database.studyProgramDao()
    private val calendars = database.academicCalendarDao()
    private val semesterPrograms = database.semesterProgramDao()
    private val classes = database.classDao()
    private val weekOverrides = database.weekOverrideDao()
    private val occurrenceNotes = database.occurrenceNoteDao()
    private val occurrenceChanges = database.occurrenceChangeDao()

    override fun observeSemesters(): Flow<List<SemesterEntity>> = semesters.observeAll()

    override fun observeActiveSemester(): Flow<SemesterEntity?> = semesters.observeActive()

    override fun observeSemester(id: Long): Flow<SemesterEntity?> = semesters.observeById(id)

    override fun observeSemesterData(id: Long): Flow<SemesterWithData?> = semesters.observeWithData(id)

    override fun observeStudyPrograms(): Flow<List<StudyProgramEntity>> = studyPrograms.observeAll()

    override fun observeSemesterPrograms(semesterId: Long): Flow<List<SemesterProgramEntity>> =
        semesterPrograms.observeForSemester(semesterId)

    override fun observeCalendars(semesterId: Long): Flow<List<AcademicCalendarEntity>> =
        calendars.observeForSemester(semesterId)

    override fun observeClasses(semesterId: Long): Flow<List<ClassEntity>> =
        classes.observeForSemester(semesterId)

    override fun observeClassesWithDetails(semesterId: Long): Flow<List<ClassWithDetails>> =
        classes.observeWithDetailsForSemester(semesterId)

    override fun observeWeekOverrides(semesterId: Long): Flow<List<WeekOverrideEntity>> =
        weekOverrides.observeForSemester(semesterId)

    override fun observeOccurrenceNotes(semesterId: Long): Flow<List<OccurrenceNoteEntity>> =
        occurrenceNotes.observeForSemester(semesterId)

    override fun observeOccurrenceChanges(semesterId: Long): Flow<List<OccurrenceChangeEntity>> =
        occurrenceChanges.observeForSemester(semesterId)

    override fun observeOccurrenceNotesForClass(classId: Long): Flow<List<OccurrenceNoteEntity>> =
        occurrenceNotes.observeForClass(classId)

    override fun observeOccurrenceChangesForClass(classId: Long): Flow<List<OccurrenceChangeEntity>> =
        occurrenceChanges.observeForClass(classId)

    override suspend fun getAllSemesterData(): List<SemesterWithData> = semesters.getAllWithData()

    override suspend fun saveSemester(entity: SemesterEntity): Long {
        require(entity.name.isNotBlank()) { "Semester name cannot be blank" }
        return database.withTransaction {
            if (entity.isActive) {
                semesters.clearActive()
            }
            if (entity.id == 0L) {
                semesters.insert(entity)
            } else {
                require(semesters.findById(entity.id) != null) { "Semester does not exist" }
                semesters.update(entity)
                entity.id
            }
        }
    }

    override suspend fun updateSemester(entity: SemesterEntity): Long {
        require(entity.name.isNotBlank()) { "Semester name cannot be blank" }
        require(entity.id != 0L) { "Use saveSemester to create a semester" }
        return database.withTransaction {
            val existing = semesters.findById(entity.id) ?: error("Semester does not exist")
            semesters.update(entity.copy(isActive = existing.isActive))
            entity.id
        }
    }

    override suspend fun setActiveSemester(id: Long) {
        database.withTransaction {
            require(semesters.findById(id) != null) { "Semester does not exist" }
            semesters.clearActive()
            semesters.markActive(id)
        }
    }

    override suspend fun clearActiveSemester() = semesters.clearActive()

    override suspend fun deleteSemester(id: Long) = semesters.deleteById(id)

    override suspend fun deleteSemesterAndSelectFallback(id: Long): SemesterDeletionResult =
        database.withTransaction {
            val target = semesters.findById(id) ?: error("Semester does not exist")
            val wasActive = target.isActive
            semesters.deleteById(id)
            val remaining = semesters.getAll()
            if (remaining.isEmpty()) {
                semesters.clearActive()
                return@withTransaction SemesterDeletionResult(null)
            }
            val fallbackId = if (wasActive) {
                val chosen = remaining.first()
                semesters.clearActive()
                semesters.markActive(chosen.id)
                chosen.id
            } else {
                remaining.firstOrNull { it.isActive }?.id ?: run {
                    val chosen = remaining.first()
                    semesters.markActive(chosen.id)
                    chosen.id
                }
            }
            SemesterDeletionResult(fallbackId)
        }

    override suspend fun saveStudyProgram(entity: StudyProgramEntity): Long {
        require(entity.name.isNotBlank()) { "Study program name cannot be blank" }
        if (entity.id == 0L) return studyPrograms.insert(entity)
        require(studyPrograms.findById(entity.id) != null) { "Study program does not exist" }
        studyPrograms.update(entity)
        return entity.id
    }

    override suspend fun deleteStudyProgram(id: Long) {
        database.withTransaction {
            require(studyPrograms.countAssignments(id) == 0) {
                "Study program is assigned to a semester"
            }
            studyPrograms.deleteById(id)
        }
    }

    override suspend fun saveCalendar(entity: AcademicCalendarEntity): Long {
        require(!entity.endDate.isBefore(entity.startDate)) {
            "Calendar end must not be before its start"
        }
        if (entity.id == 0L) {
            require(semesters.findById(entity.semesterId) != null) { "Semester does not exist" }
            return calendars.insert(entity)
        }
        val existing = calendars.findById(entity.id) ?: error("Calendar does not exist")
        require(existing.semesterId == entity.semesterId) { "Calendar semester cannot change" }
        calendars.update(entity)
        return entity.id
    }

    override suspend fun updateSemesterWithCalendar(
        semester: SemesterEntity,
        calendar: AcademicCalendarEntity
    ) {
        require(semester.name.isNotBlank()) { "Semester name cannot be blank" }
        require(calendar.id != 0L) { "Calendar must exist" }
        require(!calendar.endDate.isBefore(calendar.startDate)) {
            "Calendar end must not be before its start"
        }
        database.withTransaction {
            val existingSemester = semesters.findById(semester.id) ?: error("Semester does not exist")
            semesters.update(semester.copy(isActive = existingSemester.isActive))
            val existingCalendar = calendars.findById(calendar.id) ?: error("Calendar does not exist")
            require(existingCalendar.semesterId == semester.id) {
                "Calendar must belong to the semester"
            }
            calendars.update(calendar)
        }
    }

    override suspend fun deleteCalendarIfUnused(id: Long) {
        database.withTransaction {
            if (calendars.countAssignments(id) == 0) {
                calendars.deleteById(id)
            }
        }
    }

    override suspend fun saveSemesterProgram(entity: SemesterProgramEntity): Long {
        if (entity.id == 0L) {
            require(semesters.findById(entity.semesterId) != null) { "Semester does not exist" }
            require(studyPrograms.findById(entity.studyProgramId) != null) {
                "Study program does not exist"
            }
            require(calendars.findById(entity.academicCalendarId)?.semesterId == entity.semesterId) {
                "Calendar must belong to the semester"
            }
            return semesterPrograms.insert(entity)
        }
        database.withTransaction {
            val existing = semesterPrograms.findById(entity.id) ?: error("Assignment does not exist")
            require(existing.semesterId == entity.semesterId && existing.studyProgramId == entity.studyProgramId) {
                "Assignment owner cannot change"
            }
            require(calendars.findById(entity.academicCalendarId)?.semesterId == entity.semesterId) {
                "Calendar must belong to the semester"
            }
            semesterPrograms.update(entity)
            if (existing.academicCalendarId != entity.academicCalendarId &&
                calendars.countAssignments(existing.academicCalendarId) == 0
            ) {
                calendars.deleteById(existing.academicCalendarId)
            }
        }
        return entity.id
    }

    override suspend fun deleteSemesterProgram(id: Long) {
        database.withTransaction {
            val existing = semesterPrograms.findById(id) ?: return@withTransaction
            semesterPrograms.deleteById(id)
            if (calendars.countAssignments(existing.academicCalendarId) == 0) {
                calendars.deleteById(existing.academicCalendarId)
            }
        }
    }

    override suspend fun saveSetupConfiguration(
        semester: SemesterEntity,
        studyProgram: StudyProgramEntity,
        calendar: AcademicCalendarEntity
    ): SetupConfigurationIds {
        require(semester.name.isNotBlank()) { "Semester name cannot be blank" }
        require(studyProgram.name.isNotBlank()) { "Study program name cannot be blank" }
        require(!calendar.endDate.isBefore(calendar.startDate)) {
            "Calendar end must not be before its start"
        }
        return database.withTransaction {
            val semesterId = if (semester.id == 0L) {
                semesters.clearActive()
                semesters.insert(semester.copy(isActive = true))
            } else {
                require(semesters.findById(semester.id) != null) { "Semester does not exist" }
                semesters.clearActive()
                semesters.update(semester.copy(isActive = true))
                semester.id
            }
            val studyProgramId = if (studyProgram.id == 0L) {
                studyPrograms.insert(studyProgram)
            } else {
                require(studyPrograms.findById(studyProgram.id) != null) {
                    "Study program does not exist"
                }
                studyPrograms.update(studyProgram)
                studyProgram.id
            }
            val calendarId = if (calendar.id == 0L) {
                calendars.insert(calendar.copy(semesterId = semesterId))
            } else {
                val existing = calendars.findById(calendar.id)
                require(existing != null && existing.semesterId == semesterId) {
                    "Calendar does not belong to the semester"
                }
                calendars.update(calendar.copy(semesterId = semesterId))
                calendar.id
            }
            val existingAssignment = semesterPrograms.findByProgram(semesterId, studyProgramId)
            val semesterProgramId = if (existingAssignment == null) {
                semesterPrograms.insert(
                    SemesterProgramEntity(
                        semesterId = semesterId,
                        studyProgramId = studyProgramId,
                        academicCalendarId = calendarId
                    )
                )
            } else {
                val previousCalendarId = existingAssignment.academicCalendarId
                semesterPrograms.update(existingAssignment.copy(academicCalendarId = calendarId))
                if (previousCalendarId != calendarId &&
                    calendars.countAssignments(previousCalendarId) == 0
                ) {
                    calendars.deleteById(previousCalendarId)
                }
                existingAssignment.id
            }
            SetupConfigurationIds(semesterId, studyProgramId, calendarId, semesterProgramId)
        }
    }

    override suspend fun saveClass(entity: ClassEntity): Long {
        require(semesters.findById(entity.semesterId) != null) { "Semester does not exist" }
        require(semesterPrograms.findById(entity.semesterProgramId)?.semesterId == entity.semesterId) {
            "Assignment must belong to the class semester"
        }
        require(entity.name.isNotBlank()) { "Class name cannot be blank" }
        require(entity.endTime.isAfter(entity.startTime)) {
            "Class end time must be later than start time"
        }
        require(entity.recurrence != Recurrence.ONCE || entity.date != null) {
            "One-time classes require a date"
        }
        if (entity.id == 0L) return classes.insert(entity)
        val existing = classes.findById(entity.id)
        require(existing != null) { "Class does not exist" }
        require(existing.semesterId == entity.semesterId) { "Class semester cannot change" }
        classes.update(entity)
        return entity.id
    }

    override suspend fun deleteClass(id: Long) = classes.deleteById(id)

    override suspend fun saveWeekOverride(entity: WeekOverrideEntity): Long {
        require(semesters.findById(entity.semesterId) != null) { "Semester does not exist" }
        require(calendars.findById(entity.academicCalendarId)?.semesterId == entity.semesterId) {
            "Calendar must belong to the semester"
        }
        require(entity.weekStartDate.dayOfWeek == DayOfWeek.MONDAY) {
            "Week override must start on Monday"
        }
        if (entity.id == 0L) {
            val existing = weekOverrides.findForWeek(
                entity.academicCalendarId,
                entity.weekStartDate,
                entity.scope
            )
            if (existing == null) return weekOverrides.insert(entity)
            weekOverrides.update(entity.copy(id = existing.id))
            return existing.id
        }
        val existing = weekOverrides.findById(entity.id)
        require(existing != null) { "Week override does not exist" }
        require(existing.semesterId == entity.semesterId &&
            existing.academicCalendarId == entity.academicCalendarId) {
            "Week override owner cannot change"
        }
        weekOverrides.update(entity)
        return entity.id
    }

    override suspend fun deleteWeekOverride(id: Long) = weekOverrides.deleteById(id)

    override suspend fun saveOccurrenceNote(entity: OccurrenceNoteEntity): Long {
        validateClassOwnership(entity.classId, entity.semesterId)
        require(entity.body.isNotBlank()) { "Occurrence note cannot be blank" }
        if (entity.id == 0L) {
            val existing = occurrenceNotes.findForOccurrence(entity.classId, entity.occurrenceDate)
            if (existing == null) return occurrenceNotes.insert(entity)
            occurrenceNotes.update(entity.copy(id = existing.id))
            return existing.id
        }
        val existing = occurrenceNotes.findById(entity.id)
        require(existing != null) { "Occurrence note does not exist" }
        require(existing.classId == entity.classId && existing.semesterId == entity.semesterId) {
            "Occurrence note ownership cannot change"
        }
        occurrenceNotes.update(entity)
        return entity.id
    }

    override suspend fun deleteOccurrenceNote(id: Long) = occurrenceNotes.deleteById(id)

    override suspend fun saveOccurrenceChange(entity: OccurrenceChangeEntity): Long {
        validateClassOwnership(entity.classId, entity.semesterId)
        if (entity.newStartTime != null || entity.newEndTime != null) {
            require(entity.newStartTime != null && entity.newEndTime != null) {
                "Both replacement times are required"
            }
            require(entity.newEndTime.isAfter(entity.newStartTime)) {
                "Replacement end time must be later than start time"
            }
        }
        if (entity.id == 0L) {
            val existing = occurrenceChanges.findForOccurrence(entity.classId, entity.originalDate)
            if (existing == null) return occurrenceChanges.insert(entity)
            occurrenceChanges.update(entity.copy(id = existing.id))
            return existing.id
        }
        val existing = occurrenceChanges.findById(entity.id)
        require(existing != null) { "Occurrence change does not exist" }
        require(existing.classId == entity.classId && existing.semesterId == entity.semesterId) {
            "Occurrence change ownership cannot change"
        }
        occurrenceChanges.update(entity)
        return entity.id
    }

    override suspend fun deleteOccurrenceChange(id: Long) = occurrenceChanges.deleteById(id)

    private suspend fun validateClassOwnership(classId: Long, semesterId: Long) {
        require(semesters.findById(semesterId) != null) { "Semester does not exist" }
        require(classes.findById(classId)?.semesterId == semesterId) {
            "Class must belong to the record semester"
        }
    }
}
