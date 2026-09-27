package dev.retza.mak.update

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseHistoryTest {
    @Test
    fun knownVersionShowsOnlyPublishedUserVisibleChanges() {
        val history = releaseHistoryFor("0.1.0")

        assertEquals(1, history.size)
        assertEquals("0.1.0", history.first().versionName)
        assertEquals(3, history.first().changes.size)
    }

    @Test
    fun debugSuffixMatchesReleaseEntry() {
        assertEquals(releaseHistoryFor("0.1.0"), releaseHistoryFor("0.1.0-debug"))
    }

    @Test
    fun unknownInstalledVersionGetsNoEmptyEntry() {
        val history = releaseHistoryFor("0.9.0")

        assertEquals(listOf("0.2.0", "0.1.0"), history.map { it.versionName })
    }

    @Test
    fun newestReleaseListsItsChangesFirst() {
        val history = releaseHistoryFor("0.2.0")

        assertEquals(listOf("0.2.0", "0.1.0"), history.map { it.versionName })
        assertEquals("2026-09-27", history.first().date)
    }
}
