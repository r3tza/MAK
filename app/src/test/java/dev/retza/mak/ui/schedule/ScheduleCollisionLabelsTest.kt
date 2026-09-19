package dev.retza.mak.ui.schedule

import dev.retza.mak.domain.ClassItem
import dev.retza.mak.domain.CollisionDetector
import dev.retza.mak.domain.Course
import dev.retza.mak.domain.Semester
import dev.retza.mak.domain.ScheduleResolver
import dev.retza.mak.domain.WeekType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleCollisionLabelsTest {
    private val date = LocalDate.of(2026, 9, 21)
    private val semester = Semester(
        id = "semester",
        name = "Semestr",
        startDate = date,
        endDate = date.plusDays(7),
        firstWeekType = WeekType.A
    )
    private val course = Course("course", semester.id, "Informatyka", "#137B71")

    @Test
    fun oneCollisionGetsTheSameExactRangeForBothClasses() {
        val schedule = resolve(
            classItem("first", LocalTime.of(9, 0), LocalTime.of(10, 0)),
            classItem("second", LocalTime.of(9, 30), LocalTime.of(10, 30))
        )

        val labels = conflictLabels(CollisionDetector().detect(schedule))

        assertEquals("Kolizja 09:30-10:00", labels["first:$date"])
        assertEquals("Kolizja 09:30-10:00", labels["second:$date"])
    }

    @Test
    fun multipleCollisionsAreSortedAndDuplicatesAreRemovedForEachClass() {
        val schedule = resolve(
            classItem("first", LocalTime.of(10, 0), LocalTime.of(12, 0)),
            classItem("second", LocalTime.of(10, 30), LocalTime.of(11, 0)),
            classItem("third", LocalTime.of(11, 30), LocalTime.of(12, 30))
        )

        val labels = conflictLabels(CollisionDetector().detect(schedule))

        assertEquals("Kolizje: 10:30-11:00, 11:30-12:00", labels["first:$date"])
        assertEquals("Kolizja 10:30-11:00", labels["second:$date"])
        assertEquals("Kolizja 11:30-12:00", labels["third:$date"])
    }

    private fun resolve(vararg classes: ClassItem) = ScheduleResolver().resolve(
        date = date,
        semester = semester,
        classes = classes.toList(),
        courses = listOf(course)
    ).occurrences

    private fun classItem(id: String, start: LocalTime, end: LocalTime) = ClassItem(
        id = id,
        semesterId = semester.id,
        name = id,
        type = "Wykład",
        courseId = course.id,
        dayOfWeek = DayOfWeek.MONDAY,
        startTime = start,
        endTime = end
    )
}
