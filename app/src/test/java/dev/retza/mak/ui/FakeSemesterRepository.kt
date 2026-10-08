package dev.retza.mak.ui

import dev.retza.mak.data.repository.AcademicCalendarRecord
import dev.retza.mak.data.repository.SemesterDeletionResult
import dev.retza.mak.data.repository.SemesterProgramRecord
import dev.retza.mak.data.repository.SemesterRecord
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.data.repository.SetupConfigurationIds
import dev.retza.mak.data.repository.StudyProgramRecord
import dev.retza.mak.data.repository.WeekOverrideRecord
import dev.retza.mak.data.repository.toEntity
import dev.retza.mak.data.repository.toRecord
import kotlinx.coroutines.CompletableDeferred
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

    override fun observeSemesterPrograms(semesterId: Long): Flow<List<SemesterProgramRecord>> =
        delegate.observeSemesterPrograms(semesterId).map { list -> list.map { it.toRecord() } }

    override fun observeCalendars(semesterId: Long): Flow<List<AcademicCalendarRecord>> =
        delegate.observeCalendars(semesterId).map { list -> list.map { it.toRecord() } }

    override fun observeWeekOverrides(semesterId: Long): Flow<List<WeekOverrideRecord>> =
        delegate.observeWeekOverrides(semesterId).map { list -> list.map { it.toRecord() } }

    override suspend fun saveSemester(record: SemesterRecord): Long =
        delegate.saveSemester(record.toEntity())

    override suspend fun updateSemester(record: SemesterRecord): Long =
        delegate.updateSemester(record.toEntity())

    override suspend fun setActiveSemester(id: Long) = delegate.setActiveSemester(id)

    override suspend fun clearActiveSemester() = delegate.clearActiveSemester()

    override suspend fun deleteSemester(id: Long) = delegate.deleteSemester(id)

    override suspend fun deleteSemesterAndSelectFallback(id: Long): SemesterDeletionResult =
        delegate.deleteSemesterAndSelectFallback(id)

    override suspend fun saveStudyProgram(record: StudyProgramRecord): Long =
        delegate.saveStudyProgram(record.toEntity())

    override suspend fun deleteStudyProgram(id: Long) = delegate.deleteStudyProgram(id)

    override suspend fun saveStudyProgramAssignment(
        semesterId: Long,
        studyProgram: StudyProgramRecord,
        academicCalendarId: Long
    ): SetupConfigurationIds = delegate.saveStudyProgramAssignment(
        semesterId,
        studyProgram.toEntity(),
        academicCalendarId
    )

    override suspend fun addSeparatedSemesterProgram(
        semesterId: Long,
        studyProgram: StudyProgramRecord,
        sourceCalendarId: Long
    ): SetupConfigurationIds = delegate.addSeparatedSemesterProgram(
        semesterId,
        studyProgram.toEntity(),
        sourceCalendarId
    )

    override suspend fun saveCalendar(record: AcademicCalendarRecord): Long =
        delegate.saveCalendar(record.toEntity())

    override suspend fun updateSemesterWithCalendar(
        semester: SemesterRecord,
        calendar: AcademicCalendarRecord
    ) = delegate.updateSemesterWithCalendar(semester.toEntity(), calendar.toEntity())

    override suspend fun deleteCalendarIfUnused(id: Long) = delegate.deleteCalendarIfUnused(id)

    override suspend fun deleteCalendar(id: Long) = delegate.deleteCalendar(id)

    override suspend fun separateSemesterProgramCalendar(assignmentId: Long): SetupConfigurationIds =
        delegate.separateSemesterProgramCalendar(assignmentId)

    override suspend fun reconnectSemesterProgram(
        assignmentId: Long,
        calendarId: Long
    ): SetupConfigurationIds = delegate.reconnectSemesterProgram(assignmentId, calendarId)

    override suspend fun saveSemesterProgram(record: SemesterProgramRecord): Long =
        delegate.saveSemesterProgram(record.toEntity())

    override suspend fun deleteSemesterProgram(id: Long) = delegate.deleteSemesterProgram(id)

    override suspend fun countClassesForAssignment(assignmentId: Long): Int {
        countClassesGate?.await()
        return delegate.classes.count { it.semesterProgramId == assignmentId }
    }

    override suspend fun saveSetupConfiguration(
        semester: SemesterRecord,
        studyProgram: StudyProgramRecord,
        calendar: AcademicCalendarRecord,
        activate: Boolean
    ): SetupConfigurationIds = delegate.saveSetupConfiguration(
        semester.toEntity(),
        studyProgram.toEntity(),
        calendar.toEntity(),
        activate
    )

    override suspend fun saveWeekOverride(record: WeekOverrideRecord): Long =
        delegate.saveWeekOverride(record.toEntity())

    override suspend fun deleteWeekOverride(id: Long) = delegate.deleteWeekOverride(id)
}
