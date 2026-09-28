package dev.retza.mak.ui.semester

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.ui.theme.MAKTheme
import dev.retza.mak.ui.programs.StudyProgramEditorUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SemesterScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun course(
        assignmentId: String,
        name: String,
        calendarId: String = "1",
        calendarLabel: String = "2026-10-01 - 2027-02-28",
        sharesCalendar: Boolean = false
    ) = SemesterCourseUi(
        assignmentId = assignmentId,
        programId = assignmentId,
        name = name,
        color = "#137B71",
        calendarId = calendarId,
        calendarLabel = calendarLabel,
        sharesCalendar = sharesCalendar
    )

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
                            courseItems = listOf(course("1", "Informatyka")),
                            overrides = listOf(
                                WeekOverrideUi("override", "2026-10-05", WeekTypeUi.A, WeekOverrideScopeUi.ONE_WEEK)
                            ),
                            overrideCount = 1
                        ),
                        onSemesterNameChanged = {},
                        onSemesterStartDateChanged = {},
                        onSemesterEndDateChanged = {},
                        onSemesterFirstWeekChanged = {},
                        onSaveSemester = {},
                        onOpenCourses = { opened = "courses" },
                        onOpenOverrides = { opened = "overrides" },
                        onOpenCalendars = { opened = "calendars" },
                        onRetry = {}
                    )
                }
            }
        }

        composeTestRule.onAllNodesWithText("Pokaż kierunki").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Pokaż korekty tygodni").assertCountEquals(0)
        composeTestRule.onNodeWithText("1 kierunek").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("1 korekta").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Kierunki").performScrollTo().assertIsDisplayed().performClick()
        assertEquals("courses", opened)
        composeTestRule.onNodeWithText("Korekty tygodni").performScrollTo().assertIsDisplayed().performClick()
        assertEquals("overrides", opened)
    }

    @Test
    fun navigationRowsShowCountLabelAreButtonsAndAreNotHeadingsAt320Dp() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    SemesterScreen(
                        state = SemesterScreenUiState(
                            semester = SemesterFormUiState(name = "Semestr zimowy"),
                            courseItems = listOf(course("1", "Informatyka"), course("2", "Matematyka")),
                            calendars = listOf(calendar("1"), calendar("2", "2026-11-01", "2027-03-15")),
                            overrideCount = 0
                        ),
                        onSemesterNameChanged = {},
                        onSemesterStartDateChanged = {},
                        onSemesterEndDateChanged = {},
                        onSemesterFirstWeekChanged = {},
                        onSaveSemester = {},
                        onOpenCourses = {},
                        onOpenOverrides = {},
                        onOpenCalendars = {},
                        onRetry = {}
                    )
                }
            }
        }

        listOf(
            "Kierunki" to "2 kierunki",
            "Korekty tygodni" to "0 korekt",
            "Kalendarze" to "2 kalendarze"
        ).forEach { (title, countLabel) ->
            composeTestRule.onNodeWithText(countLabel).performScrollTo().assertIsDisplayed()
            composeTestRule.onNode(hasClickAction() and hasText(title))
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            composeTestRule.onNodeWithText(title, useUnmergedTree = true)
                .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Heading))
        }
    }

    @Test
    fun semesterConfigurationShowsMixedCalendarsAt320Dp() {
        var opened = ""

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    SemesterScreen(
                        state = SemesterScreenUiState(
                            semester = SemesterFormUiState(name = "Semestr zimowy"),
                            calendars = listOf(calendar("1"), calendar("2", "2026-11-01", "2027-03-15"))
                        ),
                        onSemesterNameChanged = {},
                        onSemesterStartDateChanged = {},
                        onSemesterEndDateChanged = {},
                        onSemesterFirstWeekChanged = {},
                        onSaveSemester = {},
                        onOpenCourses = {},
                        onOpenOverrides = {},
                        onOpenCalendars = { opened = "calendars" },
                        onRetry = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Różne kalendarze").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 kalendarze").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kalendarze").assertIsDisplayed().performClick()
        assertEquals("calendars", opened)
    }

    @Test
    fun coursesScreenKeepsLongNamesAndActionsAt320Dp() {
        var added = 0
        var edited = ""
        var deleted = ""

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    SemesterCoursesScreen(
                        state = SemesterScreenUiState(
                            courseItems = listOf(
                                course("1", "Informatyka stosowana i systemy wbudowane"),
                                course("2", "Automatyka i robotyka")
                            )
                        ),
                        onAddCourse = { added += 1 },
                        onEditCourse = { edited = it },
                        onDeleteCourse = { deleted = it },
                        onConfirmCourseDeletion = {},
                        onCancelCourseDeletion = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Informatyka stosowana i systemy wbudowane").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dodaj kierunek").assertIsDisplayed()
        assertTrue(
            composeTestRule.onNodeWithText("Dodaj kierunek").fetchSemanticsNode().boundsInRoot.top <
                composeTestRule.onNodeWithText("Informatyka stosowana i systemy wbudowane")
                    .fetchSemanticsNode().boundsInRoot.top
        )
        composeTestRule.onAllNodesWithText("Usuń").assertCountEquals(2)
        composeTestRule.onAllNodesWithText("Edytuj").assertCountEquals(2)
        composeTestRule.onAllNodesWithText("Edytuj")[0].performClick()
        assertEquals("1", edited)
        composeTestRule.onAllNodesWithText("Usuń")[1].performClick()
        assertEquals("2", deleted)
        composeTestRule.onNodeWithText("Rozdziel kalendarz").assertDoesNotExist()
        composeTestRule.onNodeWithText("Dodaj kierunek").performClick()
        assertEquals(1, added)
    }

    @Test
    fun courseAddScreenShowsFormAndCancelAt320Dp() {
        var cancelled = 0
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(1000.dp)) {
                    SemesterCourseAddScreen(
                        state = SemesterScreenUiState(
                            calendars = listOf(calendar("1"), calendar("2", "2026-11-01", "2027-03-15")),
                            courseProgramOptions = listOf(
                                SemesterProgramOptionUi("1", "Informatyka", "#137B71")
                            ),
                            selectedCalendarId = "1",
                            courseCalendarId = "1"
                        ),
                        onCourseNameChanged = {},
                        onCourseColorChanged = {},
                        onProgramModeChanged = {},
                        onSelectProgram = {},
                        onCourseModeChanged = {},
                        onCourseCalendarChanged = {},
                        onAddCourse = {},
                        onCancel = { cancelled++ },
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Wybierz istniejący").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nazwa nowego kierunku").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kolor kierunku").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Wspólne daty i tygodnie").assertIsDisplayed()
        composeTestRule.onNodeWithText("Osobne daty i tygodnie").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kalendarz kierunku").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dodaj kierunek").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Anuluj").performScrollTo().performClick()
        assertEquals(1, cancelled)
    }

    @Test
    fun courseAddScreenExistingProgramShowsSelectionInsteadOfNameAndColorDraft() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(1000.dp)) {
                    SemesterCourseAddScreen(
                        state = SemesterScreenUiState(
                            calendars = listOf(calendar("1")),
                            selectedCalendarId = "1",
                            courseCalendarId = "1",
                            courseProgramMode = CourseProgramModeUi.EXISTING,
                            courseProgramId = "1",
                            courseProgramOptions = listOf(
                                SemesterProgramOptionUi("1", "Informatyka", "#137B71")
                            )
                        ),
                        onCourseNameChanged = {},
                        onCourseColorChanged = {},
                        onProgramModeChanged = {},
                        onSelectProgram = {},
                        onCourseModeChanged = {},
                        onCourseCalendarChanged = {},
                        onAddCourse = {},
                        onCancel = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Istniejący kierunek").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nazwa nowego kierunku").assertDoesNotExist()
        composeTestRule.onNodeWithText("Kolor kierunku").assertDoesNotExist()
        composeTestRule.onNodeWithText(
            "Nazwa i kolor istniejącego kierunku są wspólne dla wszystkich semestrów."
        ).assertIsDisplayed()
    }

    @Test
    fun courseEditScreenShowsNameCalendarAndColorInOrderAndSharedSplitActionAt320Dp() {
        var separated = ""
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(1000.dp)) {
                    SemesterCourseEditScreen(
                        state = SemesterScreenUiState(
                            courseItems = listOf(course("1", "Informatyka", sharesCalendar = true)),
                            calendars = listOf(calendar("1")),
                            selectedCalendarId = "1"
                        ),
                        editor = StudyProgramEditorUi(id = 1L, name = "Informatyka", color = "#137B71"),
                        onNameChanged = {},
                        onColorChanged = {},
                        onSave = {},
                        onCancel = {},
                        onSeparateCourse = { separated = it },
                        onRequestReconnect = { _, _ -> },
                        onConfirmReconnect = {},
                        onCancelReconnect = {},
                    )
                }
            }
        }

        val nameTop = composeTestRule.onNodeWithText("Nazwa kierunku").fetchSemanticsNode().boundsInRoot.top
        val calendarTop = composeTestRule.onNodeWithText("Kalendarz w tym semestrze")
            .fetchSemanticsNode().boundsInRoot.top
        val colorTop = composeTestRule.onNodeWithText("Kolor kierunku")
            .fetchSemanticsNode().boundsInRoot.top
        assertTrue(nameTop < calendarTop)
        assertTrue(calendarTop < colorTop)
        composeTestRule.onNodeWithText("Rozdziel kalendarz").performScrollTo().performClick()
        assertEquals("1", separated)
        composeTestRule.onNodeWithText("Kalendarz").assertDoesNotExist()
    }

    @Test
    fun courseEditScreenShowsCalendarPickerOnlyWithMultipleCalendarsAndNoSplitForSeparateCalendar() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(1000.dp)) {
                    SemesterCourseEditScreen(
                        state = SemesterScreenUiState(
                            courseItems = listOf(course("1", "Informatyka", sharesCalendar = false)),
                            calendars = listOf(calendar("1"), calendar("2", "2026-11-01", "2027-03-15")),
                            selectedCalendarId = "1"
                        ),
                        editor = StudyProgramEditorUi(id = 1L, name = "Informatyka", color = "#137B71"),
                        onNameChanged = {},
                        onColorChanged = {},
                        onSave = {},
                        onCancel = {},
                        onSeparateCourse = {},
                        onRequestReconnect = { _, _ -> },
                        onConfirmReconnect = {},
                        onCancelReconnect = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Kalendarz").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Rozdziel kalendarz").assertDoesNotExist()
    }

    @Test
    fun courseEditScreenOmitsCalendarPickerWithSingleCalendar() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(1000.dp)) {
                    SemesterCourseEditScreen(
                        state = SemesterScreenUiState(
                            courseItems = listOf(course("1", "Informatyka", sharesCalendar = true)),
                            calendars = listOf(calendar("1")),
                            selectedCalendarId = "1"
                        ),
                        editor = StudyProgramEditorUi(id = 1L, name = "Informatyka", color = "#137B71"),
                        onNameChanged = {},
                        onColorChanged = {},
                        onSave = {},
                        onCancel = {},
                        onSeparateCourse = {},
                        onRequestReconnect = { _, _ -> },
                        onConfirmReconnect = {},
                        onCancelReconnect = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Kalendarz").assertDoesNotExist()
        composeTestRule.onNodeWithText("Rozdziel kalendarz").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kierunek dostanie własną kopię dat, rytmu A/B i korekt.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun coursesScreenShowsReconnectWarningAt320Dp() {
        var confirmed = 0

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    SemesterCourseEditScreen(
                        state = SemesterScreenUiState(
                            courseItems = listOf(course("1", "Informatyka", sharesCalendar = true)),
                            calendars = listOf(calendar("1"), calendar("2", "2026-11-01", "2027-03-15")),
                            selectedCalendarId = "1",
                            pendingReconnect = ReconnectCalendarUi(
                                assignmentId = "1",
                                calendarId = "2",
                                programName = "Informatyka",
                                sourceBecomesUnused = true
                            )
                        ),
                        editor = StudyProgramEditorUi(id = 1L, name = "Informatyka", color = "#137B71"),
                        onNameChanged = {},
                        onColorChanged = {},
                        onSave = {},
                        onCancel = {},
                        onSeparateCourse = {},
                        onRequestReconnect = { _, _ -> },
                        onConfirmReconnect = { confirmed += 1 },
                        onCancelReconnect = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Połączyć kalendarze?").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Dotychczasowy kalendarz nie będzie już używany i zostanie usunięty " +
                "razem ze swoimi korektami. Korekty nie zostaną przeniesione."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Połącz").performClick()
        assertEquals(1, confirmed)
    }

    @Test
    fun coursesScreenConfirmsCourseDeletionWithClassCountAt320Dp() {
        var confirmed = 0
        var cancelled = 0

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    SemesterCoursesScreen(
                        state = SemesterScreenUiState(
                            pendingCourseDeletion = CourseDeletionUi(
                                assignmentId = "1",
                                programName = "Informatyka",
                                classCount = 5
                            )
                        ),
                        onAddCourse = {},
                        onEditCourse = {},
                        onDeleteCourse = {},
                        onConfirmCourseDeletion = { confirmed += 1 },
                        onCancelCourseDeletion = { cancelled += 1 },
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Usunąć kierunek z semestru?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Liczba usuwanych zajęć: 5", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Anuluj").performClick()
        assertEquals(1, cancelled)
        assertEquals(0, confirmed)
        composeTestRule.onNodeWithText("Usuń").performClick()
        assertEquals(1, confirmed)
    }

    @Test
    fun calendarsScreenListsCalendarsAndAllowsUnusedDeleteAt320Dp() {
        var deleted = ""

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(1000.dp)) {
                    SemesterCalendarsScreen(
                        state = SemesterScreenUiState(
                            semester = SemesterFormUiState(
                                startDate = "2026-10-01",
                                endDate = "2027-02-28"
                            ),
                            calendars = listOf(
                                calendar("1", courseNames = listOf("Informatyka")),
                                calendar("2", "2026-11-01", "2027-03-15")
                            ),
                            selectedCalendarId = "1"
                        ),
                        onCalendarSelected = {},
                        onStartDateChanged = {},
                        onEndDateChanged = {},
                        onFirstWeekChanged = {},
                        onSaveCalendar = {},
                        onDeleteCalendar = { deleted = it },
                        onConfirmCalendarDeletion = {},
                        onCancelCalendarDeletion = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Edytowany kalendarz").assertIsDisplayed()
        composeTestRule.onNodeWithText("Zapisz kalendarz").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kierunki: Informatyka").assertIsDisplayed()
        composeTestRule.onNodeWithText("Brak przypisanych kierunków").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 lis 2026 - 15 mar 2027").assertIsDisplayed()
        composeTestRule.onNodeWithText("2026-11-01 - 2027-03-15").assertDoesNotExist()
        composeTestRule.onNodeWithText("Usuń").performClick()
        assertEquals("2", deleted)
    }

    @Test
    fun weekOverridesScreenOffersCalendarChoiceAt320Dp() {
        var selected = ""

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    SemesterWeekOverridesScreen(
                        state = SemesterScreenUiState(
                            calendars = listOf(calendar("1"), calendar("2", "2026-11-01", "2027-03-15")),
                            selectedCalendarId = "1"
                        ),
                        onCalendarSelected = { selected = it },
                        onWeekStartDateChanged = {},
                        onWeekTypeChanged = {},
                        onScopeChanged = {},
                        onNewOverride = {},
                        onEditOverride = {},
                        onSaveOverride = {},
                        onDeleteOverride = {},
                        onConfirmOverrideDeletion = {},
                        onCancelOverrideDeletion = {},
                        onCancelOverrideEdit = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("1 paź 2026 - 28 lut 2027, tydzień A, brak kierunków").performClick()
        composeTestRule.onNodeWithText("1 lis 2026 - 15 mar 2027, tydzień A, brak kierunków").performClick()
        assertEquals("2", selected)
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
                        onCalendarSelected = {},
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
                        onConfirmOverrideDeletion = {},
                        onCancelOverrideDeletion = {},
                        onCancelOverrideEdit = {
                            state = state.copy(overrideForm = WeekOverrideFormUiState())
                        },
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Dzień w tygodniu korekty").assertDoesNotExist()
        composeTestRule.onNodeWithText("Dodaj").performClick()
        composeTestRule.onNodeWithText("Dzień w tygodniu korekty").assertIsDisplayed()
        // The form title has the same text as the save button, so pick the clickable one.
        val addSaveLeft = composeTestRule.onNode(hasText("Dodaj korektę") and hasClickAction())
            .fetchSemanticsNode().boundsInRoot.left
        assertTrue(
            "cancel comes before save",
            composeTestRule.onNodeWithText("Anuluj").fetchSemanticsNode().boundsInRoot.left < addSaveLeft
        )
        composeTestRule.onNodeWithText("Anuluj").performClick()
        composeTestRule.onNodeWithText("Dzień w tygodniu korekty").assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("Więcej opcji").performClick()
        composeTestRule.onNodeWithText("Edytuj").performClick()
        composeTestRule.onNodeWithText("Dzień w tygodniu korekty").assertIsDisplayed()
        assertTrue(
            "cancel comes before save",
            composeTestRule.onNodeWithText("Anuluj").fetchSemanticsNode().boundsInRoot.left <
                composeTestRule.onNodeWithText("Zapisz zmiany").fetchSemanticsNode().boundsInRoot.left
        )
    }

    @Test
    fun weekOverridesScreenKeepsDarkThemeFormAt390Dp() {
        composeTestRule.setContent {
            MAKTheme(darkTheme = true, dynamicColor = false) {
                Box(modifier = Modifier.width(390.dp).height(900.dp)) {
                    SemesterWeekOverridesScreen(
                        state = SemesterScreenUiState(
                            overrides = listOf(
                                WeekOverrideUi("7", "2026-10-05", WeekTypeUi.A, WeekOverrideScopeUi.ONE_WEEK)
                            ),
                            overrideForm = WeekOverrideFormUiState(
                                id = "7",
                                weekStartDate = "2026-10-05",
                                isOpen = true
                            )
                        ),
                        onCalendarSelected = {},
                        onWeekStartDateChanged = {},
                        onWeekTypeChanged = {},
                        onScopeChanged = {},
                        onNewOverride = {},
                        onEditOverride = {},
                        onSaveOverride = {},
                        onDeleteOverride = {},
                        onConfirmOverrideDeletion = {},
                        onCancelOverrideDeletion = {},
                        onCancelOverrideEdit = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Dzień w tygodniu korekty").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Zapisz zmiany").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Anuluj").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun weekOverrideDeletionAsksForConfirmationAt320Dp() {
        var confirmed = 0
        var cancelled = 0
        val override = WeekOverrideUi("7", "2026-10-05", WeekTypeUi.A, WeekOverrideScopeUi.ONE_WEEK)
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    SemesterWeekOverridesScreen(
                        state = SemesterScreenUiState(
                            overrides = listOf(override),
                            pendingOverrideDeletion = override
                        ),
                        onCalendarSelected = {},
                        onWeekStartDateChanged = {},
                        onWeekTypeChanged = {},
                        onScopeChanged = {},
                        onNewOverride = {},
                        onEditOverride = {},
                        onSaveOverride = {},
                        onDeleteOverride = {},
                        onConfirmOverrideDeletion = { confirmed += 1 },
                        onCancelOverrideDeletion = { cancelled += 1 },
                        onCancelOverrideEdit = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Usunąć korektę tygodnia?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Korekta tygodnia od 5 października 2026", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Anuluj").performClick()
        composeTestRule.onNodeWithText("Usuń").performClick()

        assertEquals(1, cancelled)
        assertEquals(1, confirmed)
    }

    private fun calendar(
        id: String,
        startDate: String = "2026-10-01",
        endDate: String = "2027-02-28",
        courseNames: List<String> = emptyList()
    ) = SemesterCalendarUi(
        id = id,
        startDate = startDate,
        endDate = endDate,
        firstWeek = WeekTypeUi.A,
        courseNames = courseNames
    )
}
