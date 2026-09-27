package dev.retza.mak.ui.setup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
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
class SetupWizardTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyWizardShowsSemesterStepAndAcceptsBasicActionsAt320Dp() {
        var nextClicks = 0
        var selectedWeek = ""

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .height(700.dp)
                ) {
                    SetupWizard(
                        state = SetupWizardUiState(),
                        onSemesterNameChanged = {},
                        onStartDateChanged = {},
                        onEndDateChanged = {},
                        onFirstWeekChanged = { selectedWeek = it },
                        onCourseNameChanged = {},
                        onCourseColorChanged = {},
                        onProgramModeChanged = {},
                        onProgramSelected = {},
                        onNext = { nextClicks += 1 },
                        onBack = {},
                        onAddClass = {},
                        onActivateAndAddClass = {},
                        onFinish = {},
                        onRetry = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("KONFIGURACJA POCZĄTKOWA").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nazwa semestru").assertIsDisplayed()
        composeTestRule.onNodeWithText("A", substring = false).performClick()
        composeTestRule.onNodeWithText("B").performClick()
        // The step title repeats the button label, so target the clickable node.
        composeTestRule.onNode(hasText("Utwórz semestr") and hasClickAction()).performClick()

        assertEquals("B", selectedWeek)
        assertEquals(1, nextClicks)
    }

    @Test
    fun classesStepListsProgramsAndOffersAnotherProgramAt320Dp() {
        var anotherClicks = 0
        showWizard(
            SetupWizardUiState(step = SetupStep.Classes, semesterProgramNames = listOf("Informatyka", "Fizyka")),
            onAddAnotherProgram = { anotherClicks += 1 }
        )

        composeTestRule.onNodeWithText("Krok 3 z 3. Semestr i kierunki są gotowe.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kierunki w semestrze").assertIsDisplayed()
        composeTestRule.onNodeWithText("Informatyka, Fizyka").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dodaj kolejny kierunek").assertIsDisplayed().performClick()
        assertEquals(1, anotherClicks)
    }

    @Test
    fun anotherProgramStepOffersWeekChoiceAt320Dp() {
        var mode: SetupCalendarMode? = null
        showWizard(
            SetupWizardUiState(
                step = SetupStep.Course,
                semesterName = "Zimowy",
                isAddingAnotherProgram = true,
                semesterProgramNames = listOf("Informatyka")
            ),
            onCalendarModeChanged = { mode = it }
        )

        composeTestRule.onNodeWithText("Dodaj kolejny kierunek").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tygodnie A/B").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Wspólne z pierwszym kierunkiem").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Osobne dla tego kierunku").performScrollTo().performClick()
        assertEquals(SetupCalendarMode.Separate, mode)
    }

    private fun showWizard(
        state: SetupWizardUiState,
        onAddAnotherProgram: () -> Unit = {},
        onCalendarModeChanged: (SetupCalendarMode) -> Unit = {}
    ) {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(700.dp)) {
                    SetupWizard(
                        state = state,
                        onSemesterNameChanged = {},
                        onStartDateChanged = {},
                        onEndDateChanged = {},
                        onFirstWeekChanged = {},
                        onCourseNameChanged = {},
                        onCourseColorChanged = {},
                        onProgramModeChanged = {},
                        onProgramSelected = {},
                        onNext = {},
                        onBack = {},
                        onAddClass = {},
                        onActivateAndAddClass = {},
                        onFinish = {},
                        onRetry = {},
                        onAddAnotherProgram = onAddAnotherProgram,
                        onCalendarModeChanged = onCalendarModeChanged
                    )
                }
            }
        }
    }

    @Test
    fun classesStepForInactiveSemesterOffersActivationAt320Dp() {
        var activations = 0
        var addClicks = 0

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .height(700.dp)
                ) {
                    SetupWizard(
                        state = SetupWizardUiState(step = SetupStep.Classes, isSemesterActive = false),
                        onSemesterNameChanged = {},
                        onStartDateChanged = {},
                        onEndDateChanged = {},
                        onFirstWeekChanged = {},
                        onCourseNameChanged = {},
                        onCourseColorChanged = {},
                        onProgramModeChanged = {},
                        onProgramSelected = {},
                        onNext = {},
                        onBack = {},
                        onAddClass = { addClicks += 1 },
                        onActivateAndAddClass = { activations += 1 },
                        onFinish = {},
                        onRetry = {}
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithText("Zacznie obowiązywać, gdy wybierzesz go jako aktywny w ustawieniach.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Zakończ").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ustaw jako aktywny i dodaj zajęcia").performClick()

        assertEquals(1, activations)
        assertEquals(0, addClicks)
    }

    @Test
    fun courseStepOffersExistingProgramAndHidesColorAt320Dp() {
        var state by mutableStateOf(
            SetupWizardUiState(
                step = SetupStep.Course,
                programOptions = listOf(SetupProgramOptionUi(1L, "Informatyka", "#137B71"))
            )
        )
        var selectedProgram: Long? = null

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .height(700.dp)
                ) {
                    SetupWizard(
                        state = state,
                        onSemesterNameChanged = {},
                        onStartDateChanged = {},
                        onEndDateChanged = {},
                        onFirstWeekChanged = {},
                        onCourseNameChanged = {},
                        onCourseColorChanged = {},
                        onProgramModeChanged = { mode -> state = state.copy(programMode = mode) },
                        onProgramSelected = { selectedProgram = it },
                        onNext = {},
                        onBack = {},
                        onAddClass = {},
                        onActivateAndAddClass = {},
                        onFinish = {},
                        onRetry = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Kolor kierunku").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wybierz istniejący").performClick()
        composeTestRule.onNodeWithText("Kolor kierunku").assertDoesNotExist()
        composeTestRule.onNodeWithText("Nazwa kierunku").assertDoesNotExist()
        composeTestRule.onNodeWithText("Istniejący kierunek").performClick()
        composeTestRule.onNodeWithText("Informatyka").performClick()
        assertEquals(1L, selectedProgram)
    }
}
