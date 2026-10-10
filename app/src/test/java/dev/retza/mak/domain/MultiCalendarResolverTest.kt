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
    private val semester = Semester(id = 1L, name = "Winter")
    private val calendarA = AcademicCalendar(
        id = 1L,
        startDate = LocalDate.of(2026, 10, 5),
        endDate = LocalDate.of(2027, 2, 28),
        firstWeekType = WeekType.A
    )
    private val calendarB = AcademicCalendar(
        id = 2L,
        startDate = LocalDate.of(2026, 10, 5),
        endDate = LocalDate.of(2027, 2, 28),
        firstWeekType = WeekType.B
    )
    private val programA = StudyProgram(1L, "Informatyka", "#111111")
    private val programB = StudyProgram(2L, "Matematyka", "#222222")
    private val programs = listOf(
        SemesterProgram(1L, semester.id, programA.id, calendarA.id),
        SemesterProgram(2L, semester.id, programB.id, calendarB.id)
    )
    private val monday = LocalDate.of(2026, 10, 5)

    private fun classItem(
        id: Long,
        programId: Long,
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
        val first = classItem(1L, 1L, Recurrence.A_WEEK, LocalTime.of(9, 0), LocalTime.of(10, 0))
        val second = classItem(2L, 2L, Recurrence.A_WEEK, LocalTime.of(9, 0), LocalTime.of(10, 0))

        val plan = provider.resolve(data(listOf(first, second)), monday, PlanDisplaySettings.DEFAULT)

        assertEquals(listOf(1L), plan.schedule.occurrences.map { it.classId })
    }

    @Test
    fun oneOffOutsideProgramCalendarIsNotShown() {
        val outside = LocalDate.of(2026, 9, 28)
        val once = classItem(3L, 1L, Recurrence.ONCE, LocalTime.of(9, 0), LocalTime.of(10, 0))
            .copy(date = outside)

        val plan = provider.resolve(data(listOf(once)), outside, PlanDisplaySettings.DEFAULT)

        assertTrue(plan.schedule.occurrences.isEmpty())
    }

    @Test
    fun mixedWeekTypesAreReportedInsteadOfOneLabel() {
        val plan = provider.resolve(data(emptyList()), monday, PlanDisplaySettings.DEFAULT)

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

        val plan = provider.resolve(singleData, monday, PlanDisplaySettings.DEFAULT)

        assertFalse(plan.schedule.hasMixedWeekTypes)
        assertEquals(WeekType.A, plan.schedule.weekType)
    }

    @Test
    fun programsWithDifferentCalendarsShareCollisions() {
        val first = classItem(1L, 1L, Recurrence.EVERY_WEEK, LocalTime.of(9, 0), LocalTime.of(10, 0))
        val second = classItem(2L, 2L, Recurrence.EVERY_WEEK, LocalTime.of(9, 30), LocalTime.of(10, 30))

        val plan = provider.resolve(data(listOf(first, second)), monday, PlanDisplaySettings.DEFAULT)

        assertEquals(listOf(1L, 2L), plan.schedule.occurrences.map { it.classId })
        assertEquals(1, plan.collisions.size)
        assertEquals(LocalTime.of(9, 30), plan.collisions.single().start)
        assertEquals(LocalTime.of(10, 0), plan.collisions.single().end)
    }
}
