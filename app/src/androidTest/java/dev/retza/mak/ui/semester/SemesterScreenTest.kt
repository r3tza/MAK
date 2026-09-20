package dev.retza.mak.ui.semester

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
                        onOverrideWeekStartDateChanged = {},
                        onOverrideWeekTypeChanged = {},
                        onOverrideScopeChanged = {},
                        onNewOverride = {},
                        onEditOverride = {},
                        onSaveOverride = {},
                        onDeleteOverride = {},
                        onCancelOverrideEdit = {},
                        onCourseNameChanged = {},
                        onCourseColorChanged = {},
                        onAddCourse = {},
                        onDeleteCourse = {},
                        onOpenCourses = { opened = "courses" },
                        onOpenOverrides = { opened = "overrides" },
                        onBack = {},
                        onRetry = {}
                    )
                }
            }
        }

        composeTestRule.onAllNodesWithText("Pokaż kierunki").assertCountEquals(0)
        composeTestRule.onNodeWithText("Kierunki").assertIsDisplayed().performClick()
        assertEquals("courses", opened)
        composeTestRule.onNodeWithText("Korekty tygodni").assertIsDisplayed().performClick()
        assertEquals("overrides", opened)
    }
}
