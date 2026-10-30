package dev.retza.mak.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class GapCounterTest {
    private val date = LocalDate.of(2026, 9, 21)

    @Test
    fun breakLongerThanThresholdIsGapAndEqualIsNot() {
        val occurrences = listOf(
            occurrence(1L, "a", LocalTime.of(9, 0), LocalTime.of(10, 0)),
            occurrence(2L, "b", LocalTime.of(10, 30), LocalTime.of(11, 0))
        )

        assertEquals(emptyList<Gap>(), findGaps(occurrences, 30))
        assertEquals(1, findGaps(occurrences, 29).size)
    }

    @Test
    fun fewerThanTwoOccurrencesHaveNoGaps() {
        assertEquals(emptyList<Gap>(), findGaps(emptyList(), 30))
        assertEquals(emptyList<Gap>(), findGaps(listOf(occurrence(1L, "a", LocalTime.of(9, 0), LocalTime.of(10, 0))), 30))
    }

    @Test
    fun everyGapBetweenBlocksHasItsRangeAndClassesBeforeIt() {
        val a = occurrence(1L, "a", LocalTime.of(8, 0), LocalTime.of(9, 30))
        val b = occurrence(2L, "b", LocalTime.of(11, 0), LocalTime.of(12, 0))
        val c = occurrence(3L, "c", LocalTime.of(14, 0), LocalTime.of(15, 0))

        val gaps = findGaps(listOf(c, a, b), 30)

        assertEquals(
            listOf(
                Gap(LocalTime.of(9, 30), LocalTime.of(11, 0), listOf(a.id)),
                Gap(LocalTime.of(12, 0), LocalTime.of(14, 0), listOf(b.id))
            ),
            gaps
        )
    }

    @Test
    fun overlappingClassesFormOneBlockBeforeGap() {
        val a = occurrence(1L, "a", LocalTime.of(8, 0), LocalTime.of(9, 30))
        val b = occurrence(2L, "b", LocalTime.of(9, 0), LocalTime.of(10, 0))
        val c = occurrence(3L, "c", LocalTime.of(12, 0), LocalTime.of(13, 0))

        val gaps = findGaps(listOf(a, b, c), 30)

        assertEquals(
            listOf(Gap(LocalTime.of(10, 0), LocalTime.of(12, 0), listOf(a.id, b.id).sorted())),
            gaps
        )
    }

    @Test
    fun shortClassInsideALongerOneDoesNotEndTheBlock() {
        val long = occurrence(1L, "long", LocalTime.of(8, 0), LocalTime.of(12, 0))
        val inside = occurrence(2L, "inside", LocalTime.of(9, 0), LocalTime.of(10, 0))
        val after = occurrence(3L, "after", LocalTime.of(12, 40), LocalTime.of(14, 0))

        val gaps = findGaps(listOf(long, inside, after), 30)

        assertEquals(listOf(Gap(LocalTime.of(12, 0), LocalTime.of(12, 40), listOf(long.id, inside.id))), gaps)
    }

    private fun occurrence(id: Long, name: String, start: LocalTime, end: LocalTime) = PlannedOccurrence(
        classItem = ClassItem(
            id = id,
            semesterId = 1L,
            semesterProgramId = 1L,
            name = name,
            type = "Wykład",
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = start,
            endTime = end
        ),
        date = date,
        originalDate = date,
        startTime = start,
        endTime = end,
        room = null,
        building = null,
        teacherName = null,
        studyProgram = null,
        classNote = null,
        occurrenceNote = null
    )
}
