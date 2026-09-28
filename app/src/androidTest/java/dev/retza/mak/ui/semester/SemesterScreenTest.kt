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
import androidx.compose.ui.test.performScrollTo
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
        var deleted = ""

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    SemesterCoursesScreen(
                        state = SemesterScreenUiState(
                            courseItems = listOf(
                                course("1", "Informatyka stosowana i systemy wbudowane"),
                                course("2", "Automatyka i robotyka")
                            ),
                            courseNameDraft = "Nowy kierunek"
                        ),
                        onCourseNameChanged = {},
                        onCourseColorChanged = {},
                        onProgramModeChanged = {},
                        onSelectProgram = {},
                        onCourseModeChanged = {},
                        onCourseCalendarChanged = {},
                        onAddCourse = { added += 1 },
                        onDeleteCourse = { deleted = it },
                        onSeparateCourse = {},
                        onRequestReconnect = { _, _ -> },
                        onConfirmReconnect = {},
                        onCancelReconnect = {},
                        onConfirmCourseDeletion = {},
                        onCancelCourseDeletion = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Informatyka stosowana i systemy wbudowane").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Usuń").assertCountEquals(2)
        composeTestRule.onAllNodesWithText("Usuń")[1].performClick()
        assertEquals("2", deleted)
        composeTestRule.onNodeWithText("Dodaj kierunek").performScrollTo().performClick()
        assertEquals(1, added)
    }

    @Test
    fun coursesScreenOffersSeparateModeAndSharedProgramNoteAt320Dp() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(1000.dp)) {
                    SemesterCoursesScreen(
                        state = SemesterScreenUiState(
                            calendars = listOf(calendar("1"), calendar("2", "2026-11-01", "2027-03-15")),
                            courseProgramMode = CourseProgramModeUi.EXISTING,
                            courseProgramId = "1",
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
                        onDeleteCourse = {},
                        onSeparateCourse = {},
                        onRequestReconnect = { _, _ -> },
                        onConfirmReconnect = {},
                        onCancelReconnect = {},
                        onConfirmCourseDeletion = {},
                        onCancelCourseDeletion = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Wybierz istniejący").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Kierunek jest współdzielony między semestrami. " +
                "Nazwę i kolor zmienisz w ustawieniach, w pozycji Kierunki."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Wspólne daty i tygodnie").assertIsDisplayed()
        composeTestRule.onNodeWithText("Osobne daty i tygodnie").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kalendarz kierunku").assertIsDisplayed()
    }

    @Test
    fun coursesScreenSeparateModeExplainsCopyAt320Dp() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(1000.dp)) {
                    SemesterCoursesScreen(
                        state = SemesterScreenUiState(
                            courseCalendarMode = CourseCalendarModeUi.SEPARATE
                        ),
                        onCourseNameChanged = {},
                        onCourseColorChanged = {},
                        onProgramModeChanged = {},
                        onSelectProgram = {},
                        onCourseModeChanged = {},
                        onCourseCalendarChanged = {},
                        onAddCourse = {},
                        onDeleteCourse = {},
                        onSeparateCourse = {},
                        onRequestReconnect = { _, _ -> },
                        onConfirmReconnect = {},
                        onCancelReconnect = {},
                        onConfirmCourseDeletion = {},
                        onCancelCourseDeletion = {},
                    )
                }
            }
        }

        // The color picker above is tall, so the note may start below the fold.
        composeTestRule.onNodeWithText("Powstanie kopia dat, rytmu i korekt wybranego kalendarza.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun coursesScreenShowsReconnectWarningAt320Dp() {
        var confirmed = 0

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(900.dp)) {
                    SemesterCoursesScreen(
                        state = SemesterScreenUiState(
                            pendingReconnect = ReconnectCalendarUi(
                                assignmentId = "1",
                                calendarId = "2",
                                programName = "Informatyka",
                                sourceBecomesUnused = true
                            )
                        ),
                        onCourseNameChanged = {},
                        onCourseColorChanged = {},
                        onProgramModeChanged = {},
                        onSelectProgram = {},
                        onCourseModeChanged = {},
                        onCourseCalendarChanged = {},
                        onAddCourse = {},
                        onDeleteCourse = {},
                        onSeparateCourse = {},
                        onRequestReconnect = { _, _ -> },
                        onConfirmReconnect = { confirmed += 1 },
                        onCancelReconnect = {},
                        onConfirmCourseDeletion = {},
                        onCancelCourseDeletion = {},
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
                        onCourseNameChanged = {},
                        onCourseColorChanged = {},
                        onProgramModeChanged = {},
                        onSelectProgram = {},
                        onCourseModeChanged = {},
                        onCourseCalendarChanged = {},
                        onAddCourse = {},
                        onDeleteCourse = {},
                        onSeparateCourse = {},
                        onRequestReconnect = { _, _ -> },
                        onConfirmReconnect = {},
                        onCancelReconnect = {},
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
        composeTestRule.onNodeWithText("Anuluj").performClick()
        composeTestRule.onNodeWithText("Dzień w tygodniu korekty").assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("Więcej opcji").performClick()
        composeTestRule.onNodeWithText("Edytuj").performClick()
        composeTestRule.onNodeWithText("Dzień w tygodniu korekty").assertIsDisplayed()
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
