package dev.retza.mak.ui

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
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.data.repository.MakRepository
import dev.retza.mak.data.repository.SetupConfigurationIds
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

internal class FakeMakRepository : MakRepository {
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
    var cancelSaves = false
    var failSetupConfiguration = false
    var failSetActiveSemester = false
    var activeSemesterGate: CompletableDeferred<Unit>? = null
    var setActiveCount = 0
    var failGetAllSemesterData = false
    var lastSetupSemester: SemesterEntity? = null
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

    override fun observeSemesters(): Flow<List<SemesterEntity>> = semesterFlow
    override fun observeActiveSemester(): Flow<SemesterEntity?> =
        combine(semesterFlow, activeSemesterFlow) { list, id -> list.firstOrNull { it.id == id } }
    override fun observeSemester(id: Long): Flow<SemesterEntity?> = flowOf(semesterById(id))
    override fun observeSemesterData(id: Long): Flow<SemesterWithData?> = flow {
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

    override fun observeStudyPrograms(): Flow<List<StudyProgramEntity>> = studyProgramState
    override fun observeSemesterPrograms(semesterId: Long): Flow<List<SemesterProgramEntity>> =
        flowOf(semesterPrograms.filter { it.semesterId == semesterId })
    override fun observeCalendars(semesterId: Long): Flow<List<AcademicCalendarEntity>> =
        flowOf(calendars.filter { it.semesterId == semesterId })
    override fun observeClasses(semesterId: Long): Flow<List<ClassEntity>> = flowOf(classes)
    override fun observeClassesWithDetails(semesterId: Long): Flow<List<ClassWithDetails>> = flowOf(emptyList())
    override fun observeWeekOverrides(semesterId: Long): Flow<List<WeekOverrideEntity>> = flowOf(emptyList())
    override fun observeOccurrenceNotes(semesterId: Long): Flow<List<OccurrenceNoteEntity>> = flowOf(occurrenceNotes)
    override fun observeOccurrenceChanges(semesterId: Long): Flow<List<OccurrenceChangeEntity>> = flowOf(occurrenceChanges)
    override fun observeOccurrenceNotesForClass(classId: Long): Flow<List<OccurrenceNoteEntity>> = flowOf(occurrenceNotes)
    override fun observeOccurrenceChangesForClass(classId: Long): Flow<List<OccurrenceChangeEntity>> = flowOf(occurrenceChanges)
    override suspend fun getAllSemesterData(): List<SemesterWithData> {
        if (failGetAllSemesterData) throw IllegalStateException("export failed")
        return emptyList()
    }

    override suspend fun saveSemester(entity: SemesterEntity): Long {
        awaitSave()
        events += "saveSemester"
        if (entity.isActive) activeSemesterFlow.value = entity.id
        return entity.id
    }

    override suspend fun updateSemester(entity: SemesterEntity): Long {
        awaitSave()
        events += "updateSemester"
        return entity.id
    }

    override suspend fun setActiveSemester(id: Long) {
        activeSemesterGate?.await()
        if (failSetActiveSemester) throw IllegalStateException("set active failed")
        setActiveCount += 1
        if (semesterById(id) != null) activeSemesterFlow.value = id
    }

    override suspend fun clearActiveSemester() {
        activeSemesterFlow.value = 0L
    }

    override suspend fun deleteSemester(id: Long) {
        awaitSave()
        events += "deleteSemester"
        semesterFlow.value = semesterFlow.value.filterNot { it.id == id }
    }

    override suspend fun deleteSemesterAndSelectFallback(
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

    override suspend fun saveStudyProgram(entity: StudyProgramEntity): Long {
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

    override suspend fun deleteStudyProgram(id: Long) {
        awaitSave()
        events += "deleteStudyProgram"
        require(semesterPrograms.none { it.studyProgramId == id }) {
            "Study program is assigned to a semester"
        }
        studyProgramState.value = studyPrograms.filterNot { it.id == id }
    }

    override suspend fun saveStudyProgramAssignment(
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

    override suspend fun getAllStudyPrograms(): List<StudyProgramEntity> = studyPrograms

    override suspend fun saveCalendar(entity: AcademicCalendarEntity): Long {
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

    override suspend fun updateSemesterWithCalendar(
        semester: SemesterEntity,
        calendar: AcademicCalendarEntity
    ) {
        awaitSave()
        events += "updateSemesterWithCalendar"
        semesterFlow.value = semesterFlow.value.map { if (it.id == semester.id) semester else it }
        calendarState.value = calendars.map { if (it.id == calendar.id) calendar else it }
    }

    override suspend fun deleteCalendarIfUnused(id: Long) {
        awaitSave()
        events += "deleteCalendarIfUnused"
        if (semesterPrograms.none { it.academicCalendarId == id }) {
            calendarState.value = calendars.filterNot { it.id == id }
        }
    }

    override suspend fun saveSemesterProgram(entity: SemesterProgramEntity): Long {
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

    override suspend fun deleteSemesterProgram(id: Long) {
        awaitSave()
        events += "deleteSemesterProgram"
        val existing = semesterPrograms.firstOrNull { it.id == id } ?: return
        semesterProgramState.value = semesterPrograms.filterNot { it.id == id }
        if (semesterPrograms.none { it.academicCalendarId == existing.academicCalendarId }) {
            calendarState.value = calendars.filterNot { it.id == existing.academicCalendarId }
        }
    }

    override suspend fun saveSetupConfiguration(
        semester: SemesterEntity,
        studyProgram: StudyProgramEntity,
        calendar: AcademicCalendarEntity
    ): SetupConfigurationIds {
        awaitSave()
        events += "saveSetupConfiguration"
        if (failSetupConfiguration) throw IllegalStateException("setup configuration failed")
        val semesterId = if (semester.id == 0L) generatedSemesterId++ else semester.id
        val studyProgramId = if (studyProgram.id == 0L) generatedStudyProgramId++ else studyProgram.id
        val calendarId = if (calendar.id == 0L) generatedCalendarId++ else calendar.id
        lastSetupSemester = semester.copy(id = semesterId, isActive = true)
        lastSetupStudyProgram = studyProgram.copy(id = studyProgramId)
        lastSetupCalendar = calendar.copy(id = calendarId, semesterId = semesterId)
        activeSemesterFlow.value = semesterId
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
        val ids = SetupConfigurationIds(semesterId, studyProgramId, calendarId, programId)
        lastSetupConfiguration = ids
        return ids
    }

    override suspend fun saveClass(entity: ClassEntity): Long {
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

    override suspend fun saveWeekOverride(entity: WeekOverrideEntity): Long {
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

    override suspend fun deleteWeekOverride(id: Long) {
        awaitSave()
        events += "deleteWeekOverride"
        weekOverrides.removeAll { it.id == id }
    }

    override suspend fun saveOccurrenceNote(entity: OccurrenceNoteEntity): Long {
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

    override suspend fun saveOccurrenceChange(entity: OccurrenceChangeEntity): Long {
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

    private suspend fun awaitSave() {
        if (failSaves) throw IllegalStateException("save failed")
        if (cancelSaves) throw kotlinx.coroutines.CancellationException("save cancelled")
        saveGate?.await()
    }
}
