package dev.retza.mak.ui.today

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.ui.components.ClassItemUi
import dev.retza.mak.ui.theme.MAKTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import kotlin.math.floor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodayScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun updateBannerOffersViewAndDismissAtLargeFontAt320Dp() {
        var viewed = false
        var dismissed = false
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                MAKTheme(dynamicColor = false) {
                    Box(modifier = Modifier.width(320.dp).height(1400.dp)) {
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
                            onRetry = {},
                            availableUpdateVersion = "0.2.1",
                            onViewUpdate = { viewed = true },
                            onDismissUpdate = { dismissed = true }
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Dostępna aktualizacja").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wersja 0.2.1 jest gotowa do pobrania.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Zobacz").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("Nie teraz").assertIsDisplayed().performClick()
        assertTrue(viewed)
        assertTrue(dismissed)
    }

    @Test
    fun startTimeIsNotCutAtFontScaleTwoAt320Dp() {
        val item = ClassItemUi(
            id = "class-1",
            name = "Programowanie obiektowe z bardzo długą nazwą przedmiotu",
            type = "Wykład",
            courseName = "Informatyka",
            startTime = "12:00",
            endTime = "13:30"
        )
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                MAKTheme(dynamicColor = false) {
                    Box(modifier = Modifier.width(320.dp).height(1400.dp)) {
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
                            onOpenClass = {},
                            onStartSetup = {},
                            onRetry = {}
                        )
                    }
                }
            }
        }

        fun layoutOf(text: String): TextLayoutResult {
            val node = composeTestRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
            val results = mutableListOf<TextLayoutResult>()
            node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
            return results.single()
        }

        // The start time is shown in full: one line, no ellipsis, and wide enough for all digits.
        val time = layoutOf("12:00")
        assertFalse(time.multiParagraph.didExceedMaxLines)
        assertFalse(time.isLineEllipsized(0))
        assertTrue(time.size.width >= floor(time.multiParagraph.maxIntrinsicWidth).toInt())
        // Labels stay on one line instead of breaking inside a word; at this scale an ellipsis
        // is allowed, the full label stays in the content description.
        for (text in listOf("Okienka", "Od najwcześniejszego")) {
            assertEquals("$text lines", 1, layoutOf(text).lineCount)
        }
        composeTestRule.onNodeWithContentDescription("Okienka: 0").assertExists()
    }

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

        composeTestRule.onNodeWithText("DZISIAJ").assertIsDisplayed()
        composeTestRule.onNode(hasText("Twój plan na dziś") and isHeading()).assertIsDisplayed()
        composeTestRule.onNodeWithText("Dziś bez zajęć").assertDoesNotExist()
        // "Zajęcia" is both a summary label and the list title; check the summary column.
        composeTestRule.onNodeWithContentDescription("Zajęcia: 1").assertIsDisplayed()
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

        composeTestRule.onNode(hasText("Dziś bez zajęć") and isHeading()).assertIsDisplayed()
        composeTestRule.onNodeWithText("Twój plan na dziś").assertDoesNotExist()
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

    @Test
    fun classCardShowsBothNoteLabelsAt320Dp() {
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
                        onOpenClass = {},
                        onStartSetup = {},
                        onRetry = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Informatyka").assertIsDisplayed()
        // Notes show as icon lines; the kind stays in the card description for TalkBack.
        composeTestRule.onNodeWithText("Notatka do zajęć").assertDoesNotExist()
        composeTestRule.onNodeWithText("Notatka do terminu").assertDoesNotExist()
        composeTestRule.onNode(hasContentDescription("Notatka do zajęć: Przynieś projekt", substring = true)).assertExists()
        composeTestRule.onNode(hasContentDescription("Notatka do terminu: Kolokwium", substring = true)).assertExists()
        composeTestRule.onNodeWithText("Przynieś projekt").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kolokwium").assertIsDisplayed()
    }
}
