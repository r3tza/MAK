package dev.retza.mak.data

import dev.retza.mak.data.repository.AcademicCalendarRecord
import dev.retza.mak.data.repository.BackupData
import dev.retza.mak.data.repository.ClassRecord
import dev.retza.mak.data.repository.FreeDayRecord
import dev.retza.mak.data.repository.OccurrenceChangeRecord
import dev.retza.mak.data.repository.OccurrenceNoteRecord
import dev.retza.mak.data.repository.SemesterBackup
import dev.retza.mak.data.repository.SemesterProgramRecord
import dev.retza.mak.data.repository.SemesterRecord
import dev.retza.mak.data.repository.StudyProgramRecord
import dev.retza.mak.data.repository.WeekOverrideRecord
import dev.retza.mak.domain.OccurrenceChangeKind
import dev.retza.mak.domain.Recurrence
import dev.retza.mak.domain.WeekOverrideScope
import dev.retza.mak.domain.WeekType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * A plan that uses every table and every kind of stored value: both recurrences, a one-off class, both
 * override scopes, a named and an unnamed free day, a cancelled, a changed and a moved occurrence, an explicitly
 * cleared room and a study program without an assignment. Semester 1 is active; semester 2 has no programs.
 */
object RoomTestPlan {
    const val ACTIVE_SEMESTER = 1L
    const val OTHER_SEMESTER = 2L
    const val PROGRAM = 10L
    const val UNASSIGNED_PROGRAM = 11L
    const val CALENDAR = 101L
    const val UNUSED_CALENDAR = 102L
    const val OTHER_SEMESTER_CALENDAR = 103L
    const val ASSIGNMENT = 201L
    const val WEEKLY_CLASS = 301L
    const val EXAM = 302L
    const val NOTE = 601L
    val noteDate: LocalDate = LocalDate.of(2026, 10, 5)

    fun data(): BackupData = BackupData(
        studyPrograms = listOf(
            StudyProgramRecord(id = PROGRAM, name = "Informatyka", color = "#112233"),
            StudyProgramRecord(id = UNASSIGNED_PROGRAM, name = "Matematyka", color = "#445566")
        ),
        semesters = listOf(
            SemesterBackup(
                semester = SemesterRecord(id = ACTIVE_SEMESTER, name = "Zimowy", isActive = true),
                calendars = listOf(
                    AcademicCalendarRecord(CALENDAR, ACTIVE_SEMESTER, LocalDate.of(2026, 10, 1), LocalDate.of(2027, 2, 28), WeekType.A),
                    AcademicCalendarRecord(UNUSED_CALENDAR, ACTIVE_SEMESTER, LocalDate.of(2026, 10, 5), LocalDate.of(2027, 1, 31), WeekType.B)
                ),
                programs = listOf(SemesterProgramRecord(ASSIGNMENT, ACTIVE_SEMESTER, PROGRAM, CALENDAR)),
                classes = listOf(
                    ClassRecord(
                        id = WEEKLY_CLASS,
                        semesterId = ACTIVE_SEMESTER,
                        semesterProgramId = ASSIGNMENT,
                        name = "Programowanie",
                        type = "Wykład",
                        teacherName = "Jan Kowalski",
                        dayOfWeek = DayOfWeek.MONDAY,
                        startTime = LocalTime.of(8, 15),
                        endTime = LocalTime.of(9, 45),
                        room = "L204",
                        building = "B1",
                        group = "G1",
                        recurrence = Recurrence.A_WEEK,
                        date = null,
                        classNote = "Notatka wspólna"
                    ),
                    ClassRecord(
                        id = EXAM,
                        semesterId = ACTIVE_SEMESTER,
                        semesterProgramId = ASSIGNMENT,
                        name = "Programowanie",
                        type = "Egzamin",
                        teacherName = null,
                        dayOfWeek = DayOfWeek.MONDAY,
                        startTime = LocalTime.of(10, 0),
                        endTime = LocalTime.of(12, 0),
                        room = null,
                        building = null,
                        group = null,
                        recurrence = Recurrence.ONCE,
                        date = LocalDate.of(2027, 1, 25),
                        classNote = null
                    )
                ),
                weekOverrides = listOf(
                    WeekOverrideRecord(401, ACTIVE_SEMESTER, CALENDAR, LocalDate.of(2026, 10, 12), WeekType.B, WeekOverrideScope.ONE_WEEK),
                    WeekOverrideRecord(402, ACTIVE_SEMESTER, CALENDAR, LocalDate.of(2026, 11, 16), WeekType.A, WeekOverrideScope.FROM_WEEK)
                ),
                freeDays = listOf(
                    FreeDayRecord(501, ACTIVE_SEMESTER, CALENDAR, LocalDate.of(2026, 12, 24), LocalDate.of(2027, 1, 2), "Przerwa świąteczna"),
                    FreeDayRecord(502, ACTIVE_SEMESTER, CALENDAR, LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 11), null)
                ),
                occurrenceNotes = listOf(OccurrenceNoteRecord(NOTE, ACTIVE_SEMESTER, WEEKLY_CLASS, noteDate, "Kolokwium")),
                occurrenceChanges = listOf(
                    OccurrenceChangeRecord(
                        701, ACTIVE_SEMESTER, WEEKLY_CLASS, LocalDate.of(2026, 10, 19), OccurrenceChangeKind.CANCELLED,
                        targetDate = null, startTime = null, endTime = null, room = null, building = null,
                        teacherName = null, note = null
                    ),
                    OccurrenceChangeRecord(
                        702, ACTIVE_SEMESTER, WEEKLY_CLASS, LocalDate.of(2026, 11, 2), OccurrenceChangeKind.MODIFIED,
                        targetDate = null, startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 30), room = "",
                        building = "B2", teacherName = "Anna Nowak", note = "Zastępstwo"
                    ),
                    OccurrenceChangeRecord(
                        703, ACTIVE_SEMESTER, WEEKLY_CLASS, LocalDate.of(2026, 11, 30), OccurrenceChangeKind.MODIFIED,
                        targetDate = LocalDate.of(2026, 12, 2), startTime = null, endTime = null, room = null,
                        building = null, teacherName = null, note = null
                    )
                )
            ),
            SemesterBackup(
                semester = SemesterRecord(id = OTHER_SEMESTER, name = "Letni", isActive = false),
                calendars = listOf(
                    AcademicCalendarRecord(OTHER_SEMESTER_CALENDAR, OTHER_SEMESTER, LocalDate.of(2027, 2, 22), LocalDate.of(2027, 6, 30), WeekType.A)
                ),
                programs = emptyList(),
                classes = emptyList(),
                weekOverrides = emptyList(),
                freeDays = emptyList(),
                occurrenceNotes = emptyList(),
                occurrenceChanges = emptyList()
            )
        )
    )
}

/** The same plan with every list in id order, because Room does not order related rows. */
fun BackupData.sortedById(): BackupData = BackupData(
    studyPrograms = studyPrograms.sortedBy { it.id },
    semesters = semesters.map { backup ->
        backup.copy(
            calendars = backup.calendars.sortedBy { it.id },
            programs = backup.programs.sortedBy { it.id },
            classes = backup.classes.sortedBy { it.id },
            weekOverrides = backup.weekOverrides.sortedBy { it.id },
            freeDays = backup.freeDays.sortedBy { it.id },
            occurrenceNotes = backup.occurrenceNotes.sortedBy { it.id },
            occurrenceChanges = backup.occurrenceChanges.sortedBy { it.id }
        )
    }
)
