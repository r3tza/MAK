package dev.retza.mak.sync

import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.launch
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanEditTrackerTest {
    @Test
    fun reAdmissionInvalidatesAReleaseThatHasNotStartedYet() = runTest {
        val tracker = PlanEditTracker(CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler)))
        tracker.beginEdit("form")

        tracker.release("form")
        tracker.beginEdit("form")
        testScheduler.runCurrent()
        advanceTimeBy(5_001)
        testScheduler.runCurrent()

        assertTrue(tracker.isEditing)
    }

    @Test
    fun editorAdmissionWaitsForAnInProgressReplacement() = runTest {
        val tracker = PlanEditTracker(this, graceMillis = 5_000)
        val replacementStarted = CompletableDeferred<Unit>()
        val finishReplacement = CompletableDeferred<Unit>()
        val replacement = launch {
            tracker.withReplacement {
                replacementStarted.complete(Unit)
                finishReplacement.await()
                "replaced"
            }
        }
        replacementStarted.await()

        var editorAdmitted = false
        val editor = launch {
            tracker.beginEdit("form")
            editorAdmitted = true
        }
        runCurrent()
        assertFalse(editorAdmitted)
        assertFalse(editor.isCompleted)

        finishReplacement.complete(Unit)
        replacement.join()
        editor.join()

        assertTrue(editorAdmitted)
        assertTrue(tracker.isEditing)
    }

    @Test
    fun replacementDoesNotRunWhenAnEditorAlreadyHasAdmission() = runTest {
        val tracker = PlanEditTracker(this, graceMillis = 5_000)
        tracker.beginEdit("form")
        var replaced = false

        val result = tracker.withReplacement {
            replaced = true
        }

        assertFalse(replaced)
        assertTrue(result is PlanReplacementResult.Editing)
    }

    @Test
    fun everyCallbackWaitingForTheReplacementGetsEditAdmission() = runTest {
        val tracker = PlanEditTracker(this, graceMillis = 5_000)
        val replacementStarted = CompletableDeferred<Unit>()
        val finishReplacement = CompletableDeferred<Unit>()
        val replacement = launch {
            tracker.withReplacement {
                replacementStarted.complete(Unit)
                finishReplacement.await()
            }
        }
        replacementStarted.await()

        val admissions = mutableListOf<Boolean>()
        val first = launch {
            tracker.beginEdit("form")
            admissions += tracker.isEditing
        }
        val second = launch {
            tracker.beginEdit("form")
            admissions += tracker.isEditing
        }
        runCurrent()
        assertTrue(admissions.isEmpty())

        finishReplacement.complete(Unit)
        replacement.join()
        first.join()
        second.join()

        assertEquals(listOf(true, true), admissions)
        assertTrue(tracker.isEditing)
    }

    @Test
    fun releasedEditKeepsBlockingUntilTheGraceEnds() = runTest {
        val tracker = PlanEditTracker(this, graceMillis = 5_000)
        tracker.beginEdit("form")

        tracker.release("form")
        advanceTimeBy(4_000)
        assertTrue(tracker.isEditing)

        advanceTimeBy(2_000)
        assertFalse(tracker.isEditing)
    }

    @Test
    fun recreatedScreenWithTheSameKeyKeepsTheEdit() = runTest {
        val tracker = PlanEditTracker(this, graceMillis = 5_000)
        tracker.beginEdit("form")

        tracker.release("form")
        advanceTimeBy(1_000)
        tracker.beginEdit("form")
        advanceTimeBy(10_000)

        assertTrue(tracker.isEditing)
    }

    @Test
    fun screenWithoutDraftDoesNotBlockAndStopsAtOnce() = runTest {
        val tracker = PlanEditTracker(this, graceMillis = 5_000)

        assertFalse(tracker.isEditing)
    }

    @Test
    fun releasingAnUnknownKeyChangesNothing() = runTest {
        val tracker = PlanEditTracker(this, graceMillis = 5_000)
        tracker.beginEdit("form")

        tracker.release("other")
        advanceTimeBy(10_000)

        assertTrue(tracker.isEditing)
    }
}
