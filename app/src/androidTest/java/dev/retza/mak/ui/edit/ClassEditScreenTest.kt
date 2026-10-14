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
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.ui.theme.MAKTheme
import org.junit.Assert.assertEquals
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
                        onConfirmDiscard = {},
                        onDismissDiscard = {},
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
                        onConfirmDiscard = {},
                        onDismissDiscard = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Część danych przestanie być widoczna").assertIsDisplayed()
        // "Anuluj" is also the screen's own secondary action, so match the button inside the dialog pane.
        composeTestRule.onNode(dialogButton("Zapisz")).assertIsNotEnabled()
        composeTestRule.onNode(dialogButton("Anuluj")).assertIsNotEnabled()
    }

    @Test
    fun discardDialogShowsTextAndReportsBothChoices() {
        var confirmed = 0
        var dismissed = 0
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                ClassEditScreen(
                    state = ClassEditUiState(showDiscardConfirmation = true),
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
                    onConfirmDiscard = { confirmed++ },
                    onDismissDiscard = { dismissed++ }
                )
            }
        }

        composeTestRule.onNodeWithText("Odrzucić zmiany?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wpisane dane nie zostaną zapisane.").assertIsDisplayed()
        composeTestRule.onNode(hasText("Anuluj") and hasAnyAncestor(isDialog())).performClick()
        composeTestRule.onNodeWithText("Odrzuć").performClick()
        assertEquals(1, dismissed)
        assertEquals(1, confirmed)
    }

    @Test
    fun formCancelReportsCancel() {
        var cancelled = 0
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                ClassEditScreen(
                    state = ClassEditUiState(),
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
                    onCancel = { cancelled++ },
                    onConfirmDiscard = {},
                    onDismissDiscard = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Anuluj").performClick()
        assertEquals(1, cancelled)
    }

    private fun dialogButton(text: String) = hasText(text) and hasAnyAncestor(
        SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Część danych przestanie być widoczna")
    )
}
