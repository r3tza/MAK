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
import org.junit.Assert.assertFalse
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
}
