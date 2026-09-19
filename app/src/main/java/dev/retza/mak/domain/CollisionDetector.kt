package dev.retza.mak.domain

import java.time.LocalTime

class CollisionDetector {
    fun detect(occurrences: Collection<PlannedOccurrence>): List<Collision> {
        val sorted = occurrences.sortedWith(compareBy<PlannedOccurrence> { it.date }.thenBy { it.startTime })
        return buildList {
            for (firstIndex in sorted.indices) {
                val first = sorted[firstIndex]
                for (secondIndex in firstIndex + 1 until sorted.size) {
                    val second = sorted[secondIndex]
                    if (second.date.isAfter(first.date)) break
                    if (first.date != second.date) continue
                    if (first.id == second.id) continue

                    val overlapStart = maxOf(first.startTime, second.startTime)
                    val overlapEnd = minOf(first.endTime, second.endTime)
                    if (overlapStart.isBefore(overlapEnd)) {
                        add(
                            Collision(
                                first = first,
                                second = second,
                                overlapStart = overlapStart,
                                overlapEnd = overlapEnd
                            )
                        )
                    }
                }
            }
        }
    }

    fun detect(schedule: ResolvedSchedule): List<Collision> = detect(schedule.occurrences)

    fun hasCollision(occurrences: Collection<PlannedOccurrence>): Boolean = detect(occurrences).isNotEmpty()

    private fun maxOf(first: LocalTime, second: LocalTime): LocalTime =
        if (first >= second) first else second

    private fun minOf(first: LocalTime, second: LocalTime): LocalTime =
        if (first <= second) first else second
}
