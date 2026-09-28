package dev.retza.mak.ui.schedule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.ui.components.ClassItemUi
import dev.retza.mak.ui.components.CalendarDayUi
import dev.retza.mak.ui.components.CalendarLegendUi
import dev.retza.mak.ui.semester.WeekTypeUi
import dev.retza.mak.ui.theme.MAKTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun listShowsWeekStateAndOpensCorrectionWithoutDetachedMenuAt320Dp() {
        setScheduleContent(
            state = scheduleState(
                filters = listOf(
                    ScheduleFilterUi("all", "Wszystkie", true),
                    ScheduleFilterUi("course", "Informatyka", false)
                )
            )
        )

        composeTestRule.onNodeWithText("Plan zajęć").assertDoesNotExist()
        composeTestRule.onNodeWithText("1 wrz - 7 wrz").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tydzień A").assertIsDisplayed()
        composeTestRule.onNodeWithText("Korekta ręczna").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(
                "Zmień oznaczenie tygodnia, obecnie: Tydzień A, źródło: Korekta ręczna"
            )
            .performClick()
        composeTestRule.onNodeWithText("Zmień tydzień A/B").assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("Więcej opcji").assertCountEquals(0)
    }

    @Test
    fun weekRowWithoutCorrectableCalendarIsInformationOnlyAt320Dp() {
        setScheduleContent(
            state = scheduleState().copy(
                weekTypeLabel = "Różne tygodnie",
                weekSourceLabel = "Różne kalendarze",
                canCorrectWeek = false
            )
        )

        composeTestRule.onNodeWithText("Wybierz kierunek w polu „Kierunek”, aby zmienić tydzień.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Zmień").assertDoesNotExist()
        composeTestRule.onAllNodes(hasClickAction() and hasText("Różne tygodnie", substring = true))
            .assertCountEquals(0)
    }

    @Test
    fun filterShowsSelectedCourseInFieldAt320Dp() {
        val selectedCourse = "Bardzo długa nazwa kierunku Informatyka i analiza danych"
        setScheduleContent(
            state = scheduleState(
                filters = listOf(
                    ScheduleFilterUi("all", "Wszystkie", false),
                    ScheduleFilterUi(
                        "course",
                        selectedCourse,
                        true
                    )
                )
            )
        )

        composeTestRule.onNodeWithText("Kierunek").assertIsDisplayed()
        composeTestRule.onNodeWithText(selectedCourse).assertIsDisplayed()
    }

    @Test
    fun cardExposesNeutralConflictRangeAndOrderedDetailsInDarkThemeAt390Dp() {
        val item = ClassItemUi(
            id = "class-1",
            name = "Bardzo długa nazwa zajęć z analizą danych",
            type = "Laboratorium",
            courseName = "Informatyka",
            startTime = "09:00",
            endTime = "10:30",
            room = "L204",
            building = "Budynek A",
            teacherName = "Bardzo długi tytuł i nazwisko prowadzącego",
            classNote = "Przynieś projekt",
            conflictLabel = "Kolizja 09:30-10:00"
        )
        setScheduleContent(
            width = 390.dp,
            darkTheme = true,
            state = scheduleState(items = listOf(item))
        )

        composeTestRule.onNodeWithText("Laboratorium").assertIsDisplayed()
        composeTestRule.onNodeWithText("L204, Budynek A, Bardzo długi tytuł i nazwisko prowadzącego")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Kolizja 09:30-10:00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Przynieś projekt").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(
                "09:00-10:30, Bardzo długa nazwa zajęć z analizą danych, Informatyka, Laboratorium, L204, Budynek A, Bardzo długi tytuł i nazwisko prowadzącego, Kolizja 09:30-10:00, Notatka do zajęć: Przynieś projekt"
            )
            .assertIsDisplayed()
    }

    @Test
    fun cardShowsBothNoteLabelsAt320Dp() {
        val item = ClassItemUi(
            id = "class-notes",
            name = "Analiza danych",
            type = "Wykład",
            courseName = "Informatyka",
            startTime = "09:00",
            endTime = "10:30",
            classNote = "Przynieś projekt",
            occurrenceNote = "Kolokwium"
        )
        setScheduleContent(state = scheduleState(items = listOf(item)))

        composeTestRule.onNodeWithText("Informatyka").assertIsDisplayed()
        // Notes show as icon lines; the kind stays in the card description for TalkBack.
        composeTestRule.onNodeWithText("Notatka do zajęć").assertDoesNotExist()
        composeTestRule.onNodeWithText("Notatka do terminu").assertDoesNotExist()
        composeTestRule.onNode(hasContentDescription("Notatka do zajęć: Przynieś projekt", substring = true)).assertExists()
        composeTestRule.onNode(hasContentDescription("Notatka do terminu: Kolokwium", substring = true)).assertExists()
        composeTestRule.onNodeWithText("Przynieś projekt").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kolokwium").assertIsDisplayed()
    }

    @Test
    fun longCourseNameKeepsClassTypeVisibleAt320Dp() {
        val item = ClassItemUi(
            id = "class-long-course",
            name = "Analiza danych",
            type = "Laboratorium",
            courseName = "Bardzo długa nazwa kierunku Informatyka i analiza danych",
            startTime = "09:00",
            endTime = "10:30",
            room = "L204"
        )

        setScheduleContent(state = scheduleState(items = listOf(item)))

        composeTestRule.onNodeWithText(
            "Bardzo długa nazwa kierunku Informatyka i analiza danych"
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Laboratorium").assertIsDisplayed()
    }

    @Test
    fun filterWithRepeatedCourseNamesSelectsByIdAt320Dp() {
        var selected = ""
        setScheduleContent(
            state = scheduleState(
                filters = listOf(
                    ScheduleFilterUi("all", "Wszystkie", true),
                    ScheduleFilterUi("a", "Informatyka"),
                    ScheduleFilterUi("b", "Informatyka")
                )
            ),
            onFilterSelected = { selected = it }
        )

        composeTestRule.onNodeWithText("Kierunek").performClick()
        composeTestRule.onNodeWithText("Informatyka (1)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Informatyka (2)").performClick()

        assertEquals("b", selected)
    }

    @Test
    fun calendarShowsCancelledToggleAndOneOffActionWithoutExpansionAt320Dp() {
        var addedOneOff = 0
        var showCancelled: Boolean? = null
        setScheduleContent(
            state = scheduleState().copy(
                view = ScheduleView.Calendar,
                calendarMonthLabel = "wrzesień 2026",
                calendarDays = listOf(
                    CalendarDayUi(
                        id = "2026-09-01",
                        dayLabel = "1",
                        accessibilityLabel = "Wtorek, 1 września 2026",
                        isSelected = true
                    )
                ),
                calendarLegend = listOf(
                    CalendarLegendUi(label = "Informatyka"),
                    CalendarLegendUi(label = "Zmieniony termin", isChange = true)
                ),
                calendarSelectedDayLabel = "Wtorek, 1 września 2026",
                calendarSelectedDayCountLabel = "0 zajęć"
            ),
            onAddOneOff = { addedOneOff += 1 },
            onShowCancelledChanged = { showCancelled = it }
        )

        composeTestRule.onNodeWithText("Pokaż odwołane").assertIsDisplayed().performClick()
        val addAction = composeTestRule.onNodeWithText("Dodaj termin jednorazowy").assertIsDisplayed()
        val emptyState = composeTestRule.onNodeWithText("Brak zajęć w tym dniu.").assertIsDisplayed()
        assertTrue(addAction.fetchSemanticsNode().boundsInRoot.top < emptyState.fetchSemanticsNode().boundsInRoot.top)
        addAction.performClick()
        composeTestRule.onNodeWithContentDescription("opcje kalendarza").assertDoesNotExist()
        assertEquals(1, addedOneOff)
        assertEquals(true, showCancelled)
    }

    @Test
    fun calendarOneOffActionWrapsWithoutClippingAtFontScaleTwoAt320Dp() {
        setScheduleContent(
            state = scheduleState().copy(
                view = ScheduleView.Calendar,
                calendarMonthLabel = "wrzesień 2026",
                calendarDays = listOf(
                    CalendarDayUi(
                        id = "2026-09-01",
                        dayLabel = "1",
                        accessibilityLabel = "Wtorek, 1 września 2026",
                        isSelected = true
                    )
                ),
                calendarLegend = listOf(CalendarLegendUi(label = "Informatyka")),
                calendarSelectedDayLabel = "Wtorek, 1 września 2026",
                calendarSelectedDayCountLabel = "0 zajęć"
            ),
            fontScale = 2f,
            height = 1400.dp
        )

        val node = composeTestRule.onNodeWithText(
            "Dodaj termin jednorazowy",
            useUnmergedTree = true
        ).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        val layout = results.single()
        assertTrue(layout.lineCount > 1)
        assertFalse((0 until layout.lineCount).any(layout::isLineEllipsized))
    }

    private fun setScheduleContent(
        width: androidx.compose.ui.unit.Dp = 320.dp,
        darkTheme: Boolean = false,
        state: ScheduleUiState,
        onFilterSelected: (String) -> Unit = {},
        onAddOneOff: () -> Unit = {},
        onShowCancelledChanged: (Boolean) -> Unit = {},
        fontScale: Float? = null,
        height: androidx.compose.ui.unit.Dp = 720.dp
    ) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            val testDensity = fontScale?.let { Density(density.density, it) } ?: density
            CompositionLocalProvider(LocalDensity provides testDensity) {
                MAKTheme(darkTheme = darkTheme, dynamicColor = false) {
                    Box(modifier = Modifier.width(width).height(height)) {
                        ScheduleScreen(
                            state = state,
                            onViewChanged = {},
                            onPreviousWeek = {},
                            onNextWeek = {},
                            onDaySelected = {},
                            onFilterSelected = onFilterSelected,
                            onPreviousMonth = {},
                            onNextMonth = {},
                            onCalendarDaySelected = {},
                            onShowCancelledChanged = onShowCancelledChanged,
                            onAddOneOff = onAddOneOff,
                            onOpenClass = {},
                            onSaveWeekCorrection = { _, _ -> },
                            onClearWeekCorrection = {},
                            onStartSetup = {},
                            onRetry = {}
                        )
                    }
                }
            }
        }
    }

    private fun scheduleState(
        filters: List<ScheduleFilterUi> = listOf(ScheduleFilterUi("all", "Wszystkie", true)),
        items: List<ClassItemUi> = emptyList()
    ) = ScheduleUiState(
        weekRangeLabel = "1 wrz - 7 wrz",
        weekSubtitle = "Bieżący tydzień",
        weekTypeLabel = "Tydzień A",
        weekSourceLabel = "Korekta ręczna",
        weekType = WeekTypeUi.A,
        days = (1..7).map { day ->
            ScheduleDayUi(
                id = "2026-09-${day.toString().padStart(2, '0')}",
                shortLabel = "Pn",
                dateLabel = day.toString(),
                accessibilityLabel = "Poniedziałek, $day września 2026",
                isSelected = day == 1
            )
        },
        filters = filters,
        selectedDayLabel = "Poniedziałek",
        selectedDayCountLabel = "${items.size} zajęć",
        items = items
    )
}
