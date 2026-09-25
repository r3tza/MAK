package dev.retza.mak.ui.occurrence

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OccurrenceArgsTest {
    @Test
    fun parseReadsClassIdAndDate() {
        val args = OccurrenceArgs.parse("42:2026-09-21")
        assertEquals(42L, args?.classId)
        assertEquals(LocalDate.of(2026, 9, 21), args?.originalDate)
    }

    @Test
    fun parseRejectsInvalidInput() {
        assertNull(OccurrenceArgs.parse("nope"))
        assertNull(OccurrenceArgs.parse("42:not-a-date"))
        assertNull(OccurrenceArgs.parse("42"))
    }

    @Test
    fun routeIdRoundTrips() {
        val args = OccurrenceArgs(7L, LocalDate.of(2026, 10, 5))
        assertEquals("7:2026-10-05", args.toRouteId())
        assertEquals(args, OccurrenceArgs.parse(args.toRouteId()))
    }
}
