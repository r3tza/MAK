package dev.retza.mak.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.ui.theme.MAKTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The dialog draws its own actions, so a dismissal can only be a text button (I-72). */
@RunWith(AndroidJUnit4::class)
class MakDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setDialog(
        dismissAction: MakDialogAction,
        confirmAction: MakDialogAction? = null,
        choices: List<MakDialogAction> = emptyList()
    ) {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                MakDialog(
                    title = "Tytuł",
                    onDismiss = {},
                    dismissAction = dismissAction,
                    confirmAction = confirmAction,
                    choices = choices
                ) {
                    Box(Modifier.fillMaxWidth().height(1.dp).testTag("content"))
                }
            }
        }
    }

    private fun bounds(text: String): DpRect = composeTestRule.onNodeWithText(text).getUnclippedBoundsInRoot()

    private fun content(): DpRect = composeTestRule.onNodeWithTag("content").getUnclippedBoundsInRoot()

    @Test
    fun dismissOnlyDialogShowsTextActionAtStart() {
        var closed = 0
        setDialog(MakDialogAction("Zamknij", { closed += 1 }))

        val action = bounds("Zamknij")
        assertTrue(action.left - content().left <= 8.dp)
        assertTrue(action.width < content().width)
        composeTestRule.onNodeWithText("Zamknij").performClick()
        assertEquals(1, closed)
    }

    @Test
    fun formDialogPutsCancelBeforeSaveAtEnd() {
        setDialog(MakDialogAction("Anuluj", {}), confirmAction = MakDialogAction("Zapisz", {}))

        val cancel = bounds("Anuluj")
        val save = bounds("Zapisz")
        assertTrue(cancel.right <= save.left)
        assertTrue(cancel.bottom > save.top && cancel.top < save.bottom)
        assertTrue(content().right - save.right <= 16.dp)
        assertTrue(save.width < content().width)
    }

    @Test
    fun choiceDialogStacksChoicesAndPutsDismissLast() {
        setDialog(
            MakDialogAction("Później", {}),
            confirmAction = MakDialogAction("Zachowaj plan z telefonu", {}),
            choices = listOf(MakDialogAction("Zachowaj plan z Dysku", {}))
        )

        val confirm = bounds("Zachowaj plan z telefonu")
        val choice = bounds("Zachowaj plan z Dysku")
        val later = bounds("Później")
        assertTrue(confirm.bottom <= choice.top && choice.bottom <= later.top)
        assertTrue(later.width < choice.width)
        assertTrue(later.left - content().left <= 8.dp)
    }

    @Test
    fun disabledActionsDoNotRespond() {
        var calls = 0
        setDialog(
            MakDialogAction("Anuluj", { calls += 1 }, enabled = false),
            confirmAction = MakDialogAction("Zapisz", { calls += 1 }, enabled = false)
        )

        composeTestRule.onNodeWithText("Anuluj").performClick()
        composeTestRule.onNodeWithText("Zapisz").performClick()
        assertEquals(0, calls)
    }
}
