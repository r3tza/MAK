package dev.retza.mak.data.repository

import dev.retza.mak.data.database.AppDatabase
import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.entity.WeekOverrideEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

    fun observeWeekOverrides(semesterId: Long): Flow<List<WeekOverrideEntity>>

    suspend fun saveSemester(entity: SemesterEntity): Long

    suspend fun updateSemester(entity: SemesterEntity): Long

    suspend fun setActiveSemester(id: Long)

    suspend fun clearActiveSemester()

    suspend fun deleteSemester(id: Long)

    suspend fun deleteSemesterAndSelectFallback(id: Long): SemesterDeletionResult

    suspend fun saveStudyProgram(entity: StudyProgramEntity): Long

    suspend fun deleteStudyProgram(id: Long)

    suspend fun saveStudyProgramAssignment(
        semesterId: Long,
        studyProgram: StudyProgramEntity,
        academicCalendarId: Long
    ): SetupConfigurationIds

    suspend fun addSeparatedSemesterProgram(
        semesterId: Long,
        studyProgram: StudyProgramEntity,
        sourceCalendarId: Long
    ): SetupConfigurationIds

    suspend fun saveCalendar(entity: AcademicCalendarEntity): Long

    suspend fun updateSemesterWithCalendar(semester: SemesterEntity, calendar: AcademicCalendarEntity)

    suspend fun deleteCalendarIfUnused(id: Long)

    suspend fun deleteCalendar(id: Long)

    suspend fun separateSemesterProgramCalendar(assignmentId: Long): SetupConfigurationIds

    suspend fun reconnectSemesterProgram(assignmentId: Long, calendarId: Long): SetupConfigurationIds

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
    private val database: AppDatabase,
    private val scheduleRepository: ScheduleRepository,
    private val semesterRepository: SemesterRepository
) : MakRepository {
    private val semesters = database.semesterDao()

    override fun observeSemesters(): Flow<List<SemesterEntity>> =
        semesterRepository.observeSemesters().map { list -> list.map { it.toEntity() } }

    override fun observeActiveSemester(): Flow<SemesterEntity?> =
        semesterRepository.observeActiveSemester().map { it?.toEntity() }

    override fun observeSemester(id: Long): Flow<SemesterEntity?> =
        semesterRepository.observeSemester(id).map { it?.toEntity() }

    override fun observeSemesterData(id: Long): Flow<SemesterWithData?> = semesters.observeWithData(id)

    override fun observeStudyPrograms(): Flow<List<StudyProgramEntity>> =
        semesterRepository.observeStudyPrograms().map { list -> list.map { it.toEntity() } }

    override fun observeSemesterPrograms(semesterId: Long): Flow<List<SemesterProgramEntity>> =
        semesterRepository.observeSemesterPrograms(semesterId).map { list -> list.map { it.toEntity() } }

    override fun observeCalendars(semesterId: Long): Flow<List<AcademicCalendarEntity>> =
        semesterRepository.observeCalendars(semesterId).map { list -> list.map { it.toEntity() } }

    override fun observeWeekOverrides(semesterId: Long): Flow<List<WeekOverrideEntity>> =
        semesterRepository.observeWeekOverrides(semesterId).map { list -> list.map { it.toEntity() } }

    override suspend fun saveSemester(entity: SemesterEntity): Long =
        semesterRepository.saveSemester(entity.toRecord())

    override suspend fun updateSemester(entity: SemesterEntity): Long =
        semesterRepository.updateSemester(entity.toRecord())

    override suspend fun setActiveSemester(id: Long) = semesterRepository.setActiveSemester(id)

    override suspend fun clearActiveSemester() = semesterRepository.clearActiveSemester()

    override suspend fun deleteSemester(id: Long) = semesterRepository.deleteSemester(id)

    override suspend fun deleteSemesterAndSelectFallback(id: Long): SemesterDeletionResult =
        semesterRepository.deleteSemesterAndSelectFallback(id)

    override suspend fun saveStudyProgram(entity: StudyProgramEntity): Long =
        semesterRepository.saveStudyProgram(entity.toRecord())

    override suspend fun deleteStudyProgram(id: Long) = semesterRepository.deleteStudyProgram(id)

    override suspend fun saveStudyProgramAssignment(
        semesterId: Long,
        studyProgram: StudyProgramEntity,
        academicCalendarId: Long
    ): SetupConfigurationIds = semesterRepository.saveStudyProgramAssignment(
        semesterId = semesterId,
        studyProgram = studyProgram.toRecord(),
        academicCalendarId = academicCalendarId
    )

    override suspend fun addSeparatedSemesterProgram(
        semesterId: Long,
        studyProgram: StudyProgramEntity,
        sourceCalendarId: Long
    ): SetupConfigurationIds = semesterRepository.addSeparatedSemesterProgram(
        semesterId = semesterId,
        studyProgram = studyProgram.toRecord(),
        sourceCalendarId = sourceCalendarId
    )

    override suspend fun saveCalendar(entity: AcademicCalendarEntity): Long =
        semesterRepository.saveCalendar(entity.toRecord())

    override suspend fun updateSemesterWithCalendar(
        semester: SemesterEntity,
        calendar: AcademicCalendarEntity
    ) = semesterRepository.updateSemesterWithCalendar(semester.toRecord(), calendar.toRecord())

    override suspend fun deleteCalendarIfUnused(id: Long) = semesterRepository.deleteCalendarIfUnused(id)

    override suspend fun deleteCalendar(id: Long) = semesterRepository.deleteCalendar(id)

    override suspend fun separateSemesterProgramCalendar(assignmentId: Long): SetupConfigurationIds =
        semesterRepository.separateSemesterProgramCalendar(assignmentId)

    override suspend fun reconnectSemesterProgram(
        assignmentId: Long,
        calendarId: Long
    ): SetupConfigurationIds = semesterRepository.reconnectSemesterProgram(assignmentId, calendarId)

    override suspend fun saveSemesterProgram(entity: SemesterProgramEntity): Long =
        semesterRepository.saveSemesterProgram(entity.toRecord())

    override suspend fun deleteSemesterProgram(id: Long) = semesterRepository.deleteSemesterProgram(id)

    override suspend fun saveSetupConfiguration(
        semester: SemesterEntity,
        studyProgram: StudyProgramEntity,
        calendar: AcademicCalendarEntity
    ): SetupConfigurationIds = semesterRepository.saveSetupConfiguration(
        semester = semester.toRecord(),
        studyProgram = studyProgram.toRecord(),
        calendar = calendar.toRecord()
    )

    override suspend fun saveClass(entity: ClassEntity): Long =
        scheduleRepository.saveClass(entity.toRecord())

    override suspend fun deleteClass(id: Long) = scheduleRepository.deleteClass(id)

    override suspend fun saveWeekOverride(entity: WeekOverrideEntity): Long =
        semesterRepository.saveWeekOverride(entity.toRecord())

    override suspend fun deleteWeekOverride(id: Long) = semesterRepository.deleteWeekOverride(id)

    override suspend fun saveOccurrenceNote(entity: OccurrenceNoteEntity): Long =
        scheduleRepository.saveOccurrenceNote(entity.toRecord())

    override suspend fun deleteOccurrenceNote(id: Long) = scheduleRepository.deleteOccurrenceNote(id)

    override suspend fun saveOccurrenceChange(entity: OccurrenceChangeEntity): Long =
        scheduleRepository.saveOccurrenceChange(entity.toRecord())

    override suspend fun deleteOccurrenceChange(id: Long) = scheduleRepository.deleteOccurrenceChange(id)
}
