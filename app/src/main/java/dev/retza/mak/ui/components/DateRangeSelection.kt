package dev.retza.mak.ui.components

import java.time.LocalDate

/**
 * The range confirmed in the range picker. A picked start without an end keeps [previousEnd]
 * when it is not before the new start; otherwise the range is the start day alone.
 */
internal fun resolvePickedRange(
    pickedStart: LocalDate,
    pickedEnd: LocalDate?,
    previousEnd: LocalDate?
): Pair<LocalDate, LocalDate> = when {
    pickedEnd != null -> pickedStart to pickedEnd
    previousEnd != null && !previousEnd.isBefore(pickedStart) -> pickedStart to previousEnd
    else -> pickedStart to pickedStart
}
