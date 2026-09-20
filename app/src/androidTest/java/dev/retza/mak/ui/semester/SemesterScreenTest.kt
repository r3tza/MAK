package dev.retza.mak.ui.semester

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
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
class SemesterScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun semesterConfigurationUsesNavigationRowsAt320Dp() {
        var opened = ""

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(800.dp)) {
                    SemesterScreen(
                        state = SemesterScreenUiState(
                            semester = SemesterFormUiState(
                                name = "Semestr zimowy",
                                startDate = "2026-10-01",
                                endDate = "2027-02-28"
                            ),
                            courses = listOf("course" to "Informatyka"),
                            overrides = listOf(
                                WeekOverrideUi("override", "2026-10-05", WeekTypeUi.A, WeekOverrideScopeUi.ONE_WEEK)
                            )
                        ),
                        onSemesterNameChanged = {},
                        onSemesterStartDateChanged = {},
                        onSemesterEndDateChanged = {},
                        onSemesterFirstWeekChanged = {},
                        onSaveSemester = {},
                        onOpenCourses = { opened = "courses" },
                        onOpenOverrides = { opened = "overrides" },
                        onBack = {},
                        onRetry = {}
                    )
                }
            }
        }

        composeTestRule.onAllNodesWithText("Pokaż kierunki").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Pokaż korekty tygodni").assertCountEquals(0)
        composeTestRule.onNodeWithText("Zajęcia należące do tego semestru. Liczba: 1.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ręczne oznaczenia tygodni A/B. Liczba: 1.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kierunki").assertIsDisplayed().performClick()
        assertEquals("courses", opened)
        composeTestRule.onNodeWithText("Korekty tygodni").assertIsDisplayed().performClick()
        assertEquals("overrides", opened)
    }

    @Test
    fun coursesScreenKeepsLongNamesAndActionsAt320Dp() {
        var added = 0
        var deleted = ""

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    SemesterCoursesScreen(
                        state = SemesterScreenUiState(
                            courses = listOf(
                                "1" to "Informatyka stosowana i systemy wbudowane",
                                "2" to "Automatyka i robotyka"
                            ),
                            courseNameDraft = "Nowy kierunek"
                        ),
                        onCourseNameChanged = {},
                        onCourseColorChanged = {},
                        onAddCourse = { added += 1 },
                        onDeleteCourse = { deleted = it },
                        onBack = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Informatyka stosowana i systemy wbudowane").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Usuń").assertCountEquals(2)
        composeTestRule.onAllNodesWithText("Usuń")[1].performClick()
        assertEquals("2", deleted)
        composeTestRule.onNodeWithText("Dodaj kierunek").performClick()
        assertEquals(1, added)
    }

    @Test
    fun weekOverridesScreenOpensFormOnlyAfterAddOrEditAt320Dp() {
        var state by mutableStateOf(
            SemesterScreenUiState(
                overrides = listOf(
                    WeekOverrideUi("7", "2026-10-05", WeekTypeUi.A, WeekOverrideScopeUi.ONE_WEEK)
                )
            )
        )

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    SemesterWeekOverridesScreen(
                        state = state,
                        onWeekStartDateChanged = {},
                        onWeekTypeChanged = {},
                        onScopeChanged = {},
                        onNewOverride = {
                            state = state.copy(overrideForm = WeekOverrideFormUiState(isOpen = true))
                        },
                        onEditOverride = { id ->
                            state = state.copy(
                                overrideForm = WeekOverrideFormUiState(
                                    id = id,
                                    weekStartDate = "2026-10-05",
                                    isOpen = true
                                )
                            )
                        },
                        onSaveOverride = {
                            state = state.copy(overrideForm = WeekOverrideFormUiState())
                        },
                        onDeleteOverride = {},
                        onCancelOverrideEdit = {
                            state = state.copy(overrideForm = WeekOverrideFormUiState())
                        },
                        onBack = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Poniedziałek tygodnia").assertDoesNotExist()
        composeTestRule.onNodeWithText("Dodaj").performClick()
        composeTestRule.onNodeWithText("Poniedziałek tygodnia").assertIsDisplayed()
        composeTestRule.onNodeWithText("Anuluj").performClick()
        composeTestRule.onNodeWithText("Poniedziałek tygodnia").assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("Więcej opcji").performClick()
        composeTestRule.onNodeWithText("Edytuj").performClick()
        composeTestRule.onNodeWithText("Poniedziałek tygodnia").assertIsDisplayed()
    }

    @Test
    fun weekOverridesScreenKeepsDarkThemeFormAt390Dp() {
        composeTestRule.setContent {
            MAKTheme(darkTheme = true, dynamicColor = false) {
                Box(modifier = Modifier.width(390.dp).height(900.dp)) {
                    SemesterWeekOverridesScreen(
                        state = SemesterScreenUiState(
                            overrideForm = WeekOverrideFormUiState(
                                id = "7",
                                weekStartDate = "2026-10-05",
                                isOpen = true
                            )
                        ),
                        onWeekStartDateChanged = {},
                        onWeekTypeChanged = {},
                        onScopeChanged = {},
                        onNewOverride = {},
                        onEditOverride = {},
                        onSaveOverride = {},
                        onDeleteOverride = {},
                        onCancelOverrideEdit = {},
                        onBack = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Poniedziałek tygodnia").assertIsDisplayed()
        composeTestRule.onNodeWithText("Zapisz zmiany").assertIsDisplayed()
        composeTestRule.onNodeWithText("Anuluj").assertIsDisplayed()
    }
}
