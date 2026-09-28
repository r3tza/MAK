package dev.retza.mak.domain

import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class CollisionRange(
    val start: LocalTime,
    val end: LocalTime
)

fun collisionRanges(collisions: Collection<Collision>): Map<String, List<CollisionRange>> {
    val rangesByOccurrence = linkedMapOf<String, MutableSet<CollisionRange>>()
    collisions.forEach { collision ->
        val range = CollisionRange(collision.overlapStart, collision.overlapEnd)
        rangesByOccurrence.getOrPut(collision.first.id) { linkedSetOf() }.add(range)
        rangesByOccurrence.getOrPut(collision.second.id) { linkedSetOf() }.add(range)
    }
    return rangesByOccurrence.mapValues { (_, ranges) ->
        ranges.sortedWith(compareBy<CollisionRange> { it.start }.thenBy { it.end })
    }
}

fun collisionLabels(collisions: Collection<Collision>): Map<String, String> =
    collisionRanges(collisions).mapValues { (_, ranges) ->
        val labels = ranges.joinToString(", ") {
            "${it.start.format(collisionTimeFormatter)}-${it.end.format(collisionTimeFormatter)}"
        }
        if (ranges.size == 1) "Kolizja $labels" else "Kolizje: $labels"
    }

fun collisionPartnerNames(collisions: Collection<Collision>): Map<String, String> {
    val namesByOccurrence = linkedMapOf<String, MutableList<Pair<LocalTime, String>>>()
    collisions.forEach { collision ->
        namesByOccurrence.getOrPut(collision.first.id) { mutableListOf() }
            .add(collision.overlapStart to collision.second.name)
        namesByOccurrence.getOrPut(collision.second.id) { mutableListOf() }
            .add(collision.overlapStart to collision.first.name)
    }
    return namesByOccurrence.mapValues { (_, partners) ->
        partners.sortedWith(compareBy<Pair<LocalTime, String>> { it.first }.thenBy { it.second })
            .map { it.second }
            .distinct()
            .joinToString(", ")
    }
}

private val collisionTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
