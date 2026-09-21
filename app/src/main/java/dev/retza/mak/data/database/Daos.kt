package dev.retza.mak.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.entity.WeekOverrideEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {
    @Query(
        "SELECT s.* FROM semesters s LEFT JOIN academic_calendars c ON c.semester_id = s.id " +
            "GROUP BY s.id ORDER BY MIN(c.start_date) IS NULL, MIN(c.start_date), s.id"
    )
    fun observeAll(): Flow<List<SemesterEntity>>

    @Query("SELECT * FROM semesters WHERE is_active = 1 LIMIT 1")
    fun observeActive(): Flow<SemesterEntity?>

    @Query(
        "SELECT s.* FROM semesters s LEFT JOIN academic_calendars c ON c.semester_id = s.id " +
            "GROUP BY s.id ORDER BY MIN(c.start_date) IS NULL, MIN(c.start_date), s.id"
    )
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
    @Query(
        "SELECT s.* FROM semesters s LEFT JOIN academic_calendars c ON c.semester_id = s.id " +
            "GROUP BY s.id ORDER BY MIN(c.start_date) IS NULL, MIN(c.start_date), s.id"
    )
    suspend fun getAllWithData(): List<SemesterWithData>
}

@Dao
interface StudyProgramDao {
    @Query("SELECT * FROM study_programs ORDER BY name, id")
    fun observeAll(): Flow<List<StudyProgramEntity>>

    @Query("SELECT * FROM study_programs WHERE id = :id")
    suspend fun findById(id: Long): StudyProgramEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: StudyProgramEntity): Long

    @Update
    suspend fun update(entity: StudyProgramEntity)

    @Query("DELETE FROM study_programs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM semester_programs WHERE study_program_id = :id")
    suspend fun countAssignments(id: Long): Int
}

@Dao
interface AcademicCalendarDao {
    @Query("SELECT * FROM academic_calendars WHERE semester_id = :semesterId ORDER BY start_date, id")
    fun observeForSemester(semesterId: Long): Flow<List<AcademicCalendarEntity>>

    @Query("SELECT * FROM academic_calendars WHERE semester_id = :semesterId ORDER BY start_date, id")
    suspend fun getForSemester(semesterId: Long): List<AcademicCalendarEntity>

    @Query("SELECT * FROM academic_calendars WHERE id = :id")
    suspend fun findById(id: Long): AcademicCalendarEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: AcademicCalendarEntity): Long

    @Update
    suspend fun update(entity: AcademicCalendarEntity)

    @Query("DELETE FROM academic_calendars WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM semester_programs WHERE academic_calendar_id = :id")
    suspend fun countAssignments(id: Long): Int
}

@Dao
interface SemesterProgramDao {
    @Query("SELECT * FROM semester_programs WHERE semester_id = :semesterId ORDER BY id")
    fun observeForSemester(semesterId: Long): Flow<List<SemesterProgramEntity>>

    @Query(
        "SELECT * FROM semester_programs WHERE semester_id = :semesterId " +
            "AND study_program_id = :studyProgramId LIMIT 1"
    )
    suspend fun findByProgram(semesterId: Long, studyProgramId: Long): SemesterProgramEntity?

    @Query("SELECT * FROM semester_programs WHERE id = :id")
    suspend fun findById(id: Long): SemesterProgramEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: SemesterProgramEntity): Long

    @Update
    suspend fun update(entity: SemesterProgramEntity)

    @Query("DELETE FROM semester_programs WHERE id = :id")
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

    @Query(
        "SELECT * FROM week_overrides " +
            "WHERE academic_calendar_id = :calendarId ORDER BY week_start_date, scope, id"
    )
    fun observeForCalendar(calendarId: Long): Flow<List<WeekOverrideEntity>>

    @Query("SELECT * FROM week_overrides WHERE id = :id")
    suspend fun findById(id: Long): WeekOverrideEntity?

    @Query(
        "SELECT * FROM week_overrides WHERE academic_calendar_id = :calendarId " +
            "AND week_start_date = :weekStartDate AND scope = :scope LIMIT 1"
    )
    suspend fun findForWeek(
        calendarId: Long,
        weekStartDate: LocalDate,
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
    suspend fun findForOccurrence(classId: Long, date: LocalDate): OccurrenceNoteEntity?

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
    suspend fun findForOccurrence(classId: Long, date: LocalDate): OccurrenceChangeEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: OccurrenceChangeEntity): Long

    @Update
    suspend fun update(entity: OccurrenceChangeEntity)

    @Query("DELETE FROM occurrence_changes WHERE id = :id")
    suspend fun deleteById(id: Long)
}
