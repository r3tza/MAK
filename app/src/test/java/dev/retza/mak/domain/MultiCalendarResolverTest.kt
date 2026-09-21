package dev.retza.mak.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiCalendarResolverTest {
    private val provider = ActivePlanProvider()
    private val semester = Semester(id = "semester", name = "Winter")
    private val calendarA = AcademicCalendar(
        id = "calendar-a",
        startDate = LocalDate.of(2026, 10, 5),
        endDate = LocalDate.of(2027, 2, 28),
        firstWeekType = WeekType.A
    )
    private val calendarB = AcademicCalendar(
        id = "calendar-b",
        startDate = LocalDate.of(2026, 10, 5),
        endDate = LocalDate.of(2027, 2, 28),
        firstWeekType = WeekType.B
    )
    private val programA = StudyProgram("program-a", "Informatyka", "#111111")
    private val programB = StudyProgram("program-b", "Matematyka", "#222222")
    private val programs = listOf(
        SemesterProgram("assignment-a", semester.id, programA.id, calendarA.id),
        SemesterProgram("assignment-b", semester.id, programB.id, calendarB.id)
    )
    private val monday = LocalDate.of(2026, 10, 5)

    private fun classItem(
        id: String,
        programId: String,
        recurrence: Recurrence,
        startTime: LocalTime,
        endTime: LocalTime
    ) = ClassItem(
        id = id,
        semesterId = semester.id,
        name = "Zajęcia $id",
        type = "Wykład",
        semesterProgramId = programId,
        dayOfWeek = DayOfWeek.MONDAY,
        startTime = startTime,
        endTime = endTime,
        recurrence = recurrence
    )

    private fun data(classes: List<ClassItem>) = ActivePlanData(
        semester = semester,
        classes = classes,
        courses = listOf(programA, programB),
        semesterPrograms = programs,
        calendars = listOf(calendarA, calendarB)
    )

    @Test
    fun programsWithOppositeCalendarsFollowTheirOwnWeekType() {
        val first = classItem("a", "assignment-a", Recurrence.A_WEEK, LocalTime.of(9, 0), LocalTime.of(10, 0))
        val second = classItem("b", "assignment-b", Recurrence.A_WEEK, LocalTime.of(9, 0), LocalTime.of(10, 0))

        val plan = provider.resolve(data(listOf(first, second)), monday)

        assertEquals(listOf("a"), plan.schedule.occurrences.map { it.classId })
    }

    @Test
    fun oneOffOutsideProgramCalendarIsNotShown() {
        val outside = LocalDate.of(2026, 9, 28)
        val once = classItem("once", "assignment-a", Recurrence.ONCE, LocalTime.of(9, 0), LocalTime.of(10, 0))
            .copy(date = outside)

        val plan = provider.resolve(data(listOf(once)), outside)

        assertTrue(plan.schedule.occurrences.isEmpty())
    }

    @Test
    fun mixedWeekTypesAreReportedInsteadOfOneLabel() {
        val plan = provider.resolve(data(emptyList()), monday)

        assertTrue(plan.schedule.hasMixedWeekTypes)
        assertNull(plan.schedule.week)
    }

    @Test
    fun singleCalendarKeepsOneWeekLabel() {
        val singleData = ActivePlanData(
            semester = semester,
            classes = emptyList(),
            courses = listOf(programA),
            semesterPrograms = listOf(programs.first()),
            calendars = listOf(calendarA)
        )

        val plan = provider.resolve(singleData, monday)

        assertFalse(plan.schedule.hasMixedWeekTypes)
        assertEquals(WeekType.A, plan.schedule.weekType)
    }

    @Test
    fun programsWithDifferentCalendarsShareCollisions() {
        val first = classItem("a", "assignment-a", Recurrence.EVERY_WEEK, LocalTime.of(9, 0), LocalTime.of(10, 0))
        val second = classItem("b", "assignment-b", Recurrence.EVERY_WEEK, LocalTime.of(9, 30), LocalTime.of(10, 30))

        val plan = provider.resolve(data(listOf(first, second)), monday)

        assertEquals(listOf("a", "b"), plan.schedule.occurrences.map { it.classId })
        assertEquals(1, plan.collisions.size)
        assertEquals(LocalTime.of(9, 30), plan.collisions.single().overlapStart)
        assertEquals(LocalTime.of(10, 0), plan.collisions.single().overlapEnd)
    }
}
