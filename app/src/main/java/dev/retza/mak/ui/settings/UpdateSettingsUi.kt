package dev.retza.mak.ui.settings

import dev.retza.mak.update.UpdateCheckStatus
import dev.retza.mak.update.UpdateDownloadStatus
import dev.retza.mak.update.UpdateUiState

data class UpdateSettingsUi(
    val installedVersion: String = "",
    val checkSummary: String = "",
    val canCheck: Boolean = true,
    val pendingVersion: String? = null,
    val automaticChecks: Boolean = false
)

fun UpdateUiState.toSettingsUi(): UpdateSettingsUi = UpdateSettingsUi(
    installedVersion = installedVersionName,
    checkSummary = when (checkStatus) {
        UpdateCheckStatus.Idle -> ""
        UpdateCheckStatus.Checking -> "Sprawdzanie..."
        UpdateCheckStatus.UpToDate -> "Masz najnowszą wersję"
        UpdateCheckStatus.Available -> "Dostępna wersja ${availableUpdate?.versionName.orEmpty()}"
        UpdateCheckStatus.RequiresNewerAndroid -> "Wymaga nowszego Androida"
        UpdateCheckStatus.InvalidFile, UpdateCheckStatus.NetworkError -> "Nie udało się sprawdzić"
    },
    // A new check clears the available release, so it is blocked while a download is in use.
    canCheck = checkStatus != UpdateCheckStatus.Checking && downloadStatus !in busyDownloadStatuses,
    pendingVersion = availableUpdate?.versionName,
    automaticChecks = automaticChecks
)

private val busyDownloadStatuses = setOf(
    UpdateDownloadStatus.Downloading,
    UpdateDownloadStatus.Ready,
    UpdateDownloadStatus.NeedsPermission,
    UpdateDownloadStatus.Installing
)
