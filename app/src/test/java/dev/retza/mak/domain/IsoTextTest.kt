package dev.retza.mak.domain

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IsoTextTest {
    @Test
    fun toLocalDateOrNullRejectsGarbage() {
        assertEquals(LocalDate.of(2026, 10, 7), "2026-10-07".toLocalDateOrNull())
        assertNull("7.10.2026".toLocalDateOrNull())
        assertNull("25:99".toLocalTimeOrNull())
    }

    @Test
    fun surroundingSpacesAreIgnored() {
        assertEquals(LocalDate.of(2026, 10, 7), " 2026-10-07 ".toLocalDateOrNull())
        assertEquals(LocalTime.of(8, 15), " 08:15".toLocalTimeOrNull())
    }
}
