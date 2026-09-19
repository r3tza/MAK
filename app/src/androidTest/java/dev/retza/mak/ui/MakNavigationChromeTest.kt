package dev.retza.mak.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
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
}
