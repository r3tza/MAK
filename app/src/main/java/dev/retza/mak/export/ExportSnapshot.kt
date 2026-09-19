package dev.retza.mak.export

import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.CourseEntity
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.TeacherEntity
import dev.retza.mak.data.entity.WeekOverrideEntity
import kotlinx.serialization.Serializable

object ExportSchema {
    const val VERSION = 1
}

@Serializable
data class ExportSnapshot(
    val schemaVersion: Int = ExportSchema.VERSION,
    val semesters: List<SemesterSnapshot> = emptyList()
) {
    companion object {
        fun from(semesters: Collection<SemesterWithData>): ExportSnapshot =
            ExportSnapshot(semesters = semesters.sortedBy { it.semester.id }.map(SemesterSnapshot::from))
    }
}

@Serializable
data class SemesterSnapshot(
    val id: Long,
    val name: String,
    val startDate: String,
    val endDate: String,
    val firstWeekType: String,
    val isActive: Boolean,
    val courses: List<CourseSnapshot>,
    val teachers: List<TeacherSnapshot>,
    val classes: List<ClassSnapshot>,
    val weekOverrides: List<WeekOverrideSnapshot>,
    val occurrenceNotes: List<OccurrenceNoteSnapshot>,
    val occurrenceChanges: List<OccurrenceChangeSnapshot>
) {
    companion object {
        fun from(data: SemesterWithData): SemesterSnapshot =
            SemesterSnapshot(
                id = data.semester.id,
                name = data.semester.name,
                startDate = data.semester.startDate.toString(),
                endDate = data.semester.endDate.toString(),
                firstWeekType = data.semester.firstWeekType.name,
                isActive = data.semester.isActive,
                courses = data.courses.sortedBy { it.id }.map(CourseSnapshot::from),
                teachers = data.teachers.sortedBy { it.id }.map(TeacherSnapshot::from),
                classes = data.classes.sortedBy { it.id }.map(ClassSnapshot::from),
                weekOverrides = data.weekOverrides.sortedBy { it.id }.map(WeekOverrideSnapshot::from),
                occurrenceNotes = data.occurrenceNotes.sortedBy { it.id }.map(OccurrenceNoteSnapshot::from),
                occurrenceChanges = data.occurrenceChanges.sortedBy { it.id }.map(OccurrenceChangeSnapshot::from)
            )
    }
}

@Serializable
data class CourseSnapshot(
    val id: Long,
    val semesterId: Long,
    val name: String,
    val color: String
) {
    companion object {
        fun from(entity: CourseEntity): CourseSnapshot =
            CourseSnapshot(
                id = entity.id,
                semesterId = entity.semesterId,
                name = entity.name,
                color = entity.color
            )
    }
}

@Serializable
data class TeacherSnapshot(
    val id: Long,
    val semesterId: Long,
    val name: String
) {
    companion object {
        fun from(entity: TeacherEntity): TeacherSnapshot =
            TeacherSnapshot(
                id = entity.id,
                semesterId = entity.semesterId,
                name = entity.name
            )
    }
}

@Serializable
data class ClassSnapshot(
    val id: Long,
    val semesterId: Long,
    val name: String,
    val type: String,
    val courseId: Long,
    val teacherId: Long?,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String,
    val room: String?,
    val building: String?,
    val group: String?,
    val recurrence: String,
    val date: String?,
    val classNote: String?
) {
    companion object {
        fun from(entity: ClassEntity): ClassSnapshot =
            ClassSnapshot(
                id = entity.id,
                semesterId = entity.semesterId,
                name = entity.name,
                type = entity.type,
                courseId = entity.courseId,
                teacherId = entity.teacherId,
                dayOfWeek = entity.dayOfWeek.name,
                startTime = entity.startTime.toString(),
                endTime = entity.endTime.toString(),
                room = entity.room,
                building = entity.building,
                group = entity.group,
                recurrence = entity.recurrence.name,
                date = entity.date?.toString(),
                classNote = entity.classNote
            )
    }
}

@Serializable
data class WeekOverrideSnapshot(
    val id: Long,
    val semesterId: Long,
    val weekStartDate: String,
    val weekType: String,
    val scope: String
) {
    companion object {
        fun from(entity: WeekOverrideEntity): WeekOverrideSnapshot =
            WeekOverrideSnapshot(
                id = entity.id,
                semesterId = entity.semesterId,
                weekStartDate = entity.weekStartDate.toString(),
                weekType = entity.weekType.name,
                scope = entity.scope.name
            )
    }
}

@Serializable
data class OccurrenceNoteSnapshot(
    val id: Long,
    val semesterId: Long,
    val classId: Long,
    val occurrenceDate: String,
    val body: String
) {
    companion object {
        fun from(entity: OccurrenceNoteEntity): OccurrenceNoteSnapshot =
            OccurrenceNoteSnapshot(
                id = entity.id,
                semesterId = entity.semesterId,
                classId = entity.classId,
                occurrenceDate = entity.occurrenceDate.toString(),
                body = entity.body
            )
    }
}

@Serializable
data class OccurrenceChangeSnapshot(
    val id: Long,
    val semesterId: Long,
    val classId: Long,
    val originalDate: String,
    val kind: String,
    val targetDate: String?,
    val startTime: String?,
    val endTime: String?,
    val room: String?,
    val building: String?,
    val teacherId: Long?,
    val note: String?
) {
    companion object {
        fun from(entity: OccurrenceChangeEntity): OccurrenceChangeSnapshot =
            OccurrenceChangeSnapshot(
                id = entity.id,
                semesterId = entity.semesterId,
                classId = entity.classId,
                originalDate = entity.originalDate.toString(),
                kind = entity.kind.name,
                targetDate = entity.targetDate?.toString(),
                startTime = entity.newStartTime?.toString(),
                endTime = entity.newEndTime?.toString(),
                room = entity.newRoom,
                building = entity.newBuilding,
                teacherId = entity.newTeacherId,
                note = entity.newNote
            )
    }
}
