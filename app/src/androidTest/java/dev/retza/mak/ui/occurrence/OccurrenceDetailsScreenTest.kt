package dev.retza.mak.ui.occurrence

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.ui.theme.MAKTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OccurrenceDetailsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun detailsShowExpandedNoteAndStableBottomActionsAt320Dp() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .height(900.dp)
                ) {
                    OccurrenceDetailsScreen(
                        state = OccurrenceDetailsUiState(
                            subjectName = "Bardzo długa nazwa zajęć do sprawdzenia układu",
                            courseName = "Informatyka",
                            typeLabel = "Wykład",
                            dateLabel = "Poniedziałek, 21 września",
                            startTime = "09:00",
                            endTime = "10:30",
                            sharedNote = "Notatka wspólna",
                            occurrenceNoteDraft = "Notatka tylko dla tego terminu",
                            canEditBaseClass = false,
                            canDeleteBaseClass = false,
                            canCancelOccurrence = false,
                            canChangeOccurrence = true,
                            canMoveOccurrence = true,
                            canEditOccurrenceNote = false
                        ),
                        onDeleteBaseClass = {},
                        onDismissDeleteConfirmation = {},
                        onChangeOccurrence = {},
                        onMoveOccurrence = {},
                        onRestoreOccurrence = {},
                        onOccurrenceNoteDraftChanged = {},
                        onTargetDateDraftChanged = {},
                        onStartTimeDraftChanged = {},
                        onEndTimeDraftChanged = {},
                        onRoomDraftChanged = {},
                        onSaveOccurrenceNote = {},
                        onDeleteOccurrenceNote = {},
                        onBack = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Bardzo długa nazwa zajęć do sprawdzenia układu").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notatka do zajęć: Notatka wspólna").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notatka tylko dla tego terminu").assertIsDisplayed()
        composeTestRule.onNodeWithText("Zmień termin").assertIsDisplayed()
        composeTestRule.onNodeWithText("Zamknij").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("TERMIN").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Pokaż notatkę").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Pokaż zmiana terminu").assertCountEquals(0)

        composeTestRule.onNodeWithText("Zmień termin").performClick()
        composeTestRule.onAllNodesWithText("Zmień termin").assertCountEquals(0)
        composeTestRule.onNodeWithText("Zamknij").assertIsDisplayed()
        composeTestRule.onNodeWithText("Zapisz zmianę").assertIsDisplayed()
        composeTestRule.onNodeWithText("Przenieś termin").assertIsDisplayed()
    }

    @Test
    fun restoreCaseShowsAndRunsRestoreActionAt320Dp() {
        var restoreClicks = 0

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .height(900.dp)
                ) {
                    OccurrenceDetailsScreen(
                        state = OccurrenceDetailsUiState(
                            subjectName = "Matematyka",
                            courseName = "Informatyka",
                            typeLabel = "Ćwiczenia",
                            dateLabel = "Wtorek, 22 września",
                            startTime = "11:00",
                            endTime = "12:30",
                            canChangeOccurrence = false,
                            canMoveOccurrence = false,
                            canRestoreOccurrence = true,
                            canEditOccurrenceNote = false
                        ),
                        onDeleteBaseClass = {},
                        onDismissDeleteConfirmation = {},
                        onChangeOccurrence = {},
                        onMoveOccurrence = {},
                        onRestoreOccurrence = { restoreClicks += 1 },
                        onOccurrenceNoteDraftChanged = {},
                        onTargetDateDraftChanged = {},
                        onStartTimeDraftChanged = {},
                        onEndTimeDraftChanged = {},
                        onRoomDraftChanged = {},
                        onSaveOccurrenceNote = {},
                        onDeleteOccurrenceNote = {},
                        onBack = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Przywróć termin").assertIsDisplayed().performClick()
        assertEquals(1, restoreClicks)
        composeTestRule.onNodeWithText("Zamknij").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Zmień termin").assertCountEquals(0)
    }
}
