package dev.retza.mak.export

import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.data.repository.SemesterBackup
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.entity.WeekOverrideEntity
import kotlinx.serialization.Serializable

object ExportSchema {
    const val VERSION = 2
}

@Serializable
data class ExportSnapshot(
    val schemaVersion: Int = ExportSchema.VERSION,
    val studyPrograms: List<StudyProgramSnapshot> = emptyList(),
    val semesters: List<SemesterSnapshot> = emptyList()
) {
    companion object {
        fun from(
            semesters: Collection<SemesterWithData>,
            studyPrograms: Collection<StudyProgramEntity>
        ): ExportSnapshot =
            ExportSnapshot(
                studyPrograms = studyPrograms
                    .distinctBy { it.id }
                    .sortedBy { it.id }
                    .map(StudyProgramSnapshot::from),
                semesters = semesters.sortedBy { it.semester.id }.map(SemesterSnapshot::from)
            )

        fun from(data: BackupData): ExportSnapshot =
            ExportSnapshot(
                studyPrograms = data.studyPrograms
                    .distinctBy { it.id }
                    .sortedBy { it.id }
                    .map(StudyProgramSnapshot::from),
                semesters = data.semesters.sortedBy { it.semester.id }.map(SemesterSnapshot::from)
            )
    }
}

@Serializable
data class StudyProgramSnapshot(
    val id: Long,
    val name: String,
    val color: String
) {
    companion object {
        fun from(entity: StudyProgramEntity): StudyProgramSnapshot =
            StudyProgramSnapshot(
                id = entity.id,
                name = entity.name,
                color = entity.color
            )
    }
}

@Serializable
data class SemesterSnapshot(
    val id: Long,
    val name: String,
    val isActive: Boolean,
    val calendars: List<AcademicCalendarSnapshot>,
    val programs: List<SemesterProgramSnapshot>,
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
                isActive = data.semester.isActive,
                calendars = data.academicCalendars.sortedBy { it.id }.map(AcademicCalendarSnapshot::from),
                programs = data.semesterPrograms.sortedBy { it.id }.map(SemesterProgramSnapshot::from),
                classes = data.classes.sortedBy { it.id }.map(ClassSnapshot::from),
                weekOverrides = data.weekOverrides.sortedBy { it.id }.map(WeekOverrideSnapshot::from),
                occurrenceNotes = data.occurrenceNotes.sortedBy { it.id }.map(OccurrenceNoteSnapshot::from),
                occurrenceChanges = data.occurrenceChanges.sortedBy { it.id }.map(OccurrenceChangeSnapshot::from)
            )

        fun from(backup: SemesterBackup): SemesterSnapshot =
            SemesterSnapshot(
                id = backup.semester.id,
                name = backup.semester.name,
                isActive = backup.semester.isActive,
                calendars = backup.calendars.sortedBy { it.id }.map(AcademicCalendarSnapshot::from),
                programs = backup.programs.sortedBy { it.id }.map(SemesterProgramSnapshot::from),
                classes = backup.classes.sortedBy { it.id }.map(ClassSnapshot::from),
                weekOverrides = backup.weekOverrides.sortedBy { it.id }.map(WeekOverrideSnapshot::from),
                occurrenceNotes = backup.occurrenceNotes.sortedBy { it.id }.map(OccurrenceNoteSnapshot::from),
                occurrenceChanges = backup.occurrenceChanges.sortedBy { it.id }.map(OccurrenceChangeSnapshot::from)
            )
    }
}

@Serializable
data class AcademicCalendarSnapshot(
    val id: Long,
    val semesterId: Long,
    val startDate: String,
    val endDate: String,
    val firstWeekType: String
) {
    companion object {
        fun from(entity: AcademicCalendarEntity): AcademicCalendarSnapshot =
            AcademicCalendarSnapshot(
                id = entity.id,
                semesterId = entity.semesterId,
                startDate = entity.startDate.toString(),
                endDate = entity.endDate.toString(),
                firstWeekType = entity.firstWeekType.name
            )
    }
}

@Serializable
data class SemesterProgramSnapshot(
    val id: Long,
    val semesterId: Long,
    val studyProgramId: Long,
    val academicCalendarId: Long
) {
    companion object {
        fun from(entity: SemesterProgramEntity): SemesterProgramSnapshot =
            SemesterProgramSnapshot(
                id = entity.id,
                semesterId = entity.semesterId,
                studyProgramId = entity.studyProgramId,
                academicCalendarId = entity.academicCalendarId
            )
    }
}

@Serializable
data class ClassSnapshot(
    val id: Long,
    val semesterId: Long,
    val semesterProgramId: Long,
    val name: String,
    val type: String,
    val teacherName: String?,
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
                semesterProgramId = entity.semesterProgramId,
                name = entity.name,
                type = entity.type,
                teacherName = entity.teacherName,
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
    val academicCalendarId: Long,
    val weekStartDate: String,
    val weekType: String,
    val scope: String
) {
    companion object {
        fun from(entity: WeekOverrideEntity): WeekOverrideSnapshot =
            WeekOverrideSnapshot(
                id = entity.id,
                semesterId = entity.semesterId,
                academicCalendarId = entity.academicCalendarId,
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
    val teacherName: String?,
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
                teacherName = entity.newTeacherName,
                note = entity.newNote
            )
    }
}
