package dev.retza.mak.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.ui.components.MakActionMenu
import dev.retza.mak.ui.theme.MAKTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MakNavigationChromeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun childTopBarShowsAccessibleBackActionAt320Dp() {
        var backClicks = 0

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp)) {
                    MakTopBar(
                        title = "Termin",
                        showBack = true,
                        showSettings = false,
                        onBack = { backClicks += 1 },
                        onSettings = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("Wstecz")
            .assertIsDisplayed()
            .performClick()

        assertEquals(1, backClicks)
    }

    @Test
    fun topBarRendersProvidedActionsBesideBackAction() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp)) {
                    MakTopBar(
                        title = "Termin",
                        showBack = true,
                        showSettings = false,
                        onBack = {},
                        onSettings = {},
                        actions = {
                            androidx.compose.material3.Text("Akcja")
                        }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Akcja").assertIsDisplayed()
    }

    @Test
    fun occurrenceTopBarRunsEachActionMenuEntryAt320Dp() {
        var cancelClicks = 0
        var editClicks = 0
        var deleteClicks = 0

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp)) {
                    MakTopBar(
                        title = "Termin",
                        showBack = true,
                        showSettings = false,
                        onBack = {},
                        onSettings = {},
                        actions = {
                            MakActionMenu(
                                actions = listOf(
                                    "Odwołaj termin" to { cancelClicks += 1 },
                                    "Edytuj bazowe zajęcia" to { editClicks += 1 },
                                    "Usuń zajęcia" to { deleteClicks += 1 }
                                ),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    )
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("Więcej opcji").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Więcej opcji").performClick()
        composeTestRule.onNodeWithText("Odwołaj termin").assertIsDisplayed().performClick()
        assertEquals(1, cancelClicks)

        composeTestRule.onNodeWithContentDescription("Więcej opcji").performClick()
        composeTestRule.onNodeWithText("Edytuj bazowe zajęcia").assertIsDisplayed().performClick()
        assertEquals(1, editClicks)

        composeTestRule.onNodeWithContentDescription("Więcej opcji").performClick()
        composeTestRule.onNodeWithText("Usuń zajęcia").assertIsDisplayed().performClick()
        assertEquals(1, deleteClicks)
    }
}
