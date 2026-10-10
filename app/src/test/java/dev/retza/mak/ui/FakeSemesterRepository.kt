package dev.retza.mak.ui

import dev.retza.mak.data.ActivePlanSource
import dev.retza.mak.data.repository.AcademicCalendarRecord
import dev.retza.mak.data.repository.SemesterConfiguration
import dev.retza.mak.data.repository.SemesterDeletionResult
import dev.retza.mak.data.repository.SemesterProgramRecord
import dev.retza.mak.data.repository.SemesterRecord
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.data.repository.SetupConfigurationIds
import dev.retza.mak.data.repository.StudyProgramRecord
import dev.retza.mak.data.repository.WeekOverrideRecord
import dev.retza.mak.data.repository.toEntity
import dev.retza.mak.data.repository.toRecord
import dev.retza.mak.settings.InMemorySettingsPreferences
import dev.retza.mak.settings.SettingsPreferences
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal class FakeSemesterRepository(
    private val delegate: FakeRepository
) : SemesterRepository {
    var countClassesGate: CompletableDeferred<Unit>? = null

    override fun observeSemesters(): Flow<List<SemesterRecord>> =
        delegate.observeSemesters().map { list -> list.map { it.toRecord() } }

    override fun observeActiveSemester(): Flow<SemesterRecord?> =
        delegate.observeActiveSemester().map { it?.toRecord() }

    override fun observeSemester(id: Long): Flow<SemesterRecord?> = flow {
        delegate.awaitDataGate()
        emit(delegate.observeSemester(id).first()?.toRecord())
    }

    override fun observeStudyPrograms(): Flow<List<StudyProgramRecord>> =
        delegate.observeStudyPrograms().map { list -> list.map { it.toRecord() } }

    override fun observeStudyProgramSemesters(id: Long): Flow<List<String>> =
        delegate.observeSemesters().map { semesters ->
            val assigned = delegate.semesterPrograms.filter { it.studyProgramId == id }.map { it.semesterId }.toSet()
            semesters.filter { it.id in assigned }.map { it.name }.distinct().sorted()
        }

    override fun observeSemesterPrograms(semesterId: Long): Flow<List<SemesterProgramRecord>> =
        delegate.observeSemesterPrograms(semesterId).map { list -> list.map { it.toRecord() } }

    override fun observeCalendars(semesterId: Long): Flow<List<AcademicCalendarRecord>> =
        delegate.observeCalendars(semesterId).map { list -> list.map { it.toRecord() } }

    override fun observeWeekOverrides(semesterId: Long): Flow<List<WeekOverrideRecord>> =
        delegate.observeWeekOverrides(semesterId).map { list -> list.map { it.toRecord() } }

    /** Re-reads the semester after every write, as Room does for the real configuration stream. */
    override fun observeSemesterConfiguration(id: Long): Flow<SemesterConfiguration?> =
        delegate.observeSemesterData(id).map { data ->
            data?.let {
                SemesterConfiguration(
                    semester = it.semester.toRecord(),
                    programs = it.semesterPrograms.map { program -> program.toRecord() },
                    studyPrograms = delegate.studyPrograms.map { program -> program.toRecord() },
                    calendars = it.academicCalendars.map { calendar -> calendar.toRecord() },
                    overrides = it.weekOverrides.map { week -> week.toRecord() }
                )
            }
        }

    override suspend fun saveSemester(record: SemesterRecord): Long =
        written { delegate.saveSemester(record.toEntity()) }

    override suspend fun setActiveSemester(id: Long) = written { delegate.setActiveSemester(id) }

    override suspend fun ensureActiveSemester() = written { delegate.ensureActiveSemester() }

    override suspend fun deleteSemesterAndSelectFallback(id: Long): SemesterDeletionResult =
        written { delegate.deleteSemesterAndSelectFallback(id) }

    override suspend fun saveStudyProgram(record: StudyProgramRecord): Long =
        written { delegate.saveStudyProgram(record.toEntity()) }

    override suspend fun deleteStudyProgram(id: Long) = written { delegate.deleteStudyProgram(id) }

    override suspend fun saveStudyProgramAssignment(
        semesterId: Long,
        studyProgram: StudyProgramRecord,
        academicCalendarId: Long
    ): SetupConfigurationIds = written {
        delegate.saveStudyProgramAssignment(semesterId, studyProgram.toEntity(), academicCalendarId)
    }

    override suspend fun addSeparatedSemesterProgram(
        semesterId: Long,
        studyProgram: StudyProgramRecord,
        sourceCalendarId: Long
    ): SetupConfigurationIds = written {
        delegate.addSeparatedSemesterProgram(semesterId, studyProgram.toEntity(), sourceCalendarId)
    }

    override suspend fun saveCalendar(record: AcademicCalendarRecord): Long =
        written { delegate.saveCalendar(record.toEntity()) }

    override suspend fun updateSemesterWithCalendar(
        semester: SemesterRecord,
        calendar: AcademicCalendarRecord
    ) = written { delegate.updateSemesterWithCalendar(semester.toEntity(), calendar.toEntity()) }

    override suspend fun deleteCalendar(id: Long) = written { delegate.deleteCalendar(id) }

    override suspend fun separateSemesterProgramCalendar(assignmentId: Long): SetupConfigurationIds =
        written { delegate.separateSemesterProgramCalendar(assignmentId) }

    override suspend fun reconnectSemesterProgram(
        assignmentId: Long,
        calendarId: Long
    ): SetupConfigurationIds = written { delegate.reconnectSemesterProgram(assignmentId, calendarId) }

    override suspend fun saveSemesterProgram(record: SemesterProgramRecord): Long =
        written { delegate.saveSemesterProgram(record.toEntity()) }

    override suspend fun deleteSemesterProgram(id: Long) = written { delegate.deleteSemesterProgram(id) }

    override suspend fun countClassesForAssignment(assignmentId: Long): Int {
        countClassesGate?.await()
        return delegate.classes.count { it.semesterProgramId == assignmentId }
    }

    override suspend fun saveSetupConfiguration(
        semester: SemesterRecord,
        studyProgram: StudyProgramRecord,
        calendar: AcademicCalendarRecord,
        activate: Boolean
    ): SetupConfigurationIds = written {
        delegate.saveSetupConfiguration(
            semester.toEntity(),
            studyProgram.toEntity(),
            calendar.toEntity(),
            activate
        )
    }

    override suspend fun saveWeekOverride(record: WeekOverrideRecord): Long =
        delegate.saveWeekOverride(record.toEntity())

    override suspend fun deleteWeekOverride(id: Long) = delegate.deleteWeekOverride(id)

    override suspend fun deleteWeekOverride(
        calendarId: Long,
        weekStartDate: LocalDate,
        scope: dev.retza.mak.domain.WeekOverrideScope
    ) = delegate.deleteWeekOverride(
        calendarId,
        weekStartDate,
        dev.retza.mak.data.entity.WeekOverrideScope.valueOf(scope.name)
    )

    /** Runs a write and then tells the configuration stream that the data changed. */
    private suspend fun <T> written(write: suspend () -> T): T {
        val result = write()
        delegate.notifyDataChanged()
        return result
    }
}

/** The plan source the app builds from [repository] and [preferences]. */
internal fun activePlanSource(
    repository: FakeRepository,
    preferences: SettingsPreferences = InMemorySettingsPreferences(),
    scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) = ActivePlanSource(FakeSemesterRepository(repository), repository, preferences, scope)
