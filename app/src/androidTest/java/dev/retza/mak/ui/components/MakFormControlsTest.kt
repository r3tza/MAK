package dev.retza.mak.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.ui.theme.MAKTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MakFormControlsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun paletteUsesNamedAccessibleChoicesAt320Dp() {
        var selected by mutableStateOf("")

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(700.dp)) {
                    MakColorPalette(
                        selectedColor = selected,
                        onColorSelected = { selected = it }
                    )
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("Morski").performClick()
        composeTestRule.onNodeWithContentDescription("Morski").assertIsSelected()
        assertEquals("#137B71", selected)
    }

    @Test
    fun optionalSectionKeepsSecondaryContentHiddenUntilRequested() {
        var expanded by mutableStateOf(false)
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                MakExpandableSection(
                    label = "więcej opcji",
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    content = { Text("Sala") }
                )
            }
        }

        composeTestRule.onNodeWithText("Pokaż więcej opcji").performClick()
        composeTestRule.onNodeWithText("Sala").assertIsDisplayed()
    }

    @Test
    fun datePickerAndActionMenuKeepSecondaryInteractionsOutOfTheForm() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(390.dp).height(700.dp)) {
                    MakDatePickerField(
                        label = "Data",
                        value = "",
                        onValueChange = {}
                    )
                    MakActionMenu(actions = listOf("Usuń" to {}))
                }
            }
        }

        composeTestRule.onNodeWithText("Data").performClick()
        composeTestRule.onNodeWithText("Wybierz").assertIsDisplayed()
        composeTestRule.onNodeWithText("Anuluj").performClick()
        composeTestRule.onNodeWithContentDescription("Więcej opcji").performClick()
        composeTestRule.onNodeWithText("Usuń").assertIsDisplayed()
    }

    @Test
    fun darkThemeKeepsLabeledFieldVisibleAt390Dp() {
        composeTestRule.setContent {
            MAKTheme(darkTheme = true, dynamicColor = false) {
                Box(modifier = Modifier.width(390.dp).height(700.dp)) {
                    MakField(
                        label = "Bardzo długa nazwa przedmiotu",
                        value = "",
                        onValueChange = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Bardzo długa nazwa przedmiotu").assertIsDisplayed()
    }
}
