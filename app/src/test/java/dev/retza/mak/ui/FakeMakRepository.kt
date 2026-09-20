package dev.retza.mak.ui

import dev.retza.mak.data.database.ClassWithDetails
import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.CourseEntity
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.Recurrence
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.TeacherEntity
import dev.retza.mak.data.entity.WeekOverrideEntity
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.data.repository.MakRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

internal class FakeMakRepository : MakRepository {
    val semester = SemesterEntity(
        id = 1L,
        name = "Semestr",
        startDate = LocalDate.of(2026, 9, 1),
        endDate = LocalDate.of(2026, 10, 31),
        firstWeekType = WeekType.A,
        isActive = true
    )
    val secondSemester = SemesterEntity(
        id = 2L,
        name = "Semestr drugi",
        startDate = LocalDate.of(2027, 2, 1),
        endDate = LocalDate.of(2027, 6, 30),
        firstWeekType = WeekType.B,
        isActive = false
    )
    val courses = mutableListOf(
        CourseEntity(id = 1L, semesterId = 1L, name = "Informatyka", color = "#137B71")
    )
    val classes = mutableListOf(
        ClassEntity(
            id = 1L,
            semesterId = 1L,
            name = "Programowanie",
            type = "Wykład",
            courseId = 1L,
            teacherId = null,
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
    var activeSemesterId: Long = 1L

    private fun semesterById(id: Long): SemesterEntity? =
        listOf(semester, secondSemester).firstOrNull { it.id == id }

    override fun observeSemesters(): Flow<List<SemesterEntity>> = flowOf(listOf(semester))
    override fun observeActiveSemester(): Flow<SemesterEntity?> = flowOf(semesterById(activeSemesterId))
    override fun observeSemester(id: Long): Flow<SemesterEntity?> = flowOf(semesterById(id))
    override fun observeSemesterData(id: Long): Flow<SemesterWithData?> = flow {
        occurrenceDataGate?.await()
        val target = listOf(semester, secondSemester).firstOrNull { it.id == id }
        if (target == null) {
            emit(null)
            return@flow
        }
        emit(
            SemesterWithData(
                semester = target,
                courses = courses.filter { it.semesterId == id },
                teachers = emptyList(),
                classes = classes.filter { it.semesterId == id },
                weekOverrides = weekOverrides.filter { it.semesterId == id },
                occurrenceNotes = occurrenceNotes,
                occurrenceChanges = occurrenceChanges
            )
        )
    }

    override fun observeCourses(semesterId: Long): Flow<List<CourseEntity>> = flowOf(courses)
    override fun observeTeachers(semesterId: Long): Flow<List<TeacherEntity>> = flowOf(emptyList())
    override fun observeClasses(semesterId: Long): Flow<List<ClassEntity>> = flowOf(classes)
    override fun observeClassesWithDetails(semesterId: Long): Flow<List<ClassWithDetails>> = flowOf(emptyList())
    override fun observeWeekOverrides(semesterId: Long): Flow<List<WeekOverrideEntity>> = flowOf(emptyList())
    override fun observeOccurrenceNotes(semesterId: Long): Flow<List<OccurrenceNoteEntity>> = flowOf(occurrenceNotes)
    override fun observeOccurrenceChanges(semesterId: Long): Flow<List<OccurrenceChangeEntity>> = flowOf(occurrenceChanges)
    override fun observeOccurrenceNotesForClass(classId: Long): Flow<List<OccurrenceNoteEntity>> = flowOf(occurrenceNotes)
    override fun observeOccurrenceChangesForClass(classId: Long): Flow<List<OccurrenceChangeEntity>> = flowOf(occurrenceChanges)
    override suspend fun getAllSemesterData(): List<SemesterWithData> = emptyList()

    override suspend fun saveSemester(entity: SemesterEntity): Long {
        awaitSave()
        events += "saveSemester"
        if (entity.isActive) activeSemesterId = entity.id
        return entity.id
    }

    override suspend fun updateSemester(entity: SemesterEntity): Long {
        awaitSave()
        events += "updateSemester"
        return entity.id
    }

    override suspend fun setActiveSemester(id: Long) {
        if (semesterById(id) != null) activeSemesterId = id
    }

    override suspend fun clearActiveSemester() = Unit
    override suspend fun deleteSemester(id: Long) = Unit
    override suspend fun saveCourse(entity: CourseEntity): Long {
        awaitSave()
        events += "saveCourse"
        val index = courses.indexOfFirst { it.id == entity.id }
        if (index >= 0) courses[index] = entity else courses += entity.copy(id = (courses.size + 1).toLong())
        return entity.id
    }

    override suspend fun deleteCourse(id: Long) {
        awaitSave()
        events += "deleteCourse"
        courses.removeAll { it.id == id }
    }
    override suspend fun saveTeacher(entity: TeacherEntity): Long = 1L
    override suspend fun deleteTeacher(id: Long) = Unit

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
