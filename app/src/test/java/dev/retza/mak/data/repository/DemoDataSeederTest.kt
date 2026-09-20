package dev.retza.mak.data.repository

import dev.retza.mak.data.database.ClassWithDetails
import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.CourseEntity
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.TeacherEntity
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
        assertEquals(2, repository.courses.size)
        assertEquals(2, repository.teachers.size)
        assertEquals(1, repository.weekOverrides.size)
        assertEquals(1, repository.occurrenceNotes.size)
        assertEquals(1, repository.occurrenceChanges.size)
        assertEquals("2026-09-19", repository.classes.single { it.date != null }.date.toString())
    }
}

private class FakeMakRepository : MakRepository {
    val semesters = mutableListOf<SemesterEntity>()
    val courses = mutableListOf<CourseEntity>()
    val teachers = mutableListOf<TeacherEntity>()
    val classes = mutableListOf<ClassEntity>()
    val weekOverrides = mutableListOf<WeekOverrideEntity>()
    val occurrenceNotes = mutableListOf<OccurrenceNoteEntity>()
    val occurrenceChanges = mutableListOf<OccurrenceChangeEntity>()

    override fun observeSemesters(): Flow<List<SemesterEntity>> = flowOf(semesters)
    override fun observeActiveSemester(): Flow<SemesterEntity?> = flowOf(semesters.firstOrNull { it.isActive })
    override fun observeSemester(id: Long): Flow<SemesterEntity?> = flowOf(semesters.find { it.id == id })
    override fun observeSemesterData(id: Long): Flow<SemesterWithData?> = flowOf(null)
    override fun observeCourses(semesterId: Long): Flow<List<CourseEntity>> = flowOf(courses)
    override fun observeTeachers(semesterId: Long): Flow<List<TeacherEntity>> = flowOf(teachers)
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

    override suspend fun saveSetupConfiguration(
        semester: SemesterEntity,
        course: CourseEntity
    ): SetupConfigurationIds = SetupConfigurationIds(semester.id, course.id)

    override suspend fun setActiveSemester(id: Long) = Unit
    override suspend fun clearActiveSemester() = Unit
    override suspend fun deleteSemester(id: Long) = Unit

    override suspend fun saveCourse(entity: CourseEntity): Long {
        val id = courses.size + 1L
        courses += entity.copy(id = id)
        return id
    }

    override suspend fun deleteCourse(id: Long) = Unit

    override suspend fun saveTeacher(entity: TeacherEntity): Long {
        val id = teachers.size + 1L
        teachers += entity.copy(id = id)
        return id
    }

    override suspend fun deleteTeacher(id: Long) = Unit

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
}
