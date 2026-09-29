package dev.retza.mak.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.ui.theme.MAKTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MakAdaptiveLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun screenContentIsCenteredAndLimitedTo640Dp() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(Modifier.requiredWidth(1000.dp).testTag("window")) {
                    MakScreenContent {
                        Box(Modifier.fillMaxWidth().height(10.dp).testTag("content"))
                    }
                }
            }
        }

        val density = composeTestRule.density.density
        val window = composeTestRule.onNodeWithTag("window").fetchSemanticsNode().boundsInRoot
        val content = composeTestRule.onNodeWithTag("content").fetchSemanticsNode().boundsInRoot
        // 640 dp minus the 16 dp padding on each side.
        assertEquals(608f * density, content.width, density)
        assertEquals(content.left - window.left, window.right - content.right, density)
    }

    @Test
    fun phoneContentKeepsFullWidth() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(Modifier.requiredWidth(411.dp)) {
                    MakScreenContent {
                        Box(Modifier.fillMaxWidth().height(10.dp).testTag("content"))
                    }
                }
            }
        }

        val density = composeTestRule.density.density
        val content = composeTestRule.onNodeWithTag("content").fetchSemanticsNode().boundsInRoot
        assertEquals((411f - 32f) * density, content.width, density)
    }

    @Test
    fun dialogIsLimitedTo560DpAndScrolls() {
        composeTestRule.setContent {
            // At density 1 the phone screen is over 1000 dp wide, wider than the dialog limit.
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MAKTheme(dynamicColor = false) {
                    MakDialog(title = "Zmień termin", onDismiss = {}) {
                        repeat(60) { Text("Wiersz $it") }
                        Box(Modifier.fillMaxWidth().height(1.dp).testTag("dialog-content"))
                    }
                }
            }
        }

        val content = composeTestRule.onNodeWithTag("dialog-content", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        // The content sits inside the dialog's 24 dp padding, at density 1.
        assertTrue("dialog content is ${content.width} px wide", content.width <= 560f - 48f + 1f)
        composeTestRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollBy), useUnmergedTree = true)
            .assertExists()
    }
}
