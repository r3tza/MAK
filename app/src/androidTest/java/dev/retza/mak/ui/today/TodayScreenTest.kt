package dev.retza.mak.ui.today

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
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
import dev.retza.mak.ui.settings.SyncAttentionUi
import dev.retza.mak.ui.settings.SyncIssueFix
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

    // The summary label also reads "Zajęcia" but carries a "Zajęcia: n" description.
    private val listTitle = hasText("Zajęcia") and SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription)

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
    fun newerDrivePlanBannerOpensTheUpdateScreenAt320Dp() {
        var viewedUpdate = false
        var openedSync = false
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(1400.dp)) {
                    TodayScreen(
                        state = TodayUiState(
                            dateLabel = "Poniedziałek, 12 października",
                            semesterLabel = "Semestr zimowy",
                            weekLabel = "Tydzień A",
                            hasActiveSemester = true
                        ),
                        onOpenClass = {},
                        onStartSetup = {},
                        onRetry = {},
                        onViewUpdate = { viewedUpdate = true },
                        onOpenSync = { openedSync = true },
                        syncAttention = SyncAttentionUi(
                            text = "Plan na Dysku pochodzi z nowszej wersji MAK. Zaktualizuj aplikację.",
                            action = "Zaktualizuj",
                            fix = SyncIssueFix.UPDATE
                        )
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Plan na Dysku pochodzi z nowszej wersji MAK. Zaktualizuj aplikację.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Zaktualizuj").assertIsDisplayed().performClick()
        assertTrue(viewedUpdate)
        assertFalse(openedSync)
    }

    @Test
    fun hiddenProgramsHintAndAllHiddenNoteAt320Dp() {
        var openedPrograms = false
        var allHidden by mutableStateOf(false)
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.width(320.dp).height(1400.dp)) {
                    TodayScreen(
                        state = TodayUiState(
                            dateLabel = "Czwartek, 8 października",
                            semesterLabel = "Semestr zimowy",
                            weekLabel = "Tydzień B",
                            hasActiveSemester = true,
                            hiddenProgramNames = listOf("Zarządzanie i inżynieria produkcji", "Ekonomia"),
                            allProgramsHidden = allHidden
                        ),
                        onOpenClass = {},
                        onStartSetup = {},
                        onRetry = {},
                        onOpenPrograms = { openedPrograms = true }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Ukryte kierunki: Zarządzanie i inżynieria produkcji, Ekonomia").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kierunki").assertDoesNotExist()

        allHidden = true
        composeTestRule.onNodeWithText("Wszystkie kierunki są ukryte na tym telefonie.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ukryte kierunki", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Kierunki").assertIsDisplayed().performClick()
        assertTrue(openedPrograms)
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
        composeTestRule.onNodeWithText("Okienka").assertIsDisplayed()
        composeTestRule.onNodeWithText("Od najwcześniejszego").assertDoesNotExist()
        assertEquals("Okienka lines", 1, layoutOf("Okienka").lineCount)
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
                        onOpenClass = { openedClassId = it },
                        onStartSetup = {},
                        onRetry = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("DZISIAJ").assertDoesNotExist()
        composeTestRule.onNodeWithText("Od najwcześniejszego").assertDoesNotExist()
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
    fun wideTodayShowsSummaryBesideClasses() {
        val item = ClassItemUi(
            id = "wide",
            name = "Analiza danych",
            type = "Wykład",
            courseName = "Informatyka",
            startTime = "09:00",
            endTime = "10:30"
        )
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.requiredWidth(1100.dp).height(700.dp).testTag("window")) {
                    TodayScreen(
                        state = TodayUiState(
                            dateLabel = "Poniedziałek, 12 października",
                            semesterLabel = "Semestr zimowy",
                            weekLabel = "Tydzień A",
                            hasActiveSemester = true,
                            classCount = 1,
                            items = listOf(item)
                        ),
                        onOpenClass = {},
                        onStartSetup = {},
                        onRetry = {},
                        twoColumns = true
                    )
                }
            }
        }

        val summary = composeTestRule.onNode(hasText("Twój plan na dziś") and isHeading())
            .fetchSemanticsNode().boundsInRoot
        val card = composeTestRule.onNode(hasText("Analiza danych", substring = true) and hasClickAction())
            .fetchSemanticsNode().boundsInRoot
        val classesTitle = composeTestRule.onNode(listTitle).fetchSemanticsNode().boundsInRoot
        assertTrue("card starts right of the summary", card.left > summary.right)
        // Both columns start right under the date header.
        assertTrue("classes title is level with the summary", kotlin.math.abs(classesTitle.top - summary.top) < 48f * composeTestRule.density.density)
    }

    @Test
    fun wideTodayWithoutSemesterStaysSingleColumn() {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(modifier = Modifier.requiredWidth(1100.dp).height(700.dp).testTag("window")) {
                    TodayScreen(
                        state = TodayUiState(
                            dateLabel = "Brak aktywnego semestru",
                            semesterLabel = "",
                            weekLabel = "",
                            hasActiveSemester = false
                        ),
                        onOpenClass = {},
                        onStartSetup = {},
                        onRetry = {},
                        requiresSetup = true,
                        twoColumns = true
                    )
                }
            }
        }

        val density = composeTestRule.density.density
        val window = composeTestRule.onNodeWithTag("window").fetchSemanticsNode().boundsInRoot
        val title = composeTestRule.onNode(listTitle).fetchSemanticsNode().boundsInRoot
        // Without a summary the classes use the ordinary 640 dp column, centered in 1100 dp:
        // side margin, 16 dp content padding and the row title's own 2 dp.
        assertEquals((1100f - 640f) / 2f + 16f + 2f, (title.left - window.left) / density, 1f)
        composeTestRule.onNodeWithText("Twój plan na dziś").assertDoesNotExist()
    }
}
