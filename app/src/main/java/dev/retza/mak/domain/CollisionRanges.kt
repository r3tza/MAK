package dev.retza.mak.domain

import java.time.LocalTime

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
