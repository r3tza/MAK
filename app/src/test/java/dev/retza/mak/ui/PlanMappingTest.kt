package dev.retza.mak.ui

import dev.retza.mak.domain.ClassItem
import dev.retza.mak.domain.OccurrenceNote
import dev.retza.mak.domain.PlannedOccurrence
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlanMappingTest {
    private val date = LocalDate.of(2026, 9, 21)

    @Test
    fun mapsBothNotesSeparately() {
        val ui = occurrence(classNote = "Wspólna", occurrenceBody = "Na dziś").toUi(null)

        assertEquals("Wspólna", ui.classNote)
        assertEquals("Na dziś", ui.occurrenceNote)
    }

    @Test
    fun mapsOnlyClassNote() {
        val ui = occurrence(classNote = "Wspólna").toUi(null)

        assertEquals("Wspólna", ui.classNote)
        assertNull(ui.occurrenceNote)
    }

    @Test
    fun mapsOnlyOccurrenceNote() {
        val ui = occurrence(occurrenceBody = "Na dziś").toUi(null)

        assertNull(ui.classNote)
        assertEquals("Na dziś", ui.occurrenceNote)
    }

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

    private fun occurrence(
        classNote: String? = null,
        occurrenceBody: String? = null
    ) = PlannedOccurrence(
        classItem = ClassItem(
            id = "class",
            semesterId = "semester",
            semesterProgramId = "assignment",
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
            OccurrenceNote(id = "note", classId = "class", occurrenceDate = date, body = it)
        }
    )
}
