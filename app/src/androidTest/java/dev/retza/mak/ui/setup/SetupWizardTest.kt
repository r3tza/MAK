package dev.retza.mak.ui.setup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.ui.theme.MAKTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SetupWizardTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyWizardShowsSemesterStepAndAcceptsBasicActionsAt320Dp() {
        var nextClicks = 0
        var selectedWeek = ""

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .height(700.dp)
                ) {
                    SetupWizard(
                        state = SetupWizardUiState(),
                        onSemesterNameChanged = {},
                        onStartDateChanged = {},
                        onEndDateChanged = {},
                        onFirstWeekChanged = { selectedWeek = it },
                        onCourseNameChanged = {},
                        onCourseColorChanged = {},
                        onNext = { nextClicks += 1 },
                        onBack = {},
                        onAddClass = {},
                        onFinish = {},
                        onRetry = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("KONFIGURACJA POCZĄTKOWA").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nazwa semestru").assertIsDisplayed()
        composeTestRule.onNodeWithText("A", substring = false).performClick()
        composeTestRule.onNodeWithText("B").performClick()
        // The step title repeats the button label, so target the clickable node.
        composeTestRule.onNode(hasText("Utwórz semestr") and hasClickAction()).performClick()

        assertEquals("B", selectedWeek)
        assertEquals(1, nextClicks)
    }
}
