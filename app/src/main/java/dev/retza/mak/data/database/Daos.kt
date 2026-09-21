package dev.retza.mak.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.CourseEntity
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.TeacherEntity
import dev.retza.mak.data.entity.WeekOverrideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {
    @Query("SELECT * FROM semesters ORDER BY start_date, id")
    fun observeAll(): Flow<List<SemesterEntity>>

    @Query("SELECT * FROM semesters WHERE is_active = 1 LIMIT 1")
    fun observeActive(): Flow<SemesterEntity?>

    @Query("SELECT * FROM semesters ORDER BY start_date, id")
    suspend fun getAll(): List<SemesterEntity>

    @Query("SELECT * FROM semesters WHERE id = :id")
    suspend fun findById(id: Long): SemesterEntity?

    @Query("SELECT * FROM semesters WHERE id = :id")
    fun observeById(id: Long): Flow<SemesterEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: SemesterEntity): Long

    @Update
    suspend fun update(entity: SemesterEntity)

    @Query("UPDATE semesters SET is_active = 0")
    suspend fun clearActive()

    @Query("UPDATE semesters SET is_active = 1 WHERE id = :id")
    suspend fun markActive(id: Long)

    @Query("DELETE FROM semesters WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Transaction
    @Query("SELECT * FROM semesters WHERE id = :id")
    fun observeWithData(id: Long): Flow<SemesterWithData?>

    @Transaction
    @Query("SELECT * FROM semesters ORDER BY start_date, id")
    suspend fun getAllWithData(): List<SemesterWithData>
}

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses WHERE semester_id = :semesterId ORDER BY name, id")
    fun observeForSemester(semesterId: Long): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun findById(id: Long): CourseEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: CourseEntity): Long

    @Update
    suspend fun update(entity: CourseEntity)

    @Query("DELETE FROM courses WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface TeacherDao {
    @Query("SELECT * FROM teachers WHERE semester_id = :semesterId ORDER BY name, id")
    fun observeForSemester(semesterId: Long): Flow<List<TeacherEntity>>

    @Query("SELECT * FROM teachers WHERE id = :id")
    suspend fun findById(id: Long): TeacherEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: TeacherEntity): Long

    @Update
    suspend fun update(entity: TeacherEntity)

    @Query("DELETE FROM teachers WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ClassDao {
    @Query("SELECT * FROM classes WHERE semester_id = :semesterId ORDER BY day_of_week, start_time, id")
    fun observeForSemester(semesterId: Long): Flow<List<ClassEntity>>

    @Query("SELECT * FROM classes WHERE id = :id")
    suspend fun findById(id: Long): ClassEntity?

    @Transaction
    @Query("SELECT * FROM classes WHERE semester_id = :semesterId ORDER BY day_of_week, start_time, id")
    fun observeWithDetailsForSemester(semesterId: Long): Flow<List<ClassWithDetails>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ClassEntity): Long

    @Update
    suspend fun update(entity: ClassEntity)

    @Query("DELETE FROM classes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Transaction
    @Query("SELECT * FROM classes WHERE id = :id")
    fun observeWithDetails(id: Long): Flow<ClassWithDetails?>
}

@Dao
interface WeekOverrideDao {
    @Query(
        "SELECT * FROM week_overrides " +
            "WHERE semester_id = :semesterId ORDER BY week_start_date, scope, id"
    )
    fun observeForSemester(semesterId: Long): Flow<List<WeekOverrideEntity>>

    @Query("SELECT * FROM week_overrides WHERE id = :id")
    suspend fun findById(id: Long): WeekOverrideEntity?

    @Query(
        "SELECT * FROM week_overrides WHERE semester_id = :semesterId " +
            "AND week_start_date = :weekStartDate AND scope = :scope LIMIT 1"
    )
    suspend fun findForWeek(
        semesterId: Long,
        weekStartDate: java.time.LocalDate,
        scope: dev.retza.mak.data.entity.WeekOverrideScope
    ): WeekOverrideEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: WeekOverrideEntity): Long

    @Update
    suspend fun update(entity: WeekOverrideEntity)

    @Query("DELETE FROM week_overrides WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface OccurrenceNoteDao {
    @Query(
        "SELECT * FROM occurrence_notes " +
            "WHERE semester_id = :semesterId ORDER BY occurrence_date, class_id, id"
    )
    fun observeForSemester(semesterId: Long): Flow<List<OccurrenceNoteEntity>>

    @Query("SELECT * FROM occurrence_notes WHERE class_id = :classId ORDER BY occurrence_date")
    fun observeForClass(classId: Long): Flow<List<OccurrenceNoteEntity>>

    @Query("SELECT * FROM occurrence_notes WHERE id = :id")
    suspend fun findById(id: Long): OccurrenceNoteEntity?

    @Query("SELECT * FROM occurrence_notes WHERE class_id = :classId AND occurrence_date = :date LIMIT 1")
    suspend fun findForOccurrence(classId: Long, date: java.time.LocalDate): OccurrenceNoteEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: OccurrenceNoteEntity): Long

    @Update
    suspend fun update(entity: OccurrenceNoteEntity)

    @Query("DELETE FROM occurrence_notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface OccurrenceChangeDao {
    @Query(
        "SELECT * FROM occurrence_changes " +
            "WHERE semester_id = :semesterId ORDER BY original_date, class_id, id"
    )
    fun observeForSemester(semesterId: Long): Flow<List<OccurrenceChangeEntity>>

    @Query("SELECT * FROM occurrence_changes WHERE class_id = :classId ORDER BY original_date")
    fun observeForClass(classId: Long): Flow<List<OccurrenceChangeEntity>>

    @Query("SELECT * FROM occurrence_changes WHERE id = :id")
    suspend fun findById(id: Long): OccurrenceChangeEntity?

    @Query("SELECT * FROM occurrence_changes WHERE class_id = :classId AND original_date = :date LIMIT 1")
    suspend fun findForOccurrence(classId: Long, date: java.time.LocalDate): OccurrenceChangeEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: OccurrenceChangeEntity): Long

    @Update
    suspend fun update(entity: OccurrenceChangeEntity)

    @Query("DELETE FROM occurrence_changes WHERE id = :id")
    suspend fun deleteById(id: Long)
}
