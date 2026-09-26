package dev.retza.mak.data.repository

import androidx.room.withTransaction
import dev.retza.mak.data.database.AppDatabase
import dev.retza.mak.data.entity.SemesterProgramEntity
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SemesterRepository {
    fun observeSemesters(): Flow<List<SemesterRecord>>

    fun observeActiveSemester(): Flow<SemesterRecord?>

    fun observeSemester(id: Long): Flow<SemesterRecord?>

    fun observeStudyPrograms(): Flow<List<StudyProgramRecord>>

    fun observeSemesterPrograms(semesterId: Long): Flow<List<SemesterProgramRecord>>

    fun observeCalendars(semesterId: Long): Flow<List<AcademicCalendarRecord>>

    fun observeWeekOverrides(semesterId: Long): Flow<List<WeekOverrideRecord>>

    suspend fun saveSemester(record: SemesterRecord): Long

    suspend fun updateSemester(record: SemesterRecord): Long

    suspend fun setActiveSemester(id: Long)

    suspend fun clearActiveSemester()

    suspend fun deleteSemester(id: Long)

    suspend fun deleteSemesterAndSelectFallback(id: Long): SemesterDeletionResult

    suspend fun saveStudyProgram(record: StudyProgramRecord): Long

    suspend fun deleteStudyProgram(id: Long)

    suspend fun saveStudyProgramAssignment(
        semesterId: Long,
        studyProgram: StudyProgramRecord,
        academicCalendarId: Long
    ): SetupConfigurationIds

    suspend fun addSeparatedSemesterProgram(
        semesterId: Long,
        studyProgram: StudyProgramRecord,
        sourceCalendarId: Long
    ): SetupConfigurationIds

    suspend fun saveCalendar(record: AcademicCalendarRecord): Long

    suspend fun updateSemesterWithCalendar(semester: SemesterRecord, calendar: AcademicCalendarRecord)

    suspend fun deleteCalendarIfUnused(id: Long)

    suspend fun deleteCalendar(id: Long)

    suspend fun separateSemesterProgramCalendar(assignmentId: Long): SetupConfigurationIds

    suspend fun reconnectSemesterProgram(assignmentId: Long, calendarId: Long): SetupConfigurationIds

    suspend fun saveSemesterProgram(record: SemesterProgramRecord): Long

    suspend fun deleteSemesterProgram(id: Long)

    suspend fun countClassesForAssignment(assignmentId: Long): Int

    suspend fun saveSetupConfiguration(
        semester: SemesterRecord,
        studyProgram: StudyProgramRecord,
        calendar: AcademicCalendarRecord
    ): SetupConfigurationIds

    suspend fun saveWeekOverride(record: WeekOverrideRecord): Long

    suspend fun deleteWeekOverride(id: Long)
}

