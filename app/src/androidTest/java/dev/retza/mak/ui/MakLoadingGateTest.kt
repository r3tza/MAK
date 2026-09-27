package dev.retza.mak.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.ui.theme.MAKTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MakLoadingGateTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsFullNameUntilReadyAndHidesContentFromAccessibility() {
        var ready by mutableStateOf(false)
        composeTestRule.setContent {
            MAKTheme(darkTheme = true) {
                Box(modifier = Modifier.width(320.dp)) {
                    MakLoadingGate(isReady = ready) { Text("Treść aplikacji") }
                }
            }
        }

        composeTestRule.onNodeWithText(MAK_FULL_NAME).assertIsDisplayed()
        composeTestRule.onNodeWithText("Treść aplikacji").assertDoesNotExist()

        ready = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(LOADING_SCREEN_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithText("Treść aplikacji").assertIsDisplayed()

        ready = false
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(LOADING_SCREEN_TAG).assertDoesNotExist()
    }

    @Test
    fun hidesAfterTimeoutWhenDataNeverArrives() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            MAKTheme {
                MakLoadingGate(isReady = false) { Text("Treść aplikacji") }
            }
        }
        composeTestRule.onNodeWithTag(LOADING_SCREEN_TAG).assertExists()

        composeTestRule.mainClock.advanceTimeBy(LOADING_SCREEN_TIMEOUT_MILLIS + LOADING_SCREEN_FADE_MILLIS + 100)

        composeTestRule.onNodeWithTag(LOADING_SCREEN_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithText("Treść aplikacji").assertExists()
    }

    @Test
    fun playsTheWholeIntroEvenWhenDataIsReady() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            MAKTheme {
                MakLoadingGate(isReady = true, playIntro = true) { Text("Treść aplikacji") }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(INTRO_BLOOM_MILLIS / 2)
        composeTestRule.onNodeWithTag(LOADING_SCREEN_TAG).assertExists()

        composeTestRule.mainClock.advanceTimeBy(INTRO_BLOOM_MILLIS / 2 + LOADING_SCREEN_FADE_MILLIS + 100)

        composeTestRule.onNodeWithTag(LOADING_SCREEN_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithText("Treść aplikacji").assertExists()
    }
}
