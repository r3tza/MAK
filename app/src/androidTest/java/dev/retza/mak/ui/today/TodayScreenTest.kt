package dev.retza.mak.ui.today

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.ui.components.ClassItemUi
import dev.retza.mak.ui.theme.MAKTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodayScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun todayScreenShowsFixedItemAndInvokesActionsAt320Dp() {
        var openedClassId: String? = null
        val item = ClassItemUi(
            id = "class-1",
            name = "Programowanie",
            type = "Wykład",
            courseName = "Informatyka",
            startTime = "08:00",
            endTime = "09:30",
            room = "A-101",
            teacherName = "Jan Kowalski",
            weekLabel = "Tydzień A"
        )

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .height(700.dp)
                ) {
                    TodayScreen(
                        state = TodayUiState(
                            dateLabel = "Poniedziałek, 12 października",
                            semesterLabel = "Semestr zimowy",
                            weekLabel = "Tydzień A",
                            hasActiveSemester = true,
                            classCount = 1,
                            items = listOf(item)
                        ),
                        onOpenPlan = {},
                        onOpenClass = { openedClassId = it },
                        onStartSetup = {},
                        onRetry = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Dzisiaj").assertIsDisplayed()
        composeTestRule.onNodeWithText("Twój plan na dziś").assertIsDisplayed()
        composeTestRule.onNodeWithText("Zajęcia").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kolizje").assertIsDisplayed()
        composeTestRule.onNodeWithText("Okienka").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("✦").assertCountEquals(0)
        composeTestRule.onNodeWithText("Programowanie").assertIsDisplayed()
        composeTestRule
            .onNode(hasText("Programowanie") and hasClickAction())
            .performClick()

        assertEquals("class-1", openedClassId)
    }

    @Test
    fun summaryShowsZerosAndEmptyTextAt320Dp() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .height(400.dp)
                ) {
                    TodayScreen(
                        state = TodayUiState(
                            dateLabel = "Poniedziałek, 12 października",
                            semesterLabel = "Semestr zimowy",
                            weekLabel = "Tydzień A",
                            hasActiveSemester = true
                        ),
                        onOpenPlan = {},
                        onOpenClass = {},
                        onStartSetup = {},
                        onRetry = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Dziś bez zajęć").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("0").assertCountEquals(3)
    }

    @Test
    fun withoutActiveSemesterHidesSummaryCard() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .height(500.dp)
                ) {
                    TodayScreen(
                        state = TodayUiState(
                            dateLabel = "Brak aktywnego semestru",
                            semesterLabel = "",
                            weekLabel = "",
                            emptyMessage = "Nie masz jeszcze aktywnego semestru."
                        ),
                        onOpenPlan = {},
                        onOpenClass = {},
                        onStartSetup = {},
                        onRetry = {},
                        requiresSetup = true
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Dziś bez zajęć").assertDoesNotExist()
        composeTestRule.onNodeWithText("Twój plan na dziś").assertDoesNotExist()
        composeTestRule.onNodeWithText("Skonfiguruj plan").assertIsDisplayed()
    }
}
