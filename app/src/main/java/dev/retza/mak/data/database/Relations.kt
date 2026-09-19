package dev.retza.mak.data.database

import androidx.room.Embedded
import androidx.room.Relation
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.CourseEntity
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.TeacherEntity
import dev.retza.mak.data.entity.WeekOverrideEntity

data class SemesterWithData(
    @Embedded
    val semester: SemesterEntity,
    @Relation(parentColumn = "id", entityColumn = "semester_id")
    val courses: List<CourseEntity>,
    @Relation(parentColumn = "id", entityColumn = "semester_id")
    val teachers: List<TeacherEntity>,
    @Relation(parentColumn = "id", entityColumn = "semester_id")
    val classes: List<ClassEntity>,
    @Relation(parentColumn = "id", entityColumn = "semester_id")
    val weekOverrides: List<WeekOverrideEntity>,
    @Relation(parentColumn = "id", entityColumn = "semester_id")
    val occurrenceNotes: List<OccurrenceNoteEntity>,
    @Relation(parentColumn = "id", entityColumn = "semester_id")
    val occurrenceChanges: List<OccurrenceChangeEntity>
)

data class ClassWithDetails(
    @Embedded
    val classEntity: ClassEntity,
    @Relation(parentColumn = "course_id", entityColumn = "id")
    val course: CourseEntity,
    @Relation(parentColumn = "teacher_id", entityColumn = "id")
    val teacher: TeacherEntity?,
    @Relation(parentColumn = "id", entityColumn = "class_id")
    val occurrenceNotes: List<OccurrenceNoteEntity>,
    @Relation(parentColumn = "id", entityColumn = "class_id")
    val occurrenceChanges: List<OccurrenceChangeEntity>
)
