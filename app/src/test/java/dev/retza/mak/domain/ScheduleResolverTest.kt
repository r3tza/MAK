package dev.retza.mak.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleResolverTest {
    private val semester = Semester(id = 1L, name = "Winter")
    private val calendar = AcademicCalendar(
        id = 1L,
        startDate = LocalDate.of(2026, 1, 1),
        endDate = LocalDate.of(2026, 1, 31),
        firstWeekType = WeekType.A
    )
    private val course = StudyProgram(1L, "Computer science", "#123456")
    private val assignment = SemesterProgram(1L, semester.id, course.id, calendar.id)
    private val resolver = ScheduleResolver()

    private fun classItem(
        id: Long = 1L,
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
                classItem(id = 2L),
                classItem(id = 3L, recurrence = Recurrence.A_WEEK),
                classItem(id = 4L, recurrence = Recurrence.B_WEEK)
            )
        )

        assertEquals(setOf(2L, 4L), result.occurrences.map { it.classId }.toSet())
        assertEquals(WeekStatus.Single(WeekType.B), result.weekStatus)
    }

    @Test
    fun oneTimeClassIsResolvedOnlyOnItsDate() {
        val onceDate = LocalDate.of(2026, 1, 7)
        val once = classItem(
            id = 5L,
            recurrence = Recurrence.ONCE,
            day = DayOfWeek.WEDNESDAY,
            date = onceDate
        )

        assertEquals(listOf(5L), resolve(onceDate, listOf(once)).occurrences.map { it.classId })
        assertTrue(resolve(LocalDate.of(2026, 1, 8), listOf(once)).occurrences.isEmpty())
    }

    @Test
    fun modifiedOccurrenceUsesOnlyItsNewData() {
        val date = LocalDate.of(2026, 1, 5)
        val change = OccurrenceChange(
            id = 1L,
            classId = 1L,
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
            id = 2L,
            classId = 1L,
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
            id = 3L,
            classId = 1L,
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
            id = 3L,
            classId = 1L,
            originalDate = source,
            kind = OccurrenceChangeKind.MODIFIED,
            targetDate = target,
            startTime = LocalTime.of(10, 30),
            endTime = LocalTime.of(11, 30)
        )

        val schedule = resolve(target, listOf(classItem(day = DayOfWeek.MONDAY)), changes = listOf(change))

        assertEquals(2, schedule.occurrences.size)
        assertEquals(setOf("1:$target", "1:$source"), schedule.occurrences.map { it.id }.toSet())
    }

    @Test
    fun collisionBetweenMovedAndRegularOccurrenceOfSameClassIsDetected() {
        val source = LocalDate.of(2026, 1, 5)
        val target = LocalDate.of(2026, 1, 12)
        val change = OccurrenceChange(
            id = 3L,
            classId = 1L,
            originalDate = source,
            kind = OccurrenceChangeKind.MODIFIED,
            targetDate = target,
            startTime = LocalTime.of(10, 30),
            endTime = LocalTime.of(11, 30)
        )
        val schedule = resolve(target, listOf(classItem(day = DayOfWeek.MONDAY)), changes = listOf(change))

        val collision = CollisionDetector().detect(schedule, minimumBreakMinutes = 0).single()

        assertEquals(LocalTime.of(10, 30), collision.start)
        assertEquals(LocalTime.of(11, 0), collision.end)
    }

    @Test
    fun occurrenceNoteFollowsMovedOccurrenceAndReturnsAfterRestore() {
        val source = LocalDate.of(2026, 1, 5)
        val target = LocalDate.of(2026, 1, 7)
        val note = OccurrenceNote(1L, 1L, source, "Oddać projekt")
        val change = OccurrenceChange(
            id = 3L,
            classId = 1L,
            originalDate = source,
            kind = OccurrenceChangeKind.MODIFIED,
            targetDate = target
        )
        val recurring = classItem(day = DayOfWeek.MONDAY)

        val moved = resolve(target, listOf(recurring), changes = listOf(change), notes = listOf(note))
            .occurrences.single()
        val restored = resolve(source, listOf(recurring), notes = listOf(note)).occurrences.single()

        assertEquals("Oddać projekt", moved.occurrenceNoteBody)
        assertEquals("Oddać projekt", restored.occurrenceNoteBody)
    }

    @Test
    fun removingOccurrenceChangeRestoresBaseOccurrence() {
        val date = LocalDate.of(2026, 1, 5)
        val cancelled = OccurrenceChange(4L, 1L, date, OccurrenceChangeKind.CANCELLED)

        assertTrue(resolve(date, listOf(classItem()), changes = listOf(cancelled)).occurrences.isEmpty())
        assertEquals(1, resolve(date, listOf(classItem())).occurrences.size)
    }

    @Test
    fun commonAndOccurrenceNotesRemainSeparate() {
        val date = LocalDate.of(2026, 1, 5)
        val note = OccurrenceNote(1L, 1L, date, "Exam today")

        val occurrence = resolve(date, listOf(classItem()), notes = listOf(note)).occurrences.single()
        assertEquals("Bring a laptop", occurrence.classNote)
        assertEquals("Exam today", occurrence.occurrenceNoteBody)
    }

    @Test
    fun classesFromAnotherSemesterAreIgnored() {
        val otherSemesterClass = classItem().copy(semesterId = 2L, id = 6L)

        val result = resolve(LocalDate.of(2026, 1, 5), listOf(classItem(), otherSemesterClass))
        assertFalse(result.occurrences.any { it.classId == 6L })
    }

    @Test
    fun movedTermStatusIsMovedAndChangedTermIsModified() {
        val monday = LocalDate.of(2026, 1, 5)
        val tuesday = LocalDate.of(2026, 1, 6)
        val nextMonday = LocalDate.of(2026, 1, 12)
        val moved = OccurrenceChange(3L, 1L, monday, OccurrenceChangeKind.MODIFIED, targetDate = tuesday)
        val retimed = OccurrenceChange(
            5L,
            1L,
            nextMonday,
            OccurrenceChangeKind.MODIFIED,
            startTime = LocalTime.of(12, 0)
        )
        val changes = listOf(moved, retimed)
        val once = classItem(id = 5L, recurrence = Recurrence.ONCE, date = monday)

        assertEquals(
            OccurrenceStatus.MOVED,
            resolve(tuesday, listOf(classItem()), changes = changes).occurrences.single().status
        )
        assertEquals(
            OccurrenceStatus.MODIFIED,
            resolve(nextMonday, listOf(classItem()), changes = changes).occurrences.single().status
        )
        assertEquals(
            OccurrenceStatus.SCHEDULED,
            resolve(LocalDate.of(2026, 1, 19), listOf(classItem()), changes = changes).occurrences.single().status
        )
        assertEquals(OccurrenceStatus.ONE_OFF, resolve(monday, listOf(once)).occurrences.single().status)
    }

    @Test
    fun weekStatusReportsMixedAndOutsideSemester() {
        val date = LocalDate.of(2026, 1, 5)
        val otherCalendar = AcademicCalendar(
            id = 2L,
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 1, 31),
            firstWeekType = WeekType.B
        )
        val otherAssignment = SemesterProgram(2L, semester.id, course.id, otherCalendar.id)

        val mixed = resolver.resolve(
            date = date,
            semester = semester,
            classes = emptyList(),
            courses = listOf(course),
            semesterPrograms = listOf(assignment, otherAssignment),
            calendars = listOf(calendar, otherCalendar)
        )

        assertEquals(WeekStatus.Mixed, mixed.weekStatus)
        assertEquals(setOf(1L, 2L), mixed.weeksByCalendarId.keys)
        assertEquals(WeekStatus.Single(WeekType.A), resolve(LocalDate.of(2026, 1, 1), emptyList()).weekStatus)
        assertEquals(
            WeekStatus.OutsideSemester,
            resolve(LocalDate.of(2026, 2, 10), emptyList()).weekStatus
        )
    }
}
