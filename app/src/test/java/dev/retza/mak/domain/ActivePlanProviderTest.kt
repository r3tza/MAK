package dev.retza.mak.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivePlanProviderTest {
    @Test
    fun providerReturnsResolvedScheduleAndCollisionsFromTheSamePlan() {
        val date = LocalDate.of(2026, 9, 21)
        val semester = Semester(id = 1L, name = "Semestr")
        val calendar = AcademicCalendar(
            id = 1L,
            startDate = date,
            endDate = date.plusDays(7),
            firstWeekType = WeekType.A
        )
        val course = StudyProgram(1L, "Informatyka", "#137B71")
        val assignment = SemesterProgram(1L, semester.id, course.id, calendar.id)
        val first = classItem(1L, "first", semester.id, assignment.id, LocalTime.of(9, 0), LocalTime.of(10, 0))
        val second = classItem(2L, "second", semester.id, assignment.id, LocalTime.of(9, 30), LocalTime.of(10, 30))

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

    @Test
    fun hiddenStudyProgramLeavesNoClassesCollisionsOrGaps() {
        val shown = twoProgramPlan(PlanDisplaySettings.DEFAULT)
        val hidden = twoProgramPlan(PlanDisplaySettings.DEFAULT.copy(hiddenProgramIds = setOf(3L)))

        assertEquals(3, shown.schedule.occurrences.size)
        assertEquals(1, shown.collisions.size)
        assertEquals(1, countGaps(shown.schedule.occurrences, thresholdMinutes = 30))
        assertEquals(listOf("first"), hidden.schedule.occurrences.map { it.name })
        assertTrue(hidden.collisions.isEmpty())
        assertEquals(0, countGaps(hidden.schedule.occurrences, thresholdMinutes = 30))
    }

    @Test
    fun hidingEveryStudyProgramLeavesAnEmptyPlan() {
        val plan = twoProgramPlan(PlanDisplaySettings.DEFAULT.copy(hiddenProgramIds = setOf(2L, 3L)))

        assertTrue(plan.schedule.occurrences.isEmpty())
    }

    @Test
    fun gapCountUsesThresholdFromDisplaySettings() {
        val date = LocalDate.of(2026, 9, 21)
        val semester = Semester(id = 1L, name = "Semestr")
        val calendar = AcademicCalendar(id = 1L, startDate = date, endDate = date.plusDays(7), firstWeekType = WeekType.A)
        val course = StudyProgram(1L, "Informatyka", "#137B71")
        val assignment = SemesterProgram(1L, semester.id, course.id, calendar.id)
        val data = ActivePlanData(
            semester = semester,
            classes = listOf(
                classItem(1L, "first", semester.id, assignment.id, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                classItem(2L, "second", semester.id, assignment.id, LocalTime.of(10, 40), LocalTime.of(11, 40))
            ),
            courses = listOf(course),
            semesterPrograms = listOf(assignment),
            calendars = listOf(calendar)
        )

        val provider = ActivePlanProvider()
        val thirtyMinutes = provider.resolve(data, date, PlanDisplaySettings(0, gapThresholdMinutes = 30))
        val fortyFiveMinutes = provider.resolve(data, date, PlanDisplaySettings(0, gapThresholdMinutes = 45))

        assertEquals(1, thirtyMinutes.gapCount)
        assertEquals(0, fortyFiveMinutes.gapCount)
    }

    private fun twoProgramPlan(display: PlanDisplaySettings): ActivePlan {
        val date = LocalDate.of(2026, 9, 21)
        val semester = Semester(id = 1L, name = "Semestr")
        val calendar = AcademicCalendar(id = 1L, startDate = date, endDate = date.plusDays(7), firstWeekType = WeekType.A)
        val informatics = StudyProgram(2L, "Informatyka", "#137B71")
        val management = StudyProgram(3L, "Zarządzanie", "#7B1371")
        val first = SemesterProgram(2L, semester.id, informatics.id, calendar.id)
        val second = SemesterProgram(3L, semester.id, management.id, calendar.id)
        return ActivePlanProvider().resolve(
            data = ActivePlanData(
                semester = semester,
                classes = listOf(
                    classItem(1L, "first", semester.id, first.id, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                    classItem(3L, "overlapping", semester.id, second.id, LocalTime.of(9, 30), LocalTime.of(10, 30)),
                    classItem(4L, "after-gap", semester.id, second.id, LocalTime.of(12, 0), LocalTime.of(13, 0))
                ),
                courses = listOf(informatics, management),
                semesterPrograms = listOf(first, second),
                calendars = listOf(calendar)
            ),
            date = date,
            display = display
        )
    }

    private fun classItem(
        id: Long,
        name: String,
        semesterId: Long,
        semesterProgramId: Long,
        startTime: LocalTime,
        endTime: LocalTime
    ) = ClassItem(
        id = id,
        semesterId = semesterId,
        semesterProgramId = semesterProgramId,
        name = name,
        type = "Wykład",
        dayOfWeek = DayOfWeek.MONDAY,
        startTime = startTime,
        endTime = endTime
    )
}
