package dev.retza.mak.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.sync.PlanSide
import dev.retza.mak.ui.theme.MAKTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The screen „Wybierz zmiany” at 320 dp and font scale 2.0. */
@RunWith(AndroidJUnit4::class)
class SyncChangesScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val differences = listOf(
        SyncDifferenceUi("Konsultacje", "Sala, czwartek, 16:00-17:00", "A12", "B204"),
        SyncDifferenceUi("Statystyka", "Zajęcia, wtorek, 10:00-11:00", "Brak", "Wtorek, 10:00-11:00, co tydzień")
    )

    @Test
    fun nothingIsPickedAndSavingWaitsForEveryPick() {
        var saved = 0
        composeTestRule.setContent {
            var picks by remember { mutableStateOf(emptyMap<Int, PlanSide>()) }
            Screen(SyncChangesUi(differences = differences, picks = picks), { index, side -> picks = picks + (index to side) }) { saved++ }
        }

        composeTestRule.onNodeWithText("Wybrano 0 z 2").assertExists()
        composeTestRule.onNodeWithText("Zapisz plan").assertIsNotEnabled()
        composeTestRule.onNodeWithText("B204", substring = true).performScrollTo().performClick()
        composeTestRule.onNodeWithText("B204", substring = true).assertIsSelected()
        composeTestRule.onNodeWithText("A12", substring = true).assertIsNotSelected()
        composeTestRule.onNodeWithText("Zapisz plan").assertIsNotEnabled()
        composeTestRule.onAllNodesWithText("Brak", substring = true)[0].performScrollTo().performClick()

        composeTestRule.onNodeWithText("Wybrano 2 z 2").assertExists()
        composeTestRule.onNodeWithText("Zapisz plan").assertIsEnabled().performClick()
        assertEquals(1, saved)
    }

    @Test
    fun aProblemIsShownAboveTheList() {
        composeTestRule.setContent {
            Screen(SyncChangesUi(differences = differences, problem = "„Statystyka” wymaga: Ekonometria.", flagged = setOf(1)), { _, _ -> }) {}
        }

        composeTestRule.onNodeWithText("„Statystyka” wymaga: Ekonometria.", substring = true).performScrollTo()
    }

    @androidx.compose.runtime.Composable
    private fun Screen(state: SyncChangesUi, onPick: (Int, PlanSide) -> Unit, onSave: () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
            MAKTheme(dynamicColor = false) {
                Box(Modifier.width(320.dp).height(640.dp)) {
                    SyncChangesScreen(state = state, onPick = onPick, onSave = onSave)
                }
            }
        }
    }
}
