package dev.retza.mak.data.repository

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OccurrenceNoteRemapTest {
    private val monday = LocalDate.of(2026, 10, 5)
    private val wednesday = LocalDate.of(2026, 10, 7)
    private val nextMonday = LocalDate.of(2026, 10, 12)
    private val weeklyMonday = ClassRow(id = 1, dayOfWeek = DayOfWeek.MONDAY, isOneOff = false)

    private fun note(id: Long, date: LocalDate, body: String = "Notatka $id") =
        NoteRow(id = id, semesterId = 1, classId = 1, date = date, body = body)

    private fun moved(from: LocalDate, to: LocalDate) =
        ChangeRow(classId = 1, originalDate = from, isModified = true, targetDate = to)

    @Test
    fun noteOnMoveTargetGoesToOriginalDate() {
        val result = remapOccurrenceNotesToOriginalDates(
            notes = listOf(note(1, wednesday)),
            changes = listOf(moved(monday, wednesday)),
            classes = listOf(weeklyMonday)
        )

        assertEquals(listOf(note(1, monday)), result.updated)
        assertTrue(result.deletedIds.isEmpty())
    }

    @Test
    fun noteWithoutMoveIsUnchanged() {
        val result = remapOccurrenceNotesToOriginalDates(
            notes = listOf(note(1, monday)),
            changes = emptyList(),
            classes = listOf(weeklyMonday)
        )

        assertTrue(result.updated.isEmpty())
        assertTrue(result.deletedIds.isEmpty())
    }

    @Test
    fun noteStaysWhenTwoOccurrencesWereMovedToItsDate() {
        val result = remapOccurrenceNotesToOriginalDates(
            notes = listOf(note(1, wednesday)),
            changes = listOf(moved(monday, wednesday), moved(nextMonday, wednesday)),
            classes = listOf(weeklyMonday)
        )

        assertTrue(result.updated.isEmpty())
    }

    @Test
    fun noteStaysWhenRegularOccurrenceMayHappenOnItsDate() {
        val result = remapOccurrenceNotesToOriginalDates(
            notes = listOf(note(1, nextMonday)),
            changes = listOf(moved(monday, nextMonday)),
            classes = listOf(weeklyMonday)
        )

        assertTrue(result.updated.isEmpty())
    }

    @Test
    fun noteMovesWhenRegularOccurrenceOnItsDateWasChangedToo() {
        val result = remapOccurrenceNotesToOriginalDates(
            notes = listOf(note(1, nextMonday)),
            changes = listOf(
                moved(monday, nextMonday),
                ChangeRow(classId = 1, originalDate = nextMonday, isModified = false, targetDate = null)
            ),
            classes = listOf(weeklyMonday)
        )

        assertEquals(listOf(note(1, monday)), result.updated)
    }

    @Test
    fun cancellationDoesNotMoveNotes() {
        val result = remapOccurrenceNotesToOriginalDates(
            notes = listOf(note(1, wednesday)),
            changes = listOf(ChangeRow(classId = 1, originalDate = monday, isModified = false, targetDate = wednesday)),
            classes = listOf(weeklyMonday)
        )

        assertTrue(result.updated.isEmpty())
    }

    @Test
    fun hiddenAndVisibleNotesOfOneOccurrenceAreMergedWithoutLosingText() {
        val result = remapOccurrenceNotesToOriginalDates(
            notes = listOf(note(2, monday, "Przed przeniesieniem"), note(5, wednesday, "Po przeniesieniu")),
            changes = listOf(moved(monday, wednesday)),
            classes = listOf(weeklyMonday)
        )

        assertEquals(listOf(note(2, monday, "Przed przeniesieniem\n\nPo przeniesieniu")), result.updated)
        assertEquals(listOf(5L), result.deletedIds)
    }

    @Test
    fun noteOfUnknownClassIsUnchanged() {
        val result = remapOccurrenceNotesToOriginalDates(
            notes = listOf(note(1, wednesday)),
            changes = listOf(moved(monday, wednesday)),
            classes = emptyList()
        )

        assertTrue(result.updated.isEmpty())
    }
}
