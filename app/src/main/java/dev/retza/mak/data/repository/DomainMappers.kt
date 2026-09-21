package dev.retza.mak.data.repository

import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.WeekOverrideEntity
import dev.retza.mak.domain.ActivePlanData
import dev.retza.mak.domain.ClassItem
import dev.retza.mak.domain.OccurrenceChange
import dev.retza.mak.domain.OccurrenceChangeKind
import dev.retza.mak.domain.OccurrenceNote
import dev.retza.mak.domain.Recurrence
import dev.retza.mak.domain.Semester
import dev.retza.mak.domain.StudyProgram
import dev.retza.mak.domain.Teacher
import dev.retza.mak.domain.WeekOverride
import dev.retza.mak.domain.WeekOverrideScope
import dev.retza.mak.domain.WeekType

fun SemesterWithData.toActivePlanData(): ActivePlanData = ActivePlanData(
    semester = semester.toDomain(),
    classes = classes.map(ClassEntity::toDomain),
    courses = courses.map { it.toDomain() },
    teachers = teachers.map { it.toDomain() },
    weekOverrides = weekOverrides.map(WeekOverrideEntity::toDomain),
    occurrenceChanges = occurrenceChanges.map(OccurrenceChangeEntity::toDomain),
    occurrenceNotes = occurrenceNotes.map(OccurrenceNoteEntity::toDomain)
)

private fun dev.retza.mak.data.entity.SemesterEntity.toDomain() = Semester(
    id = id.toString(),
    name = name,
    startDate = startDate,
    endDate = endDate,
    firstWeekType = WeekType.valueOf(firstWeekType.name)
)

private fun dev.retza.mak.data.entity.CourseEntity.toDomain() =
    StudyProgram(id.toString(), name, color)

private fun dev.retza.mak.data.entity.TeacherEntity.toDomain() =
    Teacher(id.toString(), semesterId.toString(), name)

private fun ClassEntity.toDomain() = ClassItem(
    id = id.toString(),
    semesterId = semesterId.toString(),
    name = name,
    type = type,
    courseId = courseId.toString(),
    teacherId = teacherId?.toString(),
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
    semesterId = semesterId.toString(),
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
    teacherId = newTeacherId?.toString(),
    note = newNote
)
