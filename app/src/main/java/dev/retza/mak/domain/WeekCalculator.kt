package dev.retza.mak.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Monday of the week that contains this date. */
fun LocalDate.weekStart(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

class WeekCalculator {
    fun weekStart(date: LocalDate): LocalDate =
        date.weekStart()

    fun calculate(
        calendar: AcademicCalendar,
        date: LocalDate,
        overrides: Collection<WeekOverride> = emptyList()
    ): WeekCalculation? {
        if (date !in calendar) {
            return null
        }

        val firstWeekStart = weekStart(calendar.startDate)
        val currentWeekStart = weekStart(date)
        val relevantOverrides = overrides.filter { it.academicCalendarId == calendar.id }

        val oneWeekOverride = relevantOverrides
            .filter {
                it.scope == WeekOverrideScope.ONE_WEEK &&
                    it.weekStartDate == currentWeekStart
            }
            .lastOrNull()

        if (oneWeekOverride != null) {
            return WeekCalculation(
                weekStartDate = currentWeekStart,
                weekType = oneWeekOverride.weekType,
                overrideSource = oneWeekOverride
            )
        }

        val fromWeekOverride = relevantOverrides
            .filter {
                it.scope == WeekOverrideScope.FROM_WEEK &&
                    !it.weekStartDate.isAfter(currentWeekStart)
            }
            .maxWithOrNull(compareBy<WeekOverride> { it.weekStartDate }.thenBy { it.id })

        val weekType = if (fromWeekOverride == null) {
            alternatingType(
                firstType = calendar.firstWeekType,
                weeksFromAnchor = ChronoUnit.WEEKS.between(firstWeekStart, currentWeekStart)
            )
        } else {
            alternatingType(
                firstType = fromWeekOverride.weekType,
                weeksFromAnchor = ChronoUnit.WEEKS.between(
                    fromWeekOverride.weekStartDate,
                    currentWeekStart
                )
            )
        }

        return WeekCalculation(
            weekStartDate = currentWeekStart,
            weekType = weekType,
            overrideSource = fromWeekOverride
        )
    }

    fun calculateWeekType(
        calendar: AcademicCalendar,
        date: LocalDate,
        overrides: Collection<WeekOverride> = emptyList()
    ): WeekType? = calculate(calendar, date, overrides)?.weekType

    private fun alternatingType(firstType: WeekType, weeksFromAnchor: Long): WeekType =
        if (weeksFromAnchor % 2L == 0L) firstType else firstType.toggled()
}
