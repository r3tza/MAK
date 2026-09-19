package dev.retza.mak.ui.schedule

import dev.retza.mak.domain.Collision
import dev.retza.mak.domain.collisionRanges
import java.time.format.DateTimeFormatter

internal fun conflictLabels(collisions: Collection<Collision>): Map<String, String> {
    return collisionRanges(collisions).mapValues { (_, ranges) ->
        val labels = ranges
            .joinToString(", ") { it.label() }
        if (ranges.size == 1) "Kolizja $labels" else "Kolizje: $labels"
    }
}

private fun dev.retza.mak.domain.CollisionRange.label(): String {
    return "${start.format(conflictTimeFormatter)}-${end.format(conflictTimeFormatter)}"
}

private val conflictTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
