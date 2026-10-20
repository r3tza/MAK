package dev.retza.mak.ui.freedays

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.ui.theme.MAKTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FreeDaysScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val group = FreeDayGroupUi(
        ids = listOf(10L, 11L),
        title = "Przerwa świąteczna",
        dateLabel = "23 gru 2026 - 1 sty 2027, 10 dni",
        calendarsLabel = "Kalendarze: Informatyka, Zarządzanie"
    )

    private fun setScreen(
        state: FreeDaysUiState,
        onEdit: (List<Long>) -> Unit = {},
        onDelete: (List<Long>) -> Unit = {},
        onConfirmDeletion: () -> Unit = {},
        onToggleHoliday: (LocalDate) -> Unit = {}
    ) {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                FreeDaysScreen(
                    state = state,
                    onAdd = {},
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onConfirmDeletion = onConfirmDeletion,
                    onCancelDeletion = {},
                    onOpenHolidays = {},
                    onToggleHoliday = onToggleHoliday,
                    onAddHolidays = {},
                    onCancelHolidays = {},
                    modifier = Modifier
                )
            }
        }
    }

    @Test
    fun groupCardShowsLabelsAndActions() {
        val edited = mutableListOf<List<Long>>()
        val deleted = mutableListOf<List<Long>>()
        setScreen(
            state = FreeDaysUiState(groups = listOf(group)),
            onEdit = { edited += it },
            onDelete = { deleted += it }
        )

        composeTestRule.onNodeWithText("Przerwa świąteczna").assertIsDisplayed()
        composeTestRule.onNodeWithText("23 gru 2026 - 1 sty 2027, 10 dni").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kalendarze: Informatyka, Zarządzanie").assertIsDisplayed()

        composeTestRule.onNodeWithText("Edytuj").performClick()
        composeTestRule.onNodeWithText("Usuń").performClick()

        assertEquals(listOf(listOf(10L, 11L)), edited)
        assertEquals(listOf(listOf(10L, 11L)), deleted)
    }

    @Test
    fun deletionDialogConfirms() {
        var confirmed = false
        setScreen(
            state = FreeDaysUiState(groups = listOf(group), pendingDeletion = group),
            onConfirmDeletion = { confirmed = true }
        )

        composeTestRule.onNodeWithText("Usunąć dni wolne?").assertIsDisplayed()
        // The card also has a "Usuń" button, so pick the one inside the dialog window.
        composeTestRule.onAllNodesWithText("Usuń")
            .filter(hasAnyAncestor(isDialog()))
            .onFirst()
            .performClick()

        assertTrue(confirmed)
    }

    @Test
    fun holidaysDialogTogglesOnlyEnabledHolidays() {
        val allSaintsDay = LocalDate.of(2026, 11, 1)
        val christmas = LocalDate.of(2026, 12, 25)
        val holidays = HolidaysDialogUi(
            intro = "Wybierz święta, które mają być dniami wolnymi.",
            options = listOf(
                HolidayOptionUi(
                    date = allSaintsDay,
                    name = "Wszystkich Świętych",
                    detail = "Niedziela, 1 lis 2026",
                    checked = false,
                    enabled = true
                ),
                HolidayOptionUi(
                    date = christmas,
                    name = "Boże Narodzenie",
                    detail = "Już w „Przerwa świąteczna”",
                    checked = true,
                    enabled = false
                )
            ),
            calendarsLabel = "Kalendarze: Informatyka"
        )
        val toggled = mutableListOf<LocalDate>()
        setScreen(
            state = FreeDaysUiState(holidays = holidays),
            onToggleHoliday = { toggled += it }
        )

        composeTestRule.onNodeWithText("Wszystkich Świętych").performClick()
        composeTestRule.onNodeWithText("Boże Narodzenie").performClick()
        composeTestRule.onNodeWithText(holidays.confirmLabel).assertIsDisplayed()

        assertEquals(listOf(allSaintsDay), toggled)
    }
}
