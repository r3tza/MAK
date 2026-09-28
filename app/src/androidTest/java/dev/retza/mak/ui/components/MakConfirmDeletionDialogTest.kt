package dev.retza.mak.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.ui.theme.MAKTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MakConfirmDeletionDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setDialog(
        isDeleting: Boolean = false,
        confirmLabel: String? = null,
        onConfirm: () -> Unit = {},
        onCancel: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                if (confirmLabel == null) {
                    MakConfirmDeletionDialog(
                        title = "Usunąć kalendarz?",
                        text = "Tej operacji nie można cofnąć.",
                        isDeleting = isDeleting,
                        onConfirm = onConfirm,
                        onCancel = onCancel
                    )
                } else {
                    MakConfirmDeletionDialog(
                        title = "Usunąć zajęcia?",
                        text = "Tej operacji nie można cofnąć.",
                        confirmLabel = confirmLabel,
                        isDeleting = isDeleting,
                        onConfirm = onConfirm,
                        onCancel = onCancel
                    )
                }
            }
        }
    }

    @Test
    fun showsTitleTextAndDefaultConfirmLabelAndReportsChoices() {
        var confirmed = 0
        var cancelled = 0
        setDialog(onConfirm = { confirmed += 1 }, onCancel = { cancelled += 1 })

        composeTestRule.onNodeWithText("Usunąć kalendarz?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tej operacji nie można cofnąć.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Anuluj").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("Usuń").assertIsDisplayed().performClick()

        assertEquals(1, cancelled)
        assertEquals(1, confirmed)
    }

    @Test
    fun customConfirmLabelReplacesDefault() {
        setDialog(confirmLabel = "Usuń zajęcia")

        composeTestRule.onNodeWithText("Usuń zajęcia").assertIsDisplayed()
        composeTestRule.onNodeWithText("Usuń").assertDoesNotExist()
    }

    @Test
    fun bothActionsAreBlockedWhileDeleting() {
        setDialog(isDeleting = true)

        composeTestRule.onNodeWithText("Usuń").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Anuluj").assertIsNotEnabled()
    }

    @Test
    fun bothActionsAreEnabledWhenNotDeleting() {
        setDialog()

        composeTestRule.onNodeWithText("Usuń").assertIsEnabled()
        composeTestRule.onNodeWithText("Anuluj").assertIsEnabled()
    }
}
