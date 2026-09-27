package dev.retza.mak.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.update.ReleaseHistoryEntry
import dev.retza.mak.ui.theme.MAKTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsDescriptionAuthorAndHistoryWithoutUpdateActionsAt320Dp() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(Modifier.width(320.dp).height(900.dp)) {
                    AboutScreen(
                        installedVersionName = "0.1.0-debug",
                        releaseHistory = listOf(
                            ReleaseHistoryEntry("0.1.0", "2026-09-19", listOf("Dodano semestry."))
                        )
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Wersja 0.1.0-debug").assertIsDisplayed()
        composeTestRule.onNodeWithText("Autor: r3tza").assertIsDisplayed()
        composeTestRule.onNodeWithText("0.1.0 (2026-09-19)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dodano semestry.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Brak informacji").assertDoesNotExist()
        composeTestRule.onNodeWithText("Sprawdź teraz").assertDoesNotExist()
        composeTestRule.onNodeWithText("Sprawdzaj przy uruchomieniu").assertDoesNotExist()
    }
}
