package dev.retza.mak.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollisionDetectorTest {
    private val semester = Semester(id = "semester", name = "Winter")
    private val calendar = AcademicCalendar(
        id = "calendar",
        startDate = LocalDate.of(2026, 1, 1),
        endDate = LocalDate.of(2026, 1, 31),
        firstWeekType = WeekType.A
    )
    private val course = StudyProgram("course", "Course", "#000000")
    private val assignment = SemesterProgram("assignment", semester.id, course.id, calendar.id)
    private val resolver = ScheduleResolver()
    private val detector = CollisionDetector()

    private fun item(id: String, start: LocalTime, end: LocalTime) = ClassItem(
        id = id,
        semesterId = semester.id,
        semesterProgramId = assignment.id,
        name = id,
        type = "lecture",
        dayOfWeek = DayOfWeek.MONDAY,
        startTime = start,
        endTime = end
    )

    private fun occurrences(classes: Collection<ClassItem>, changes: Collection<OccurrenceChange> = emptyList()) =
        resolver.resolve(
            date = LocalDate.of(2026, 1, 5),
            semester = semester,
            classes = classes,
            courses = listOf(course),
            semesterPrograms = listOf(assignment),
            calendars = listOf(calendar),
            occurrenceChanges = changes
        ).occurrences

    @Test
    fun overlappingIntervalsProduceCollisionWithDuration() {
        val collisions = detector.detect(
            occurrences(
                listOf(
                    item("first", LocalTime.of(10, 0), LocalTime.of(11, 30)),
                    item("second", LocalTime.of(11, 0), LocalTime.of(12, 30))
                )
            ),
            minimumBreakMinutes = 0
        )

        val overlap = collisions.single() as Collision.Overlap
        assertEquals(LocalTime.of(11, 0), overlap.start)
        assertEquals(LocalTime.of(11, 30), overlap.end)
    }

    @Test
    fun touchingIntervalsAreCollisionWithoutBreak() {
        val collision = detector.detect(
            occurrences(
                listOf(
                    item("first", LocalTime.of(8, 15), LocalTime.of(9, 45)),
                    item("second", LocalTime.of(9, 45), LocalTime.of(11, 15))
                )
            ),
            minimumBreakMinutes = 0
        ).single() as Collision.NoBreak

        assertEquals(LocalTime.of(9, 45), collision.start)
        assertEquals(0L, collision.breakMinutes)
    }

    @Test
    fun breakLongerThanMinimumIsNotCollision() {
        val classes = listOf(
            item("first", LocalTime.of(8, 15), LocalTime.of(9, 45)),
            item("second", LocalTime.of(9, 46), LocalTime.of(11, 15))
        )

        assertTrue(detector.detect(occurrences(classes), minimumBreakMinutes = 0).isEmpty())
    }

    @Test
    fun breakEqualToMinimumIsCollisionAndLongerIsNot() {
        val tenMinutes = listOf(
            item("first", LocalTime.of(8, 15), LocalTime.of(9, 45)),
            item("second", LocalTime.of(9, 55), LocalTime.of(11, 15))
        )
        val elevenMinutes = listOf(
            item("first", LocalTime.of(8, 15), LocalTime.of(9, 45)),
            item("second", LocalTime.of(9, 56), LocalTime.of(11, 15))
        )

        val tenMinuteBreak = detector.detect(occurrences(tenMinutes), minimumBreakMinutes = 10).single() as Collision.NoBreak
        assertEquals(10L, tenMinuteBreak.breakMinutes)
        assertTrue(detector.detect(occurrences(elevenMinutes), minimumBreakMinutes = 10).isEmpty())
    }

    @Test
    fun shortClassIsComparedWithEveryLaterClassThatFollowsIt() {
        val collisions = detector.detect(
            occurrences(
                listOf(
                    item("long", LocalTime.of(8, 0), LocalTime.of(12, 0)),
                    item("short", LocalTime.of(8, 30), LocalTime.of(9, 0)),
                    item("after", LocalTime.of(9, 0), LocalTime.of(10, 0))
                )
            ),
            minimumBreakMinutes = 0
        )

        val noBreak = collisions.filterIsInstance<Collision.NoBreak>().single()
        assertEquals(setOf("short", "after"), setOf(noBreak.first.name, noBreak.second.name))
        assertEquals(2, collisions.count { it is Collision.Overlap })
    }

    @Test
    fun collisionsAreCalculatedAfterOccurrenceChanges() {
        val change = OccurrenceChange(
            id = "modify",
            classId = "second",
            originalDate = LocalDate.of(2026, 1, 5),
            kind = OccurrenceChangeKind.MODIFIED,
            startTime = LocalTime.of(10, 30),
            endTime = LocalTime.of(11, 30)
        )

        assertEquals(
            1,
            detector.detect(
                occurrences(
                    listOf(
                        item("first", LocalTime.of(10, 0), LocalTime.of(11, 0)),
                        item("second", LocalTime.of(12, 0), LocalTime.of(13, 0))
                    ),
                    changes = listOf(change)
                ),
                minimumBreakMinutes = 0
            ).size
        )
    }

    @Test
    fun cancelledOccurrenceDoesNotCollideWithoutBreak() {
        val cancel = OccurrenceChange(
            id = "cancel",
            classId = "second",
            originalDate = LocalDate.of(2026, 1, 5),
            kind = OccurrenceChangeKind.CANCELLED
        )

        val collisions = detector.detect(
            occurrences(
                listOf(
                    item("first", LocalTime.of(8, 15), LocalTime.of(9, 45)),
                    item("second", LocalTime.of(9, 45), LocalTime.of(11, 15))
                ),
                changes = listOf(cancel)
            ),
            minimumBreakMinutes = 0
        )

        assertTrue(collisions.isEmpty())
    }
}
