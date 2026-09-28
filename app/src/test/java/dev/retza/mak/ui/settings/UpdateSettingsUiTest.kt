package dev.retza.mak.ui.settings

import dev.retza.mak.update.UpdateCheckStatus
import dev.retza.mak.update.UpdateDownloadStatus
import dev.retza.mak.update.UpdateInfo
import dev.retza.mak.update.UpdateUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateSettingsUiTest {
    @Test
    fun idleStateInvitesCheckWithoutPendingRow() {
        val ui = state().toSettingsUi()

        assertEquals("", ui.checkSummary)
        assertTrue(ui.canCheck)
        assertNull(ui.pendingVersion)
    }

    @Test
    fun checkingDisablesRow() {
        val ui = state(checkStatus = UpdateCheckStatus.Checking).toSettingsUi()

        assertEquals("Sprawdzanie...", ui.checkSummary)
        assertFalse(ui.canCheck)
    }

    @Test
    fun availableReleaseShowsPendingRow() {
        val ui = state(checkStatus = UpdateCheckStatus.Available, availableUpdate = release).toSettingsUi()

        assertEquals("Dostępna wersja 0.2.0", ui.checkSummary)
        assertEquals("0.2.0", ui.pendingVersion)
    }

    @Test
    fun downloadInUseBlocksNewCheck() {
        val ui = state(
            checkStatus = UpdateCheckStatus.Available,
            availableUpdate = release,
            downloadStatus = UpdateDownloadStatus.Downloading
        ).toSettingsUi()

        assertFalse(ui.canCheck)
    }

    @Test
    fun failedCheckHasShortSummary() {
        assertEquals("Nie udało się sprawdzić", state(checkStatus = UpdateCheckStatus.NetworkError).toSettingsUi().checkSummary)
        assertEquals("Nie udało się sprawdzić", state(checkStatus = UpdateCheckStatus.InvalidFile).toSettingsUi().checkSummary)
        assertEquals("Masz najnowszą wersję", state(checkStatus = UpdateCheckStatus.UpToDate).toSettingsUi().checkSummary)
    }

    private val release = UpdateInfo(
        200,
        "0.2.0",
        "https://github.com/r3tza/MAK/releases/download/v0.2.0/MAK-0.2.0.apk",
        "a".repeat(64),
        31,
        ""
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
}
