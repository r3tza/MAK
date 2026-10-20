package dev.retza.mak.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class GapCounterTest {
    private val date = LocalDate.of(2026, 9, 21)

    @Test
    fun gapEqualToThresholdIsNotCounted() {
        val occurrences = listOf(
            occurrence(1L, "a", LocalTime.of(9, 0), LocalTime.of(10, 0)),
            occurrence(2L, "b", LocalTime.of(10, 30), LocalTime.of(11, 0))
        )

        assertEquals(0, countGaps(occurrences, 30))
        assertEquals(1, countGaps(occurrences, 29))
    }

    @Test
    fun overlappingClassesFormOneBlock() {
        val occurrences = listOf(
            occurrence(1L, "a", LocalTime.of(9, 0), LocalTime.of(10, 0)),
            occurrence(2L, "b", LocalTime.of(9, 30), LocalTime.of(10, 30))
        )

        assertEquals(0, countGaps(occurrences, 30))
    }

    @Test
    fun countsEveryGapBetweenBlocks() {
        val occurrences = listOf(
            occurrence(1L, "a", LocalTime.of(9, 0), LocalTime.of(10, 0)),
            occurrence(2L, "b", LocalTime.of(10, 31), LocalTime.of(11, 0)),
            occurrence(3L, "c", LocalTime.of(12, 0), LocalTime.of(13, 0))
        )

        assertEquals(2, countGaps(occurrences, 30))
    }

    @Test
    fun fewerThanTwoOccurrencesHaveNoGaps() {
        assertEquals(0, countGaps(emptyList(), 30))
        assertEquals(0, countGaps(listOf(occurrence(1L, "a", LocalTime.of(9, 0), LocalTime.of(10, 0))), 30))
    }

    @Test
    fun findGapsReturnsRangeAndClassesBeforeGap() {
        val a = occurrence(1L, "a", LocalTime.of(8, 0), LocalTime.of(9, 30))
        val b = occurrence(2L, "b", LocalTime.of(11, 0), LocalTime.of(12, 0))

        val gaps = findGaps(listOf(a, b), 30)

        assertEquals(
            listOf(Gap(LocalTime.of(9, 30), LocalTime.of(11, 0), listOf(a.id))),
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
    fun breakEqualToThresholdIsNotGap() {
        val occurrences = listOf(
            occurrence(1L, "a", LocalTime.of(9, 0), LocalTime.of(10, 0)),
            occurrence(2L, "b", LocalTime.of(10, 30), LocalTime.of(11, 0))
        )

        assertEquals(emptyList<Gap>(), findGaps(occurrences, 30))
    }

    @Test
    fun countGapsMatchesFindGaps() {
        val occurrences = listOf(
            occurrence(1L, "a", LocalTime.of(9, 0), LocalTime.of(10, 0)),
            occurrence(2L, "b", LocalTime.of(11, 0), LocalTime.of(12, 0)),
            occurrence(3L, "c", LocalTime.of(14, 0), LocalTime.of(15, 0))
        )

        assertEquals(2, countGaps(occurrences, 30))
        assertEquals(2, findGaps(occurrences, 30).size)
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
