package dev.retza.mak.ui.edit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
                            semesterProgramId = "2",
                            courseOptions = listOf(
                                ClassCourseOptionUi("1", "Informatyka"),
                                ClassCourseOptionUi("2", "Zarządzanie", isHidden = true)
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
                        onRetry = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Ten kierunek jest ukryty na tym telefonie.", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Zarządzanie").performClick()
        composeTestRule.onNodeWithText("ukryty").assertIsDisplayed()
    }
}
