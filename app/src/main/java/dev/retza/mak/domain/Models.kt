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

enum class OccurrenceStatus { SCHEDULED, CANCELLED, MOVED, MODIFIED, ONE_OFF }

sealed interface WeekStatus {
    data class Single(val type: WeekType) : WeekStatus
    data object Mixed : WeekStatus
    data object OutsideSemester : WeekStatus
}

data class Semester(
    val id: Long,
    val name: String
) {
    init {
        require(name.isNotBlank()) { "Semester name must not be blank" }
    }
}

data class StudyProgram(
    val id: Long,
    val name: String,
    val color: String
)

data class AcademicCalendar(
    val id: Long,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val firstWeekType: WeekType
) {
    init {
        require(!endDate.isBefore(startDate)) {
            "Academic calendar end must not be before its start"
        }
    }

    /** Whether [date] falls within the calendar, both ends included. */
    operator fun contains(date: LocalDate): Boolean = !date.isBefore(startDate) && !date.isAfter(endDate)
}

data class SemesterProgram(
    val id: Long,
    val semesterId: Long,
    val studyProgramId: Long,
    val academicCalendarId: Long
)

data class ClassItem(
    val id: Long,
    val semesterId: Long,
    val semesterProgramId: Long,
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
    val id: Long,
    val academicCalendarId: Long,
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
    val id: Long,
    val classId: Long,
    val occurrenceDate: LocalDate,
    val body: String
)

data class OccurrenceChange(
    val id: Long,
    val classId: Long,
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
    val status: OccurrenceStatus
        get() {
            val change = occurrenceChange
            return when {
                change?.kind == OccurrenceChangeKind.CANCELLED -> OccurrenceStatus.CANCELLED
                classItem.recurrence == Recurrence.ONCE -> OccurrenceStatus.ONE_OFF
                change?.targetDate != null && change.targetDate != change.originalDate -> OccurrenceStatus.MOVED
                change != null -> OccurrenceStatus.MODIFIED
                else -> OccurrenceStatus.SCHEDULED
            }
        }

    // Identified by the class and its original planned date, like iCalendar RECURRENCE-ID,
    // so an occurrence moved onto a day with a regular one of the same class stays distinct.
    val id: String
        get() = occurrenceId(classItem.id, originalDate)

    val classId: Long
        get() = classItem.id

    val semesterId: Long
        get() = classItem.semesterId

    val name: String
        get() = classItem.name

    val dayOfWeek: DayOfWeek
        get() = date.dayOfWeek

    val occurrenceNoteBody: String?
        get() = occurrenceNote?.body
}

fun occurrenceId(classId: Long, originalDate: LocalDate): String = "$classId:$originalDate"

data class ResolvedSchedule(
    val date: LocalDate,
    val semesterId: Long,
    val occurrences: List<PlannedOccurrence>,
    val weekStatus: WeekStatus,
    val cancelled: List<PlannedOccurrence> = emptyList(),
    val weeksByCalendarId: Map<Long, WeekCalculation> = emptyMap()
) {
    val items: List<PlannedOccurrence>
        get() = occurrences
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
