package dev.retza.mak.data.repository

import androidx.room.withTransaction
import dev.retza.mak.data.database.AppDatabase
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
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek

data class SetupConfigurationIds(val semesterId: Long, val courseId: Long)

interface MakRepository {
    fun observeSemesters(): Flow<List<SemesterEntity>>

    fun observeActiveSemester(): Flow<SemesterEntity?>

    fun observeSemester(id: Long): Flow<SemesterEntity?>

    fun observeSemesterData(id: Long): Flow<SemesterWithData?>

    fun observeCourses(semesterId: Long): Flow<List<CourseEntity>>

    fun observeTeachers(semesterId: Long): Flow<List<TeacherEntity>>

    fun observeClasses(semesterId: Long): Flow<List<ClassEntity>>

    fun observeClassesWithDetails(semesterId: Long): Flow<List<ClassWithDetails>>

    fun observeWeekOverrides(semesterId: Long): Flow<List<WeekOverrideEntity>>

    fun observeOccurrenceNotes(semesterId: Long): Flow<List<OccurrenceNoteEntity>>

    fun observeOccurrenceChanges(semesterId: Long): Flow<List<OccurrenceChangeEntity>>

    fun observeOccurrenceNotesForClass(classId: Long): Flow<List<OccurrenceNoteEntity>>

    fun observeOccurrenceChangesForClass(classId: Long): Flow<List<OccurrenceChangeEntity>>

    suspend fun getAllSemesterData(): List<SemesterWithData>

    suspend fun saveSemester(entity: SemesterEntity): Long

    suspend fun updateSemester(entity: SemesterEntity): Long

    suspend fun setActiveSemester(id: Long)

    suspend fun clearActiveSemester()

    suspend fun deleteSemester(id: Long)

    suspend fun saveCourse(entity: CourseEntity): Long

    suspend fun saveSetupConfiguration(
        semester: SemesterEntity,
        course: CourseEntity
    ): SetupConfigurationIds

    suspend fun deleteCourse(id: Long)

    suspend fun saveTeacher(entity: TeacherEntity): Long

    suspend fun deleteTeacher(id: Long)

    suspend fun saveClass(entity: ClassEntity): Long

    suspend fun deleteClass(id: Long)

    suspend fun saveWeekOverride(entity: WeekOverrideEntity): Long

    suspend fun deleteWeekOverride(id: Long)

    suspend fun saveOccurrenceNote(entity: OccurrenceNoteEntity): Long

    suspend fun deleteOccurrenceNote(id: Long)

    suspend fun saveOccurrenceChange(entity: OccurrenceChangeEntity): Long

    suspend fun deleteOccurrenceChange(id: Long)
}

