package dev.retza.mak.ui.feedback

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.ui.theme.MAKTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MakSnackbarHostTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsEachVariantOnceAndQueuesThemInOrder() {
        val hostState = SnackbarHostState()
        val channel = Channel<UiFeedback>(Channel.BUFFERED)

        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                MakSnackbarHost(feedback = channel.receiveAsFlow(), hostState = hostState)
            }
        }

        listOf(
            UiFeedback("Zapisano", UiFeedbackKind.Success),
            UiFeedback("Nie udało się", UiFeedbackKind.Error),
            UiFeedback("Uwaga", UiFeedbackKind.Warning),
            UiFeedback("Gotowe", UiFeedbackKind.Info)
        ).forEach(channel::trySend)

        assertVariant(hostState, "Zapisano", "Sukces: Zapisano")
        assertVariant(hostState, "Nie udało się", "Błąd: Nie udało się")
        assertVariant(hostState, "Uwaga", "Ostrzeżenie: Uwaga")
        assertVariant(hostState, "Gotowe", "Informacja: Gotowe")
    }

    private fun assertVariant(hostState: SnackbarHostState, message: String, description: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            hostState.currentSnackbarData?.visuals?.message == message
        }
        composeTestRule.onNodeWithText(message).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(description).assertIsDisplayed()
        hostState.currentSnackbarData?.dismiss()
    }
}
