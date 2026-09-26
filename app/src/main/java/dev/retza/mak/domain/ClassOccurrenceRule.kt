package dev.retza.mak.domain

import java.time.LocalDate

/**
 * Whether the plan of this class has a term on [date], before any occurrence change is applied.
 * This is the single rule used by [ScheduleResolver] and by [hiddenByClassEdit].
 */
fun ClassItem.hasBaseOccurrenceOn(
    date: LocalDate,
    calendar: AcademicCalendar,
    overrides: Collection<WeekOverride> = emptyList(),
    weekCalculator: WeekCalculator = WeekCalculator()
): Boolean {
    if (recurrence == Recurrence.ONCE) {
        return this.date == date && !date.isBefore(calendar.startDate) && !date.isAfter(calendar.endDate)
    }
    if (dayOfWeek != date.dayOfWeek) return false
    val week = weekCalculator.calculate(calendar, date, overrides) ?: return false
    return when (recurrence) {
        Recurrence.EVERY_WEEK -> true
        Recurrence.A_WEEK -> week.weekType == WeekType.A
        Recurrence.B_WEEK -> week.weekType == WeekType.B
        Recurrence.ONCE -> false
    }
}

data class ClassEditImpact(
    val changeCount: Int,
    val noteCount: Int
) {
    val isEmpty: Boolean get() = changeCount == 0 && noteCount == 0
}

/**
 * Counts occurrence changes and occurrence notes of [before] that are visible now and would
 * stop being visible after saving [after]. The data itself is kept; it returns when the
 * previous term is restored.
 */
fun hiddenByClassEdit(
    before: ClassItem,
    after: ClassItem,
    beforeCalendar: AcademicCalendar?,
    afterCalendar: AcademicCalendar?,
    overrides: Collection<WeekOverride>,
    changes: Collection<OccurrenceChange>,
    notes: Collection<OccurrenceNote>,
    weekCalculator: WeekCalculator = WeekCalculator()
): ClassEditImpact {
    fun ClassItem.hasTerm(date: LocalDate, calendar: AcademicCalendar?): Boolean =
        calendar != null && hasBaseOccurrenceOn(date, calendar, overrides, weekCalculator)

    // The resolver applies occurrence changes only to recurring classes.
    fun ClassItem.showsChange(date: LocalDate, calendar: AcademicCalendar?): Boolean =
        recurrence != Recurrence.ONCE && hasTerm(date, calendar)

    val changeCount = changes.count {
        it.classId == before.id &&
            before.showsChange(it.originalDate, beforeCalendar) &&
            !after.showsChange(it.originalDate, afterCalendar)
    }
    val noteCount = notes.count {
        it.classId == before.id &&
            before.hasTerm(it.occurrenceDate, beforeCalendar) &&
            !after.hasTerm(it.occurrenceDate, afterCalendar)
    }
    return ClassEditImpact(changeCount, noteCount)
}
