package dev.retza.mak.data.repository

import dev.retza.mak.domain.OccurrenceChangeKind
import dev.retza.mak.domain.Recurrence
import dev.retza.mak.domain.WeekOverrideScope
import dev.retza.mak.domain.WeekType
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.first

/**
 * Seeds a useful preview dataset only when the local database is empty.
 */
suspend fun seedDemoDataIfEmpty(
    semesterRepository: SemesterRepository,
    scheduleRepository: ScheduleRepository,
    clock: Clock = Clock.systemDefaultZone()
) {
    if (semesterRepository.observeSemesters().first().isNotEmpty()) return

    val today = LocalDate.now(clock)
    val semesterStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(4)
    val semesterEnd = semesterStart.plusWeeks(22).plusDays(6)
    val semesterId = semesterRepository.saveSemester(
        SemesterRecord(name = "Semestr demonstracyjny 2026/27", isActive = true)
    )
    val calendarId = semesterRepository.saveCalendar(
        AcademicCalendarRecord(
            semesterId = semesterId,
            startDate = semesterStart,
            endDate = semesterEnd,
            firstWeekType = WeekType.A
        )
    )

    val computerScienceId = semesterRepository.saveStudyProgram(
        StudyProgramRecord(name = "Informatyka", color = "#137B71")
    )
    val managementId = semesterRepository.saveStudyProgram(
        StudyProgramRecord(name = "Zarządzanie", color = "#334FCE")
    )
    val computerScienceProgramId = semesterRepository.saveSemesterProgram(
        SemesterProgramRecord(
            semesterId = semesterId,
            studyProgramId = computerScienceId,
            academicCalendarId = calendarId
        )
    )
    val managementProgramId = semesterRepository.saveSemesterProgram(
        SemesterProgramRecord(
            semesterId = semesterId,
            studyProgramId = managementId,
            academicCalendarId = calendarId
        )
    )

    scheduleRepository.saveClass(
        ClassRecord(
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
    scheduleRepository.saveClass(
        ClassRecord(
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
    val analyticsId = scheduleRepository.saveClass(
        ClassRecord(
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
    scheduleRepository.saveClass(
        ClassRecord(
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
    val oneOffId = scheduleRepository.saveClass(
        ClassRecord(
            semesterId = semesterId,
            semesterProgramId = managementProgramId,
            name = "Konsultacje przed kolokwium",
            type = "Inne",
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
    semesterRepository.saveWeekOverride(
        WeekOverrideRecord(
            semesterId = semesterId,
            academicCalendarId = calendarId,
            weekStartDate = currentMonday,
            weekType = WeekType.B,
            scope = WeekOverrideScope.ONE_WEEK
        )
    )

    scheduleRepository.saveOccurrenceNote(
        OccurrenceNoteRecord(
            semesterId = semesterId,
            classId = oneOffId,
            occurrenceDate = today,
            body = "Przykład notatki przypiętej do konkretnego terminu."
        )
    )

    val changeDate = nextDate(today, DayOfWeek.WEDNESDAY)
    scheduleRepository.saveOccurrenceChange(
        OccurrenceChangeRecord(
            semesterId = semesterId,
            classId = analyticsId,
            originalDate = changeDate,
            kind = OccurrenceChangeKind.MODIFIED,
            targetDate = changeDate.plusDays(1),
            startTime = LocalTime.of(15, 0),
            endTime = LocalTime.of(16, 30),
            room = "A204",
            building = "Budynek B",
            teacherName = null,
            note = "Przykład zmienionego i przeniesionego terminu."
        )
    )
}

private fun nextDate(from: LocalDate, dayOfWeek: DayOfWeek): LocalDate {
    val candidate = from.with(TemporalAdjusters.nextOrSame(dayOfWeek))
    return if (candidate == from) candidate.plusWeeks(1) else candidate
}
