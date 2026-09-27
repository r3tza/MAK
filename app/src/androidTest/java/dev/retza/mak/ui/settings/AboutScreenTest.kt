package dev.retza.mak.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.update.ReleaseHistoryEntry
import dev.retza.mak.update.UpdateCheckStatus
import dev.retza.mak.update.UpdateInfo
import dev.retza.mak.update.UpdateUiState
import dev.retza.mak.ui.theme.MAKTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsVersionHistoryAndCheckActionAt320Dp() {
        var checked = false
        show(
            UpdateUiState(
                installedVersionName = "0.2.0",
                installedVersionCode = 200,
                releaseHistory = listOf(ReleaseHistoryEntry("0.2.0"))
            ),
            onCheck = { checked = true }
        )

        composeTestRule.onAllNodesWithText("0.2.0")[0].assertIsDisplayed()
        composeTestRule.onNodeWithText("Brak informacji").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sprawdź teraz").performClick()
        assertTrue(checked)
    }

    @Test
    fun showsAvailableVersionAndNotes() {
        show(
            UpdateUiState(
                installedVersionName = "0.1.0",
                installedVersionCode = 100,
                checkStatus = UpdateCheckStatus.Available,
                availableUpdate = UpdateInfo(
                    200,
                    "0.2.0",
                    "https://github.com/r3tza/MAK/releases/download/v0.2.0/MAK-0.2.0.apk",
                    "a".repeat(64),
                    31,
                    "Nowy wygląd planu"
                )
            )
        )

        composeTestRule.onNodeWithText("Dostępna wersja 0.2.0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nowy wygląd planu").assertIsDisplayed()
    }

    private fun show(state: UpdateUiState, onCheck: () -> Unit = {}) {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(Modifier.width(320.dp).height(900.dp)) {
                    AboutScreen(state = state, onCheckNow = onCheck)
                }
            }
        }
    }
}
