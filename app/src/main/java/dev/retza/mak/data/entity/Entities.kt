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
    @ColumnInfo(name = "start_date")
    val startDate: LocalDate,
    @ColumnInfo(name = "end_date")
    val endDate: LocalDate,
    @ColumnInfo(name = "first_week_type")
    val firstWeekType: WeekType,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false
)

@Entity(
    tableName = "courses",
    foreignKeys = [
        ForeignKey(
            entity = SemesterEntity::class,
            parentColumns = ["id"],
            childColumns = ["semester_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["semester_id"])]
)
data class CourseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "semester_id")
    val semesterId: Long,
    val name: String,
    val color: String
)

@Entity(
    tableName = "teachers",
    foreignKeys = [
        ForeignKey(
            entity = SemesterEntity::class,
            parentColumns = ["id"],
            childColumns = ["semester_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["semester_id"])]
)
data class TeacherEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "semester_id")
    val semesterId: Long,
    val name: String
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
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["course_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TeacherEntity::class,
            parentColumns = ["id"],
            childColumns = ["teacher_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["semester_id"]),
        Index(value = ["course_id"]),
        Index(value = ["teacher_id"])
    ]
)
data class ClassEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "semester_id")
    val semesterId: Long,
    val name: String,
    val type: String,
    @ColumnInfo(name = "course_id")
    val courseId: Long,
    @ColumnInfo(name = "teacher_id")
    val teacherId: Long?,
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
        )
    ],
    indices = [
        Index(value = ["semester_id"]),
        Index(value = ["semester_id", "week_start_date", "scope"], unique = true)
    ]
)
data class WeekOverrideEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "semester_id")
    val semesterId: Long,
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
            parentColumns = ["id"],
            childColumns = ["class_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["semester_id"]),
        Index(value = ["class_id"]),
        Index(value = ["semester_id", "class_id", "occurrence_date"], unique = true)
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
            parentColumns = ["id"],
            childColumns = ["class_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TeacherEntity::class,
            parentColumns = ["id"],
            childColumns = ["new_teacher_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["semester_id"]),
        Index(value = ["class_id"]),
        Index(value = ["new_teacher_id"]),
        Index(value = ["semester_id", "class_id", "original_date"], unique = true)
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
    @ColumnInfo(name = "new_teacher_id")
    val newTeacherId: Long?,
    @ColumnInfo(name = "new_note")
    val newNote: String?
)
