package dev.retza.mak.domain

import java.time.Duration
import java.time.LocalTime

/**
 * A break between two blocks of classes on one day.
 *
 * @property start end of the block of classes before the gap.
 * @property end start of the next block of classes.
 * @property beforeOccurrenceIds ids of the occurrences in the block that ends where the gap starts, sorted.
 */
data class Gap(
    val start: LocalTime,
    val end: LocalTime,
    val beforeOccurrenceIds: List<String>
)

/**
 * Finds the gaps between blocks of overlapping occurrences. A gap is a break strictly longer than
 * [thresholdMinutes]; a break equal to the threshold is not a gap. There are no gaps before the first
 * or after the last block.
 */
fun findGaps(occurrences: Collection<PlannedOccurrence>, thresholdMinutes: Long): List<Gap> {
    val blocks = mutableListOf<Block>()
    occurrences
        .sortedWith(compareBy({ it.startTime }, { it.endTime }))
        .forEach { occurrence ->
            val last = blocks.lastOrNull()
            if (last != null && occurrence.startTime.isBefore(last.end)) {
                if (occurrence.endTime.isAfter(last.end)) {
                    last.end = occurrence.endTime
                }
                last.occurrenceIds += occurrence.id
            } else {
                blocks += Block(occurrence.startTime, occurrence.endTime, mutableListOf(occurrence.id))
            }
        }

    val gaps = mutableListOf<Gap>()
    for (index in 1 until blocks.size) {
        val previous = blocks[index - 1]
        val next = blocks[index]
        val minutes = Duration.between(previous.end, next.start).toMinutes()
        if (minutes > thresholdMinutes) {
            gaps += Gap(
                start = previous.end,
                end = next.start,
                beforeOccurrenceIds = previous.occurrenceIds.sorted()
            )
        }
    }
    return gaps
}

fun countGaps(occurrences: Collection<PlannedOccurrence>, thresholdMinutes: Long): Int =
    findGaps(occurrences, thresholdMinutes).size

private class Block(
    val start: LocalTime,
    var end: LocalTime,
    val occurrenceIds: MutableList<String>
)
