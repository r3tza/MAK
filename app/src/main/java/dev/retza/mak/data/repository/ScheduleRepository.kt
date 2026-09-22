package dev.retza.mak.data.repository

import dev.retza.mak.data.database.AppDatabase
import dev.retza.mak.domain.ActivePlanData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ScheduleRepository {
    fun observeActivePlanData(semesterId: Long): Flow<ActivePlanData?>

    fun observeClasses(semesterId: Long): Flow<List<ClassRecord>>

    fun observeOccurrenceNotes(semesterId: Long): Flow<List<OccurrenceNoteRecord>>

    fun observeOccurrenceChanges(semesterId: Long): Flow<List<OccurrenceChangeRecord>>

    fun observeOccurrenceNotesForClass(classId: Long): Flow<List<OccurrenceNoteRecord>>

    fun observeOccurrenceChangesForClass(classId: Long): Flow<List<OccurrenceChangeRecord>>

    suspend fun saveClass(record: ClassRecord): Long

    suspend fun deleteClass(id: Long)

    suspend fun saveOccurrenceNote(record: OccurrenceNoteRecord): Long

    suspend fun deleteOccurrenceNote(id: Long)

    suspend fun saveOccurrenceChange(record: OccurrenceChangeRecord): Long

    suspend fun deleteOccurrenceChange(id: Long)
}

@org.koin.core.annotation.Single(binds = [ScheduleRepository::class])
class RoomScheduleRepository(
    private val database: AppDatabase
) : ScheduleRepository {
    private val semesters = database.semesterDao()
    private val semesterPrograms = database.semesterProgramDao()
    private val classes = database.classDao()
    private val occurrenceNotes = database.occurrenceNoteDao()
    private val occurrenceChanges = database.occurrenceChangeDao()

    override fun observeActivePlanData(semesterId: Long): Flow<ActivePlanData?> =
        semesters.observeWithData(semesterId).map { it?.toActivePlanData() }

    override fun observeClasses(semesterId: Long): Flow<List<ClassRecord>> =
        classes.observeForSemester(semesterId).map { list -> list.map { it.toRecord() } }

    override fun observeOccurrenceNotes(semesterId: Long): Flow<List<OccurrenceNoteRecord>> =
        occurrenceNotes.observeForSemester(semesterId).map { list -> list.map { it.toRecord() } }

    override fun observeOccurrenceChanges(semesterId: Long): Flow<List<OccurrenceChangeRecord>> =
        occurrenceChanges.observeForSemester(semesterId).map { list -> list.map { it.toRecord() } }

    override fun observeOccurrenceNotesForClass(classId: Long): Flow<List<OccurrenceNoteRecord>> =
        occurrenceNotes.observeForClass(classId).map { list -> list.map { it.toRecord() } }

    override fun observeOccurrenceChangesForClass(classId: Long): Flow<List<OccurrenceChangeRecord>> =
        occurrenceChanges.observeForClass(classId).map { list -> list.map { it.toRecord() } }

    override suspend fun saveClass(record: ClassRecord): Long {
        val entity = record.toEntity()
        require(semesters.findById(entity.semesterId) != null) { "Semester does not exist" }
        require(semesterPrograms.findById(entity.semesterProgramId)?.semesterId == entity.semesterId) {
            "Assignment must belong to the class semester"
        }
        require(entity.name.isNotBlank()) { "Class name cannot be blank" }
        require(entity.endTime.isAfter(entity.startTime)) {
            "Class end time must be later than start time"
        }
        require(entity.recurrence != dev.retza.mak.data.entity.Recurrence.ONCE || entity.date != null) {
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

    override suspend fun saveOccurrenceNote(record: OccurrenceNoteRecord): Long {
        val entity = record.toEntity()
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

    override suspend fun saveOccurrenceChange(record: OccurrenceChangeRecord): Long {
        val entity = record.toEntity()
        validateClassOwnership(entity.classId, entity.semesterId)
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

    private suspend fun validateClassOwnership(classId: Long, semesterId: Long) {
        require(semesters.findById(semesterId) != null) { "Semester does not exist" }
        require(classes.findById(classId)?.semesterId == semesterId) {
            "Class must belong to the record semester"
        }
    }
}
