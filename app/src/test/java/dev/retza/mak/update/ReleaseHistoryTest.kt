package dev.retza.mak.update

import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseHistoryTest {
    private val releases = listOf(
        ReleaseHistoryEntry("0.2.0", "2026-09-27", listOf("Nowość")),
        ReleaseHistoryEntry("0.1.0", "2026-09-19", listOf("Start"))
    )

    @Test
    fun installedVersionStartsTheHistory() {
        assertEquals(listOf("0.1.0"), releaseHistoryFor("0.1.0", releases).map { it.versionName })
    }

    @Test
    fun debugSuffixMatchesReleaseEntry() {
        assertEquals(releaseHistoryFor("0.1.0", releases), releaseHistoryFor("0.1.0-debug", releases))
    }

    @Test
    fun unknownInstalledVersionGetsNoEmptyEntry() {
        assertEquals(listOf("0.2.0", "0.1.0"), releaseHistoryFor("0.9.0", releases).map { it.versionName })
    }

    @Test
    fun releaseWithoutChangesShowsMinorChangesText() {
        val parsed = parseReleaseNotes(
            """{"minorChanges": "Pomniejsze poprawki", "releases": [{"version": "0.2.1", "date": "2026-10-01", "changes": []}]}"""
        )

        assertEquals(listOf("Pomniejsze poprawki"), parsed.single().changes)
    }

    @Test
    fun bundledReleaseNotesAreValidAndNewestFirst() {
        val parsed = parseReleaseNotes(File("src/main/assets/$RELEASE_NOTES_ASSET").readText())

        assertTrue(parsed.isNotEmpty())
        val versions = parsed.map { entry -> entry.versionName.split('.').map { it.toInt() } }
        versions.zipWithNext().forEach { (newer, older) ->
            assertTrue("$newer should be newer than $older", compareVersions(newer, older) > 0)
        }
        parsed.forEach { entry ->
            LocalDate.parse(entry.date)
            assertTrue(entry.changes.all { it.isNotBlank() })
        }
    }

    private fun compareVersions(first: List<Int>, second: List<Int>): Int =
        first.zip(second).map { (a, b) -> a.compareTo(b) }.firstOrNull { it != 0 } ?: 0
}
