package dev.retza.mak.ui.programs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.ui.theme.MAKTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudyProgramsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun listShowsProgramsWithDistinctNamesAndOpensEditAt320Dp() {
        var opened: Long? = null
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(700.dp)) {
                    StudyProgramsScreen(
                        state = StudyProgramsUiState(
                            programs = listOf(
                                StudyProgramUi(1L, "Informatyka", "#137B71"),
                                StudyProgramUi(2L, "Informatyka", "#334FCE"),
                                StudyProgramUi(3L, "Bardzo długa nazwa kierunku Zarządzanie i inżynieria produkcji", "#A65724")
                            )
                        ),
                        onOpenProgram = { opened = it },
                        onVisibleChange = { _, _ -> }
                    )
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("Informatyka (1), edytuj").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(
            "Bardzo długa nazwa kierunku Zarządzanie i inżynieria produkcji, edytuj"
        ).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Informatyka (2), edytuj").performClick()

        assertEquals(2L, opened)
    }

    @Test
    fun checkboxAndEditAreSeparateTargetsAndHiddenProgramIsMarkedAt320Dp() {
        var visibility: Pair<Long, Boolean>? = null
        var opened: Long? = null
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(700.dp)) {
                    StudyProgramsScreen(
                        state = StudyProgramsUiState(
                            programs = listOf(
                                StudyProgramUi(1L, "Informatyka", "#137B71"),
                                StudyProgramUi(2L, "Zarządzanie", "#334FCE", isHidden = true)
                            )
                        ),
                        onOpenProgram = { opened = it },
                        onVisibleChange = { id, visible -> visibility = id to visible }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Odznacz kierunek, aby ukryć jego zajęcia na tym telefonie.", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Pokazuj Informatyka").assertIsOn()
        composeTestRule.onNodeWithContentDescription("Pokazuj Zarządzanie").assertIsOff().performClick()
        assertEquals(2L to true, visibility)
        assertEquals(null, opened)

        composeTestRule.onNodeWithContentDescription("Zarządzanie, ukryty na tym telefonie, edytuj").performClick()
        assertEquals(2L, opened)
    }

    @Test
    fun emptyListExplainsWhereProgramsComeFrom() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                StudyProgramsScreen(state = StudyProgramsUiState(), onOpenProgram = {}, onVisibleChange = { _, _ -> })
            }
        }

        composeTestRule.onNodeWithText("Kierunki pojawią się po skonfigurowaniu planu.").assertIsDisplayed()
    }

    @Test
    fun editScreenShowsErrorAndActionsAt320Dp() {
        var saved = 0
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    StudyProgramEditScreen(
                        editor = StudyProgramEditorUi(id = 1L, name = "", nameError = "Podaj nazwę kierunku."),
                        onNameChanged = {},
                        onColorChanged = {},
                        onSave = { saved += 1 },
                        onCancel = {},
                        onRequestDelete = {},
                        onConfirmDelete = {},
                        onCancelDelete = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Podaj nazwę kierunku.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Anuluj").assertIsDisplayed()
        composeTestRule.onNodeWithText("Zapisz kierunek").performClick()

        assertEquals(1, saved)
    }

    @Test
    fun assignedProgramHasDisabledDeleteWithExplanationAt320Dp() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    StudyProgramEditScreen(
                        editor = StudyProgramEditorUi(
                            id = 1L,
                            name = "Informatyka",
                            usedInSemesters = listOf("Semestr zimowy", "Semestr letni")
                        ),
                        onNameChanged = {},
                        onColorChanged = {},
                        onSave = {},
                        onCancel = {},
                        onRequestDelete = {},
                        onConfirmDelete = {},
                        onCancelDelete = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Usuń kierunek").performScrollTo().assertIsNotEnabled()
        composeTestRule.onNodeWithText("Semestr zimowy, Semestr letni", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }
}
