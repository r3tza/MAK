package dev.retza.mak.sync

import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.Recurrence
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.data.repository.SemesterBackup
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

internal val TODAY: LocalDate = LocalDate.of(2026, 10, 8)

/** One semester, one study program and the given classes and notes. */
internal fun basePlan(
    classes: List<ClassEntity> = listOf(classRow(1, "Konsultacje", DayOfWeek.THURSDAY, 16)),
    notes: List<OccurrenceNoteEntity> = emptyList(),
    programs: List<StudyProgramEntity> = listOf(StudyProgramEntity(id = 1, name = "Informatyka", color = "#137B71")),
    assignments: List<SemesterProgramEntity> = listOf(SemesterProgramEntity(id = 1, semesterId = 1, studyProgramId = 1, academicCalendarId = 1)),
    active: Boolean = true
): BackupData = BackupData(
    studyPrograms = programs,
    semesters = listOf(
        SemesterBackup(
            semester = SemesterEntity(id = 1, name = "Zimowy", isActive = active),
            calendars = listOf(AcademicCalendarEntity(1, 1, LocalDate.of(2026, 10, 1), LocalDate.of(2027, 2, 28), WeekType.A)),
            programs = assignments,
            classes = classes,
            weekOverrides = emptyList(),
            occurrenceNotes = notes,
            occurrenceChanges = emptyList()
        )
    )
)

internal fun classRow(
    id: Long,
    name: String,
    day: DayOfWeek,
    hour: Int,
    room: String? = "A12",
    semesterProgramId: Long = 1
) = ClassEntity(
    id = id,
    semesterId = 1,
    semesterProgramId = semesterProgramId,
    name = name,
    type = "Wykład",
    teacherName = null,
    dayOfWeek = day,
    startTime = LocalTime.of(hour, 0),
    endTime = LocalTime.of(hour + 1, 0),
    room = room,
    building = null,
    group = null,
    recurrence = Recurrence.EVERY_WEEK,
    date = null,
    classNote = null
)

internal fun noteRow(id: Long, classId: Long, body: String, date: LocalDate = TODAY) =
    OccurrenceNoteEntity(id = id, semesterId = 1, classId = classId, occurrenceDate = date, body = body)
