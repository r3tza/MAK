package dev.retza.mak.domain

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

/** Classes show tomorrow this long after the last class of today (decision of 2026-10-21, I-111). */
val TomorrowSwitchDelay: Duration = Duration.ofHours(2)

/** A day without classes shows tomorrow from this time. */
val NoClassesSwitchTime: LocalTime = LocalTime.of(18, 0)

// Two hours after 22:00 is already the next day, so there is no switch.
private val LastSwitchableClassEnd: LocalTime = LocalTime.of(22, 0)

/**
 * Returns the switch time after the latest non-cancelled class, or 18:00 when none remain.
 * Returns null when the latest class ends at or after 22:00.
 */
fun tomorrowSwitchTime(todayOccurrences: List<PlannedOccurrence>): LocalTime? {
    val lastEnd = todayOccurrences
        .filter { it.status != OccurrenceStatus.CANCELLED }
        .maxOfOrNull { it.endTime }
        ?: return NoClassesSwitchTime
    return if (lastEnd.isBefore(LastSwitchableClassEnd)) lastEnd.plus(TomorrowSwitchDelay) else null
}

/** The date whose plan the Today screen shows at [now]. */
fun shownPlanDate(today: LocalDate, now: LocalTime, switchTime: LocalTime?, enabled: Boolean): LocalDate =
    if (enabled && switchTime != null && !now.isBefore(switchTime)) today.plusDays(1) else today
