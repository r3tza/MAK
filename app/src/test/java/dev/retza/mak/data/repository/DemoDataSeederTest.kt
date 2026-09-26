package dev.retza.mak.data.repository

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DemoDataSeederTest {
    @Test
    fun seedsPreviewDataOnlyIntoAnEmptyRepository() = runTest {
        val repository = RecordingRepository()
        val clock = Clock.fixed(
            Instant.parse("2026-09-19T10:00:00Z"),
            ZoneId.of("Europe/Warsaw")
        )

        seedDemoDataIfEmpty(repository, repository, clock)
        seedDemoDataIfEmpty(repository, repository, clock)

        assertEquals(1, repository.semesters.size)
        assertEquals("Semestr demonstracyjny 2026/27", repository.semesters.single().name)
        assertEquals(5, repository.classes.size)
        assertEquals(2, repository.studyPrograms.size)
        assertEquals(1, repository.calendars.size)
        assertEquals(2, repository.semesterPrograms.size)
        assertEquals(1, repository.weekOverrides.size)
        assertEquals(1, repository.occurrenceNotes.size)
        assertEquals(1, repository.occurrenceChanges.size)
        assertEquals("2026-09-19", repository.classes.single { it.date != null }.date.toString())
    }
}

private class RecordingRepository : SemesterRepository, ScheduleRepository {
    val semesters = mutableListOf<SemesterRecord>()
    val studyPrograms = mutableListOf<StudyProgramRecord>()
    val calendars = mutableListOf<AcademicCalendarRecord>()
    val semesterPrograms = mutableListOf<SemesterProgramRecord>()
    val classes = mutableListOf<ClassRecord>()
    val weekOverrides = mutableListOf<WeekOverrideRecord>()
    val occurrenceNotes = mutableListOf<OccurrenceNoteRecord>()
    val occurrenceChanges = mutableListOf<OccurrenceChangeRecord>()

    private var nextId = 1L

    override fun observeSemesters(): Flow<List<SemesterRecord>> = flowOf(semesters)

    override fun observeActiveSemester(): Flow<SemesterRecord?> = flowOf(semesters.firstOrNull { it.isActive })

    override fun observeSemester(id: Long): Flow<SemesterRecord?> = flowOf(semesters.find { it.id == id })

    override fun observeStudyPrograms(): Flow<List<StudyProgramRecord>> = flowOf(studyPrograms)

    override fun observeSemesterPrograms(semesterId: Long): Flow<List<SemesterProgramRecord>> =
        flowOf(semesterPrograms.filter { it.semesterId == semesterId })

    override fun observeCalendars(semesterId: Long): Flow<List<AcademicCalendarRecord>> =
        flowOf(calendars.filter { it.semesterId == semesterId })

    override fun observeWeekOverrides(semesterId: Long): Flow<List<WeekOverrideRecord>> =
        flowOf(weekOverrides.filter { it.semesterId == semesterId })

    override suspend fun saveSemester(record: SemesterRecord): Long {
        val id = record.id.takeIf { it != 0L } ?: nextId++
        semesters += record.copy(id = id)
        return id
    }

    override suspend fun updateSemester(record: SemesterRecord): Long = record.id

    override suspend fun setActiveSemester(id: Long) = Unit

    override suspend fun clearActiveSemester() = Unit

    override suspend fun deleteSemester(id: Long) = Unit

    override suspend fun deleteSemesterAndSelectFallback(id: Long): SemesterDeletionResult =
        SemesterDeletionResult(null)

    override suspend fun saveStudyProgram(record: StudyProgramRecord): Long {
        val id = record.id.takeIf { it != 0L } ?: nextId++
        studyPrograms += record.copy(id = id)
        return id
    }

    override suspend fun deleteStudyProgram(id: Long) = Unit

