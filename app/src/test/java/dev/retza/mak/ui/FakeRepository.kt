package dev.retza.mak.ui

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
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.data.repository.PlanBackupGateway
import dev.retza.mak.data.repository.SemesterBackup
import dev.retza.mak.data.repository.ClassRecord
import dev.retza.mak.data.repository.OccurrenceChangeRecord
import dev.retza.mak.data.repository.OccurrenceNoteRecord
import dev.retza.mak.data.repository.ScheduleRepository
import dev.retza.mak.data.repository.SetupConfigurationIds
import dev.retza.mak.data.repository.toActivePlanData
import dev.retza.mak.data.repository.toEntity
import dev.retza.mak.data.repository.toRecord
import dev.retza.mak.domain.ActivePlanData
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
internal class FakeRepository : ScheduleRepository, PlanBackupGateway {
    val semester = SemesterEntity(id = 1L, name = "Semestr", isActive = true)
    val secondSemester = SemesterEntity(id = 2L, name = "Semestr drugi", isActive = false)
    val calendar = AcademicCalendarEntity(
        id = 1L,
        semesterId = 1L,
        startDate = LocalDate.of(2026, 9, 1),
        endDate = LocalDate.of(2026, 10, 31),
        firstWeekType = WeekType.A
    )
    val secondCalendar = AcademicCalendarEntity(
        id = 2L,
        semesterId = 2L,
        startDate = LocalDate.of(2027, 2, 1),
        endDate = LocalDate.of(2027, 6, 30),
        firstWeekType = WeekType.B
    )

    private val calendarState = MutableStateFlow(listOf(calendar, secondCalendar))
    private val studyProgramState = MutableStateFlow(
        listOf(StudyProgramEntity(id = 1L, name = "Informatyka", color = "#137B71"))
    )
    private val semesterProgramState = MutableStateFlow(
        listOf(SemesterProgramEntity(id = 1L, semesterId = 1L, studyProgramId = 1L, academicCalendarId = 1L))
    )

    val calendars: List<AcademicCalendarEntity> get() = calendarState.value
    val studyPrograms: List<StudyProgramEntity> get() = studyProgramState.value
    val semesterPrograms: List<SemesterProgramEntity> get() = semesterProgramState.value

