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
            occurrence("a", LocalTime.of(9, 0), LocalTime.of(10, 0)),
            occurrence("b", LocalTime.of(10, 30), LocalTime.of(11, 0))
        )

        assertEquals(0, countGaps(occurrences, 30))
        assertEquals(1, countGaps(occurrences, 29))
    }

    @Test
    fun overlappingClassesFormOneBlock() {
        val occurrences = listOf(
            occurrence("a", LocalTime.of(9, 0), LocalTime.of(10, 0)),
            occurrence("b", LocalTime.of(9, 30), LocalTime.of(10, 30))
        )

        assertEquals(0, countGaps(occurrences, 30))
    }

    @Test
    fun countsEveryGapBetweenBlocks() {
        val occurrences = listOf(
            occurrence("a", LocalTime.of(9, 0), LocalTime.of(10, 0)),
            occurrence("b", LocalTime.of(10, 31), LocalTime.of(11, 0)),
            occurrence("c", LocalTime.of(12, 0), LocalTime.of(13, 0))
        )

        assertEquals(2, countGaps(occurrences, 30))
    }

    @Test
    fun fewerThanTwoOccurrencesHaveNoGaps() {
        assertEquals(0, countGaps(emptyList(), 30))
        assertEquals(0, countGaps(listOf(occurrence("a", LocalTime.of(9, 0), LocalTime.of(10, 0))), 30))
    }

    private fun occurrence(id: String, start: LocalTime, end: LocalTime) = PlannedOccurrence(
        classItem = ClassItem(
            id = id,
            semesterId = "semester",
            semesterProgramId = "assignment",
            name = id,
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
