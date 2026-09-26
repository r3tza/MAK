package dev.retza.mak.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
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
    fun colorPickerAcceptsReadableCodeAndNamesSlidersAt320Dp() {
        var selected by mutableStateOf(DefaultCourseColor)

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    MakCourseColorPicker(
                        selectedColor = selected,
                        onColorSelected = { selected = it },
                        previewName = "Informatyka"
                    )
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("Odcień").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Jasność").assertIsDisplayed()
        composeTestRule.onNodeWithText("Informatyka").assertIsDisplayed()
        val readable = courseHex(courseColorFrom(210f, 0.5f))
        composeTestRule.onNodeWithText("Kod koloru").performTextReplacement(readable)
        assertEquals(readable, selected)
    }

    @Test
    fun colorPickerRejectsTooLightCode() {
        var selected by mutableStateOf(DefaultCourseColor)

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                MakCourseColorPicker(selectedColor = selected, onColorSelected = { selected = it })
            }
        }

        composeTestRule.onNodeWithText("Kod koloru").performTextReplacement("#FFEB3B")

        composeTestRule
            .onNodeWithText("Ten kolor będzie słabo widoczny. Wybierz inny odcień albo jasność.")
            .assertIsDisplayed()
        assertEquals(DefaultCourseColor, selected)
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

        composeTestRule.onNodeWithContentDescription("więcej opcji")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Zwinięte"))
        composeTestRule.onNodeWithText("Sala").assertDoesNotExist()
        composeTestRule.onNodeWithText("Pokaż więcej opcji").performClick()
        composeTestRule.onNodeWithText("Sala").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("więcej opcji")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Rozwinięte"))
        composeTestRule.onNodeWithText("Ukryj więcej opcji").performClick()
        composeTestRule.onNodeWithText("Sala").assertDoesNotExist()
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
    fun dateAndTimePickersConfirmThroughExplicitAccessibleActions() {
        var date by mutableStateOf("2026-09-19")
        var time by mutableStateOf("09:30")

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Column(modifier = Modifier.width(390.dp)) {
                    MakDatePickerField(
                        label = "Data",
                        value = date,
                        onValueChange = { date = it },
                        minDate = java.time.LocalDate.of(2026, 9, 1),
                        maxDate = java.time.LocalDate.of(2026, 9, 30)
                    )
                    MakTimePickerField(
                        label = "Godzina",
                        value = time,
                        onValueChange = { time = it }
                    )
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("Wybierz datę").performClick()
        composeTestRule.onNodeWithText("Wybierz").performClick()
        composeTestRule.onNodeWithContentDescription("Wybierz godzinę").performClick()
        composeTestRule.onNodeWithText("Wybierz").performClick()

        assertEquals("2026-09-19", date)
        assertEquals("09:30", time)
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
