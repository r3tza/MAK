package dev.retza.mak.ui

import dev.retza.mak.sync.PlanEditTracker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PlanEditingControllerTest {
    @Test
    fun callbacksWaitingForReplacementRefreshOnlyOnceAndEachRunAfterAdmission() = runTest {
        val tracker = PlanEditTracker(this)
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val replacement = launch {
            tracker.withReplacement {
                started.complete(Unit)
                finish.await()
            }
        }
        started.await()

        var admitted = false
        var ready = false
        var refreshCount = 0
        var actions = 0
        val controller = PlanEditingController(
            key = "form",
            tracker = tracker,
            scope = this,
            isAdmitted = { admitted },
            setAdmitted = { admitted = it },
            setReady = { ready = it }
        )
        val refresh: suspend () -> Boolean = {
            refreshCount += 1
            true
        }

        controller.run(refresh) { actions += 1 }
        controller.run(refresh) { actions += 1 }
        runCurrent()
        assertEquals(0, actions)
        assertFalse(ready)

        finish.complete(Unit)
        replacement.join()
        advanceUntilIdle()

        assertEquals(1, refreshCount)
        assertEquals(2, actions)
        assertTrue(admitted)
        assertTrue(ready)
    }

    @Test
    fun unsuccessfulFreshReadDoesNotRunThePendingActionAndDropsAdmission() = runTest {
        val tracker = PlanEditTracker(this)
        var admitted = false
        val controller = PlanEditingController(
            key = "deleted",
            tracker = tracker,
            scope = this,
            isAdmitted = { admitted },
            setAdmitted = { admitted = it },
            setReady = {}
        )
        var actionRan = false

        controller.run(refreshBeforeFirstEdit = { false }) { actionRan = true }
        advanceUntilIdle()

        assertFalse(actionRan)
        assertFalse(admitted)
        assertFalse(tracker.isEditing)
    }

    @Test
    fun noOpFirstActionEndsAdmissionOnlyAfterTheCallbackCompletes() = runTest {
        val tracker = PlanEditTracker(this)
        var admitted = false
        var operationFinished = false
        val actionStarted = CompletableDeferred<Unit>()
        val finishAction = CompletableDeferred<Unit>()
        val controller = PlanEditingController(
            key = "noop",
            tracker = tracker,
            scope = this,
            isAdmitted = { admitted },
            setAdmitted = { admitted = it },
            setReady = {},
            isActiveNow = { false }
        )

        controller.run {
            actionStarted.complete(Unit)
            finishAction.await()
            operationFinished = true
        }
        actionStarted.await()
        assertTrue(tracker.isEditing)
        assertFalse(operationFinished)
        finishAction.complete(Unit)
        advanceUntilIdle()

        assertTrue(operationFinished)
        assertFalse(admitted)
        assertFalse(tracker.isEditing)
    }
}
