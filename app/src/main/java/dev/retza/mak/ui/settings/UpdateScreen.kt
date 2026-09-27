package dev.retza.mak.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.retza.mak.update.UpdateCheckStatus
import dev.retza.mak.update.UpdateDownloadStatus
import dev.retza.mak.update.UpdateInfo
import dev.retza.mak.update.UpdateUiState
import dev.retza.mak.ui.components.MakNoteBanner
import dev.retza.mak.ui.components.MakPrimaryAction
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakSpacing

@Composable
fun UpdateScreen(
    state: UpdateUiState,
    onCheckNow: () -> Unit,
    modifier: Modifier = Modifier,
    onDownload: () -> Unit = {},
    onCancelDownload: () -> Unit = {},
    onInstall: () -> Unit = {},
    onGrantInstallPermission: () -> Unit = {}
) {
    MakScreenContent(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.lg)
    ) {
        val update = state.availableUpdate
        if (update == null) {
            AboutSection("Nowa wersja") {
                NoUpdateMessage(state)
                MakSecondaryAction(
                    text = "Sprawdź teraz",
                    onClick = onCheckNow,
                    enabled = state.checkStatus != UpdateCheckStatus.Checking
                )
            }
        } else {
            AvailableUpdate(
                state = state,
                update = update,
                onDownload = onDownload,
                onCancel = onCancelDownload,
                onInstall = onInstall,
                onGrantPermission = onGrantInstallPermission
            )
        }
    }
}

@Composable
private fun NoUpdateMessage(state: UpdateUiState) {
    when (state.checkStatus) {
        UpdateCheckStatus.Checking -> Row(
            horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text("Sprawdzanie dostępności aktualizacji...")
        }
        UpdateCheckStatus.UpToDate -> Text("Masz najnowszą wersję.")
        UpdateCheckStatus.RequiresNewerAndroid ->
            Text("Nowa wersja wymaga Android API ${state.requiredMinSdk}.")
        UpdateCheckStatus.InvalidFile -> Text("Nie udało się sprawdzić. Informacje o wydaniu są nieprawidłowe.")
        UpdateCheckStatus.NetworkError -> Text("Nie udało się sprawdzić. Sprawdź połączenie.")
        UpdateCheckStatus.Idle, UpdateCheckStatus.Available -> Text("Brak informacji o nowej wersji.")
    }
}

@Composable
private fun AvailableUpdate(
    state: UpdateUiState,
    update: UpdateInfo,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
    onGrantPermission: () -> Unit
) {
    AboutSection("Dostępna wersja ${update.versionName}") {
        Text("Zainstalowana: ${state.installedVersionName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Zmiany", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(update.notes.ifBlank { "Brak informacji" })
    }
    when (state.downloadStatus) {
        UpdateDownloadStatus.Idle -> MakPrimaryAction("Pobierz aktualizację", onDownload)
        UpdateDownloadStatus.Downloading -> {
            val total = state.downloadTotalBytes
            Text(
                if (total == null) {
                    "Pobrano ${state.downloadedBytes / 1024} KB"
                } else {
                    "Pobrano ${state.downloadedBytes / 1024} z ${total / 1024} KB"
                }
            )
            MakSecondaryAction("Anuluj", onCancel)
        }
        UpdateDownloadStatus.Ready -> MakPrimaryAction("Zainstaluj", onInstall)
        UpdateDownloadStatus.NeedsPermission -> {
            MakNoteBanner("Wymagana zgoda", "Zezwól systemowi na instalowanie aktualizacji z aplikacji MAK.")
            MakPrimaryAction("Otwórz ustawienia systemu", onGrantPermission)
        }
        UpdateDownloadStatus.Installing -> Text("Oczekiwanie na potwierdzenie instalacji...")
        UpdateDownloadStatus.NotEnoughSpace -> DownloadError("Brak miejsca na pobranie aktualizacji.", onDownload)
        UpdateDownloadStatus.NetworkError -> DownloadError("Pobieranie zostało przerwane.", onDownload)
        UpdateDownloadStatus.Corrupted -> DownloadError("Pobrany plik jest uszkodzony.", onDownload)
        UpdateDownloadStatus.WrongPackage -> DownloadError("Plik zawiera inną aplikację.", null)
        UpdateDownloadStatus.NotNewer -> DownloadError("Pobrana wersja nie jest nowsza.", null)
        UpdateDownloadStatus.WrongSignature -> DownloadError("Podpis aktualizacji jest inny.", null)
        UpdateDownloadStatus.InstallCancelled -> DownloadError("Instalacja została anulowana.", onDownload)
        UpdateDownloadStatus.InstallFailed -> DownloadError("Nie udało się zainstalować aktualizacji.", onDownload)
    }
}

// Temporary failures can be retried; a file from another package, older version or signing key cannot.
@Composable
private fun DownloadError(message: String, onRetry: (() -> Unit)?) {
    MakNoteBanner("Aktualizacja", message)
    onRetry?.let { MakSecondaryAction("Pobierz ponownie", it) }
}
