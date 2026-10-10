package dev.retza.mak.ui.edit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.ui.theme.MAKTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClassEditScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun hiddenStudyProgramIsMarkedAndExplainedAt320Dp() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    ClassEditScreen(
                        state = ClassEditUiState(
                            courseName = "Zarządzanie",
                            semesterProgramId = 2L,
                            courseOptions = listOf(
                                ClassCourseOptionUi(1L, "Informatyka"),
                                ClassCourseOptionUi(2L, "Zarządzanie", isHidden = true)
                            )
                        ),
                        onNameChanged = {},
                        onCourseChanged = {},
                        onTypeChanged = {},
                        onDayChanged = {},
                        onStartTimeChanged = {},
                        onEndTimeChanged = {},
                        onRecurrenceChanged = {},
                        onOccurrenceDateChanged = {},
                        onRoomChanged = {},
                        onBuildingChanged = {},
                        onGroupChanged = {},
                        onTeacherChanged = {},
                        onNoteChanged = {},
                        onSave = {},
                        onConfirmHiddenData = {},
                        onDismissHiddenData = {},
                        onCancel = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Ten kierunek jest ukryty na tym telefonie.", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Zarządzanie").performClick()
        composeTestRule.onNodeWithText("ukryty").assertIsDisplayed()
    }

    @Test
    fun hiddenDataDialogBlocksActionsWhileSavingAt320Dp() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    ClassEditScreen(
                        state = ClassEditUiState(
                            courseName = "Matematyka",
                            isSaving = true,
                            pendingHiddenData = HiddenDataWarningUi(changeCount = 1, noteCount = 0)
                        ),
                        onNameChanged = {},
                        onCourseChanged = {},
                        onTypeChanged = {},
                        onDayChanged = {},
                        onStartTimeChanged = {},
                        onEndTimeChanged = {},
                        onRecurrenceChanged = {},
                        onOccurrenceDateChanged = {},
                        onRoomChanged = {},
                        onBuildingChanged = {},
                        onGroupChanged = {},
                        onTeacherChanged = {},
                        onNoteChanged = {},
                        onSave = {},
                        onConfirmHiddenData = {},
                        onDismissHiddenData = {},
                        onCancel = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Część danych przestanie być widoczna").assertIsDisplayed()
        // "Anuluj" is also the screen's own secondary action, so match the button inside the dialog pane.
        composeTestRule.onNode(dialogButton("Zapisz")).assertIsNotEnabled()
        composeTestRule.onNode(dialogButton("Anuluj")).assertIsNotEnabled()
    }

    private fun dialogButton(text: String) = hasText(text) and hasAnyAncestor(
        SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Część danych przestanie być widoczna")
    )
}
