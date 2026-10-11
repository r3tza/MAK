package dev.retza.mak.export

import dev.retza.mak.data.repository.AcademicCalendarRecord
import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.data.repository.SemesterBackup
import dev.retza.mak.data.repository.ClassRecord
import dev.retza.mak.data.repository.OccurrenceChangeRecord
import dev.retza.mak.data.repository.OccurrenceNoteRecord
import dev.retza.mak.data.repository.SemesterProgramRecord
import dev.retza.mak.data.repository.StudyProgramRecord
import dev.retza.mak.data.repository.WeekOverrideRecord
import kotlinx.serialization.Serializable

object ExportSchema {
    const val VERSION = 3

    /** Version 2 stored occurrence notes under the actual date of a moved occurrence. */
    const val NOTES_ON_ACTUAL_DATE_VERSION = 2
}

@Serializable
data class ExportSnapshot(
    val schemaVersion: Int = ExportSchema.VERSION,
    val studyPrograms: List<StudyProgramSnapshot> = emptyList(),
    val semesters: List<SemesterSnapshot> = emptyList()
) {
    companion object {
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
        fun from(record: StudyProgramRecord): StudyProgramSnapshot =
            StudyProgramSnapshot(
                id = record.id,
                name = record.name,
                color = record.color
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
        fun from(record: AcademicCalendarRecord): AcademicCalendarSnapshot =
            AcademicCalendarSnapshot(
                id = record.id,
                semesterId = record.semesterId,
                startDate = record.startDate.toString(),
                endDate = record.endDate.toString(),
                firstWeekType = record.firstWeekType.name
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
        fun from(record: SemesterProgramRecord): SemesterProgramSnapshot =
            SemesterProgramSnapshot(
                id = record.id,
                semesterId = record.semesterId,
                studyProgramId = record.studyProgramId,
                academicCalendarId = record.academicCalendarId
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
        fun from(record: ClassRecord): ClassSnapshot =
            ClassSnapshot(
                id = record.id,
                semesterId = record.semesterId,
                semesterProgramId = record.semesterProgramId,
                name = record.name,
                type = record.type,
                teacherName = record.teacherName,
                dayOfWeek = record.dayOfWeek.name,
                startTime = record.startTime.toString(),
                endTime = record.endTime.toString(),
                room = record.room,
                building = record.building,
                group = record.group,
                recurrence = record.recurrence.name,
                date = record.date?.toString(),
                classNote = record.classNote
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
        fun from(record: WeekOverrideRecord): WeekOverrideSnapshot =
            WeekOverrideSnapshot(
                id = record.id,
                semesterId = record.semesterId,
                academicCalendarId = record.academicCalendarId,
                weekStartDate = record.weekStartDate.toString(),
                weekType = record.weekType.name,
                scope = record.scope.name
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
        fun from(record: OccurrenceNoteRecord): OccurrenceNoteSnapshot =
            OccurrenceNoteSnapshot(
                id = record.id,
                semesterId = record.semesterId,
                classId = record.classId,
                occurrenceDate = record.occurrenceDate.toString(),
                body = record.body
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
        fun from(record: OccurrenceChangeRecord): OccurrenceChangeSnapshot =
            OccurrenceChangeSnapshot(
                id = record.id,
                semesterId = record.semesterId,
                classId = record.classId,
                originalDate = record.originalDate.toString(),
                kind = record.kind.name,
                targetDate = record.targetDate?.toString(),
                startTime = record.startTime?.toString(),
                endTime = record.endTime?.toString(),
                room = record.room,
                building = record.building,
                teacherName = record.teacherName,
                note = record.note
            )
    }
}
