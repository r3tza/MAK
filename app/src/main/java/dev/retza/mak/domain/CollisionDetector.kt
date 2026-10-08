package dev.retza.mak.domain

import java.time.Duration
import java.time.LocalTime

class CollisionDetector {
    /**
     * Finds classes of one day that overlap, or that follow each other with a break no longer than
     * [minimumBreakMinutes] (`DOMAIN.md`, „Kolizje”).
     */
    fun detect(occurrences: Collection<PlannedOccurrence>, minimumBreakMinutes: Int): List<Collision> {
        require(minimumBreakMinutes >= 0) { "The minimum break cannot be negative." }
        val sorted = occurrences.sortedWith(compareBy<PlannedOccurrence> { it.date }.thenBy { it.startTime })
        return buildList {
            for (firstIndex in sorted.indices) {
                val first = sorted[firstIndex]
                val lastPartnerStart = first.endTime.plusMinutes(minimumBreakMinutes.toLong())
                for (secondIndex in firstIndex + 1 until sorted.size) {
                    val second = sorted[secondIndex]
                    if (second.date.isAfter(first.date)) break
                    if (first.date != second.date) continue
                    if (first.id == second.id) continue
                    // Sorted by start, so no later class can overlap or follow [first] closely enough.
                    // A limit that wraps past midnight never stops the scan.
                    if (second.startTime.isAfter(lastPartnerStart) && !lastPartnerStart.isBefore(first.endTime)) break

                    collision(first, second, minimumBreakMinutes)?.let(::add)
                }
            }
        }
    }

    fun detect(schedule: ResolvedSchedule, minimumBreakMinutes: Int): List<Collision> =
        detect(schedule.occurrences, minimumBreakMinutes)

    fun hasCollision(occurrences: Collection<PlannedOccurrence>, minimumBreakMinutes: Int): Boolean =
        detect(occurrences, minimumBreakMinutes).isNotEmpty()

    private fun collision(first: PlannedOccurrence, second: PlannedOccurrence, minimumBreakMinutes: Int): Collision? {
        val overlapStart = maxOf(first.startTime, second.startTime)
        val overlapEnd = minOf(first.endTime, second.endTime)
        if (overlapStart.isBefore(overlapEnd)) {
            return Collision(first = first, second = second, start = overlapStart, end = overlapEnd)
        }
        val earlier = if (first.endTime <= second.startTime) first else second
        val later = if (earlier === first) second else first
        if (earlier.endTime.isAfter(later.startTime)) return null
        val breakMinutes = Duration.between(earlier.endTime, later.startTime).toMinutes()
        if (breakMinutes > minimumBreakMinutes) return null
        return Collision(
            first = first,
            second = second,
            start = earlier.endTime,
            end = later.startTime,
            kind = CollisionKind.NO_BREAK
        )
    }

    private fun maxOf(first: LocalTime, second: LocalTime): LocalTime =
        if (first >= second) first else second

    private fun minOf(first: LocalTime, second: LocalTime): LocalTime =
        if (first <= second) first else second
}
