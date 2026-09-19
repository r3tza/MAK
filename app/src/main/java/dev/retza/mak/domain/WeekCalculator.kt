package dev.retza.mak.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class WeekCalculator {
    fun weekStart(date: LocalDate): LocalDate =
        date.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun calculate(
        semester: Semester,
        date: LocalDate,
        overrides: Collection<WeekOverride> = emptyList()
    ): WeekCalculation? {
        if (date.isBefore(semester.startDate) || date.isAfter(semester.endDate)) {
            return null
        }

        val firstWeekStart = weekStart(semester.startDate)
        val currentWeekStart = weekStart(date)
        val relevantOverrides = overrides.filter { it.semesterId == semester.id }

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
                firstType = semester.firstWeekType,
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
        semester: Semester,
        date: LocalDate,
        overrides: Collection<WeekOverride> = emptyList()
    ): WeekType? = calculate(semester, date, overrides)?.weekType

    private fun alternatingType(firstType: WeekType, weeksFromAnchor: Long): WeekType =
        if (weeksFromAnchor % 2L == 0L) firstType else firstType.toggled()
}
