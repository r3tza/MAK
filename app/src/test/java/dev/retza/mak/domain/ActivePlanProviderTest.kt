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
        val semester = Semester(id = "semester", name = "Semestr")
        val calendar = AcademicCalendar(
            id = "calendar",
            startDate = date,
            endDate = date.plusDays(7),
            firstWeekType = WeekType.A
        )
        val course = StudyProgram("course", "Informatyka", "#137B71")
        val assignment = SemesterProgram("assignment", semester.id, course.id, calendar.id)
        val first = classItem("first", semester.id, assignment.id, LocalTime.of(9, 0), LocalTime.of(10, 0))
        val second = classItem("second", semester.id, assignment.id, LocalTime.of(9, 30), LocalTime.of(10, 30))

        val plan = ActivePlanProvider().resolve(
            data = ActivePlanData(
                semester = semester,
                classes = listOf(first, second),
                courses = listOf(course),
                semesterPrograms = listOf(assignment),
                calendars = listOf(calendar)
            ),
            date = date,
            display = PlanDisplaySettings.DEFAULT
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
        semesterProgramId: String,
        startTime: LocalTime,
        endTime: LocalTime
    ) = ClassItem(
        id = id,
        semesterId = semesterId,
        semesterProgramId = semesterProgramId,
        name = id,
        type = "Wykład",
        dayOfWeek = DayOfWeek.MONDAY,
        startTime = startTime,
        endTime = endTime
    )
}
