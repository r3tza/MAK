package dev.retza.mak.data.repository

import androidx.room.withTransaction
import dev.retza.mak.data.database.AppDatabase
import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.entity.FreeDayEntity
import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.domain.SemesterSpan
import dev.retza.mak.domain.WeekOverrideScope
import dev.retza.mak.domain.fallbackActiveSemester
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface SemesterRepository {
    fun observeSemesters(): Flow<List<SemesterRecord>>

    fun observeActiveSemester(): Flow<SemesterRecord?>

    fun observeSemester(id: Long): Flow<SemesterRecord?>

    fun observeStudyPrograms(): Flow<List<StudyProgramRecord>>

    /** Names of the semesters the study program is assigned to; an assigned program cannot be deleted. */
    fun observeStudyProgramSemesters(id: Long): Flow<List<String>>

    fun observeSemesterPrograms(semesterId: Long): Flow<List<SemesterProgramRecord>>

    fun observeCalendars(semesterId: Long): Flow<List<AcademicCalendarRecord>>

    /** The shared calendar of each semester by semester id: its lowest-id calendar, as `ActivePlanData.sharedCalendar()`. */
    fun observeSharedCalendars(): Flow<Map<Long, AcademicCalendarRecord>>

    fun observeWeekOverrides(semesterId: Long): Flow<List<WeekOverrideRecord>>

    /** The semester with its programs, calendars and week overrides, or null when it does not exist. */
    fun observeSemesterConfiguration(id: Long): Flow<SemesterConfiguration?>

    suspend fun saveSemester(record: SemesterRecord): Long

    suspend fun setActiveSemester(id: Long)

    suspend fun deleteSemesterAndSelectFallback(id: Long): SemesterDeletionResult

    /** Activates the fallback semester when there is none and some semester exists; a no-op otherwise. */
    suspend fun ensureActiveSemester()

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

    suspend fun deleteCalendar(id: Long)

    suspend fun separateSemesterProgramCalendar(assignmentId: Long): SetupConfigurationIds

    suspend fun reconnectSemesterProgram(assignmentId: Long, calendarId: Long): SetupConfigurationIds

    suspend fun saveSemesterProgram(record: SemesterProgramRecord): Long

    suspend fun deleteSemesterProgram(id: Long)

    suspend fun countClassesForAssignment(assignmentId: Long): Int

    /**
     * Saves the wizard's semester, program, calendar and assignment in one transaction.
     * A new semester becomes active only when [activate] is true; an existing semester keeps
     * its current activity.
     */
    suspend fun saveSetupConfiguration(
        semester: SemesterRecord,
        studyProgram: StudyProgramRecord,
        calendar: AcademicCalendarRecord,
        activate: Boolean
    ): SetupConfigurationIds

    suspend fun saveWeekOverride(record: WeekOverrideRecord): Long

    suspend fun deleteWeekOverride(id: Long)

    suspend fun deleteWeekOverride(calendarId: Long, weekStartDate: LocalDate, scope: WeekOverrideScope)

    fun observeFreeDays(semesterId: Long): Flow<List<FreeDayRecord>>

    /** Replaces [replacedIds] with the range in each calendar of [calendarIds], in one transaction. */
    suspend fun saveFreeDayGroup(
        semesterId: Long,
        replacedIds: List<Long>,
        startDate: LocalDate,
        endDate: LocalDate,
        name: String?,
        calendarIds: List<Long>
    )

    suspend fun deleteFreeDays(ids: List<Long>)
}

