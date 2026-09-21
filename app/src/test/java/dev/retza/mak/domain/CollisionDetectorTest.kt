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
            )
        )

        assertEquals(1, collisions.size)
        assertEquals(30L, collisions.single().durationMinutes)
    }

    @Test
    fun TouchingIntervalsAreNotCollision() {
        val collisions = detector.detect(
            occurrences(
                listOf(
                    item("first", LocalTime.of(10, 0), LocalTime.of(11, 0)),
                    item("second", LocalTime.of(11, 0), LocalTime.of(12, 0))
                )
            )
        )

        assertTrue(collisions.isEmpty())
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
                )
            ).size
        )
    }
}
