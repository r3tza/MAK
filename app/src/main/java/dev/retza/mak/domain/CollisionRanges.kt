package dev.retza.mak.domain

import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class CollisionRange(
    val start: LocalTime,
    val end: LocalTime
)

/** Overlap ranges per occurrence; collisions without an overlap have no range. */
fun collisionRanges(collisions: Collection<Collision>): Map<String, List<CollisionRange>> {
    val rangesByOccurrence = linkedMapOf<String, MutableSet<CollisionRange>>()
    collisions.filter { it.kind == CollisionKind.OVERLAP }.forEach { collision ->
        val range = CollisionRange(collision.start, collision.end)
        rangesByOccurrence.getOrPut(collision.first.id) { linkedSetOf() }.add(range)
        rangesByOccurrence.getOrPut(collision.second.id) { linkedSetOf() }.add(range)
    }
    return rangesByOccurrence.mapValues { (_, ranges) ->
        ranges.sortedWith(compareBy<CollisionRange> { it.start }.thenBy { it.end })
    }
}

/**
 * „Kolizja 10:00-11:30”, „Bez przerwy o 09:45”, „Przerwa 5 min o 09:45” or a combination such as
 * „Kolizja 10:00-10:30, bez przerwy o 11:15” (`DOMAIN.md`, „Kolizje”).
 */
fun collisionLabels(collisions: Collection<Collision>): Map<String, String> {
    val ranges = collisionRanges(collisions)
    val breaks = linkedMapOf<String, MutableList<Collision>>()
    collisions.filter { it.kind == CollisionKind.NO_BREAK }.forEach { collision ->
        breaks.getOrPut(collision.first.id) { mutableListOf() }.add(collision)
        breaks.getOrPut(collision.second.id) { mutableListOf() }.add(collision)
    }
    return (ranges.keys + breaks.keys).associateWith { id ->
        val overlapLabel = ranges[id]?.let { occurrenceRanges ->
            val labels = occurrenceRanges.joinToString(", ") {
                "${it.start.format(collisionTimeFormatter)}-${it.end.format(collisionTimeFormatter)}"
            }
            if (occurrenceRanges.size == 1) "Kolizja $labels" else "Kolizje: $labels"
        }
        val breakLabels = breaks[id].orEmpty()
            .sortedWith(compareBy<Collision> { it.start }.thenBy { it.end })
            .map(::breakLabel)
            .distinct()
        when {
            overlapLabel == null -> breakLabels.joinToString(", ").replaceFirstChar { it.uppercaseChar() }
            breakLabels.isEmpty() -> overlapLabel
            else -> (listOf(overlapLabel) + breakLabels).joinToString(", ")
        }
    }
}

/** „bez przerwy o 09:45” or „przerwa 5 min o 09:45”, starting with a small letter. */
fun breakLabel(collision: Collision): String {
    val time = collision.start.format(collisionTimeFormatter)
    val minutes = collision.breakMinutes
    return if (minutes == 0L) "bez przerwy o $time" else "przerwa $minutes min o $time"
}

fun collisionPartnerNames(collisions: Collection<Collision>): Map<String, String> {
    val namesByOccurrence = linkedMapOf<String, MutableList<Pair<LocalTime, String>>>()
    collisions.forEach { collision ->
        namesByOccurrence.getOrPut(collision.first.id) { mutableListOf() }
            .add(collision.start to collision.second.name)
        namesByOccurrence.getOrPut(collision.second.id) { mutableListOf() }
            .add(collision.start to collision.first.name)
    }
    return namesByOccurrence.mapValues { (_, partners) ->
        partners.sortedWith(compareBy<Pair<LocalTime, String>> { it.first }.thenBy { it.second })
            .map { it.second }
            .distinct()
            .joinToString(", ")
    }
}

internal val collisionTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
