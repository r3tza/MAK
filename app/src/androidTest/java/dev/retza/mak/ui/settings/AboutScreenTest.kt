package dev.retza.mak.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
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
        composeTestRule.onNodeWithText("Wersja przed pełnym wydaniem").assertIsDisplayed()
        composeTestRule.onNodeWithText("plan kilku kierunków i semestrów").assertIsDisplayed()
        // Sections without a card keep their heading as the only structure.
        listOf("Możliwości", "Dane i prywatność").forEach { title ->
            composeTestRule.onNodeWithText(title).performScrollTo().assertIsDisplayed()
                .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        }
        // Privacy text states that Android backup may include the plan (I-14).
        composeTestRule.onNodeWithText(BACKUP_NOTICE).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("tylko na telefonie", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Ostatnie zmiany").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Wersja 0.1.0").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("19 września 2026").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Zainstalowana").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Dodano semestry.").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Brak informacji").assertDoesNotExist()
        composeTestRule.onNodeWithText("Sprawdź teraz").assertDoesNotExist()
        composeTestRule.onNodeWithText("Sprawdzaj przy uruchomieniu").assertDoesNotExist()
    }
}
