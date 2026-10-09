package dev.retza.mak.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class CollisionCountTest {
    private val date = LocalDate.of(2026, 9, 21)

    @Test
    fun sameCollisionCountsOnceRegardlessOfOrder() {
        val first = occurrence("a", LocalTime.of(9, 0), LocalTime.of(10, 0))
        val second = occurrence("b", LocalTime.of(9, 30), LocalTime.of(10, 30))
        val collisions = listOf(
            Collision.Overlap(first, second, LocalTime.of(9, 30), LocalTime.of(10, 0)),
            Collision.Overlap(second, first, LocalTime.of(9, 30), LocalTime.of(10, 0))
        )

        assertEquals(1, uniqueCollisionCount(collisions))
    }

    @Test
    fun distinctCollisionsCountSeparately() {
        val first = occurrence("a", LocalTime.of(9, 0), LocalTime.of(10, 0))
        val second = occurrence("b", LocalTime.of(9, 30), LocalTime.of(10, 30))
        val third = occurrence("c", LocalTime.of(12, 0), LocalTime.of(13, 0))
        val fourth = occurrence("d", LocalTime.of(12, 30), LocalTime.of(13, 30))
        val collisions = listOf(
            Collision.Overlap(first, second, LocalTime.of(9, 30), LocalTime.of(10, 0)),
            Collision.Overlap(third, fourth, LocalTime.of(12, 30), LocalTime.of(13, 0))
        )

        assertEquals(2, uniqueCollisionCount(collisions))
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