@org.koin.core.annotation.Single(binds = [SemesterRepository::class])
class RoomSemesterRepository(
    private val database: AppDatabase
) : SemesterRepository {
    private val semesters = database.semesterDao()
    private val studyPrograms = database.studyProgramDao()
    private val calendars = database.academicCalendarDao()
    private val semesterPrograms = database.semesterProgramDao()
    private val weekOverrides = database.weekOverrideDao()
    private val classes = database.classDao()

    override fun observeSemesters(): Flow<List<SemesterRecord>> =
        semesters.observeAll().map { list -> list.map { it.toRecord() } }

    override fun observeActiveSemester(): Flow<SemesterRecord?> =
        semesters.observeActive().map { it?.toRecord() }

    override fun observeSemester(id: Long): Flow<SemesterRecord?> =
        semesters.observeById(id).map { it?.toRecord() }

    override fun observeStudyPrograms(): Flow<List<StudyProgramRecord>> =
        studyPrograms.observeAll().map { list -> list.map { it.toRecord() } }

    override fun observeSemesterPrograms(semesterId: Long): Flow<List<SemesterProgramRecord>> =
        semesterPrograms.observeForSemester(semesterId).map { list -> list.map { it.toRecord() } }

    override fun observeCalendars(semesterId: Long): Flow<List<AcademicCalendarRecord>> =
        calendars.observeForSemester(semesterId).map { list -> list.map { it.toRecord() } }

    override fun observeWeekOverrides(semesterId: Long): Flow<List<WeekOverrideRecord>> =
        weekOverrides.observeForSemester(semesterId).map { list -> list.map { it.toRecord() } }

    override suspend fun saveSemester(record: SemesterRecord): Long {
        val entity = record.toEntity()
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

    override suspend fun updateSemester(record: SemesterRecord): Long {
        val entity = record.toEntity()
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

    override suspend fun saveStudyProgram(record: StudyProgramRecord): Long {
        val entity = record.toEntity()
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

    override suspend fun saveStudyProgramAssignment(
        semesterId: Long,
        studyProgram: StudyProgramRecord,
        academicCalendarId: Long
    ): SetupConfigurationIds {
        val programEntity = studyProgram.toEntity()
        require(programEntity.name.isNotBlank()) { "Study program name cannot be blank" }
        return database.withTransaction {
            require(semesters.findById(semesterId) != null) { "Semester does not exist" }
            require(calendars.findById(academicCalendarId)?.semesterId == semesterId) {
                "Calendar must belong to the semester"
            }
            val studyProgramId = if (programEntity.id == 0L) {
                studyPrograms.insert(programEntity)
            } else {
                require(studyPrograms.findById(programEntity.id) != null) {
                    "Study program does not exist"
                }
                programEntity.id
            }
            val existing = semesterPrograms.findByProgram(semesterId, studyProgramId)
            val semesterProgramId = if (existing == null) {
                semesterPrograms.insert(
                    SemesterProgramEntity(
                        semesterId = semesterId,
                        studyProgramId = studyProgramId,
                        academicCalendarId = academicCalendarId
                    )
                )
            } else {
                semesterPrograms.update(existing.copy(academicCalendarId = academicCalendarId))
                existing.id
            }
            SetupConfigurationIds(semesterId, studyProgramId, academicCalendarId, semesterProgramId)
        }
    }

    override suspend fun addSeparatedSemesterProgram(
        semesterId: Long,
        studyProgram: StudyProgramRecord,
        sourceCalendarId: Long
    ): SetupConfigurationIds {
        val programEntity = studyProgram.toEntity()
        require(programEntity.name.isNotBlank()) { "Study program name cannot be blank" }
        return database.withTransaction {
            require(semesters.findById(semesterId) != null) { "Semester does not exist" }
            val source = calendars.findById(sourceCalendarId) ?: error("Calendar does not exist")
            require(source.semesterId == semesterId) { "Calendar must belong to the semester" }
            val studyProgramId = if (programEntity.id == 0L) {
                studyPrograms.insert(programEntity)
            } else {
                require(studyPrograms.findById(programEntity.id) != null) {
                    "Study program does not exist"
                }
                programEntity.id
            }
            val calendarId = calendars.insert(
                dev.retza.mak.data.entity.AcademicCalendarEntity(
                    semesterId = semesterId,
                    startDate = source.startDate,
                    endDate = source.endDate,
                    firstWeekType = source.firstWeekType
                )
            )
            weekOverrides.getForCalendar(source.id).forEach { override ->
                weekOverrides.insert(override.copy(id = 0, academicCalendarId = calendarId))
            }
            val semesterProgramId = semesterPrograms.insert(
                SemesterProgramEntity(
                    semesterId = semesterId,
                    studyProgramId = studyProgramId,
                    academicCalendarId = calendarId
                )
            )
            SetupConfigurationIds(semesterId, studyProgramId, calendarId, semesterProgramId)
        }
    }

    override suspend fun saveCalendar(record: AcademicCalendarRecord): Long {
        val entity = record.toEntity()
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
        semester: SemesterRecord,
        calendar: AcademicCalendarRecord
    ) {
        val semesterEntity = semester.toEntity()
        val calendarEntity = calendar.toEntity()
        require(semesterEntity.name.isNotBlank()) { "Semester name cannot be blank" }
        require(calendarEntity.id != 0L) { "Calendar must exist" }
        require(!calendarEntity.endDate.isBefore(calendarEntity.startDate)) {
            "Calendar end must not be before its start"
        }
        database.withTransaction {
            val existingSemester = semesters.findById(semesterEntity.id) ?: error("Semester does not exist")
            semesters.update(semesterEntity.copy(isActive = existingSemester.isActive))
            val existingCalendar = calendars.findById(calendarEntity.id) ?: error("Calendar does not exist")
            require(existingCalendar.semesterId == semesterEntity.id) {
                "Calendar must belong to the semester"
            }
            calendars.update(calendarEntity)
        }
    }

    override suspend fun deleteCalendarIfUnused(id: Long) {
        database.withTransaction {
            if (calendars.countAssignments(id) == 0) {
                calendars.deleteById(id)
            }
        }
    }

    override suspend fun deleteCalendar(id: Long) {
        database.withTransaction {
            require(calendars.findById(id) != null) { "Calendar does not exist" }
            require(calendars.countAssignments(id) == 0) {
                "Calendar is assigned to a study program"
            }
            calendars.deleteById(id)
        }
    }

    override suspend fun separateSemesterProgramCalendar(assignmentId: Long): SetupConfigurationIds =
        database.withTransaction {
            val assignment = semesterPrograms.findById(assignmentId)
                ?: error("Assignment does not exist")
            val source = calendars.findById(assignment.academicCalendarId)
                ?: error("Calendar does not exist")
            val newCalendarId = calendars.insert(
                dev.retza.mak.data.entity.AcademicCalendarEntity(
                    semesterId = source.semesterId,
                    startDate = source.startDate,
                    endDate = source.endDate,
                    firstWeekType = source.firstWeekType
                )
            )
            weekOverrides.getForCalendar(source.id).forEach { override ->
                weekOverrides.insert(override.copy(id = 0, academicCalendarId = newCalendarId))
            }
            semesterPrograms.update(assignment.copy(academicCalendarId = newCalendarId))
            SetupConfigurationIds(
                semesterId = assignment.semesterId,
                studyProgramId = assignment.studyProgramId,
                academicCalendarId = newCalendarId,
                semesterProgramId = assignment.id
            )
        }

    override suspend fun reconnectSemesterProgram(
        assignmentId: Long,
        calendarId: Long
    ): SetupConfigurationIds = database.withTransaction {
        val assignment = semesterPrograms.findById(assignmentId)
            ?: error("Assignment does not exist")
        val target = calendars.findById(calendarId) ?: error("Calendar does not exist")
        require(target.semesterId == assignment.semesterId) {
            "Calendar must belong to the semester"
        }
        val previousCalendarId = assignment.academicCalendarId
        semesterPrograms.update(assignment.copy(academicCalendarId = calendarId))
        if (previousCalendarId != calendarId && calendars.countAssignments(previousCalendarId) == 0) {
            calendars.deleteById(previousCalendarId)
        }
        SetupConfigurationIds(
            semesterId = assignment.semesterId,
            studyProgramId = assignment.studyProgramId,
            academicCalendarId = calendarId,
            semesterProgramId = assignment.id
        )
    }

    override suspend fun saveSemesterProgram(record: SemesterProgramRecord): Long {
        val entity = record.toEntity()
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

    override suspend fun countClassesForAssignment(assignmentId: Long): Int =
        classes.countForAssignment(assignmentId)

    override suspend fun saveSetupConfiguration(
        semester: SemesterRecord,
        studyProgram: StudyProgramRecord,
        calendar: AcademicCalendarRecord
    ): SetupConfigurationIds {
        val semesterEntity = semester.toEntity()
        val programEntity = studyProgram.toEntity()
        val calendarEntity = calendar.toEntity()
        require(semesterEntity.name.isNotBlank()) { "Semester name cannot be blank" }
        require(programEntity.name.isNotBlank()) { "Study program name cannot be blank" }
        require(!calendarEntity.endDate.isBefore(calendarEntity.startDate)) {
            "Calendar end must not be before its start"
        }
        return database.withTransaction {
            val semesterId = if (semesterEntity.id == 0L) {
                semesters.clearActive()
                semesters.insert(semesterEntity.copy(isActive = true))
            } else {
                require(semesters.findById(semesterEntity.id) != null) { "Semester does not exist" }
                semesters.clearActive()
                semesters.update(semesterEntity.copy(isActive = true))
                semesterEntity.id
            }
            val studyProgramId = if (programEntity.id == 0L) {
                studyPrograms.insert(programEntity)
            } else {
                require(studyPrograms.findById(programEntity.id) != null) {
                    "Study program does not exist"
                }
                studyPrograms.update(programEntity)
                programEntity.id
            }
            val calendarId = if (calendarEntity.id == 0L) {
                calendars.insert(calendarEntity.copy(semesterId = semesterId))
            } else {
                val existing = calendars.findById(calendarEntity.id)
                require(existing != null && existing.semesterId == semesterId) {
                    "Calendar does not belong to the semester"
                }
                calendars.update(calendarEntity.copy(semesterId = semesterId))
                calendarEntity.id
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

    override suspend fun saveWeekOverride(record: WeekOverrideRecord): Long {
        val entity = record.toEntity()
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
}
