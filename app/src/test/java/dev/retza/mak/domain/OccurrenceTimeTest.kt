package dev.retza.mak.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OccurrenceTimeTest {
    private val date = LocalDate.of(2026, 11, 11)
    private val occurrence = occurrence(date)

    @Test
    fun includesStartAndExcludesEnd() {
        assertFalse(occurrence.isOccurrenceCurrent(date.atTime(8, 59)))
        assertTrue(occurrence.isOccurrenceCurrent(date.atTime(9, 0)))
        assertTrue(occurrence.isOccurrenceCurrent(date.atTime(9, 59, 59)))
        assertFalse(occurrence.isOccurrenceCurrent(date.atTime(10, 0)))
    }

    @Test
    fun occurrenceOnAnotherDateIsNotCurrent() {
        assertFalse(occurrence.isOccurrenceCurrent(date.plusDays(1).atTime(9, 30)))
    }

    @Test
    fun cancelledOccurrenceIsNotCurrent() {
        val cancelled = occurrence.copy(
            occurrenceChange = OccurrenceChange(7, 1, date, OccurrenceChangeKind.CANCELLED)
        )
        assertFalse(cancelled.isOccurrenceCurrent(date.atTime(9, 30)))
    }

    @Test
    fun movedOccurrenceUsesItsEffectiveDate() {
        val moved = occurrence.copy(
            date = date.plusDays(1),
            originalDate = date,
            occurrenceChange = OccurrenceChange(8, 1, date, OccurrenceChangeKind.MODIFIED, targetDate = date.plusDays(1))
        )
        assertFalse(moved.isOccurrenceCurrent(date.atTime(9, 30)))
        assertTrue(moved.isOccurrenceCurrent(date.plusDays(1).atTime(9, 30)))
    }

    @Test
    fun overlappingOccurrencesCanBothBeCurrent() {
        val second = occurrence(date).copy(
            classItem = occurrence(date).classItem.copy(id = 2, name = "Second"),
            startTime = LocalTime.of(9, 15),
            endTime = LocalTime.of(10, 15)
        )
        val moment = date.atTime(9, 30)
        assertTrue(occurrence.isOccurrenceCurrent(moment))
        assertTrue(second.isOccurrenceCurrent(moment))
    }

    @Test
    fun nextChangeIsTheNearestStartOrEndLaterToday() {
        val cancelled = occurrence.copy(
            startTime = LocalTime.of(8, 0),
            occurrenceChange = OccurrenceChange(7, 1, date, OccurrenceChangeKind.CANCELLED)
        )
        val tomorrow = occurrence(date.plusDays(1))
        val occurrences = listOf(cancelled, occurrence, tomorrow)

        assertEquals(date.atTime(9, 0), nextCurrentStateChange(occurrences, date.atTime(7, 30)))
        assertEquals(date.atTime(10, 0), nextCurrentStateChange(occurrences, date.atTime(9, 0)))
        assertNull(nextCurrentStateChange(occurrences, date.atTime(10, 0)))
    }

    private fun occurrence(date: LocalDate) = PlannedOccurrence(
        classItem = ClassItem(1, 1, 1, "Class", "Lecture", dayOfWeek = DayOfWeek.WEDNESDAY,
            startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 0)),
        date = date,
        originalDate = date,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(10, 0),
        room = null,
        building = null,
        teacherName = null,
        studyProgram = null,
        classNote = null,
        occurrenceNote = null
    )
}
