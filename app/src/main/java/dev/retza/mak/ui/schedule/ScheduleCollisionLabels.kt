package dev.retza.mak.ui.schedule

import dev.retza.mak.domain.Collision
import java.time.format.DateTimeFormatter

internal fun conflictLabels(collisions: Collection<Collision>): Map<String, String> {
    val rangesByOccurrence = linkedMapOf<String, MutableSet<ConflictRange>>()
    collisions.forEach { collision ->
        val range = ConflictRange(collision.overlapStart, collision.overlapEnd)
        rangesByOccurrence.getOrPut(collision.first.id) { linkedSetOf() }.add(range)
        rangesByOccurrence.getOrPut(collision.second.id) { linkedSetOf() }.add(range)
    }

    return rangesByOccurrence.mapValues { (_, ranges) ->
        val labels = ranges
            .sortedWith(compareBy<ConflictRange> { it.start }.thenBy { it.end })
            .joinToString(", ") { it.label() }
        if (ranges.size == 1) "Kolizja $labels" else "Kolizje: $labels"
    }
}

private data class ConflictRange(
    val start: java.time.LocalTime,
    val end: java.time.LocalTime
) {
    fun label(): String = "${start.format(conflictTimeFormatter)}-${end.format(conflictTimeFormatter)}"
}

private val conflictTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
