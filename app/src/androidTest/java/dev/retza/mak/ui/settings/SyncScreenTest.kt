package dev.retza.mak.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.sync.SyncChoice
import dev.retza.mak.ui.theme.MAKTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Both dialogs end with stacked actions; at font scale 2.0 each must stay reachable. A dialog takes the
 * width of the window, not of the 320 dp box, so narrow screens are checked on a device.
 */
@RunWith(AndroidJUnit4::class)
class SyncScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val connected = SyncUiState(
        accountEmail = "ala@example.com",
        lastSyncLabel = "Ostatnia synchronizacja: 1 października 2026, 10:00",
        choice = SyncChoiceUi(
            phone = listOf("Kierunki: 2", "Semestry: 2", "Zajęcia: 34"),
            drive = listOf("Kierunki: 1", "Semestry: 1", "Zajęcia: 12")
        )
    )

    @Test
    fun everyVersionChoiceIsReachableWithLargeFont() {
        val chosen = mutableListOf<SyncChoice>()
        var dismissed = false
        show(connected.copy(showChoiceDialog = true), onChoose = { chosen += it }, onDismissChoice = { dismissed = true })

        tap("Zachowaj plan z telefonu")
        tap("Zachowaj plan z Dysku")
        tap("Później")

        assertEquals(listOf(SyncChoice.KEEP_PHONE, SyncChoice.KEEP_DRIVE), chosen)
        assertEquals(true, dismissed)
    }

    @Test
    fun everyDisconnectActionIsReachableWithLargeFont() {
        val removed = mutableListOf<Boolean>()
        var dismissed = false
        show(connected.copy(showDisconnectDialog = true), onDisconnect = { removed += it }, onDismissDisconnect = { dismissed = true })

        tap("Wyłącz i zachowaj kopię na Dysku")
        tap("Wyłącz i usuń kopię z Dysku")
        tap("Anuluj")

        assertEquals(listOf(false, true), removed)
        assertEquals(true, dismissed)
    }

    private fun tap(text: String) {
        composeTestRule.onNodeWithText(text).performScrollTo().assertIsDisplayed().performClick()
    }

    private fun show(
        state: SyncUiState,
        onChoose: (SyncChoice) -> Unit = {},
        onDismissChoice: () -> Unit = {},
        onDisconnect: (Boolean) -> Unit = {},
        onDismissDisconnect: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MAKTheme(dynamicColor = false) {
                    Box(Modifier.width(320.dp).height(640.dp)) {
                        SyncScreen(
                            state = state,
                            onConnect = {},
                            onReconnect = {},
                            onSyncNow = {},
                            onOpenChoice = {},
                            onChoose = onChoose,
                            onDismissChoice = onDismissChoice,
                            onRequestDisconnect = {},
                            onDisconnect = onDisconnect,
                            onDismissDisconnect = onDismissDisconnect,
                            onExportArchived = {},
                            onDismissError = {}
                        )
                    }
                }
            }
        }
    }
}
