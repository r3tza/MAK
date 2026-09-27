package dev.retza.mak.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun unknownInstalledVersionGetsNoInventedChanges() {
        val history = releaseHistoryFor("0.2.0")

        assertEquals("0.2.0", history.first().versionName)
        assertTrue(history.first().changes.isEmpty())
        assertTrue(history.size <= 3)
    }
}
