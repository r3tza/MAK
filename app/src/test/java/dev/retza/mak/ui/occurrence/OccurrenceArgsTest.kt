package dev.retza.mak.ui.occurrence

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OccurrenceArgsTest {
    @Test
    fun fromRejectsInvalidInput() {
        assertNull(OccurrenceArgs.from("nope", "2026-10-05"))
        assertNull(OccurrenceArgs.from("42", "not-a-date"))
        assertNull(OccurrenceArgs.from(null, "2026-10-05"))
        assertNull(OccurrenceArgs.from("42", null))
    }

    @Test
    fun fromReadsRouteArguments() {
        assertEquals(OccurrenceArgs(7L, LocalDate.of(2026, 10, 5)), OccurrenceArgs.from("7", "2026-10-05"))
    }
}
