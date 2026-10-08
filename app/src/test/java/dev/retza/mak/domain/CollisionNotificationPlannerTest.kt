package dev.retza.mak.domain

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollisionNotificationPlannerTest {
    private val clock = Clock.fixed(
        Instant.parse("2026-09-21T05:00:00Z"),
        ZoneId.of("Europe/Warsaw")
    )
    private val semester = Semester(id = "semester", name = "Semestr")
    private val calendar = AcademicCalendar(
        id = "calendar",
        startDate = LocalDate.of(2026, 9, 1),
        endDate = LocalDate.of(2026, 12, 31),
        firstWeekType = WeekType.A
    )
    private val course = StudyProgram(id = "course", name = "Informatyka", color = "#137B71")
    private val assignment = SemesterProgram(
        id = "assignment",
        semesterId = semester.id,
        studyProgramId = course.id,
        academicCalendarId = calendar.id
    )
    private val settings = CollisionNotificationSettings(
        enabled = true,
        eveningEnabled = true,
        beforeClassEnabled = true
    )

    private val monday = LocalDate.of(2026, 9, 21)
    private val tuesday = LocalDate.of(2026, 9, 22)
    private val wednesday = LocalDate.of(2026, 9, 23)

    @Test
    fun disabledProducesNothing() {
        val plan = planner().plan(
            data(item("a", monday, LocalTime.of(9, 0), LocalTime.of(10, 0))),
            settings.copy(enabled = false),
            PlanDisplaySettings.DEFAULT
        )

        assertTrue(plan.isEmpty())
    }

    @Test
    fun noCollisionProducesNothing() {
        val plan = planner().plan(
            data(
                item("a", monday, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                item("b", monday, LocalTime.of(10, 15), LocalTime.of(11, 0))
            ),
            settings,
            PlanDisplaySettings.DEFAULT
        )

        assertTrue(plan.isEmpty())
    }

    @Test
    fun classesWithoutBreakGetNotificationDescribingTheBreak() {
        val plan = planner().plan(
            data(
                item("a", tuesday, LocalTime.of(8, 15), LocalTime.of(9, 45)),
                item("b", tuesday, LocalTime.of(9, 55), LocalTime.of(11, 15))
            ),
            settings,
            PlanDisplaySettings(minimumBreakMinutes = 10)
        )

        val evening = plan.single { it.kind == CollisionNotificationKind.EVENING && it.date == tuesday }
        assertEquals("przerwa 10 min o 09:45", evening.groups.single().label)
    }

    @Test
    fun overlapGroupLabelIsItsRange() {
        val plan = planner().plan(
            data(
                item("a", tuesday, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                item("b", tuesday, LocalTime.of(9, 30), LocalTime.of(10, 30))
            ),
            settings,
            PlanDisplaySettings.DEFAULT
        )

        val evening = plan.single { it.kind == CollisionNotificationKind.EVENING && it.date == tuesday }
        assertEquals("09:30-10:00", evening.groups.single().label)
    }

    @Test
    fun schedulesEveningAndBeforeClassWithinHorizon() {
        val plan = planner().plan(
            data(
                item("a", monday, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                item("b", monday, LocalTime.of(9, 30), LocalTime.of(10, 30)),
                item("c", tuesday, LocalTime.of(12, 0), LocalTime.of(13, 0)),
                item("d", tuesday, LocalTime.of(12, 30), LocalTime.of(13, 30))
            ),
            settings,
            PlanDisplaySettings.DEFAULT
        )

        val mondayBefore = plan.single { it.kind == CollisionNotificationKind.BEFORE_CLASS && it.date == monday }
        assertEquals(LocalDateTime.of(2026, 9, 21, 8, 15), mondayBefore.triggerFrom)
        assertEquals(LocalDateTime.of(2026, 9, 21, 8, 30), mondayBefore.triggerUntil)
        assertEquals(listOf("a:2026-09-21", "b:2026-09-21"), mondayBefore.groups.single().occurrenceIds)
        assertTrue(plan.none { it.kind == CollisionNotificationKind.EVENING && it.date == monday })

        val evening = plan.single { it.kind == CollisionNotificationKind.EVENING && it.date == tuesday }
        assertEquals(LocalDateTime.of(2026, 9, 21, 20, 0), evening.triggerFrom)
        assertEquals(LocalDateTime.of(2026, 9, 21, 20, 15), evening.triggerUntil)
        assertEquals(1, evening.groups.size)

        val tuesdayBefore = plan.single { it.kind == CollisionNotificationKind.BEFORE_CLASS && it.date == tuesday }
        assertEquals(LocalDateTime.of(2026, 9, 22, 11, 15), tuesdayBefore.triggerFrom)
        assertEquals(LocalDateTime.of(2026, 9, 22, 11, 30), tuesdayBefore.triggerUntil)
    }

    @Test
    fun mergesTransitiveOverlapIntoOneGroup() {
        val plan = planner().plan(
            data(
                item("a", wednesday, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                item("b", wednesday, LocalTime.of(9, 30), LocalTime.of(10, 30)),
                item("c", wednesday, LocalTime.of(10, 0), LocalTime.of(11, 0))
            ),
            settings,
            PlanDisplaySettings.DEFAULT
        )

        val before = plan.filter { it.kind == CollisionNotificationKind.BEFORE_CLASS }
        assertEquals(1, before.size)
        assertEquals(
            listOf("a:2026-09-23", "b:2026-09-23", "c:2026-09-23"),
            before.single().groups.single().occurrenceIds
        )
    }

    @Test
    fun keepsIndependentGroupsSeparate() {
        val plan = planner().plan(
            data(
                item("a", wednesday, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                item("b", wednesday, LocalTime.of(9, 30), LocalTime.of(10, 30)),
                item("c", wednesday, LocalTime.of(12, 0), LocalTime.of(13, 0)),
                item("d", wednesday, LocalTime.of(12, 30), LocalTime.of(13, 30))
            ),
            settings,
            PlanDisplaySettings.DEFAULT
        )

        val before = plan.filter { it.kind == CollisionNotificationKind.BEFORE_CLASS }
        assertEquals(2, before.size)
        assertEquals(2, before.map { it.id }.toSet().size)
        val evening = plan.single { it.kind == CollisionNotificationKind.EVENING && it.date == wednesday }
        assertEquals(2, evening.groups.size)
    }

    @Test
    fun ignoresCollisionsBeyondHorizon() {
        val plan = planner().plan(
            data(
                item("a", LocalDate.of(2026, 10, 10), LocalTime.of(9, 0), LocalTime.of(10, 0)),
                item("b", LocalDate.of(2026, 10, 10), LocalTime.of(9, 30), LocalTime.of(10, 30))
            ),
            settings,
            PlanDisplaySettings.DEFAULT
        )

        assertTrue(plan.isEmpty())
    }

    @Test
    fun idsAreStableAcrossRuns() {
        val data = data(
            item("a", wednesday, LocalTime.of(9, 0), LocalTime.of(10, 0)),
            item("b", wednesday, LocalTime.of(9, 30), LocalTime.of(10, 30))
        )

        val first = planner().plan(data, settings, PlanDisplaySettings.DEFAULT).map { it.id }
        val second = planner().plan(data, settings, PlanDisplaySettings.DEFAULT).map { it.id }

        assertEquals(first, second)
    }

    @Test
    fun eveningCanBeDisabledIndependently() {
        val plan = planner().plan(
            data(
                item("a", tuesday, LocalTime.of(12, 0), LocalTime.of(13, 0)),
                item("b", tuesday, LocalTime.of(12, 30), LocalTime.of(13, 30))
            ),
            settings.copy(eveningEnabled = false),
            PlanDisplaySettings.DEFAULT
        )

        assertTrue(plan.none { it.kind == CollisionNotificationKind.EVENING })
        assertTrue(plan.any { it.kind == CollisionNotificationKind.BEFORE_CLASS })
    }

    private fun planner() = CollisionNotificationPlanner(ActivePlanProvider(), clock)

    private fun data(vararg classes: ClassItem) = ActivePlanData(
        semester = semester,
        classes = classes.toList(),
        courses = listOf(course),
        semesterPrograms = listOf(assignment),
        calendars = listOf(calendar)
    )

    private fun item(id: String, date: LocalDate, start: LocalTime, end: LocalTime) = ClassItem(
        id = id,
        semesterId = semester.id,
        semesterProgramId = assignment.id,
        name = id,
        type = "Wykład",
        dayOfWeek = date.dayOfWeek,
        startTime = start,
        endTime = end,
        recurrence = Recurrence.ONCE,
        date = date
    )
}