class RoomMakRepository(
    private val database: AppDatabase
) : MakRepository {
    private val semesters = database.semesterDao()
    private val courses = database.courseDao()
    private val teachers = database.teacherDao()
    private val classes = database.classDao()
    private val weekOverrides = database.weekOverrideDao()
    private val occurrenceNotes = database.occurrenceNoteDao()
    private val occurrenceChanges = database.occurrenceChangeDao()

    override fun observeSemesters(): Flow<List<SemesterEntity>> = semesters.observeAll()

    override fun observeActiveSemester(): Flow<SemesterEntity?> = semesters.observeActive()

    override fun observeSemester(id: Long): Flow<SemesterEntity?> = semesters.observeById(id)

    override fun observeSemesterData(id: Long): Flow<SemesterWithData?> = semesters.observeWithData(id)

    override fun observeCourses(semesterId: Long): Flow<List<CourseEntity>> =
        courses.observeForSemester(semesterId)

    override fun observeTeachers(semesterId: Long): Flow<List<TeacherEntity>> =
        teachers.observeForSemester(semesterId)

    override fun observeClasses(semesterId: Long): Flow<List<ClassEntity>> =
        classes.observeForSemester(semesterId)

    override fun observeClassesWithDetails(semesterId: Long): Flow<List<ClassWithDetails>> =
        database.classDao().observeWithDetailsForSemester(semesterId)

    override fun observeWeekOverrides(semesterId: Long): Flow<List<WeekOverrideEntity>> =
        weekOverrides.observeForSemester(semesterId)

    override fun observeOccurrenceNotes(semesterId: Long): Flow<List<OccurrenceNoteEntity>> =
        occurrenceNotes.observeForSemester(semesterId)

    override fun observeOccurrenceChanges(semesterId: Long): Flow<List<OccurrenceChangeEntity>> =
        occurrenceChanges.observeForSemester(semesterId)

    override fun observeOccurrenceNotesForClass(classId: Long): Flow<List<OccurrenceNoteEntity>> =
        occurrenceNotes.observeForClass(classId)

    override fun observeOccurrenceChangesForClass(classId: Long): Flow<List<OccurrenceChangeEntity>> =
        occurrenceChanges.observeForClass(classId)

    override suspend fun getAllSemesterData(): List<SemesterWithData> = semesters.getAllWithData()

    override suspend fun saveSemester(entity: SemesterEntity): Long {
        require(entity.name.isNotBlank()) { "Semester name cannot be blank" }
        require(!entity.endDate.isBefore(entity.startDate)) {
            "Semester end date cannot be before start date"
        }
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

    override suspend fun updateSemester(entity: SemesterEntity): Long {
        require(entity.name.isNotBlank()) { "Semester name cannot be blank" }
        require(!entity.endDate.isBefore(entity.startDate)) {
            "Semester end date cannot be before start date"
        }
        require(entity.id != 0L) { "Use saveSemester to create a semester" }
        return database.withTransaction {
            val existing = semesters.findById(entity.id) ?: error("Semester does not exist")
            semesters.update(entity.copy(isActive = existing.isActive))
            entity.id
        }
    }

    override suspend fun setActiveSemester(id: Long) {
        database.withTransaction {
            require(semesters.findById(id) != null) { "Semester does not exist" }
            semesters.clearActive()
            semesters.markActive(id)
        }
    }

    override suspend fun clearActiveSemester() = semesters.clearActive()

    override suspend fun deleteSemester(id: Long) = semesters.deleteById(id)

    override suspend fun saveCourse(entity: CourseEntity): Long {
        require(coursesSemesterExists(entity.semesterId))
        require(entity.name.isNotBlank()) { "Course name cannot be blank" }
        if (entity.id == 0L) return courses.insert(entity)
        val existing = courses.findById(entity.id)
        require(existing != null) { "Course does not exist" }
        require(existing.semesterId == entity.semesterId) { "Course semester cannot change" }
        courses.update(entity)
        return entity.id
    }

    override suspend fun saveSetupConfiguration(
        semester: SemesterEntity,
        course: CourseEntity
    ): SetupConfigurationIds {
        require(semester.name.isNotBlank()) { "Semester name cannot be blank" }
        require(!semester.endDate.isBefore(semester.startDate)) {
            "Semester end date cannot be before start date"
        }
        require(course.name.isNotBlank()) { "Course name cannot be blank" }
        return database.withTransaction {
            val semesterId = if (semester.id == 0L) {
                semesters.clearActive()
                semesters.insert(semester.copy(isActive = true))
            } else {
                require(semesters.findById(semester.id) != null) { "Semester does not exist" }
                semesters.clearActive()
                semesters.update(semester.copy(isActive = true))
                semester.id
            }
            val courseId = if (course.id == 0L) {
                courses.insert(course.copy(semesterId = semesterId))
            } else {
                val existing = courses.findById(course.id)
                require(existing != null && existing.semesterId == semesterId) {
                    "Course must belong to the semester"
                }
                courses.update(course.copy(semesterId = semesterId))
                course.id
            }
            SetupConfigurationIds(semesterId, courseId)
        }
    }

    override suspend fun deleteCourse(id: Long) = courses.deleteById(id)

    override suspend fun saveTeacher(entity: TeacherEntity): Long {
        require(teachersSemesterExists(entity.semesterId))
        require(entity.name.isNotBlank()) { "Teacher name cannot be blank" }
        if (entity.id == 0L) return teachers.insert(entity)
        val existing = teachers.findById(entity.id)
        require(existing != null) { "Teacher does not exist" }
        require(existing.semesterId == entity.semesterId) { "Teacher semester cannot change" }
        teachers.update(entity)
        return entity.id
    }

    override suspend fun deleteTeacher(id: Long) = teachers.deleteById(id)

    override suspend fun saveClass(entity: ClassEntity): Long {
        require(semesters.findById(entity.semesterId) != null) { "Semester does not exist" }
        require(courses.findById(entity.courseId)?.semesterId == entity.semesterId) {
            "Course must belong to the class semester"
        }
        if (entity.teacherId != null) {
            require(teachers.findById(entity.teacherId)?.semesterId == entity.semesterId) {
                "Teacher must belong to the class semester"
            }
        }
        require(entity.name.isNotBlank()) { "Class name cannot be blank" }
        require(entity.endTime.isAfter(entity.startTime)) {
            "Class end time must be later than start time"
        }
        require(entity.recurrence != Recurrence.ONCE || entity.date != null) {
            "One-time classes require a date"
        }
        if (entity.id == 0L) return classes.insert(entity)
        val existing = classes.findById(entity.id)
        require(existing != null) { "Class does not exist" }
        require(existing.semesterId == entity.semesterId) { "Class semester cannot change" }
        classes.update(entity)
        return entity.id
    }

    override suspend fun deleteClass(id: Long) = classes.deleteById(id)

    override suspend fun saveWeekOverride(entity: WeekOverrideEntity): Long {
        require(semesters.findById(entity.semesterId) != null) { "Semester does not exist" }
        require(entity.weekStartDate.dayOfWeek == DayOfWeek.MONDAY) {
            "Week override must start on Monday"
        }
        if (entity.id == 0L) {
            val existing = weekOverrides.findForWeek(entity.semesterId, entity.weekStartDate, entity.scope)
            if (existing == null) return weekOverrides.insert(entity)
            weekOverrides.update(entity.copy(id = existing.id))
            return existing.id
        }
        val existing = weekOverrides.findById(entity.id)
        require(existing != null) { "Week override does not exist" }
        require(existing.semesterId == entity.semesterId) {
            "Week override semester cannot change"
        }
        weekOverrides.update(entity)
        return entity.id
    }

    override suspend fun deleteWeekOverride(id: Long) = weekOverrides.deleteById(id)

    override suspend fun saveOccurrenceNote(entity: OccurrenceNoteEntity): Long {
        validateClassOwnership(entity.classId, entity.semesterId)
        require(entity.body.isNotBlank()) { "Occurrence note cannot be blank" }
        if (entity.id == 0L) {
            val existing = occurrenceNotes.findForOccurrence(entity.classId, entity.occurrenceDate)
            if (existing == null) return occurrenceNotes.insert(entity)
            occurrenceNotes.update(entity.copy(id = existing.id))
            return existing.id
        }
        val existing = occurrenceNotes.findById(entity.id)
        require(existing != null) { "Occurrence note does not exist" }
        require(existing.classId == entity.classId && existing.semesterId == entity.semesterId) {
            "Occurrence note ownership cannot change"
        }
        occurrenceNotes.update(entity)
        return entity.id
    }

    override suspend fun deleteOccurrenceNote(id: Long) = occurrenceNotes.deleteById(id)

    override suspend fun saveOccurrenceChange(entity: OccurrenceChangeEntity): Long {
        validateClassOwnership(entity.classId, entity.semesterId)
        if (entity.newTeacherId != null) {
            require(teachers.findById(entity.newTeacherId)?.semesterId == entity.semesterId) {
                "Replacement teacher must belong to the change semester"
            }
        }
        if (entity.newStartTime != null || entity.newEndTime != null) {
            require(entity.newStartTime != null && entity.newEndTime != null) {
                "Both replacement times are required"
            }
            require(entity.newEndTime.isAfter(entity.newStartTime)) {
                "Replacement end time must be later than start time"
            }
        }
        if (entity.id == 0L) {
            val existing = occurrenceChanges.findForOccurrence(entity.classId, entity.originalDate)
            if (existing == null) return occurrenceChanges.insert(entity)
            occurrenceChanges.update(entity.copy(id = existing.id))
            return existing.id
        }
        val existing = occurrenceChanges.findById(entity.id)
        require(existing != null) { "Occurrence change does not exist" }
        require(existing.classId == entity.classId && existing.semesterId == entity.semesterId) {
            "Occurrence change ownership cannot change"
        }
        occurrenceChanges.update(entity)
        return entity.id
    }

    override suspend fun deleteOccurrenceChange(id: Long) = occurrenceChanges.deleteById(id)

    private suspend fun coursesSemesterExists(semesterId: Long): Boolean =
        semesters.findById(semesterId) != null

    private suspend fun teachersSemesterExists(semesterId: Long): Boolean =
        semesters.findById(semesterId) != null

    private suspend fun validateClassOwnership(classId: Long, semesterId: Long) {
        require(semesters.findById(semesterId) != null) { "Semester does not exist" }
        require(classes.findById(classId)?.semesterId == semesterId) {
            "Class must belong to the record semester"
        }
    }
}
