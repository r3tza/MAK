package dev.retza.mak.ui.occurrence

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso
import dev.retza.mak.ui.components.FieldErrorUi
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
    fun detailsShowBothNotesAndSingleEditDialogAt320Dp() {
        var state by mutableStateOf(
            OccurrenceDetailsUiState(
                subjectName = "Bardzo długa nazwa zajęć do sprawdzenia układu",
                courseName = "Informatyka",
                typeLabel = "Wykład",
                dateLabel = "Poniedziałek, 21 września",
                currentDate = "2026-09-21",
                startTime = "09:00",
                endTime = "10:30",
                room = "L204",
                baseDate = "2026-09-21",
                baseStartTime = "09:00",
                baseEndTime = "10:30",
                baseRoom = "L204",
                targetDateDraft = "2026-09-21",
                startTimeDraft = "09:00",
                endTimeDraft = "10:30",
                roomDraft = "L204",
                sharedNote = "Notatka wspólna",
                occurrenceNoteDraft = "Notatka tylko dla tego terminu",
                canChangeOccurrence = true,
                canMoveOccurrence = true,
                canRestoreOccurrence = false,
                canSaveOccurrenceEdit = true
            )
        )

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .height(900.dp)
                ) {
                    OccurrenceDetailsScreen(
                        state = state,
                        onDeleteBaseClass = {},
                        onDismissDeleteConfirmation = {},
                        onOpenOccurrenceEdit = { state = state.copy(showEditDialog = true) },
                        onDismissOccurrenceEdit = { state = state.copy(showEditDialog = false) },
                        onSaveOccurrenceChange = {},
                        onRestoreOccurrence = {},
                        onOccurrenceNoteDraftChanged = {},
                        onSharedNoteDraftChanged = {},
                        onTargetDateDraftChanged = {},
                        onStartTimeDraftChanged = {},
                        onEndTimeDraftChanged = {},
                        onRoomDraftChanged = {},
                        onSaveSharedNote = {},
                        onSaveOccurrenceNote = {},
                        onBack = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Bardzo długa nazwa zajęć do sprawdzenia układu").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notatki").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notatka wspólna").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notatka tylko dla tego terminu").assertIsDisplayed()
        composeTestRule.onNodeWithText("Zmień termin").assertIsDisplayed()
        composeTestRule.onNodeWithText("Zamknij").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("TERMIN").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Pokaż notatkę").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Pokaż zmiana terminu").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Zapisz zmianę").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Przenieś termin").assertCountEquals(0)

        composeTestRule.onNodeWithText("Zmień termin").performClick()
        composeTestRule.onNodeWithText("Edytuj ten termin").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Zmiany dotyczą tylko tego terminu. Pozostałe wystąpienia zajęć pozostaną bez zmian.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Od").assertIsDisplayed()
        composeTestRule.onNodeWithText("Do").assertIsDisplayed()
        composeTestRule.onNodeWithText("Anuluj").assertIsDisplayed()
        composeTestRule.onNodeWithText("Zapisz").assertIsDisplayed()
    }

    @Test
    fun notesFieldsSaveIndependentlyAt320Dp() {
        var sharedSaves = 0
        var occurrenceSaves = 0

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .height(900.dp)
                ) {
                    OccurrenceDetailsScreen(
                        state = OccurrenceDetailsUiState(
                            dateLabel = "Poniedziałek, 21 września",
                            currentDate = "2026-09-21",
                            startTime = "09:00",
                            endTime = "10:30",
                            sharedNote = "Wspólna",
                            sharedNoteDraft = "Wspólna",
                            occurrenceNote = "Daty",
                            occurrenceNoteDraft = "Daty",
                            canSaveSharedNote = true,
                            canSaveOccurrenceNote = true
                        ),
                        onDeleteBaseClass = {},
                        onDismissDeleteConfirmation = {},
                        onOpenOccurrenceEdit = {},
                        onDismissOccurrenceEdit = {},
                        onSaveOccurrenceChange = {},
                        onRestoreOccurrence = {},
                        onOccurrenceNoteDraftChanged = {},
                        onSharedNoteDraftChanged = {},
                        onTargetDateDraftChanged = {},
                        onStartTimeDraftChanged = {},
                        onEndTimeDraftChanged = {},
                        onRoomDraftChanged = {},
                        onSaveSharedNote = { sharedSaves += 1 },
                        onSaveOccurrenceNote = { occurrenceSaves += 1 },
                        onBack = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Notatka dla wszystkich terminów").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notatka tylko dla tej daty").assertIsDisplayed()
        composeTestRule.onNodeWithText("Zapisz notatkę dla wszystkich terminów").assertIsDisplayed().performClick()
        assertEquals(1, sharedSaves)
        assertEquals(0, occurrenceSaves)
        composeTestRule.onNodeWithText("Zapisz notatkę dla tej daty").assertIsDisplayed().performClick()
        assertEquals(1, occurrenceSaves)
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
                            currentDate = "2026-09-22",
                            startTime = "11:00",
                            endTime = "12:30",
                            baseDate = "2026-09-22",
                            baseStartTime = "11:00",
                            baseEndTime = "12:30",
                            canChangeOccurrence = false,
                            canMoveOccurrence = false,
                            canRestoreOccurrence = true
                        ),
                        onDeleteBaseClass = {},
                        onDismissDeleteConfirmation = {},
                        onOpenOccurrenceEdit = {},
                        onDismissOccurrenceEdit = {},
                        onSaveOccurrenceChange = {},
                        onRestoreOccurrence = { restoreClicks += 1 },
                        onOccurrenceNoteDraftChanged = {},
                        onSharedNoteDraftChanged = {},
                        onTargetDateDraftChanged = {},
                        onStartTimeDraftChanged = {},
                        onEndTimeDraftChanged = {},
                        onRoomDraftChanged = {},
                        onSaveSharedNote = {},
                        onSaveOccurrenceNote = {},
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

    @Test
    fun editDialogIgnoresDismissWhileSavingAt320Dp() {
        var dismissCalls = 0

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
                            dateLabel = "Poniedziałek, 21 września",
                            currentDate = "2026-09-21",
                            startTime = "09:00",
                            endTime = "10:30",
                            targetDateDraft = "2026-09-21",
                            startTimeDraft = "09:00",
                            endTimeDraft = "10:30",
                            roomDraft = "L204",
                            baseDate = "2026-09-21",
                            baseStartTime = "09:00",
                            baseEndTime = "10:30",
                            baseRoom = "L204",
                            showEditDialog = true,
                            isSaving = true,
                            canSaveOccurrenceEdit = true
                        ),
                        onDeleteBaseClass = {},
                        onDismissDeleteConfirmation = {},
                        onOpenOccurrenceEdit = {},
                        onDismissOccurrenceEdit = { dismissCalls += 1 },
                        onSaveOccurrenceChange = {},
                        onRestoreOccurrence = {},
                        onOccurrenceNoteDraftChanged = {},
                        onSharedNoteDraftChanged = {},
                        onTargetDateDraftChanged = {},
                        onStartTimeDraftChanged = {},
                        onEndTimeDraftChanged = {},
                        onRoomDraftChanged = {},
                        onSaveSharedNote = {},
                        onSaveOccurrenceNote = {},
                        onBack = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Edytuj ten termin").assertIsDisplayed()
        composeTestRule.onNodeWithText("Anuluj").assertIsNotEnabled()
        Espresso.pressBack()
        composeTestRule.onNodeWithText("Edytuj ten termin").assertIsDisplayed()
        assertEquals(0, dismissCalls)
    }

    @Test
    fun editDialogShowsFieldValidationErrorAt320Dp() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .height(900.dp)
                ) {
                    OccurrenceDetailsScreen(
                        state = OccurrenceDetailsUiState(
                            dateLabel = "Poniedziałek, 21 września",
                            currentDate = "2026-09-21",
                            startTime = "09:00",
                            endTime = "10:30",
                            targetDateDraft = "2026-09-21",
                            startTimeDraft = "11:00",
                            endTimeDraft = "10:30",
                            baseDate = "2026-09-21",
                            baseStartTime = "09:00",
                            baseEndTime = "10:30",
                            showEditDialog = true,
                            draftErrors = mapOf(
                                OccurrenceEditField.EndTime to FieldErrorUi(
                                    "Koniec musi być późniejszy niż początek tego samego dnia."
                                )
                            )
                        ),
                        onDeleteBaseClass = {},
                        onDismissDeleteConfirmation = {},
                        onOpenOccurrenceEdit = {},
                        onDismissOccurrenceEdit = {},
                        onSaveOccurrenceChange = {},
                        onRestoreOccurrence = {},
                        onOccurrenceNoteDraftChanged = {},
                        onSharedNoteDraftChanged = {},
                        onTargetDateDraftChanged = {},
                        onStartTimeDraftChanged = {},
                        onEndTimeDraftChanged = {},
                        onRoomDraftChanged = {},
                        onSaveSharedNote = {},
                        onSaveOccurrenceNote = {},
                        onBack = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Edytuj ten termin").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Koniec musi być późniejszy niż początek tego samego dnia.")
            .assertIsDisplayed()
    }
}
