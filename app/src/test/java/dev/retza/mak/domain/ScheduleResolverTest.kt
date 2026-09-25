package dev.retza.mak.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleResolverTest {
    private val semester = Semester(id = "semester-1", name = "Winter")
    private val calendar = AcademicCalendar(
        id = "calendar-1",
        startDate = LocalDate.of(2026, 1, 1),
        endDate = LocalDate.of(2026, 1, 31),
        firstWeekType = WeekType.A
    )
    private val course = StudyProgram("course-1", "Computer science", "#123456")
    private val assignment = SemesterProgram("assignment-1", semester.id, course.id, calendar.id)
    private val resolver = ScheduleResolver()

    private fun classItem(
        id: String = "class-1",
        recurrence: Recurrence = Recurrence.EVERY_WEEK,
        day: DayOfWeek = DayOfWeek.MONDAY,
        date: LocalDate? = null,
        start: LocalTime = LocalTime.of(10, 0),
        end: LocalTime = LocalTime.of(11, 0),
        note: String? = "Bring a laptop",
        room: String? = null
    ) = ClassItem(
        id = id,
        semesterId = semester.id,
        semesterProgramId = assignment.id,
        name = "Programming",
        type = "lecture",
        teacherName = "Jan Kowalski",
        dayOfWeek = day,
        startTime = start,
        endTime = end,
        room = room,
        recurrence = recurrence,
        date = date,
        classNote = note
    )

    private fun resolve(
        date: LocalDate,
        classes: Collection<ClassItem>,
        changes: Collection<OccurrenceChange> = emptyList(),
        notes: Collection<OccurrenceNote> = emptyList(),
        overrides: Collection<WeekOverride> = emptyList(),
        extraCourses: Collection<StudyProgram> = emptyList()
    ) = resolver.resolve(
        date = date,
        semester = semester,
        classes = classes,
        courses = listOf(course) + extraCourses,
        semesterPrograms = listOf(assignment),
        calendars = listOf(calendar),
        weekOverrides = overrides,
        occurrenceChanges = changes,
        occurrenceNotes = notes
    )

    @Test
    fun recurringClassesUseEveryAAndBRecurrence() {
        val date = LocalDate.of(2026, 1, 5)
        val result = resolve(
            date = date,
            classes = listOf(
                classItem(id = "every"),
                classItem(id = "a", recurrence = Recurrence.A_WEEK),
                classItem(id = "b", recurrence = Recurrence.B_WEEK)
            )
        )

        assertEquals(setOf("every", "b"), result.occurrences.map { it.classId }.toSet())
        assertEquals(WeekType.B, result.weekType)
    }

    @Test
    fun oneTimeClassIsResolvedOnlyOnItsDate() {
        val onceDate = LocalDate.of(2026, 1, 7)
        val once = classItem(
            id = "once",
            recurrence = Recurrence.ONCE,
            day = DayOfWeek.WEDNESDAY,
            date = onceDate
        )

        assertEquals(listOf("once"), resolve(onceDate, listOf(once)).occurrences.map { it.classId })
        assertTrue(resolve(LocalDate.of(2026, 1, 8), listOf(once)).occurrences.isEmpty())
    }

    @Test
    fun cancelledOccurrenceIsRemoved() {
        val date = LocalDate.of(2026, 1, 5)
        val change = OccurrenceChange("cancel", "class-1", date, OccurrenceChangeKind.CANCELLED)

        assertTrue(resolve(date, listOf(classItem()), changes = listOf(change)).occurrences.isEmpty())
    }

    @Test
    fun modifiedOccurrenceUsesOnlyItsNewData() {
        val date = LocalDate.of(2026, 1, 5)
        val change = OccurrenceChange(
            id = "modify",
            classId = "class-1",
            originalDate = date,
            kind = OccurrenceChangeKind.MODIFIED,
            startTime = LocalTime.of(12, 0),
            endTime = LocalTime.of(13, 30),
            room = "L204"
        )

        val occurrence = resolve(date, listOf(classItem()), changes = listOf(change)).occurrences.single()
        assertEquals(LocalTime.of(12, 0), occurrence.startTime)
        assertEquals(LocalTime.of(13, 30), occurrence.endTime)
        assertEquals("L204", occurrence.room)
        assertEquals("Bring a laptop", occurrence.classNote)
    }

    @Test
    fun emptyRoomOverrideRemovesRoomAndRestoreBringsItBack() {
        val date = LocalDate.of(2026, 1, 5)
        val baseWithRoom = classItem(room = "L204")
        val cleared = OccurrenceChange(
            id = "clear-room",
            classId = "class-1",
            originalDate = date,
            kind = OccurrenceChangeKind.MODIFIED,
            room = ""
        )

        val withoutRoom = resolve(date, listOf(baseWithRoom), changes = listOf(cleared)).occurrences.single()
        assertEquals(null, withoutRoom.room)
        val restored = resolve(date, listOf(baseWithRoom)).occurrences.single()
        assertEquals("L204", restored.room)
    }

    @Test
    fun movedOccurrenceDisappearsFromSourceAndAppearsAtTarget() {
        val source = LocalDate.of(2026, 1, 5)
        val target = LocalDate.of(2026, 1, 7)
        val change = OccurrenceChange(
            id = "move",
            classId = "class-1",
            originalDate = source,
            kind = OccurrenceChangeKind.MODIFIED,
            targetDate = target
        )
        val recurring = classItem(day = DayOfWeek.MONDAY)

        assertTrue(resolve(source, listOf(recurring), changes = listOf(change)).occurrences.isEmpty())
        val moved = resolve(target, listOf(recurring), changes = listOf(change)).occurrences.single()
        assertEquals(target, moved.date)
        assertEquals(source, moved.originalDate)
    }

    @Test
    fun occurrenceMovedOntoRegularDayOfSameClassKeepsDistinctIdentity() {
        val source = LocalDate.of(2026, 1, 5)
        val target = LocalDate.of(2026, 1, 12)
        val change = OccurrenceChange(
            id = "move",
            classId = "class-1",
            originalDate = source,
            kind = OccurrenceChangeKind.MODIFIED,
            targetDate = target,
            startTime = LocalTime.of(10, 30),
            endTime = LocalTime.of(11, 30)
        )

        val schedule = resolve(target, listOf(classItem(day = DayOfWeek.MONDAY)), changes = listOf(change))

        assertEquals(2, schedule.occurrences.size)
        assertEquals(setOf("class-1:$target", "class-1:$source"), schedule.occurrences.map { it.id }.toSet())
    }

    @Test
    fun collisionBetweenMovedAndRegularOccurrenceOfSameClassIsDetected() {
        val source = LocalDate.of(2026, 1, 5)
        val target = LocalDate.of(2026, 1, 12)
        val change = OccurrenceChange(
            id = "move",
            classId = "class-1",
            originalDate = source,
            kind = OccurrenceChangeKind.MODIFIED,
            targetDate = target,
            startTime = LocalTime.of(10, 30),
            endTime = LocalTime.of(11, 30)
        )
        val schedule = resolve(target, listOf(classItem(day = DayOfWeek.MONDAY)), changes = listOf(change))

        val collision = CollisionDetector().detect(schedule).single()

        assertEquals(LocalTime.of(10, 30), collision.overlapStart)
        assertEquals(LocalTime.of(11, 0), collision.overlapEnd)
    }

    @Test
    fun removingOccurrenceChangeRestoresBaseOccurrence() {
        val date = LocalDate.of(2026, 1, 5)
        val cancelled = OccurrenceChange("cancel", "class-1", date, OccurrenceChangeKind.CANCELLED)

        assertTrue(resolve(date, listOf(classItem()), changes = listOf(cancelled)).occurrences.isEmpty())
        assertEquals(1, resolve(date, listOf(classItem())).occurrences.size)
    }

    @Test
    fun commonAndOccurrenceNotesRemainSeparate() {
        val date = LocalDate.of(2026, 1, 5)
        val note = OccurrenceNote("note", "class-1", date, "Exam today")

        val occurrence = resolve(date, listOf(classItem()), notes = listOf(note)).occurrences.single()
        assertEquals("Bring a laptop", occurrence.classNote)
        assertEquals("Exam today", occurrence.occurrenceNoteBody)
    }

    @Test
    fun classesFromAnotherSemesterAreIgnored() {
        val otherSemesterClass = classItem().copy(semesterId = "semester-2", id = "other")

        val result = resolve(LocalDate.of(2026, 1, 5), listOf(classItem(), otherSemesterClass))
        assertFalse(result.occurrences.any { it.classId == "other" })
    }
}
