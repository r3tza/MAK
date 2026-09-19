package dev.retza.mak.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivePlanProviderTest {
    @Test
    fun providerReturnsResolvedScheduleAndCollisionsFromTheSamePlan() {
        val date = LocalDate.of(2026, 9, 21)
        val semester = Semester(
            id = "semester",
            name = "Semestr",
            startDate = date,
            endDate = date.plusDays(7),
            firstWeekType = WeekType.A
        )
        val course = Course("course", semester.id, "Informatyka", "#137B71")
        val first = classItem("first", semester.id, course.id, LocalTime.of(9, 0), LocalTime.of(10, 0))
        val second = classItem("second", semester.id, course.id, LocalTime.of(9, 30), LocalTime.of(10, 30))

        val plan = ActivePlanProvider().resolve(
            data = ActivePlanData(
                semester = semester,
                classes = listOf(first, second),
                courses = listOf(course)
            ),
            date = date
        )

        assertEquals(2, plan.schedule.occurrences.size)
        assertEquals(1, plan.collisions.size)
        assertEquals(
            plan.schedule.occurrences.map { it.id }.toSet(),
            setOf(plan.collisions.single().first.id, plan.collisions.single().second.id)
        )
    }

    private fun classItem(
        id: String,
        semesterId: String,
        courseId: String,
        startTime: LocalTime,
        endTime: LocalTime
    ) = ClassItem(
        id = id,
        semesterId = semesterId,
        name = id,
        type = "Wykład",
        courseId = courseId,
        dayOfWeek = DayOfWeek.MONDAY,
        startTime = startTime,
        endTime = endTime
    )
}
