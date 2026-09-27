package dev.retza.mak.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AboutInfoTest {
    @Test
    fun isoDateUsesPolishMonthInGenitive() {
        assertEquals("19 września 2026", formatReleaseDate("2026-09-19"))
    }

    @Test
    fun invalidDateIsShownAsIs() {
        assertEquals("wkrótce", formatReleaseDate("wkrótce"))
    }

    @Test
    fun versionsBeforeOneAreMarkedAsPreRelease() {
        assertTrue(isPreRelease("0.1.0"))
        assertTrue(isPreRelease("0.2.1-debug"))
        assertFalse(isPreRelease("1.0.0"))
        assertFalse(isPreRelease("nieznana"))
    }

    @Test
    fun updateNotesSplitIntoLinesWithoutEmptyOnes() {
        assertEquals(listOf("Nowość", "Poprawka"), releaseNoteLines("Nowość\n\n- Poprawka\n"))
        assertEquals(emptyList<String>(), releaseNoteLines(""))
    }
}
