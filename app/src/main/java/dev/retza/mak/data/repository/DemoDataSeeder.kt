package dev.retza.mak.data.repository

import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceChangeKind
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.Recurrence
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.entity.WeekOverrideEntity
import dev.retza.mak.data.entity.WeekOverrideScope
import dev.retza.mak.data.entity.WeekType
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.first

/**
 * Seeds a useful preview dataset only when the local database is empty.
 */
suspend fun MakRepository.seedDemoDataIfEmpty(clock: Clock = Clock.systemDefaultZone()) {
    if (observeSemesters().first().isNotEmpty()) return

    val today = LocalDate.now(clock)
    val semesterStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(4)
    val semesterEnd = semesterStart.plusWeeks(22).plusDays(6)
    val semesterId = saveSemester(
        SemesterEntity(
            name = "Semestr demonstracyjny 2026/27",
            isActive = true
        )
    )
    val calendarId = saveCalendar(
        AcademicCalendarEntity(
            semesterId = semesterId,
            startDate = semesterStart,
            endDate = semesterEnd,
            firstWeekType = WeekType.A
        )
    )

    val computerScienceId = saveStudyProgram(
        StudyProgramEntity(name = "Informatyka", color = "#137B71")
    )
    val managementId = saveStudyProgram(
        StudyProgramEntity(name = "Zarządzanie", color = "#334FCE")
    )
    val computerScienceProgramId = saveSemesterProgram(
        SemesterProgramEntity(
            semesterId = semesterId,
            studyProgramId = computerScienceId,
            academicCalendarId = calendarId
        )
    )
    val managementProgramId = saveSemesterProgram(
        SemesterProgramEntity(
            semesterId = semesterId,
            studyProgramId = managementId,
            academicCalendarId = calendarId
        )
    )

    saveClass(
        ClassEntity(
            semesterId = semesterId,
            semesterProgramId = computerScienceProgramId,
            name = "Programowanie aplikacji",
            type = "Laboratorium",
            teacherName = "dr Anna Nowak",
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(9, 30),
            room = "L204",
            building = "Budynek A",
            group = "Grupa 1",
            recurrence = Recurrence.EVERY_WEEK,
            date = null,
            classNote = "Przynieś laptop i konto testowe."
        )
    )
    saveClass(
        ClassEntity(
            semesterId = semesterId,
            semesterProgramId = computerScienceProgramId,
            name = "Projekt zespołowy",
            type = "Projekt",
            teacherName = "prof. Jan Kowalski",
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 30),
            room = "L205",
            building = "Budynek A",
            group = "Grupa 1",
            recurrence = Recurrence.EVERY_WEEK,
            date = null,
            classNote = null
        )
    )
    val analyticsId = saveClass(
        ClassEntity(
            semesterId = semesterId,
            semesterProgramId = managementProgramId,
            name = "Analiza danych",
            type = "Wykład",
            teacherName = "prof. Jan Kowalski",
            dayOfWeek = DayOfWeek.WEDNESDAY,
            startTime = LocalTime.of(12, 15),
            endTime = LocalTime.of(13, 45),
            room = "A101",
            building = "Budynek B",
            group = null,
            recurrence = Recurrence.EVERY_WEEK,
            date = null,
            classNote = null
        )
    )
    saveClass(
        ClassEntity(
            semesterId = semesterId,
            semesterProgramId = computerScienceProgramId,
            name = "Warsztat UX",
            type = "Ćwiczenia",
            teacherName = "dr Anna Nowak",
            dayOfWeek = DayOfWeek.THURSDAY,
            startTime = LocalTime.of(14, 0),
            endTime = LocalTime.of(15, 30),
            room = "C12",
            building = "Budynek C",
            group = "Tydzień A",
            recurrence = Recurrence.A_WEEK,
            date = null,
            classNote = null
        )
    )
    val oneOffId = saveClass(
        ClassEntity(
            semesterId = semesterId,
            semesterProgramId = managementProgramId,
            name = "Konsultacje przed kolokwium",
            type = "Jednorazowe",
            teacherName = "dr Anna Nowak",
            dayOfWeek = today.dayOfWeek,
            startTime = LocalTime.of(16, 0),
            endTime = LocalTime.of(17, 0),
            room = "A12",
            building = "Budynek B",
            group = null,
            recurrence = Recurrence.ONCE,
            date = today,
            classNote = "Przykład zajęć jednorazowych."
        )
    )

    val currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    saveWeekOverride(
        WeekOverrideEntity(
            semesterId = semesterId,
            academicCalendarId = calendarId,
            weekStartDate = currentMonday,
            weekType = WeekType.B,
            scope = WeekOverrideScope.ONE_WEEK
        )
    )

    saveOccurrenceNote(
        OccurrenceNoteEntity(
            semesterId = semesterId,
            classId = oneOffId,
            occurrenceDate = today,
            body = "Przykład notatki przypiętej do konkretnego terminu."
        )
    )

    val changeDate = nextDate(today, DayOfWeek.WEDNESDAY)
    saveOccurrenceChange(
        OccurrenceChangeEntity(
            semesterId = semesterId,
            classId = analyticsId,
            originalDate = changeDate,
            kind = OccurrenceChangeKind.MODIFIED,
            targetDate = changeDate.plusDays(1),
            newStartTime = LocalTime.of(15, 0),
            newEndTime = LocalTime.of(16, 30),
            newRoom = "A204",
            newBuilding = "Budynek B",
            newTeacherName = null,
            newNote = "Przykład zmienionego i przeniesionego terminu."
        )
    )
}

private fun nextDate(from: LocalDate, dayOfWeek: DayOfWeek): LocalDate {
    val candidate = from.with(TemporalAdjusters.nextOrSame(dayOfWeek))
    return if (candidate == from) candidate.plusWeeks(1) else candidate
}
