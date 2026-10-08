package dev.retza.mak.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.sync.PlanEditTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlanEditingTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val tracker = PlanEditTracker(CoroutineScope(SupervisorJob() + Dispatchers.Default))

    @Test
    fun recreatingTheScreenKeepsTheEditWithoutAGap() {
        val restoration = StateRestorationTester(composeTestRule)
        restoration.setContent { TrackPlanEditing(active = true, tracker = tracker) }
        composeTestRule.waitForIdle()
        assertTrue(tracker.isEditing)

        restoration.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()

        assertTrue(tracker.isEditing)
    }

    @Test
    fun screenBlocksOnlyWhileItHasADraft() {
        var hasDraft by mutableStateOf(false)
        composeTestRule.setContent { TrackPlanEditing(active = hasDraft, tracker = tracker) }
        composeTestRule.waitForIdle()
        assertFalse(tracker.isEditing)

        hasDraft = true
        composeTestRule.waitForIdle()
        assertTrue(tracker.isEditing)

        hasDraft = false
        composeTestRule.waitForIdle()
        assertFalse(tracker.isEditing)
    }

    @Test
    fun firstMutationWaitsForReplacementThenTracksActivationAndCompletion() {
        val replacementStarted = CompletableDeferred<Unit>()
        val finishReplacement = CompletableDeferred<Unit>()
        val replacement = CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            tracker.withReplacement {
                replacementStarted.complete(Unit)
                finishReplacement.await()
            }
        }
        runBlocking { replacementStarted.await() }

        var hasDraft by mutableStateOf(false)
        var refreshCount = 0
        lateinit var editing: PlanEditingController
        composeTestRule.setContent {
            editing = TrackPlanEditing(
                active = hasDraft,
                isActiveNow = { hasDraft },
                tracker = tracker
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            editing.run(refreshBeforeFirstEdit = {
                refreshCount += 1
                true
            }) {
                hasDraft = true
            }
        }
        composeTestRule.waitForIdle()
        assertFalse(tracker.isEditing)
        assertEquals(0, refreshCount)

        finishReplacement.complete(Unit)
        runBlocking { replacement.join() }
        composeTestRule.waitForIdle()
        assertTrue(tracker.isEditing)
        assertEquals(1, refreshCount)

        composeTestRule.runOnIdle { hasDraft = false }
        composeTestRule.waitForIdle()
        assertFalse(tracker.isEditing)

        composeTestRule.runOnIdle {
            editing.run(refreshBeforeFirstEdit = { true }) { hasDraft = true }
        }
        composeTestRule.waitForIdle()
        assertTrue(tracker.isEditing)
    }
}
