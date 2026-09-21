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
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val firstWeekType: WeekType
) {
    init {
        require(name.isNotBlank()) { "Semester name must not be blank" }
        require(!endDate.isBefore(startDate)) { "Semester end must not be before its start" }
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

data class Teacher(
    val id: String,
    val semesterId: String,
    val name: String
)

data class ClassItem(
    val id: String,
    val semesterId: String,
    val name: String,
    val type: String,
    val courseId: String,
    val teacherId: String? = null,
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
    val semesterId: String,
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
    val teacherId: String? = null,
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
    val teacher: Teacher?,
    val course: StudyProgram?,
    val classNote: String?,
    val occurrenceNote: OccurrenceNote?,
    val occurrenceChange: OccurrenceChange? = null
) {
    val id: String
        get() = "${classItem.id}:$date"

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

data class ResolvedSchedule(
    val date: LocalDate,
    val semesterId: String,
    val week: WeekCalculation?,
    val occurrences: List<PlannedOccurrence>
) {
    val items: List<PlannedOccurrence>
        get() = occurrences

    val weekType: WeekType?
        get() = week?.weekType

    val correction: WeekOverride?
        get() = week?.overrideSource
}

data class Collision(
    val first: PlannedOccurrence,
    val second: PlannedOccurrence,
    val overlapStart: LocalTime,
    val overlapEnd: LocalTime
) {
    val date: LocalDate
        get() = first.date

    val durationMinutes: Long
        get() = java.time.Duration.between(overlapStart, overlapEnd).toMinutes()
}
