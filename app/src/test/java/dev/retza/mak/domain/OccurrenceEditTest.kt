package dev.retza.mak.domain

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OccurrenceEditTest {
    private val base = OccurrenceSlot(
        date = LocalDate.of(2026, 9, 21),
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(10, 30),
        room = "L204"
    )

    private fun decide(
        current: OccurrenceSlot = base,
        hasChange: Boolean = false,
        date: String = base.date.toString(),
        start: String = base.startTime.toString(),
        end: String = base.endTime.toString(),
        room: String = base.room.orEmpty()
    ): OccurrenceEditDecision =
        decideOccurrenceEdit(base, current, hasChange, date, start, end, room)

    @Test
    fun baseValuesWithoutChangeAreNoChange() {
        val decision = decide()
        assertEquals(OccurrenceEditResult.NoChange, ready(decision).result)
    }

    @Test
    fun changedEndTimeIsModified() {
        val decision = decide(end = "11:00")
        assertEquals(OccurrenceEditResult.Modified, ready(decision).result)
    }

    @Test
    fun trimmedRoomMatchesBase() {
        val decision = decide(room = "  L204  ")
        assertEquals(OccurrenceEditResult.NoChange, ready(decision).result)
    }

    @Test
    fun changedRoomIsModified() {
        val decision = decide(room = "L301")
        assertEquals(OccurrenceEditResult.Modified, ready(decision).result)
    }

    @Test
    fun changedDateAndTimeTogetherIsMoved() {
        val decision = decide(date = "2026-09-23", start = "12:00", end = "13:30")
        val slot = ready(decision).slot
        assertEquals(OccurrenceEditResult.Moved, ready(decision).result)
        assertEquals(LocalDate.of(2026, 9, 23), slot.date)
        assertEquals(LocalTime.of(12, 0), slot.startTime)
    }

    @Test
    fun returningToBaseWithExistingChangeIsRestored() {
        val changed = base.copy(date = LocalDate.of(2026, 9, 23))
        val decision = decide(current = changed, hasChange = true)
        assertEquals(OccurrenceEditResult.Restored, ready(decision).result)
    }

    @Test
    fun invalidTimeIsRejected() {
        val decision = decide(end = "nie-czas")
        assertTrue(decision is OccurrenceEditDecision.InvalidDateTime)
    }

    @Test
    fun movedTermReturnedToBaseDateWithOtherTimeIsModified() {
        val moved = base.copy(date = LocalDate.of(2026, 9, 23))
        val decision = decide(current = moved, hasChange = true, start = "11:00", end = "12:30")
        assertEquals(OccurrenceEditResult.Modified, ready(decision).result)
    }

    @Test
    fun movedTermMovedAgainIsMoved() {
        val moved = base.copy(date = LocalDate.of(2026, 9, 23))
        val decision = decide(current = moved, hasChange = true, date = "2026-09-24")
        assertEquals(OccurrenceEditResult.Moved, ready(decision).result)
    }

    @Test
    fun emptyRoomDraftOverBaseRoomIsModified() {
        // A blank draft clears the room just like an empty one.
        listOf("", "   ").forEach { room ->
            val decision = decide(room = room)
            assertEquals(OccurrenceEditResult.Modified, ready(decision).result)
            assertEquals(null, ready(decision).slot.room)
        }
    }

    @Test
    fun emptyRoomDraftWithEmptyBaseRoomIsNoChange() {
        val emptyBase = base.copy(room = null)
        val decision = decideOccurrenceEdit(
            emptyBase,
            emptyBase,
            false,
            emptyBase.date.toString(),
            emptyBase.startTime.toString(),
            emptyBase.endTime.toString(),
            ""
        )
        assertEquals(OccurrenceEditResult.NoChange, ready(decision).result)
    }

    @Test
    fun roomOverrideStoresExplicitClearOnlyWhenBaseHasRoom() {
        assertEquals("", occurrenceRoomOverride("L204", null))
        assertEquals("L301", occurrenceRoomOverride("L204", "L301"))
        assertEquals(null, occurrenceRoomOverride(null, null))
        assertEquals("L301", occurrenceRoomOverride(null, "L301"))
    }

    @Test
    fun noteContentChangeTreatsEmptyAndNullAsSame() {
        assertFalse(noteContentChanged("", null))
        assertFalse(noteContentChanged("   ", null))
        assertFalse(noteContentChanged("abc", "abc"))
        assertFalse(noteContentChanged(" abc ", "abc"))
        assertTrue(noteContentChanged("abc", null))
        assertTrue(noteContentChanged("", "abc"))
    }

    private fun ready(decision: OccurrenceEditDecision): OccurrenceEditDecision.Ready {
        assertTrue("Expected Ready but was $decision", decision is OccurrenceEditDecision.Ready)
        return decision as OccurrenceEditDecision.Ready
    }
}
