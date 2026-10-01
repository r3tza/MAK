package dev.retza.mak.sync

import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanEditTrackerTest {
    @Test
    fun releasedEditKeepsBlockingUntilTheGraceEnds() = runTest {
        val tracker = PlanEditTracker(this, graceMillis = 5_000)
        tracker.set("form", true)

        tracker.release("form")
        advanceTimeBy(4_000)
        assertTrue(tracker.isEditing)

        advanceTimeBy(2_000)
        assertFalse(tracker.isEditing)
    }

    @Test
    fun recreatedScreenWithTheSameKeyKeepsTheEdit() = runTest {
        val tracker = PlanEditTracker(this, graceMillis = 5_000)
        tracker.set("form", true)

        tracker.release("form")
        advanceTimeBy(1_000)
        tracker.set("form", true)
        advanceTimeBy(10_000)

        assertTrue(tracker.isEditing)
    }

    @Test
    fun screenWithoutDraftDoesNotBlockAndStopsAtOnce() = runTest {
        val tracker = PlanEditTracker(this, graceMillis = 5_000)

        tracker.set("details", false)
        assertFalse(tracker.isEditing)

        tracker.set("details", true)
        tracker.set("details", false)
        runCurrent()
        assertFalse(tracker.isEditing)
    }

    @Test
    fun releasingAnUnknownKeyChangesNothing() = runTest {
        val tracker = PlanEditTracker(this, graceMillis = 5_000)
        tracker.set("form", true)

        tracker.release("other")
        advanceTimeBy(10_000)

        assertTrue(tracker.isEditing)
    }
}
