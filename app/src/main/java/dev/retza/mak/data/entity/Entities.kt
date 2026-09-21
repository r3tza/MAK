package dev.retza.mak.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "semesters")
data class SemesterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false
)

@Entity(tableName = "study_programs")
data class StudyProgramEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: String
)

@Entity(
    tableName = "academic_calendars",
    foreignKeys = [
        ForeignKey(
            entity = SemesterEntity::class,
            parentColumns = ["id"],
            childColumns = ["semester_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["semester_id"]),
        Index(value = ["id", "semester_id"], unique = true)
    ]
)
data class AcademicCalendarEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "semester_id")
    val semesterId: Long,
    @ColumnInfo(name = "start_date")
    val startDate: LocalDate,
    @ColumnInfo(name = "end_date")
    val endDate: LocalDate,
    @ColumnInfo(name = "first_week_type")
    val firstWeekType: WeekType
)

@Entity(
    tableName = "semester_programs",
    foreignKeys = [
        ForeignKey(
            entity = SemesterEntity::class,
            parentColumns = ["id"],
            childColumns = ["semester_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StudyProgramEntity::class,
            parentColumns = ["id"],
            childColumns = ["study_program_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AcademicCalendarEntity::class,
            parentColumns = ["id", "semester_id"],
            childColumns = ["academic_calendar_id", "semester_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["semester_id"]),
        Index(value = ["study_program_id"]),
        Index(value = ["academic_calendar_id"]),
        Index(value = ["semester_id", "study_program_id"], unique = true),
        Index(value = ["id", "semester_id"], unique = true),
        Index(value = ["academic_calendar_id", "semester_id"])
    ]
)
data class SemesterProgramEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "semester_id")
    val semesterId: Long,
    @ColumnInfo(name = "study_program_id")
    val studyProgramId: Long,
    @ColumnInfo(name = "academic_calendar_id")
    val academicCalendarId: Long
)

@Entity(
    tableName = "classes",
    foreignKeys = [
        ForeignKey(
            entity = SemesterEntity::class,
            parentColumns = ["id"],
            childColumns = ["semester_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SemesterProgramEntity::class,
            parentColumns = ["id", "semester_id"],
            childColumns = ["semester_program_id", "semester_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["semester_id"]),
        Index(value = ["semester_program_id"]),
        Index(value = ["id", "semester_id"], unique = true),
        Index(value = ["semester_program_id", "semester_id"])
    ]
)
data class ClassEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "semester_id")
    val semesterId: Long,
    @ColumnInfo(name = "semester_program_id")
    val semesterProgramId: Long,
    val name: String,
    val type: String,
    @ColumnInfo(name = "teacher_name")
    val teacherName: String?,
    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: DayOfWeek,
    @ColumnInfo(name = "start_time")
    val startTime: LocalTime,
    @ColumnInfo(name = "end_time")
    val endTime: LocalTime,
    val room: String?,
    val building: String?,
    val group: String?,
    val recurrence: Recurrence,
    val date: LocalDate?,
    @ColumnInfo(name = "class_note")
    val classNote: String?
)

@Entity(
    tableName = "week_overrides",
    foreignKeys = [
        ForeignKey(
            entity = SemesterEntity::class,
            parentColumns = ["id"],
            childColumns = ["semester_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AcademicCalendarEntity::class,
            parentColumns = ["id", "semester_id"],
            childColumns = ["academic_calendar_id", "semester_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["semester_id"]),
        Index(value = ["academic_calendar_id"]),
        Index(value = ["academic_calendar_id", "week_start_date", "scope"], unique = true),
        Index(value = ["academic_calendar_id", "semester_id"])
    ]
)
data class WeekOverrideEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "semester_id")
    val semesterId: Long,
    @ColumnInfo(name = "academic_calendar_id")
    val academicCalendarId: Long,
    @ColumnInfo(name = "week_start_date")
    val weekStartDate: LocalDate,
    @ColumnInfo(name = "week_type")
    val weekType: WeekType,
    val scope: WeekOverrideScope
)

@Entity(
    tableName = "occurrence_notes",
    foreignKeys = [
        ForeignKey(
            entity = SemesterEntity::class,
            parentColumns = ["id"],
            childColumns = ["semester_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id", "semester_id"],
            childColumns = ["class_id", "semester_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["semester_id"]),
        Index(value = ["class_id"]),
        Index(value = ["semester_id", "class_id", "occurrence_date"], unique = true),
        Index(value = ["class_id", "semester_id"])
    ]
)
data class OccurrenceNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "semester_id")
    val semesterId: Long,
    @ColumnInfo(name = "class_id")
    val classId: Long,
    @ColumnInfo(name = "occurrence_date")
    val occurrenceDate: LocalDate,
    val body: String
)

@Entity(
    tableName = "occurrence_changes",
    foreignKeys = [
        ForeignKey(
            entity = SemesterEntity::class,
            parentColumns = ["id"],
            childColumns = ["semester_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id", "semester_id"],
            childColumns = ["class_id", "semester_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["semester_id"]),
        Index(value = ["class_id"]),
        Index(value = ["semester_id", "class_id", "original_date"], unique = true),
        Index(value = ["class_id", "semester_id"])
    ]
)
data class OccurrenceChangeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "semester_id")
    val semesterId: Long,
    @ColumnInfo(name = "class_id")
    val classId: Long,
    @ColumnInfo(name = "original_date")
    val originalDate: LocalDate,
    val kind: OccurrenceChangeKind,
    @ColumnInfo(name = "target_date")
    val targetDate: LocalDate?,
    @ColumnInfo(name = "new_start_time")
    val newStartTime: LocalTime?,
    @ColumnInfo(name = "new_end_time")
    val newEndTime: LocalTime?,
    @ColumnInfo(name = "new_room")
    val newRoom: String?,
    @ColumnInfo(name = "new_building")
    val newBuilding: String?,
    @ColumnInfo(name = "new_teacher_name")
    val newTeacherName: String?,
    @ColumnInfo(name = "new_note")
    val newNote: String?
)
