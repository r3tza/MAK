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
    private val semester = Semester(id = 1L, name = "Semestr")
    private val calendar = AcademicCalendar(
        id = 1L,
        startDate = LocalDate.of(2026, 9, 1),
        endDate = LocalDate.of(2026, 12, 31),
        firstWeekType = WeekType.A
    )
    private val course = StudyProgram(id = 1L, name = "Informatyka", color = "#137B71")
    private val assignment = SemesterProgram(
        id = 1L,
        semesterId = semester.id,
        studyProgramId = course.id,
        academicCalendarId = calendar.id
    )
    private val settings = CollisionNotificationSettings(
        enabled = true,
        collisions = NotificationTopicSettings(eveningEnabled = true, beforeClassEnabled = true)
    )

    private val gapSettings = CollisionNotificationSettings(
        enabled = true,
        collisions = NotificationTopicSettings(eveningEnabled = false, beforeClassEnabled = false),
        gaps = NotificationTopicSettings(eveningEnabled = true, beforeClassEnabled = true)
    )

    private val monday = LocalDate.of(2026, 9, 21)
    private val tuesday = LocalDate.of(2026, 9, 22)
    private val wednesday = LocalDate.of(2026, 9, 23)

    @Test
    fun disabledProducesNothing() {
        val plan = planner().plan(
            data(item(1L, monday, LocalTime.of(9, 0), LocalTime.of(10, 0))),
            settings.copy(enabled = false),
            PlanDisplaySettings.DEFAULT
        )

        assertTrue(plan.isEmpty())
    }

    @Test
    fun noCollisionProducesNothing() {
        val plan = planner().plan(
            data(
                item(1L, monday, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                item(2L, monday, LocalTime.of(10, 15), LocalTime.of(11, 0))
            ),
            settings,
            PlanDisplaySettings.DEFAULT
        )

        assertTrue(plan.isEmpty())
    }

    @Test
    fun hiddenStudyProgramProducesNoNotification() {
        val plan = planner().plan(
            data(
                item(1L, tuesday, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                item(2L, tuesday, LocalTime.of(9, 30), LocalTime.of(10, 30))
            ),
            settings,
            PlanDisplaySettings.DEFAULT.copy(hiddenProgramIds = setOf(course.id))
        )

        assertTrue(plan.isEmpty())
    }

    @Test
    fun classesWithoutBreakGetNotificationDescribingTheBreak() {
        val plan = planner().plan(
            data(
                item(1L, tuesday, LocalTime.of(8, 15), LocalTime.of(9, 45)),
                item(2L, tuesday, LocalTime.of(9, 55), LocalTime.of(11, 15))
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
                item(1L, tuesday, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                item(2L, tuesday, LocalTime.of(9, 30), LocalTime.of(10, 30))
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
                item(1L, monday, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                item(2L, monday, LocalTime.of(9, 30), LocalTime.of(10, 30)),
                item(3L, tuesday, LocalTime.of(12, 0), LocalTime.of(13, 0)),
                item(4L, tuesday, LocalTime.of(12, 30), LocalTime.of(13, 30))
            ),
            settings,
            PlanDisplaySettings.DEFAULT
        )

        val mondayBefore = plan.single { it.kind == CollisionNotificationKind.BEFORE_CLASS && it.date == monday }
        assertEquals(LocalDateTime.of(2026, 9, 21, 8, 15), mondayBefore.triggerFrom)
        assertEquals(LocalDateTime.of(2026, 9, 21, 8, 30), mondayBefore.triggerUntil)
        assertEquals(listOf("1:2026-09-21", "2:2026-09-21"), mondayBefore.groups.single().occurrenceIds)
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
                item(1L, wednesday, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                item(2L, wednesday, LocalTime.of(9, 30), LocalTime.of(10, 30)),
                item(3L, wednesday, LocalTime.of(10, 0), LocalTime.of(11, 0))
            ),
            settings,
            PlanDisplaySettings.DEFAULT
        )

        val before = plan.filter { it.kind == CollisionNotificationKind.BEFORE_CLASS }
        assertEquals(1, before.size)
        assertEquals(
            listOf("1:2026-09-23", "2:2026-09-23", "3:2026-09-23"),
            before.single().groups.single().occurrenceIds
        )
    }

    @Test
    fun keepsIndependentGroupsSeparate() {
        val plan = planner().plan(
            data(
                item(1L, wednesday, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                item(2L, wednesday, LocalTime.of(9, 30), LocalTime.of(10, 30)),
                item(3L, wednesday, LocalTime.of(12, 0), LocalTime.of(13, 0)),
                item(4L, wednesday, LocalTime.of(12, 30), LocalTime.of(13, 30))
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
                item(1L, LocalDate.of(2026, 10, 10), LocalTime.of(9, 0), LocalTime.of(10, 0)),
                item(2L, LocalDate.of(2026, 10, 10), LocalTime.of(9, 30), LocalTime.of(10, 30))
            ),
            settings,
            PlanDisplaySettings.DEFAULT
        )

        assertTrue(plan.isEmpty())
    }

    @Test
    fun idsAreStableAcrossRuns() {
        val data = data(
            item(1L, wednesday, LocalTime.of(9, 0), LocalTime.of(10, 0)),
            item(2L, wednesday, LocalTime.of(9, 30), LocalTime.of(10, 30))
        )

        val first = planner().plan(data, settings, PlanDisplaySettings.DEFAULT).map { it.id }
        val second = planner().plan(data, settings, PlanDisplaySettings.DEFAULT).map { it.id }

        assertEquals(first, second)
    }

    @Test
    fun eveningCanBeDisabledIndependently() {
        val plan = planner().plan(
            data(
                item(1L, tuesday, LocalTime.of(12, 0), LocalTime.of(13, 0)),
                item(2L, tuesday, LocalTime.of(12, 30), LocalTime.of(13, 30))
            ),
            settings.copy(collisions = settings.collisions.copy(eveningEnabled = false)),
            PlanDisplaySettings.DEFAULT
        )

        assertTrue(plan.none { it.kind == CollisionNotificationKind.EVENING })
        assertTrue(plan.any { it.kind == CollisionNotificationKind.BEFORE_CLASS })
    }

    @Test
    fun gapEveningIsPlannedAtItsOwnHour() {
        val plan = planner().plan(
            data(
                item(1L, tuesday, LocalTime.of(8, 0), LocalTime.of(9, 30)),
                item(2L, tuesday, LocalTime.of(11, 0), LocalTime.of(12, 0))
            ),
            gapSettings.copy(gaps = gapSettings.gaps.copy(beforeClassEnabled = false, eveningHour = LocalTime.of(19, 0))),
            PlanDisplaySettings.DEFAULT
        )

        val evening = plan.single()
        assertEquals(CollisionNotificationKind.GAP_EVENING, evening.kind)
        assertEquals(tuesday, evening.date)
        assertEquals(LocalDateTime.of(2026, 9, 21, 19, 0), evening.triggerFrom)
        assertEquals(LocalDateTime.of(2026, 9, 21, 19, 15), evening.triggerUntil)
        assertEquals("09:30-11:00", evening.groups.single().label)
    }

    @Test
    fun gapBeforeClassFiresLeadMinutesBeforeClassBeforeGap() {
        val plan = planner().plan(
            data(
                item(1L, tuesday, LocalTime.of(8, 0), LocalTime.of(9, 30)),
                item(2L, tuesday, LocalTime.of(11, 0), LocalTime.of(12, 0))
            ),
            gapSettings.copy(gaps = gapSettings.gaps.copy(eveningEnabled = false, leadMinutes = 15)),
            PlanDisplaySettings.DEFAULT
        )

        val beforeClass = plan.single()
        assertEquals(CollisionNotificationKind.GAP_BEFORE_CLASS, beforeClass.kind)
        assertEquals(LocalDateTime.of(2026, 9, 22, 7, 30), beforeClass.triggerFrom)
        assertEquals(LocalDateTime.of(2026, 9, 22, 7, 45), beforeClass.triggerUntil)
        assertEquals(listOf("1:2026-09-22"), beforeClass.groups.single().occurrenceIds)
    }

    @Test
    fun cancelledClassDoesNotCreateGapNotification() {
        val weeklyClass = item(2L, tuesday, LocalTime.of(11, 0), LocalTime.of(12, 0))
            .copy(recurrence = Recurrence.EVERY_WEEK, date = null)
        val base = data(item(1L, tuesday, LocalTime.of(8, 0), LocalTime.of(9, 30)), weeklyClass)
        val cancellation = OccurrenceChange(
            id = 1L,
            classId = weeklyClass.id,
            originalDate = tuesday,
            kind = OccurrenceChangeKind.CANCELLED
        )

        val plan = planner().plan(
            base.copy(occurrenceChanges = listOf(cancellation)),
            gapSettings,
            PlanDisplaySettings.DEFAULT
        )

        assertTrue(plan.none { it.date == tuesday })
    }

    @Test
    fun hiddenProgramDoesNotCreateGapNotification() {
        val otherProgram = StudyProgram(id = 2L, name = "Filologia", color = "#AA0000")
        val otherAssignment = SemesterProgram(
            id = 2L,
            semesterId = semester.id,
            studyProgramId = otherProgram.id,
            academicCalendarId = calendar.id
        )
        val plan = planner().plan(
            ActivePlanData(
                semester = semester,
                classes = listOf(
                    item(1L, tuesday, LocalTime.of(8, 0), LocalTime.of(9, 30)),
                    item(2L, tuesday, LocalTime.of(11, 0), LocalTime.of(12, 0))
                        .copy(semesterProgramId = otherAssignment.id)
                ),
                courses = listOf(course, otherProgram),
                semesterPrograms = listOf(assignment, otherAssignment),
                calendars = listOf(calendar)
            ),
            gapSettings,
            PlanDisplaySettings.DEFAULT.copy(hiddenProgramIds = setOf(otherProgram.id))
        )

        assertTrue(plan.isEmpty())
    }

    @Test
    fun gapKindsOffPlanNothingForGaps() {
        val plan = planner().plan(
            data(
                item(1L, tuesday, LocalTime.of(8, 0), LocalTime.of(9, 30)),
                item(2L, tuesday, LocalTime.of(11, 0), LocalTime.of(12, 0))
            ),
            settings.copy(collisions = settings.collisions.copy(eveningEnabled = false, beforeClassEnabled = false)),
            PlanDisplaySettings.DEFAULT
        )

        assertTrue(plan.isEmpty())
    }

    @Test
    fun twoGapsGiveTwoBeforeClassAndOneEvening() {
        val plan = planner().plan(
            data(
                item(1L, tuesday, LocalTime.of(8, 0), LocalTime.of(9, 0)),
                item(2L, tuesday, LocalTime.of(10, 0), LocalTime.of(11, 0)),
                item(3L, tuesday, LocalTime.of(13, 0), LocalTime.of(14, 0))
            ),
            gapSettings,
            PlanDisplaySettings.DEFAULT
        )

        val beforeClass = plan.filter { it.kind == CollisionNotificationKind.GAP_BEFORE_CLASS }
        assertEquals(2, beforeClass.size)
        assertEquals(2, beforeClass.map { it.id }.toSet().size)
        val evening = plan.single { it.kind == CollisionNotificationKind.GAP_EVENING }
        assertEquals(2, evening.groups.size)
        assertEquals(3, plan.size)
    }

    private fun planner() = CollisionNotificationPlanner(ActivePlanProvider(), clock)

    private fun data(vararg classes: ClassItem) = ActivePlanData(
        semester = semester,
        classes = classes.toList(),
        courses = listOf(course),
        semesterPrograms = listOf(assignment),
        calendars = listOf(calendar)
    )

    private fun item(id: Long, date: LocalDate, start: LocalTime, end: LocalTime) = ClassItem(
        id = id,
        semesterId = semester.id,
        semesterProgramId = assignment.id,
        name = "Class $id",
        type = "Wykład",
        dayOfWeek = date.dayOfWeek,
        startTime = start,
        endTime = end,
        recurrence = Recurrence.ONCE,
        date = date
    )
}

private fun CollisionNotificationPlanner.plan(
    data: ActivePlanData,
    settings: CollisionNotificationSettings,
    display: PlanDisplaySettings
) = plan(ActivePlanInputs(data, display), settings)