@org.koin.core.annotation.Single(binds = [SemesterRepository::class])
class RoomSemesterRepository(
    private val database: AppDatabase,
    private val clock: Clock
) : SemesterRepository {
    private val semesters = database.semesterDao()
    private val studyPrograms = database.studyProgramDao()
    private val calendars = database.academicCalendarDao()
    private val semesterPrograms = database.semesterProgramDao()
    private val weekOverrides = database.weekOverrideDao()
    private val classes = database.classDao()
    private val freeDays = database.freeDayDao()

    override fun observeSemesters(): Flow<List<SemesterRecord>> =
        semesters.observeAll().map { list -> list.map { it.toRecord() } }

    override fun observeActiveSemester(): Flow<SemesterRecord?> =
        semesters.observeActive().map { it?.toRecord() }

    override fun observeSemester(id: Long): Flow<SemesterRecord?> =
        semesters.observeById(id).map { it?.toRecord() }

    override fun observeStudyPrograms(): Flow<List<StudyProgramRecord>> =
        studyPrograms.observeAll().map { list -> list.map { it.toRecord() } }

    override fun observeStudyProgramSemesters(id: Long): Flow<List<String>> =
        studyPrograms.observeSemesterNames(id)

    override fun observeSemesterPrograms(semesterId: Long): Flow<List<SemesterProgramRecord>> =
        semesterPrograms.observeForSemester(semesterId).map { list -> list.map { it.toRecord() } }

    override fun observeCalendars(semesterId: Long): Flow<List<AcademicCalendarRecord>> =
        calendars.observeForSemester(semesterId).map { list -> list.map { it.toRecord() } }

    override fun observeSharedCalendars(): Flow<Map<Long, AcademicCalendarRecord>> =
        calendars.observeAll().map { list ->
            list.map { it.toRecord() }
                .groupBy { it.semesterId }
                .mapValues { (_, records) -> records.minBy { it.id } }
        }

    override fun observeWeekOverrides(semesterId: Long): Flow<List<WeekOverrideRecord>> =
        weekOverrides.observeForSemester(semesterId).map { list -> list.map { it.toRecord() } }

    override fun observeSemesterConfiguration(id: Long): Flow<SemesterConfiguration?> =
        combine(semesters.observeWithData(id), studyPrograms.observeAll()) { data, programs ->
            data?.let {
                SemesterConfiguration(
                    semester = it.semester.toRecord(),
                    programs = it.semesterPrograms.map { program -> program.toRecord() },
                    studyPrograms = programs.map { program -> program.toRecord() },
                    calendars = it.academicCalendars.map { calendar -> calendar.toRecord() },
                    overrides = it.weekOverrides.map { week -> week.toRecord() },
                    freeDays = it.freeDays.map { day -> day.toRecord() }
                )
            }
        }
            // The query also covers classes and notes; their writes must not reset an unsaved semester form.
            .distinctUntilChanged()

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

    override suspend fun setActiveSemester(id: Long) {
        database.withTransaction {
            require(semesters.findById(id) != null) { "Semester does not exist" }
            semesters.clearActive()
            semesters.markActive(id)
        }
    }

    override suspend fun deleteSemesterAndSelectFallback(id: Long): SemesterDeletionResult =
        database.withTransaction {
            require(semesters.findById(id) != null) { "Semester does not exist" }
            semesters.deleteById(id)
            SemesterDeletionResult(activateFallbackIfNone())
        }

    override suspend fun ensureActiveSemester() {
        database.withTransaction { activateFallbackIfNone() }
    }

    /** Inside a transaction: when no semester is active, activates [fallbackActiveSemester] and returns its id. */
    private suspend fun activateFallbackIfNone(): Long? {
        val all = semesters.getAllWithData()
        all.firstOrNull { it.semester.isActive }?.let { return it.semester.id }
        val chosen = fallbackActiveSemester(
            all.map { data -> SemesterSpan(data.semester.id, data.academicCalendars.map { it.startDate..it.endDate }) },
            LocalDate.now(clock)
        ) ?: return null
        semesters.clearActive()
        semesters.markActive(chosen)
        return chosen
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
                // Like the other reassignments, a calendar left without programs is removed.
                val previousCalendarId = existing.academicCalendarId
                if (previousCalendarId != academicCalendarId) {
                    deleteCalendarIfOrphaned(previousCalendarId)
                }
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
            val calendarId = copyCalendarWithOverrides(source)
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
            val newCalendarId = copyCalendarWithOverrides(source)
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
        if (previousCalendarId != calendarId) {
            deleteCalendarIfOrphaned(previousCalendarId)
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
            if (existing.academicCalendarId != entity.academicCalendarId) {
                deleteCalendarIfOrphaned(existing.academicCalendarId)
            }
        }
        return entity.id
    }

    override suspend fun deleteSemesterProgram(id: Long) {
        database.withTransaction {
            val existing = semesterPrograms.findById(id) ?: return@withTransaction
            semesterPrograms.deleteById(id)
            deleteCalendarIfOrphaned(existing.academicCalendarId)
        }
    }

    override suspend fun countClassesForAssignment(assignmentId: Long): Int =
        classes.countForAssignment(assignmentId)

    override suspend fun saveSetupConfiguration(
        semester: SemesterRecord,
        studyProgram: StudyProgramRecord,
        calendar: AcademicCalendarRecord,
        activate: Boolean
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
            val semesterIsActive: Boolean
            val semesterId = if (semesterEntity.id == 0L) {
                semesterIsActive = activate
                if (activate) semesters.clearActive()
                semesters.insert(semesterEntity.copy(isActive = activate))
            } else {
                val existing = requireNotNull(semesters.findById(semesterEntity.id)) { "Semester does not exist" }
                semesterIsActive = existing.isActive
                semesters.update(semesterEntity.copy(isActive = existing.isActive))
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
                if (previousCalendarId != calendarId) {
                    deleteCalendarIfOrphaned(previousCalendarId)
                }
                existingAssignment.id
            }
            SetupConfigurationIds(
                semesterId,
                studyProgramId,
                calendarId,
                semesterProgramId,
                semesterIsActive
            )
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

    override suspend fun deleteWeekOverride(
        calendarId: Long,
        weekStartDate: LocalDate,
        scope: WeekOverrideScope
    ) = weekOverrides.deleteForWeek(
        calendarId,
        weekStartDate,
        dev.retza.mak.data.entity.WeekOverrideScope.valueOf(scope.name)
    )

    override fun observeFreeDays(semesterId: Long): Flow<List<FreeDayRecord>> =
        freeDays.observeForSemester(semesterId).map { list -> list.map { it.toRecord() } }

    override suspend fun saveFreeDayGroup(
        semesterId: Long,
        replacedIds: List<Long>,
        startDate: LocalDate,
        endDate: LocalDate,
        name: String?,
        calendarIds: List<Long>
    ) {
        database.withTransaction {
            require(semesters.findById(semesterId) != null) { "Semester does not exist" }
            require(calendarIds.isNotEmpty()) { "Free day group needs at least one calendar" }
            require(!endDate.isBefore(startDate)) { "Free day must not end before it starts" }
            calendarIds.forEach { id ->
                require(calendars.findById(id)?.semesterId == semesterId) {
                    "Calendar must belong to the semester"
                }
            }
            val existingIds = freeDays.getForSemester(semesterId).map { it.id }.toSet()
            require(replacedIds.all { it in existingIds }) { "Free day does not exist in the semester" }
            freeDays.deleteByIds(replacedIds)
            val trimmedName = name?.trim()?.takeIf { it.isNotEmpty() }
            calendarIds.forEach { calendarId ->
                freeDays.insert(
                    FreeDayEntity(
                        semesterId = semesterId,
                        academicCalendarId = calendarId,
                        startDate = startDate,
                        endDate = endDate,
                        name = trimmedName
                    )
                )
            }
        }
    }

    override suspend fun deleteFreeDays(ids: List<Long>) = freeDays.deleteByIds(ids)

    /**
     * A new calendar with the dates, rhythm, week overrides and free days of [source]
     * (`ARCHITECTURE.md`, section 7).
     */
    private suspend fun copyCalendarWithOverrides(source: AcademicCalendarEntity): Long {
        val calendarId = calendars.insert(
            AcademicCalendarEntity(
                semesterId = source.semesterId,
                startDate = source.startDate,
                endDate = source.endDate,
                firstWeekType = source.firstWeekType
            )
        )
        weekOverrides.getForCalendar(source.id).forEach { override ->
            weekOverrides.insert(override.copy(id = 0, academicCalendarId = calendarId))
        }
        freeDays.getForCalendar(source.id).forEach { day ->
            freeDays.insert(day.copy(id = 0, academicCalendarId = calendarId))
        }
        return calendarId
    }

    /** Removes a calendar no study program uses any more, with its week overrides. */
    private suspend fun deleteCalendarIfOrphaned(calendarId: Long) {
        if (calendars.countAssignments(calendarId) == 0) calendars.deleteById(calendarId)
    }
}