    val classes = mutableListOf(
        ClassEntity(
            id = 1L,
            semesterId = 1L,
            semesterProgramId = 1L,
            name = "Programowanie",
            type = "Wykład",
            teacherName = null,
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 30),
            room = "L204",
            building = null,
            group = null,
            recurrence = Recurrence.EVERY_WEEK,
            date = null,
            classNote = "Wspólna"
        )
    )
    val occurrenceNotes = mutableListOf<OccurrenceNoteEntity>()
    val occurrenceChanges = mutableListOf<OccurrenceChangeEntity>()
    val weekOverrides = mutableListOf<WeekOverrideEntity>()

    val events = mutableListOf<String>()
    var saveGate: CompletableDeferred<Unit>? = null
    var occurrenceDataGate: CompletableDeferred<Unit>? = null
    var failSaves = false
    var failSetupConfiguration = false
    var setActiveCount = 0
    var lastSetupSemester: SemesterEntity? = null
    var lastSetupActivate: Boolean? = null
    var lastSetupStudyProgram: StudyProgramEntity? = null
    var lastSetupCalendar: AcademicCalendarEntity? = null
    var lastSetupConfiguration: SetupConfigurationIds? = null
    private var generatedSemesterId = 100L
    private var generatedStudyProgramId = 100L
    private var generatedCalendarId = 100L
    private var generatedProgramId = 100L

    private val semesterFlow = MutableStateFlow(listOf(semester, secondSemester))
    private val activeSemesterFlow = MutableStateFlow(1L)
    val activeSemesterId: Long get() = activeSemesterFlow.value

    fun clearSemesters() {
        semesterFlow.value = emptyList()
        activeSemesterFlow.value = 0L
    }

    private fun semesterById(id: Long): SemesterEntity? =
        semesterFlow.value.firstOrNull { it.id == id }

    suspend fun awaitDataGate() {
        occurrenceDataGate?.await()
    }

    fun observeSemesters(): Flow<List<SemesterEntity>> = semesterFlow
    fun observeActiveSemester(): Flow<SemesterEntity?> =
        combine(semesterFlow, activeSemesterFlow) { list, id -> list.firstOrNull { it.id == id } }
    fun observeSemester(id: Long): Flow<SemesterEntity?> = flowOf(semesterById(id))
    private val dataVersion = MutableStateFlow(0)

    /** Emits the semester data again, as Room does after a write; direct list edits do not notify by themselves. */
    fun notifyDataChanged() {
        dataVersion.value += 1
    }

    fun observeSemesterData(id: Long): Flow<SemesterWithData?> = dataVersion.flatMapLatest { semesterDataOnce(id) }

    private fun semesterDataOnce(id: Long): Flow<SemesterWithData?> = flow {
        occurrenceDataGate?.await()
        val target = semesterFlow.value.firstOrNull { it.id == id }
        if (target == null) {
            emit(null)
            return@flow
        }
        val assignments = semesterPrograms.filter { it.semesterId == id }
        emit(
            SemesterWithData(
                semester = target,
                semesterPrograms = assignments,
                academicCalendars = calendars.filter { it.semesterId == id },
                studyPrograms = studyPrograms.filter { program ->
                    assignments.any { it.studyProgramId == program.id }
                },
                classes = classes.filter { it.semesterId == id },
                weekOverrides = weekOverrides.filter { it.semesterId == id },
                occurrenceNotes = occurrenceNotes,
                occurrenceChanges = occurrenceChanges
            )
        )
    }

    override fun observeActivePlanData(semesterId: Long): Flow<ActivePlanData?> =
        observeSemesterData(semesterId).map { it?.toActivePlanData() }

    fun observeStudyPrograms(): Flow<List<StudyProgramEntity>> = studyProgramState
    fun observeSemesterPrograms(semesterId: Long): Flow<List<SemesterProgramEntity>> =
        flowOf(semesterPrograms.filter { it.semesterId == semesterId })
    fun observeCalendars(semesterId: Long): Flow<List<AcademicCalendarEntity>> =
        flowOf(calendars.filter { it.semesterId == semesterId })
    fun observeWeekOverrides(semesterId: Long): Flow<List<WeekOverrideEntity>> =
        flowOf(weekOverrides.filter { it.semesterId == semesterId })
    override fun observeClasses(semesterId: Long): Flow<List<ClassRecord>> =
        flowOf(classes.map { it.toRecord() })
    override fun observeOccurrenceNotes(semesterId: Long): Flow<List<OccurrenceNoteRecord>> =
        flowOf(occurrenceNotes.map { it.toRecord() })
    override fun observeOccurrenceChanges(semesterId: Long): Flow<List<OccurrenceChangeRecord>> =
        flowOf(occurrenceChanges.map { it.toRecord() })
    override fun observeOccurrenceNotesForClass(classId: Long): Flow<List<OccurrenceNoteRecord>> =
        flowOf(occurrenceNotes.filter { it.classId == classId }.map { it.toRecord() })
    override fun observeOccurrenceChangesForClass(classId: Long): Flow<List<OccurrenceChangeRecord>> =
        flowOf(occurrenceChanges.filter { it.classId == classId }.map { it.toRecord() })
    override suspend fun snapshot(): BackupData {
        return BackupData(
            studyPrograms = studyPrograms,
            semesters = semesterFlow.value.map { semester ->
                SemesterBackup(
                    semester = semester,
                    calendars = calendars.filter { it.semesterId == semester.id },
                    programs = semesterPrograms.filter { it.semesterId == semester.id },
                    classes = classes.filter { it.semesterId == semester.id },
                    weekOverrides = weekOverrides.filter { it.semesterId == semester.id },
                    occurrenceNotes = occurrenceNotes,
                    occurrenceChanges = occurrenceChanges
                )
            }
        )
    }

    suspend fun saveSemester(entity: SemesterEntity): Long {
        awaitSave()
        events += "saveSemester"
        if (entity.isActive) activeSemesterFlow.value = entity.id
        return entity.id
    }

    suspend fun updateSemester(entity: SemesterEntity): Long {
        awaitSave()
        events += "updateSemester"
        return entity.id
    }

    suspend fun setActiveSemester(id: Long) {
        setActiveCount += 1
        if (semesterById(id) != null) activeSemesterFlow.value = id
    }

    suspend fun clearActiveSemester() {
        activeSemesterFlow.value = 0L
    }

    suspend fun deleteSemester(id: Long) {
        awaitSave()
        events += "deleteSemester"
        semesterFlow.value = semesterFlow.value.filterNot { it.id == id }
    }

    suspend fun deleteSemesterAndSelectFallback(
        id: Long
    ): dev.retza.mak.data.repository.SemesterDeletionResult {
        awaitSave()
        events += "deleteSemesterAndSelectFallback"
        val target = semesterById(id) ?: error("Semester does not exist")
        val wasActive = activeSemesterFlow.value == id
        semesterFlow.value = semesterFlow.value.filterNot { it.id == id }
        val remaining = semesterFlow.value
        if (remaining.isEmpty()) {
            activeSemesterFlow.value = 0L
            return dev.retza.mak.data.repository.SemesterDeletionResult(null)
        }
        return if (wasActive) {
            val chosen = remaining.first()
            activeSemesterFlow.value = chosen.id
            dev.retza.mak.data.repository.SemesterDeletionResult(chosen.id)
        } else {
            dev.retza.mak.data.repository.SemesterDeletionResult(activeSemesterFlow.value)
        }
    }

    suspend fun saveStudyProgram(entity: StudyProgramEntity): Long {
        awaitSave()
        events += "saveStudyProgram"
        val id = if (entity.id == 0L) generatedStudyProgramId++ else entity.id
        val saved = entity.copy(id = id)
        val index = studyPrograms.indexOfFirst { it.id == id }
        studyProgramState.value = if (index >= 0) {
            studyPrograms.toMutableList().also { it[index] = saved }
        } else {
            studyPrograms + saved
        }
        return id
    }

    suspend fun deleteStudyProgram(id: Long) {
        awaitSave()
        events += "deleteStudyProgram"
        require(semesterPrograms.none { it.studyProgramId == id }) {
            "Study program is assigned to a semester"
        }
        studyProgramState.value = studyPrograms.filterNot { it.id == id }
    }

    suspend fun saveStudyProgramAssignment(
        semesterId: Long,
        studyProgram: StudyProgramEntity,
        academicCalendarId: Long
    ): SetupConfigurationIds {
        awaitSave()
        events += "saveStudyProgramAssignment"
        val studyProgramId = if (studyProgram.id == 0L) {
            val id = generatedStudyProgramId++
            studyProgramState.value = studyPrograms + studyProgram.copy(id = id)
            id
        } else {
            studyProgram.id
        }
        val existing = semesterPrograms.firstOrNull {
            it.semesterId == semesterId && it.studyProgramId == studyProgramId
        }
        val semesterProgramId = if (existing == null) {
            val id = generatedProgramId++
            semesterProgramState.value = semesterPrograms + SemesterProgramEntity(
                id = id,
                semesterId = semesterId,
                studyProgramId = studyProgramId,
                academicCalendarId = academicCalendarId
            )
            id
        } else {
            existing.id
        }
        return SetupConfigurationIds(semesterId, studyProgramId, academicCalendarId, semesterProgramId)
    }

    suspend fun addSeparatedSemesterProgram(
        semesterId: Long,
        studyProgram: StudyProgramEntity,
        sourceCalendarId: Long
    ): SetupConfigurationIds {
        awaitSave()
        events += "addSeparatedSemesterProgram"
        val source = calendars.firstOrNull { it.id == sourceCalendarId }
            ?: error("Calendar does not exist")
        val studyProgramId = if (studyProgram.id == 0L) {
            val id = generatedStudyProgramId++
            studyProgramState.value = studyPrograms + studyProgram.copy(id = id)
            id
        } else {
            studyProgram.id
        }
        val calendarId = generatedCalendarId++
        calendarState.value = calendars + source.copy(id = calendarId)
        weekOverrides
            .filter { it.academicCalendarId == sourceCalendarId }
            .forEach { override ->
                weekOverrides += override.copy(
                    id = (weekOverrides.maxOfOrNull { it.id } ?: 0L) + 1L,
                    academicCalendarId = calendarId
                )
            }
        val semesterProgramId = generatedProgramId++
        semesterProgramState.value = semesterPrograms + SemesterProgramEntity(
            id = semesterProgramId,
            semesterId = semesterId,
            studyProgramId = studyProgramId,
            academicCalendarId = calendarId
        )
        return SetupConfigurationIds(semesterId, studyProgramId, calendarId, semesterProgramId)
    }


    suspend fun saveCalendar(entity: AcademicCalendarEntity): Long {
        awaitSave()
        events += "saveCalendar"
        val id = if (entity.id == 0L) generatedCalendarId++ else entity.id
        val saved = entity.copy(id = id)
        val index = calendars.indexOfFirst { it.id == id }
        calendarState.value = if (index >= 0) {
            calendars.toMutableList().also { it[index] = saved }
        } else {
            calendars + saved
        }
        return id
    }

    suspend fun updateSemesterWithCalendar(
        semester: SemesterEntity,
        calendar: AcademicCalendarEntity
    ) {
        awaitSave()
        events += "updateSemesterWithCalendar"
        semesterFlow.value = semesterFlow.value.map { if (it.id == semester.id) semester else it }
        calendarState.value = calendars.map { if (it.id == calendar.id) calendar else it }
    }

    suspend fun deleteCalendarIfUnused(id: Long) {
        awaitSave()
        events += "deleteCalendarIfUnused"
        if (semesterPrograms.none { it.academicCalendarId == id }) {
            calendarState.value = calendars.filterNot { it.id == id }
        }
    }

    suspend fun saveSemesterProgram(entity: SemesterProgramEntity): Long {
        awaitSave()
        events += "saveSemesterProgram"
        val id = if (entity.id == 0L) generatedProgramId++ else entity.id
        val saved = entity.copy(id = id)
        val index = semesterPrograms.indexOfFirst { it.id == id }
        semesterProgramState.value = if (index >= 0) {
            semesterPrograms.toMutableList().also { it[index] = saved }
        } else {
            semesterPrograms + saved
        }
        return id
    }

    suspend fun deleteSemesterProgram(id: Long) {
        awaitSave()
        events += "deleteSemesterProgram"
        val existing = semesterPrograms.firstOrNull { it.id == id } ?: return
        semesterProgramState.value = semesterPrograms.filterNot { it.id == id }
        if (semesterPrograms.none { it.academicCalendarId == existing.academicCalendarId }) {
            calendarState.value = calendars.filterNot { it.id == existing.academicCalendarId }
        }
    }

    suspend fun deleteCalendar(id: Long) {
        awaitSave()
        events += "deleteCalendar"
        require(semesterPrograms.none { it.academicCalendarId == id }) {
            "Calendar is assigned to a study program"
        }
        calendarState.value = calendars.filterNot { it.id == id }
        weekOverrides.removeAll { it.academicCalendarId == id }
    }

    suspend fun separateSemesterProgramCalendar(assignmentId: Long): SetupConfigurationIds {
        awaitSave()
        events += "separateSemesterProgramCalendar"
        val assignment = semesterPrograms.firstOrNull { it.id == assignmentId }
            ?: error("Assignment does not exist")
        val source = calendars.firstOrNull { it.id == assignment.academicCalendarId }
            ?: error("Calendar does not exist")
        val newCalendarId = generatedCalendarId++
        calendarState.value = calendars + source.copy(id = newCalendarId)
        weekOverrides
            .filter { it.academicCalendarId == source.id }
            .forEach { override ->
                weekOverrides += override.copy(
                    id = (weekOverrides.maxOfOrNull { it.id } ?: 0L) + 1L,
                    academicCalendarId = newCalendarId
                )
            }
        semesterProgramState.value = semesterPrograms.map {
            if (it.id == assignment.id) it.copy(academicCalendarId = newCalendarId) else it
        }
        return SetupConfigurationIds(
            semesterId = assignment.semesterId,
            studyProgramId = assignment.studyProgramId,
            academicCalendarId = newCalendarId,
            semesterProgramId = assignment.id
        )
    }

    suspend fun reconnectSemesterProgram(
        assignmentId: Long,
        calendarId: Long
    ): SetupConfigurationIds {
        awaitSave()
        events += "reconnectSemesterProgram"
        val assignment = semesterPrograms.firstOrNull { it.id == assignmentId }
            ?: error("Assignment does not exist")
        val previousCalendarId = assignment.academicCalendarId
        semesterProgramState.value = semesterPrograms.map {
            if (it.id == assignment.id) it.copy(academicCalendarId = calendarId) else it
        }
        if (previousCalendarId != calendarId &&
            semesterPrograms.none { it.academicCalendarId == previousCalendarId }
        ) {
            calendarState.value = calendars.filterNot { it.id == previousCalendarId }
            weekOverrides.removeAll { it.academicCalendarId == previousCalendarId }
        }
        return SetupConfigurationIds(
            semesterId = assignment.semesterId,
            studyProgramId = assignment.studyProgramId,
            academicCalendarId = calendarId,
            semesterProgramId = assignment.id
        )
    }

    suspend fun saveSetupConfiguration(
        semester: SemesterEntity,
        studyProgram: StudyProgramEntity,
        calendar: AcademicCalendarEntity,
        activate: Boolean = true
    ): SetupConfigurationIds {
        awaitSave()
        events += "saveSetupConfiguration"
        lastSetupActivate = activate
        if (failSetupConfiguration) throw IllegalStateException("setup configuration failed")
        val semesterId = if (semester.id == 0L) generatedSemesterId++ else semester.id
        val studyProgramId = if (studyProgram.id == 0L) generatedStudyProgramId++ else studyProgram.id
        val calendarId = if (calendar.id == 0L) generatedCalendarId++ else calendar.id
        val semesterIsActive = if (semester.id == 0L) activate else activeSemesterFlow.value == semester.id
        lastSetupSemester = semester.copy(id = semesterId, isActive = semesterIsActive)
        lastSetupStudyProgram = studyProgram.copy(id = studyProgramId)
        lastSetupCalendar = calendar.copy(id = calendarId, semesterId = semesterId)
        if (calendars.none { it.id == calendarId }) {
            calendarState.value = calendars + calendar.copy(id = calendarId, semesterId = semesterId)
        }
        if (semesterIsActive) activeSemesterFlow.value = semesterId
        val existing = semesterPrograms.firstOrNull {
            it.semesterId == semesterId && it.studyProgramId == studyProgramId
        }
        val programId = if (existing == null) {
            val id = generatedProgramId++
            semesterProgramState.value = semesterPrograms + SemesterProgramEntity(
                id = id,
                semesterId = semesterId,
                studyProgramId = studyProgramId,
                academicCalendarId = calendarId
            )
            id
        } else {
            semesterProgramState.value = semesterPrograms.map {
                if (it.id == existing.id) it.copy(academicCalendarId = calendarId) else it
            }
            existing.id
        }
        val ids = SetupConfigurationIds(semesterId, studyProgramId, calendarId, programId, semesterIsActive)
        lastSetupConfiguration = ids
        return ids
    }

    override suspend fun saveClass(record: ClassRecord): Long = saveClass(record.toEntity())

    suspend fun saveClass(entity: ClassEntity): Long {
        awaitSave()
        events += "saveClass"
        val index = classes.indexOfFirst { it.id == entity.id }
        if (index >= 0) classes[index] = entity else classes += entity.copy(id = classes.size + 1L)
        return entity.id
    }

    override suspend fun deleteClass(id: Long) {
        awaitSave()
        events += "deleteClass"
        classes.removeAll { it.id == id }
    }

    suspend fun saveWeekOverride(entity: WeekOverrideEntity): Long {
        awaitSave()
        events += "saveWeekOverride"
        if (entity.id == 0L) {
            weekOverrides += entity.copy(id = (weekOverrides.size + 1).toLong())
        } else {
            val index = weekOverrides.indexOfFirst { it.id == entity.id }
            if (index >= 0) weekOverrides[index] = entity else weekOverrides += entity
        }
        return entity.id
    }

    suspend fun deleteWeekOverride(id: Long) {
        awaitSave()
        events += "deleteWeekOverride"
        weekOverrides.removeAll { it.id == id }
    }

    override suspend fun saveOccurrenceNote(record: OccurrenceNoteRecord): Long =
        saveOccurrenceNote(record.toEntity())

    suspend fun saveOccurrenceNote(entity: OccurrenceNoteEntity): Long {
        awaitSave()
        events += "saveOccurrenceNote"
        val index = occurrenceNotes.indexOfFirst {
            it.classId == entity.classId && it.occurrenceDate == entity.occurrenceDate
        }
        if (index >= 0) {
            occurrenceNotes[index] = entity.copy(id = occurrenceNotes[index].id)
        } else {
            occurrenceNotes += entity.copy(id = (occurrenceNotes.size + 1).toLong())
        }
        return entity.id
    }

    override suspend fun deleteOccurrenceNote(id: Long) {
        awaitSave()
        events += "deleteOccurrenceNote"
        occurrenceNotes.removeAll { it.id == id }
    }

    override suspend fun saveOccurrenceChange(record: OccurrenceChangeRecord): Long =
        saveOccurrenceChange(record.toEntity())

    suspend fun saveOccurrenceChange(entity: OccurrenceChangeEntity): Long {
        awaitSave()
        events += "saveOccurrenceChange"
        val index = occurrenceChanges.indexOfFirst {
            it.classId == entity.classId && it.originalDate == entity.originalDate
        }
        if (index >= 0) {
            occurrenceChanges[index] = entity.copy(id = occurrenceChanges[index].id)
        } else {
            occurrenceChanges += entity.copy(id = (occurrenceChanges.size + 1).toLong())
        }
        return entity.id
    }

    override suspend fun deleteOccurrenceChange(id: Long) {
        awaitSave()
        events += "deleteOccurrenceChange"
        occurrenceChanges.removeAll { it.id == id }
    }

    override suspend fun replaceAll(data: BackupData): Long? {
        awaitSave()
        events += "replaceAllData"
        semesterFlow.value = data.semesters.map { it.semester }
        activeSemesterFlow.value = data.activeSemesterId ?: 0L
        studyProgramState.value = data.studyPrograms
        calendarState.value = data.semesters.flatMap { it.calendars }
        semesterProgramState.value = data.semesters.flatMap { it.programs }
        classes.clear()
        classes += data.semesters.flatMap { it.classes }
        weekOverrides.clear()
        weekOverrides += data.semesters.flatMap { it.weekOverrides }
        occurrenceNotes.clear()
        occurrenceNotes += data.semesters.flatMap { it.occurrenceNotes }
        occurrenceChanges.clear()
        occurrenceChanges += data.semesters.flatMap { it.occurrenceChanges }
        return data.activeSemesterId
    }

    private suspend fun awaitSave() {
        if (failSaves) throw IllegalStateException("save failed")
        saveGate?.await()
    }
}
