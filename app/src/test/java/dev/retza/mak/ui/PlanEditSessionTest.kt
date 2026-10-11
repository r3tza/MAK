package dev.retza.mak.ui

import dev.retza.mak.sync.PlanEditTracker
import dev.retza.mak.sync.PlanReplacementResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanEditSessionTest {
    @Test
    fun editAppliesTheChangeBeforeAdmission() = runTest {
        val tracker = PlanEditTracker(this)
        var text = ""
        val session = PlanEditSession(
            tracker = tracker,
            scope = this,
            refreshBase = { true },
            hasDraft = { text.isNotEmpty() }
        )

        session.edit { text = "K" }
        // A text field reads its value right after the callback; the change must not wait for the refresh.
        assertEquals("K", text)
        advanceUntilIdle()
        assertTrue(tracker.isEditing)
    }

    @Test
    fun firstActionRefreshesOnce() = runTest {
        val tracker = PlanEditTracker(this)
        var refreshes = 0
        val session = PlanEditSession(
            tracker = tracker,
            scope = this,
            refreshBase = {
                refreshes++
                true
            },
            hasDraft = { true }
        )

        session.run { }
        session.run { }
        advanceUntilIdle()

        assertEquals(1, refreshes)
    }

    @Test
    fun failedRefreshSkipsTheActionAndReleases() = runTest {
        val tracker = PlanEditTracker(this)
        var acted = false
        val session = PlanEditSession(
            tracker = tracker,
            scope = this,
            refreshBase = { false },
            hasDraft = { true }
        )

        session.run { acted = true }
        advanceUntilIdle()

        assertFalse(acted)
        assertFalse(tracker.isEditing)
    }

    @Test
    fun keyStaysWhileDraftExists() = runTest {
        val tracker = PlanEditTracker(this)
        var draft = true
        val session = PlanEditSession(
            tracker = tracker,
            scope = this,
            refreshBase = { true },
            hasDraft = { draft }
        )

        session.run { }
        advanceUntilIdle()
        assertTrue(tracker.isEditing)

        draft = false
        session.releaseIfNoDraft()

        assertFalse(tracker.isEditing)
    }

    @Test
    fun replacementWaitsForAnAdmittedForm() = runTest {
        val tracker = PlanEditTracker(this)
        val session = PlanEditSession(
            tracker = tracker,
            scope = this,
            refreshBase = { true },
            hasDraft = { true }
        )

        assertTrue(session.admitForm())
        var replaced = false
        val result = tracker.withReplacement { replaced = true }

        assertFalse(replaced)
        assertTrue(result is PlanReplacementResult.Editing)
    }

    @Test
    fun firstActionWaitsForReplacementBeforeItRefreshes() = runTest {
        val tracker = PlanEditTracker(this)
        val replacementStarted = CompletableDeferred<Unit>()
        val finishReplacement = CompletableDeferred<Unit>()
        val replacement = launch {
            tracker.withReplacement {
                replacementStarted.complete(Unit)
                finishReplacement.await()
            }
        }
        replacementStarted.await()
        var refreshes = 0
        val session = PlanEditSession(
            tracker = tracker,
            scope = this,
            refreshBase = {
                refreshes += 1
                true
            },
            hasDraft = { true }
        )

        session.run { }
        advanceUntilIdle()
        assertEquals(0, refreshes)

        finishReplacement.complete(Unit)
        replacement.join()
        advanceUntilIdle()
        assertEquals(1, refreshes)
        assertTrue(tracker.isEditing)
    }

    @Test
    fun keyStaysUntilTheActionCompletes() = runTest {
        val tracker = PlanEditTracker(this)
        var finished = false
        val actionStarted = CompletableDeferred<Unit>()
        val finishAction = CompletableDeferred<Unit>()
        val session = PlanEditSession(
            tracker = tracker,
            scope = this,
            refreshBase = { true },
            hasDraft = { false }
        )

        session.run {
            actionStarted.complete(Unit)
            finishAction.await()
            finished = true
        }
        actionStarted.await()
        assertTrue(tracker.isEditing)

        finishAction.complete(Unit)
        advanceUntilIdle()
        assertTrue(finished)
        assertFalse(tracker.isEditing)
    }
}
