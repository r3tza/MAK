package dev.retza.mak.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CancelledOccurrencesTest {
    private val monday = LocalDate.of(2026, 9, 21)
    private val semester = Semester(id = "s", name = "Semestr")
    private val calendar = AcademicCalendar("c", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31), WeekType.A)
    private val program = StudyProgram("p", "Informatyka", "#137B71")
    private val assignment = SemesterProgram("a", semester.id, program.id, calendar.id)
    private val lecture = ClassItem(
        id = "1",
        semesterId = semester.id,
        semesterProgramId = assignment.id,
        name = "Analiza",
        type = "Wykład",
        dayOfWeek = DayOfWeek.MONDAY,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(10, 30)
    )

    private fun cancellation(date: LocalDate) = OccurrenceChange(
        id = "x$date",
        classId = lecture.id,
        originalDate = date,
        kind = OccurrenceChangeKind.CANCELLED
    )

    private fun data(
        item: ClassItem = lecture,
        changes: List<OccurrenceChange>,
        notes: List<OccurrenceNote> = emptyList()
    ) = ActivePlanData(
        semester = semester,
        classes = listOf(item),
        courses = listOf(program),
        semesterPrograms = listOf(assignment),
        calendars = listOf(calendar),
        occurrenceChanges = changes,
        occurrenceNotes = notes
    )

    @Test
    fun cancellationOfExistingTermIsReturnedWithItsNote() {
        val note = OccurrenceNote("n", lecture.id, monday, "Przynieś kartkę")

        val result = cancelledOccurrences(data(changes = listOf(cancellation(monday)), notes = listOf(note)), monday)

        assertEquals(listOf(lecture.id), result.map { it.classItem.id })
        assertEquals(program, result.single().studyProgram)
        assertEquals(note, result.single().occurrenceNote)
    }

    @Test
    fun cancellationHiddenAfterClassMovedToAnotherDay() {
        val moved = lecture.copy(dayOfWeek = DayOfWeek.TUESDAY)

        assertTrue(cancelledOccurrences(data(item = moved, changes = listOf(cancellation(monday))), monday).isEmpty())
    }

    @Test
    fun cancellationOutsideCalendarIsNotReturned() {
        val outside = LocalDate.of(2027, 1, 4)

        assertTrue(cancelledOccurrences(data(changes = listOf(cancellation(outside))), outside).isEmpty())
    }

    @Test
    fun oneTimeClassHasNoCancelledTerms() {
        val once = lecture.copy(recurrence = Recurrence.ONCE, date = monday)

        assertTrue(cancelledOccurrences(data(item = once, changes = listOf(cancellation(monday))), monday).isEmpty())
    }
}
