package dev.retza.mak.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.ui.theme.MAKTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MakNoteBannerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun warningAndErrorHaveRolePrefixesAndActionsWork() {
        var actionClicked = false
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(Modifier.width(320.dp).height(500.dp)) {
                    androidx.compose.foundation.layout.Column {
                        MakNoteBanner(
                            title = "Uwaga",
                            subtitle = "Sprawdź te dane.",
                            role = MakNoteRole.Warning,
                            action = MakBannerAction("Otwórz", { actionClicked = true })
                        )
                        MakNoteBanner(
                            title = null,
                            subtitle = "Nie udało się pobrać pliku.",
                            role = MakNoteRole.Error
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Sprawdź te dane.").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Ostrzeżenie: Uwaga. Sprawdź te dane.").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Błąd: Nie udało się pobrać pliku.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Otwórz").assertIsDisplayed().performClick()
        assertTrue(actionClicked)
    }

    @Test
    fun dismissActionStandsBeforeTheMainAction() {
        val clicked = mutableListOf<String>()
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(Modifier.width(320.dp).height(200.dp)) {
                    MakNoteBanner(
                        title = "Dostępna aktualizacja",
                        subtitle = null,
                        role = MakNoteRole.Neutral,
                        action = MakBannerAction("Zobacz", { clicked += "Zobacz" }),
                        dismissAction = MakBannerAction("Nie teraz", { clicked += "Nie teraz" })
                    )
                }
            }
        }

        val dismiss = composeTestRule.onNodeWithText("Nie teraz").assertIsDisplayed()
        val main = composeTestRule.onNodeWithText("Zobacz").assertIsDisplayed()
        assertTrue(dismiss.getUnclippedBoundsInRoot().left < main.getUnclippedBoundsInRoot().left)
        dismiss.performClick()
        main.performClick()
        assertEquals(listOf("Nie teraz", "Zobacz"), clicked)
    }
}
