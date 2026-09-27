package dev.retza.mak.ui.occurrence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OccurrenceChangeLinesTest {
    private val base = OccurrenceDetailsUiState(
        startTime = "10:00",
        endTime = "11:30",
        room = "A12",
        baseStartTime = "10:00",
        baseEndTime = "11:30",
        baseRoom = "A12"
    )

    @Test
    fun changedRoomAndTimesAreDescribed() {
        val state = base.copy(
            status = OccurrenceStatusUi.Changed,
            startTime = "12:00",
            endTime = "13:30",
            room = "204"
        )

        assertEquals(listOf("Godziny zmienione z 10:00 - 11:30 na 12:00 - 13:30", "Sala zmieniona z A12 na 204"), state.changeLines())
    }

    @Test
    fun movedOccurrenceNamesBothDates() {
        val state = base.copy(
            status = OccurrenceStatusUi.Moved,
            originalDateLabel = "2026-10-12",
            targetDateLabel = "2026-10-14"
        )

        assertEquals(listOf("Przeniesione z 12 października na 14 października"), state.changeLines())
    }

    @Test
    fun cancelledAndScheduledOccurrencesHaveNoChangeLines() {
        assertTrue(base.copy(status = OccurrenceStatusUi.Cancelled, room = "204").changeLines().isEmpty())
        assertTrue(base.changeLines().isEmpty())
    }

    @Test
    fun removedRoomIsShownAsMissing() {
        val state = base.copy(status = OccurrenceStatusUi.Changed, room = null)

        assertEquals(listOf("Usunięto salę A12"), state.changeLines())
    }
}
