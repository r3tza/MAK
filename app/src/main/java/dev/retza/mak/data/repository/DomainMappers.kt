package dev.retza.mak.data.repository

import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.entity.WeekOverrideEntity
import dev.retza.mak.domain.ActivePlanData
import dev.retza.mak.domain.AcademicCalendar
import dev.retza.mak.domain.ClassItem
import dev.retza.mak.domain.OccurrenceChange
import dev.retza.mak.domain.OccurrenceChangeKind
import dev.retza.mak.domain.OccurrenceNote
import dev.retza.mak.domain.Recurrence
import dev.retza.mak.domain.Semester
import dev.retza.mak.domain.SemesterProgram
import dev.retza.mak.domain.StudyProgram
import dev.retza.mak.domain.WeekOverride
import dev.retza.mak.domain.WeekOverrideScope
import dev.retza.mak.domain.WeekType

fun SemesterWithData.toActivePlanData(): ActivePlanData = ActivePlanData(
    semester = semester.toDomain(),
    classes = classes.map(ClassEntity::toDomain),
    courses = studyPrograms.map(StudyProgramEntity::toDomain),
    semesterPrograms = semesterPrograms.map(SemesterProgramEntity::toDomain),
    calendars = academicCalendars.map(AcademicCalendarEntity::toDomain),
    weekOverrides = weekOverrides.map(WeekOverrideEntity::toDomain),
    occurrenceChanges = occurrenceChanges.map(OccurrenceChangeEntity::toDomain),
    occurrenceNotes = occurrenceNotes.map(OccurrenceNoteEntity::toDomain)
)

private fun SemesterEntity.toDomain() = Semester(
    id = id.toString(),
    name = name
)

private fun StudyProgramEntity.toDomain() = StudyProgram(
    id = id.toString(),
    name = name,
    color = color
)

private fun AcademicCalendarEntity.toDomain() = AcademicCalendar(
    id = id.toString(),
    startDate = startDate,
    endDate = endDate,
    firstWeekType = WeekType.valueOf(firstWeekType.name)
)

private fun SemesterProgramEntity.toDomain() = SemesterProgram(
    id = id.toString(),
    semesterId = semesterId.toString(),
    studyProgramId = studyProgramId.toString(),
    academicCalendarId = academicCalendarId.toString()
)

private fun ClassEntity.toDomain() = ClassItem(
    id = id.toString(),
    semesterId = semesterId.toString(),
    semesterProgramId = semesterProgramId.toString(),
    name = name,
    type = type,
    teacherName = teacherName,
    dayOfWeek = dayOfWeek,
    startTime = startTime,
    endTime = endTime,
    room = room,
    building = building,
    group = group,
    recurrence = Recurrence.valueOf(recurrence.name),
    date = date,
    classNote = classNote
)

private fun WeekOverrideEntity.toDomain() = WeekOverride(
    id = id.toString(),
    academicCalendarId = academicCalendarId.toString(),
    weekStartDate = weekStartDate,
    weekType = WeekType.valueOf(weekType.name),
    scope = WeekOverrideScope.valueOf(scope.name)
)

private fun OccurrenceNoteEntity.toDomain() = OccurrenceNote(
    id = id.toString(),
    classId = classId.toString(),
    occurrenceDate = occurrenceDate,
    body = body
)

private fun OccurrenceChangeEntity.toDomain() = OccurrenceChange(
    id = id.toString(),
    classId = classId.toString(),
    originalDate = originalDate,
    kind = OccurrenceChangeKind.valueOf(kind.name),
    targetDate = targetDate,
    startTime = newStartTime,
    endTime = newEndTime,
    room = newRoom,
    building = newBuilding,
    teacherName = newTeacherName,
    note = newNote
)
