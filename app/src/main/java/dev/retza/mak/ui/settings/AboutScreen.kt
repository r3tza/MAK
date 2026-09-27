package dev.retza.mak.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.retza.mak.update.ReleaseHistoryEntry
import dev.retza.mak.update.UpdateCheckStatus
import dev.retza.mak.update.UpdateDownloadStatus
import dev.retza.mak.update.UpdateUiState
import dev.retza.mak.ui.components.MakNoteBanner
import dev.retza.mak.ui.components.MakPrimaryAction
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakSpacing

@Composable
fun AboutScreen(
    state: UpdateUiState,
    onCheckNow: () -> Unit,
    modifier: Modifier = Modifier,
    onDownload: () -> Unit = {},
    onCancelDownload: () -> Unit = {},
    onInstall: () -> Unit = {},
    onGrantInstallPermission: () -> Unit = {},
    onAutomaticChecksChanged: (Boolean) -> Unit = {}
) {
    MakScreenContent(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.lg)
    ) {
        AboutSection("Wersja aplikacji") {
            Text(
                text = state.installedVersionName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            UpdateCheckResult(state)
            MakPrimaryAction(
                text = if (state.checkStatus == UpdateCheckStatus.Checking) "Sprawdzanie..." else "Sprawdź teraz",
                onClick = onCheckNow,
                enabled = state.checkStatus != UpdateCheckStatus.Checking
            )
            UpdateDownloadActions(
                state = state,
                onDownload = onDownload,
                onCancel = onCancelDownload,
                onInstall = onInstall,
                onGrantPermission = onGrantInstallPermission
            )
        }

        AboutSection("Ostatnie zmiany") {
            state.releaseHistory.take(3).forEach { entry ->
                ReleaseHistory(entry)
            }
        }

        AboutSection("Automatyczne sprawdzanie") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sprawdzaj przy uruchomieniu")
                Switch(checked = state.automaticChecks, onCheckedChange = onAutomaticChecksChanged)
            }
            Text(
                if (state.automaticChecks) {
                    "Aplikacja sprawdza GitHub najwyżej raz na 24 godziny. Nie wysyła danych planu. GitHub widzi adres IP."
                } else {
                    "Nowe wersje nie pojawią się same. Nadal możesz sprawdzić je ręcznie."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UpdateDownloadActions(
    state: UpdateUiState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
    onGrantPermission: () -> Unit
) {
    if (state.checkStatus == UpdateCheckStatus.Available && state.downloadStatus == UpdateDownloadStatus.Idle) {
        MakPrimaryAction("Pobierz aktualizację", onDownload)
    }
    when (state.downloadStatus) {
        UpdateDownloadStatus.Downloading -> {
            val total = state.downloadTotalBytes
            Text(if (total == null) "Pobrano ${state.downloadedBytes / 1024} KB" else "Pobrano ${state.downloadedBytes / 1024} z ${total / 1024} KB")
            MakSecondaryAction("Anuluj", onCancel)
        }
        UpdateDownloadStatus.Ready -> MakPrimaryAction("Zainstaluj", onInstall)
        UpdateDownloadStatus.NeedsPermission -> {
            MakNoteBanner("Wymagana zgoda", "Zezwól systemowi na instalowanie aktualizacji z aplikacji MAK.")
            MakPrimaryAction("Otwórz ustawienia systemu", onGrantPermission)
        }
        UpdateDownloadStatus.Installing -> Text("Oczekiwanie na potwierdzenie instalacji...")
        UpdateDownloadStatus.NotEnoughSpace -> DownloadError("Brak miejsca na pobranie aktualizacji.")
        UpdateDownloadStatus.NetworkError -> DownloadError("Pobieranie zostało przerwane.")
        UpdateDownloadStatus.Corrupted -> DownloadError("Pobrany plik jest uszkodzony.")
        UpdateDownloadStatus.WrongPackage -> DownloadError("Plik zawiera inną aplikację.")
        UpdateDownloadStatus.NotNewer -> DownloadError("Pobrana wersja nie jest nowsza.")
        UpdateDownloadStatus.WrongSignature -> DownloadError("Podpis aktualizacji jest inny.")
        UpdateDownloadStatus.InstallCancelled -> DownloadError("Instalacja została anulowana.")
        UpdateDownloadStatus.InstallFailed -> DownloadError("Nie udało się zainstalować aktualizacji.")
        UpdateDownloadStatus.Idle -> Unit
    }
}

@Composable
private fun DownloadError(message: String) = MakNoteBanner("Aktualizacja", message)

@Composable
private fun UpdateCheckResult(state: UpdateUiState) {
    when (state.checkStatus) {
        UpdateCheckStatus.Idle -> Unit
        UpdateCheckStatus.Checking -> Row(
            horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.padding(MakSpacing.xs))
            Text("Sprawdzanie dostępności aktualizacji...")
        }
        UpdateCheckStatus.UpToDate -> MakNoteBanner(
            title = "Aktualizacje",
            subtitle = "Masz najnowszą wersję."
        )
        UpdateCheckStatus.Available -> MakNoteBanner(
            title = "Dostępna wersja ${state.availableUpdate?.versionName.orEmpty()}",
            subtitle = state.availableUpdate?.notes?.ifBlank { "Brak informacji" } ?: "Brak informacji"
        )
        UpdateCheckStatus.RequiresNewerAndroid -> MakNoteBanner(
            title = "Wymagany nowszy Android",
            subtitle = "Nowa wersja wymaga Android API ${state.requiredMinSdk}."
        )
        UpdateCheckStatus.InvalidFile -> MakNoteBanner(
            title = "Nie udało się sprawdzić",
            subtitle = "Informacje o wydaniu są nieprawidłowe."
        )
        UpdateCheckStatus.NetworkError -> MakNoteBanner(
            title = "Nie udało się sprawdzić",
            subtitle = "Sprawdź połączenie."
        )
    }
}

@Composable
private fun AboutSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(14.dp))
                .padding(MakSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(MakSpacing.md),
            content = { content() }
        )
    }
}

@Composable
private fun ReleaseHistory(entry: ReleaseHistoryEntry) {
    Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)) {
        Text(
            text = entry.date?.let { "${entry.versionName} ($it)" } ?: entry.versionName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        if (entry.changes.isEmpty()) {
            Text("Brak informacji", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            entry.changes.forEach { change ->
                Text(change, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
