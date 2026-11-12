package dev.retza.mak.domain

import java.time.LocalDateTime

/** True while a non-cancelled occurrence on its effective date is in progress. */
fun PlannedOccurrence.isOccurrenceCurrent(moment: LocalDateTime): Boolean =
    takesPlaceOn(moment) &&
        !moment.toLocalTime().isBefore(startTime) &&
        moment.toLocalTime().isBefore(endTime)

/**
 * Returns the next start or end of a non-cancelled occurrence on the date of [moment], strictly after it.
 * The widget refreshes there so its "Teraz" row follows the classes (decision of 2026-11-12, I-112).
 */
fun nextCurrentStateChange(occurrences: List<PlannedOccurrence>, moment: LocalDateTime): LocalDateTime? =
    occurrences
        .filter { it.takesPlaceOn(moment) }
        .flatMap { listOf(it.startTime, it.endTime) }
        .map { moment.toLocalDate().atTime(it) }
        .filter { it.isAfter(moment) }
        .minOrNull()

private fun PlannedOccurrence.takesPlaceOn(moment: LocalDateTime): Boolean =
    date == moment.toLocalDate() && status != OccurrenceStatus.CANCELLED
