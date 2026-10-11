package dev.retza.mak.ui

import dev.retza.mak.domain.ClassItem
import dev.retza.mak.domain.OccurrenceNote
import dev.retza.mak.domain.PlannedOccurrence
import dev.retza.mak.domain.WeekStatus
import dev.retza.mak.domain.WeekType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlanMappingTest {
    private val date = LocalDate.of(2026, 9, 21)

    @Test
    fun blankNotesBecomeNull() {
        val ui = occurrence(classNote = "   ", occurrenceBody = "").toUi(null)

        assertNull(ui.classNote)
        assertNull(ui.occurrenceNote)
    }

    @Test
    fun conflictDoesNotOverrideNotes() {
        val ui = occurrence(classNote = "Wspólna", occurrenceBody = "Na dziś")
            .toUi("Kolizja 09:30-10:00", "Matematyka")

        assertEquals("Wspólna", ui.classNote)
        assertEquals("Na dziś", ui.occurrenceNote)
        assertEquals("Kolizja 09:30-10:00", ui.conflictLabel)
        assertEquals("Matematyka", ui.conflictWith)
    }

    @Test
    fun dayMonthFormatterUsesPolishMonthName() {
        assertEquals("7 października", LocalDate.of(2026, 10, 7).format(dayMonthFormatter))
    }

    @Test
    fun dateRangeInOneYearShowsYearOnce() {
        assertEquals(
            "22 lut - 15 cze 2027",
            dateRangeWithYearLabel(LocalDate.of(2027, 2, 22), LocalDate.of(2027, 6, 15))
        )
    }

    @Test
    fun dateRangeAcrossYearsShowsBothYears() {
        assertEquals(
            "7 wrz 2026 - 14 lut 2027",
            dateRangeWithYearLabel(LocalDate.of(2026, 9, 7), LocalDate.of(2027, 2, 14))
        )
    }

    @Test
    fun weekStatusLabels() {
        assertEquals("Tydzień A", WeekStatus.Single(WeekType.A).label())
        assertEquals("Różne tygodnie", WeekStatus.Mixed.label())
        assertEquals("Poza semestrem", WeekStatus.OutsideSemester.label())
    }

    private fun occurrence(
        classNote: String? = null,
        occurrenceBody: String? = null
    ) = PlannedOccurrence(
        classItem = ClassItem(
            id = 1L,
            semesterId = 1L,
            semesterProgramId = 1L,
            name = "Programowanie",
            type = "Wykład",
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0),
            classNote = classNote
        ),
        date = date,
        originalDate = date,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(10, 0),
        room = null,
        building = null,
        teacherName = null,
        studyProgram = null,
        classNote = classNote,
        occurrenceNote = occurrenceBody?.let {
            OccurrenceNote(id = 1L, classId = 1L, occurrenceDate = date, body = it)
        }
    )
}
