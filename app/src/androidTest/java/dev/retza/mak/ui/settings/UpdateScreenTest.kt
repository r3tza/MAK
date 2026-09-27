package dev.retza.mak.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.update.UpdateCheckStatus
import dev.retza.mak.update.UpdateDownloadStatus
import dev.retza.mak.update.UpdateInfo
import dev.retza.mak.update.UpdateUiState
import dev.retza.mak.ui.theme.MAKTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpdateScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun availableVersionShowsNotesAndDownloadAt320Dp() {
        var downloaded = false
        show(
            state(checkStatus = UpdateCheckStatus.Available, availableUpdate = release),
            onDownload = { downloaded = true }
        )

        composeTestRule.onNodeWithText("Dostępna wersja 0.2.0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nowy wygląd planu").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pobierz aktualizację").performClick()
        assertTrue(downloaded)
    }

    @Test
    fun downloadProgressOffersCancel() {
        var cancelled = false
        show(
            state(
                checkStatus = UpdateCheckStatus.Available,
                availableUpdate = release,
                downloadStatus = UpdateDownloadStatus.Downloading
            ).copy(downloadedBytes = 2048, downloadTotalBytes = 4096),
            onCancel = { cancelled = true }
        )

        composeTestRule.onNodeWithText("Pobrano 2 z 4 KB").assertIsDisplayed()
        composeTestRule.onNodeWithText("Anuluj").performClick()
        assertTrue(cancelled)
    }

    @Test
    fun missingReleaseOffersCheckAt320Dp() {
        var checked = false
        show(state(), onCheck = { checked = true })

        composeTestRule.onNodeWithText("Brak informacji o nowej wersji.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sprawdź teraz").performClick()
        assertTrue(checked)
    }

    private val release = UpdateInfo(
        200,
        "0.2.0",
        "https://github.com/r3tza/MAK/releases/download/v0.2.0/MAK-0.2.0.apk",
        "a".repeat(64),
        31,
        "Nowy wygląd planu"
    )

    private fun state(
        checkStatus: UpdateCheckStatus = UpdateCheckStatus.Idle,
        availableUpdate: UpdateInfo? = null,
        downloadStatus: UpdateDownloadStatus = UpdateDownloadStatus.Idle
    ) = UpdateUiState(
        installedVersionName = "0.1.0",
        installedVersionCode = 100,
        checkStatus = checkStatus,
        availableUpdate = availableUpdate,
        downloadStatus = downloadStatus
    )

    private fun show(
        state: UpdateUiState,
        onCheck: () -> Unit = {},
        onDownload: () -> Unit = {},
        onCancel: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            MAKTheme(dynamicColor = false) {
                Box(Modifier.width(320.dp).height(900.dp)) {
                    UpdateScreen(
                        state = state,
                        onCheckNow = onCheck,
                        onDownload = onDownload,
                        onCancelDownload = onCancel
                    )
                }
            }
        }
    }
}
