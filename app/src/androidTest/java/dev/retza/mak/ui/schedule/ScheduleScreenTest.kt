package dev.retza.mak.ui.schedule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.ui.components.ClassItemUi
import dev.retza.mak.ui.semester.WeekTypeUi
import dev.retza.mak.ui.theme.MAKTheme
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

        composeTestRule.onNodeWithText("Plan zajęć").assertIsDisplayed()
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
    fun collapsedFilterNamesSelectedCourseAndExpandsAt320Dp() {
        setScheduleContent(
            state = scheduleState(
                filters = listOf(
                    ScheduleFilterUi("all", "Wszystkie", false),
                    ScheduleFilterUi(
                        "course",
                        "Bardzo długa nazwa kierunku Informatyka i analiza danych",
                        true
                    )
                )
            )
        )

        composeTestRule
            .onNodeWithText("Filtry: Bardzo długa nazwa kierunku Informatyka i analiza danych")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Filtry: Bardzo długa nazwa kierunku Informatyka i analiza danych")
            .performClick()
        composeTestRule.onNodeWithText("Kierunek").assertIsDisplayed()
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
            note = "Przynieś projekt",
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
                "09:00-10:30, Bardzo długa nazwa zajęć z analizą danych, Informatyka, Laboratorium, L204, Budynek A, Bardzo długi tytuł i nazwisko prowadzącego, Kolizja 09:30-10:00, Przynieś projekt"
            )
            .assertIsDisplayed()
    }

    private fun setScheduleContent(
        width: androidx.compose.ui.unit.Dp = 320.dp,
        darkTheme: Boolean = false,
        state: ScheduleUiState
    ) {
        composeTestRule.setContent {
            MAKTheme(darkTheme = darkTheme, dynamicColor = false) {
                Box(modifier = Modifier.width(width).height(720.dp)) {
                    ScheduleScreen(
                        state = state,
                        onViewChanged = {},
                        onPreviousWeek = {},
                        onNextWeek = {},
                        onDaySelected = {},
                        onFilterSelected = {},
                        onPreviousMonth = {},
                        onNextMonth = {},
                        onCalendarDaySelected = {},
                        onShowCancelledChanged = {},
                        onAddOneOff = {},
                        onOpenClass = {},
                        onSaveWeekCorrection = { _, _ -> },
                        onClearWeekCorrection = {},
                        onRetry = {}
                    )
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
