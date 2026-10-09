package dev.retza.mak.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

enum class WeekType {
    A,
    B;

    fun toggled(): WeekType = if (this == A) B else A
}

enum class WeekOverrideScope {
    ONE_WEEK,
    FROM_WEEK
}

enum class Recurrence {
    EVERY_WEEK,
    A_WEEK,
    B_WEEK,
    ONCE
}

enum class OccurrenceChangeKind {
    CANCELLED,
    MODIFIED
}

data class Semester(
    val id: String,
    val name: String
) {
    init {
        require(name.isNotBlank()) { "Semester name must not be blank" }
    }
}

data class StudyProgram(
    val id: String,
    val name: String,
    val color: String
)

data class AcademicCalendar(
    val id: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val firstWeekType: WeekType
) {
    init {
        require(!endDate.isBefore(startDate)) {
            "Academic calendar end must not be before its start"
        }
    }
}

data class SemesterProgram(
    val id: String,
    val semesterId: String,
    val studyProgramId: String,
    val academicCalendarId: String
)

data class ClassItem(
    val id: String,
    val semesterId: String,
    val semesterProgramId: String,
    val name: String,
    val type: String,
    val teacherName: String? = null,
    val dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val room: String? = null,
    val building: String? = null,
    val group: String? = null,
    val recurrence: Recurrence = Recurrence.EVERY_WEEK,
    val date: LocalDate? = null,
    val classNote: String? = null
) {
    init {
        require(endTime.isAfter(startTime)) {
            "Class end time must be later than its start time"
        }
    }
}

data class WeekOverride(
    val id: String,
    val academicCalendarId: String,
    val weekStartDate: LocalDate,
    val weekType: WeekType,
    val scope: WeekOverrideScope
) {
    init {
        require(weekStartDate.dayOfWeek == DayOfWeek.MONDAY) {
            "Week override must start on a Monday"
        }
    }
}

data class OccurrenceNote(
    val id: String,
    val classId: String,
    val occurrenceDate: LocalDate,
    val body: String
)

data class OccurrenceChange(
    val id: String,
    val classId: String,
    val originalDate: LocalDate,
    val kind: OccurrenceChangeKind,
    val targetDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val room: String? = null,
    val building: String? = null,
    val teacherName: String? = null,
    val note: String? = null
)

data class WeekCalculation(
    val weekStartDate: LocalDate,
    val weekType: WeekType,
    val overrideSource: WeekOverride? = null
)

data class PlannedOccurrence(
    val classItem: ClassItem,
    val date: LocalDate,
    val originalDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val room: String?,
    val building: String?,
    val teacherName: String?,
    val studyProgram: StudyProgram?,
    val classNote: String?,
    val occurrenceNote: OccurrenceNote?,
    val occurrenceChange: OccurrenceChange? = null
) {
    // Identified by the class and its original planned date, like iCalendar RECURRENCE-ID,
    // so an occurrence moved onto a day with a regular one of the same class stays distinct.
    val id: String
        get() = occurrenceId(classItem.id, originalDate)

    val classId: String
        get() = classItem.id

    val semesterId: String
        get() = classItem.semesterId

    val name: String
        get() = classItem.name

    val dayOfWeek: DayOfWeek
        get() = date.dayOfWeek

    val occurrenceNoteBody: String?
        get() = occurrenceNote?.body
}

fun occurrenceId(classId: String, originalDate: LocalDate): String = "$classId:$originalDate"

data class ResolvedSchedule(
    val date: LocalDate,
    val semesterId: String,
    val week: WeekCalculation?,
    val occurrences: List<PlannedOccurrence>,
    val hasMixedWeekTypes: Boolean = false
) {
    val items: List<PlannedOccurrence>
        get() = occurrences

    val weekType: WeekType?
        get() = week?.weekType

    val correction: WeekOverride?
        get() = week?.overrideSource
}

/** Two classes of one day that cannot both be attended comfortably (`DOMAIN.md`, „Kolizje”). */
sealed interface Collision {
    val first: PlannedOccurrence
    val second: PlannedOccurrence
    val start: LocalTime
    val end: LocalTime

    val date: LocalDate
        get() = first.date

    /** The classes overlap from [start] to [end]. */
    data class Overlap(
        override val first: PlannedOccurrence,
        override val second: PlannedOccurrence,
        override val start: LocalTime,
        override val end: LocalTime
    ) : Collision

    /** The earlier class ends at [start] and the later one starts at [end]. */
    data class NoBreak(
        override val first: PlannedOccurrence,
        override val second: PlannedOccurrence,
        override val start: LocalTime,
        override val end: LocalTime
    ) : Collision {
        val breakMinutes: Long
            get() = java.time.Duration.between(start, end).toMinutes()
    }
}
