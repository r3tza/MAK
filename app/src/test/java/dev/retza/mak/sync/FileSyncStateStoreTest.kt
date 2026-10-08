package dev.retza.mak.sync

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileSyncStateStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun stateWrittenBeforeDatesAndDifferencesStillLoads() {
        val file = File(folder.root, "state.json")
        file.writeText(
            """{"account":{"subject":"s","email":"a@b.pl","androidAccountName":"a@b.pl"},"remoteMd5":"aa",""" +
                """"pendingChoice":{"localFingerprint":"f","remoteMd5":"aa","local":{"semesterCount":1,"classCount":2},""" +
                """"remote":{"semesterCount":1,"classCount":3}}}"""
        )

        val state = FileSyncStateStore(file).load()

        assertEquals("aa", state.remoteMd5)
        assertEquals(emptyList<PlanDifference>(), state.pendingChoice?.differences)
        assertEquals(null, state.localChangedAtMillis)
    }
}