    override suspend fun saveStudyProgramAssignment(
        semesterId: Long,
        studyProgram: StudyProgramRecord,
        academicCalendarId: Long
    ): SetupConfigurationIds = SetupConfigurationIds(semesterId, 0L, academicCalendarId, 0L)

    override suspend fun addSeparatedSemesterProgram(
        semesterId: Long,
        studyProgram: StudyProgramRecord,
        sourceCalendarId: Long
    ): SetupConfigurationIds = SetupConfigurationIds(semesterId, 0L, sourceCalendarId, 0L)

    override suspend fun saveCalendar(record: AcademicCalendarRecord): Long {
        val id = record.id.takeIf { it != 0L } ?: nextId++
        calendars += record.copy(id = id)
        return id
    }

    override suspend fun updateSemesterWithCalendar(
        semester: SemesterRecord,
        calendar: AcademicCalendarRecord
    ) = Unit

    override suspend fun deleteCalendarIfUnused(id: Long) = Unit

    override suspend fun deleteCalendar(id: Long) = Unit

    override suspend fun separateSemesterProgramCalendar(assignmentId: Long): SetupConfigurationIds =
        SetupConfigurationIds(0L, 0L, 0L, 0L)

    override suspend fun reconnectSemesterProgram(
        assignmentId: Long,
        calendarId: Long
    ): SetupConfigurationIds = SetupConfigurationIds(0L, 0L, 0L, 0L)

    override suspend fun saveSemesterProgram(record: SemesterProgramRecord): Long {
        val id = record.id.takeIf { it != 0L } ?: nextId++
        semesterPrograms += record.copy(id = id)
        return id
    }

    override suspend fun deleteSemesterProgram(id: Long) = Unit

    override suspend fun countClassesForAssignment(assignmentId: Long): Int = 0

    override suspend fun saveSetupConfiguration(
        semester: SemesterRecord,
        studyProgram: StudyProgramRecord,
        calendar: AcademicCalendarRecord
    ): SetupConfigurationIds = SetupConfigurationIds(0L, 0L, 0L, 0L)

    override suspend fun saveWeekOverride(record: WeekOverrideRecord): Long {
        weekOverrides += record
        return nextId++
    }

    override suspend fun deleteWeekOverride(id: Long) = Unit

    override fun observeActivePlanData(semesterId: Long): Flow<dev.retza.mak.domain.ActivePlanData?> =
        flowOf(null)

    override fun observeClasses(semesterId: Long): Flow<List<ClassRecord>> =
        flowOf(classes.filter { it.semesterId == semesterId })

    override fun observeOccurrenceNotes(semesterId: Long): Flow<List<OccurrenceNoteRecord>> =
        flowOf(occurrenceNotes.filter { it.semesterId == semesterId })

    override fun observeOccurrenceChanges(semesterId: Long): Flow<List<OccurrenceChangeRecord>> =
        flowOf(occurrenceChanges.filter { it.semesterId == semesterId })

    override fun observeOccurrenceNotesForClass(classId: Long): Flow<List<OccurrenceNoteRecord>> =
        flowOf(occurrenceNotes.filter { it.classId == classId })

    override fun observeOccurrenceChangesForClass(classId: Long): Flow<List<OccurrenceChangeRecord>> =
        flowOf(occurrenceChanges.filter { it.classId == classId })

    override suspend fun saveClass(record: ClassRecord): Long {
        val id = record.id.takeIf { it != 0L } ?: nextId++
        classes += record.copy(id = id)
        return id
    }

    override suspend fun deleteClass(id: Long) = Unit

    override suspend fun saveOccurrenceNote(record: OccurrenceNoteRecord): Long {
        occurrenceNotes += record
        return nextId++
    }

    override suspend fun deleteOccurrenceNote(id: Long) = Unit

    override suspend fun saveOccurrenceChange(record: OccurrenceChangeRecord): Long {
        occurrenceChanges += record
        return nextId++
    }

    override suspend fun deleteOccurrenceChange(id: Long) = Unit
}
