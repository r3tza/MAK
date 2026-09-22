package dev.retza.mak.domain

import java.time.Duration
import java.time.LocalTime

fun countGaps(occurrences: Collection<PlannedOccurrence>, thresholdMinutes: Long): Int {
    if (occurrences.size < 2) return 0

    val blocks = mutableListOf<Pair<LocalTime, LocalTime>>()
    occurrences
        .sortedWith(compareBy({ it.startTime }, { it.endTime }))
        .forEach { occurrence ->
            val last = blocks.lastOrNull()
            if (last != null && occurrence.startTime.isBefore(last.second)) {
                if (occurrence.endTime.isAfter(last.second)) {
                    blocks[blocks.lastIndex] = last.first to occurrence.endTime
                }
            } else {
                blocks += occurrence.startTime to occurrence.endTime
            }
        }

    if (blocks.size < 2) return 0

    var gaps = 0
    for (index in 1 until blocks.size) {
        val minutes = Duration.between(blocks[index - 1].second, blocks[index].first).toMinutes()
        if (minutes > thresholdMinutes) gaps += 1
    }
    return gaps
}
