package dev.retza.mak.data.repository

import dev.retza.mak.data.database.ClassWithDetails
import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.entity.WeekOverrideEntity
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
        val repository = FakeMakRepository()
        val clock = Clock.fixed(
            Instant.parse("2026-09-19T10:00:00Z"),
            ZoneId.of("Europe/Warsaw")
        )

        repository.seedDemoDataIfEmpty(clock)
        repository.seedDemoDataIfEmpty(clock)

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

private class FakeMakRepository : MakRepository {
    val semesters = mutableListOf<SemesterEntity>()
    val studyPrograms = mutableListOf<StudyProgramEntity>()
    val calendars = mutableListOf<AcademicCalendarEntity>()
    val semesterPrograms = mutableListOf<SemesterProgramEntity>()
    val classes = mutableListOf<ClassEntity>()
    val weekOverrides = mutableListOf<WeekOverrideEntity>()
    val occurrenceNotes = mutableListOf<OccurrenceNoteEntity>()
    val occurrenceChanges = mutableListOf<OccurrenceChangeEntity>()

    override fun observeSemesters(): Flow<List<SemesterEntity>> = flowOf(semesters)
    override fun observeActiveSemester(): Flow<SemesterEntity?> = flowOf(semesters.firstOrNull { it.isActive })
    override fun observeSemester(id: Long): Flow<SemesterEntity?> = flowOf(semesters.find { it.id == id })
    override fun observeSemesterData(id: Long): Flow<SemesterWithData?> = flowOf(null)
    override fun observeStudyPrograms(): Flow<List<StudyProgramEntity>> = flowOf(studyPrograms)
    override fun observeSemesterPrograms(semesterId: Long): Flow<List<SemesterProgramEntity>> = flowOf(semesterPrograms)
    override fun observeCalendars(semesterId: Long): Flow<List<AcademicCalendarEntity>> = flowOf(calendars)
    override fun observeClasses(semesterId: Long): Flow<List<ClassEntity>> = flowOf(classes)
    override fun observeClassesWithDetails(semesterId: Long): Flow<List<ClassWithDetails>> = flowOf(emptyList())
    override fun observeWeekOverrides(semesterId: Long): Flow<List<WeekOverrideEntity>> = flowOf(weekOverrides)
    override fun observeOccurrenceNotes(semesterId: Long): Flow<List<OccurrenceNoteEntity>> = flowOf(occurrenceNotes)
    override fun observeOccurrenceChanges(semesterId: Long): Flow<List<OccurrenceChangeEntity>> = flowOf(occurrenceChanges)
    override fun observeOccurrenceNotesForClass(classId: Long): Flow<List<OccurrenceNoteEntity>> = flowOf(occurrenceNotes)
    override fun observeOccurrenceChangesForClass(classId: Long): Flow<List<OccurrenceChangeEntity>> = flowOf(occurrenceChanges)
    override suspend fun getAllSemesterData(): List<SemesterWithData> = emptyList()

    override suspend fun saveSemester(entity: SemesterEntity): Long {
        val saved = entity.copy(id = 1L)
        semesters += saved
        return saved.id
    }

    override suspend fun updateSemester(entity: SemesterEntity): Long = entity.id

    override suspend fun setActiveSemester(id: Long) = Unit
    override suspend fun clearActiveSemester() = Unit
    override suspend fun deleteSemester(id: Long) = Unit

    override suspend fun deleteSemesterAndSelectFallback(id: Long): SemesterDeletionResult =
        SemesterDeletionResult(null)

    override suspend fun saveStudyProgram(entity: StudyProgramEntity): Long {
        val id = studyPrograms.size + 1L
        studyPrograms += entity.copy(id = id)
        return id
    }

    override suspend fun deleteStudyProgram(id: Long) = Unit

    override suspend fun addSeparatedSemesterProgram(
        semesterId: Long,
        studyProgram: StudyProgramEntity,
        sourceCalendarId: Long
    ): SetupConfigurationIds = SetupConfigurationIds(1L, 1L, 1L, 1L)

    override suspend fun getAllStudyPrograms(): List<StudyProgramEntity> = studyPrograms

    override suspend fun saveStudyProgramAssignment(
        semesterId: Long,
        studyProgram: StudyProgramEntity,
        academicCalendarId: Long
    ): SetupConfigurationIds = SetupConfigurationIds(1L, 1L, 1L, 1L)

    override suspend fun saveCalendar(entity: AcademicCalendarEntity): Long {
        val id = calendars.size + 1L
        calendars += entity.copy(id = id)
        return id
    }

    override suspend fun updateSemesterWithCalendar(
        semester: SemesterEntity,
        calendar: AcademicCalendarEntity
    ) = Unit

    override suspend fun deleteCalendarIfUnused(id: Long) = Unit

    override suspend fun deleteCalendar(id: Long) = Unit

    override suspend fun separateSemesterProgramCalendar(assignmentId: Long): SetupConfigurationIds =
        SetupConfigurationIds(1L, 1L, 1L, 1L)

    override suspend fun reconnectSemesterProgram(
        assignmentId: Long,
        calendarId: Long
    ): SetupConfigurationIds = SetupConfigurationIds(1L, 1L, 1L, 1L)

    override suspend fun saveSemesterProgram(entity: SemesterProgramEntity): Long {
        val id = semesterPrograms.size + 1L
        semesterPrograms += entity.copy(id = id)
        return id
    }

    override suspend fun deleteSemesterProgram(id: Long) = Unit

    override suspend fun saveSetupConfiguration(
        semester: SemesterEntity,
        studyProgram: StudyProgramEntity,
        calendar: AcademicCalendarEntity
    ): SetupConfigurationIds = SetupConfigurationIds(1L, 1L, 1L, 1L)

    override suspend fun saveClass(entity: ClassEntity): Long {
        val id = classes.size + 1L
        classes += entity.copy(id = id)
        return id
    }

    override suspend fun deleteClass(id: Long) = Unit
    override suspend fun saveWeekOverride(entity: WeekOverrideEntity): Long {
        weekOverrides += entity.copy(id = 1L)
        return 1L
    }

    override suspend fun deleteWeekOverride(id: Long) = Unit
    override suspend fun saveOccurrenceNote(entity: OccurrenceNoteEntity): Long {
        occurrenceNotes += entity.copy(id = 1L)
        return 1L
    }

    override suspend fun deleteOccurrenceNote(id: Long) = Unit
    override suspend fun saveOccurrenceChange(entity: OccurrenceChangeEntity): Long {
        occurrenceChanges += entity.copy(id = 1L)
        return 1L
    }

    override suspend fun deleteOccurrenceChange(id: Long) = Unit

    override suspend fun replaceAllData(data: BackupData): Long? {
        semesters.clear()
        semesters += data.semesters.map { it.semester }
        studyPrograms.clear()
        studyPrograms += data.studyPrograms
        calendars.clear()
        calendars += data.semesters.flatMap { it.calendars }
        semesterPrograms.clear()
        semesterPrograms += data.semesters.flatMap { it.programs }
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
}
