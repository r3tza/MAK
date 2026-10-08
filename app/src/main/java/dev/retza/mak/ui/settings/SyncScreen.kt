package dev.retza.mak.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.retza.mak.sync.SyncChoice
import dev.retza.mak.ui.components.MakBannerAction
import dev.retza.mak.ui.components.MakDialog
import dev.retza.mak.ui.components.MakDialogAction
import dev.retza.mak.ui.components.MakHelperText
import dev.retza.mak.ui.components.MakNoteBanner
import dev.retza.mak.ui.components.MakNoteRole
import dev.retza.mak.ui.components.MakPrimaryAction
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakScreenIntro
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakSpacing
import dev.retza.mak.ui.components.MakTextAction

/** Summary for the row in the main settings screen. */
fun SyncUiState.settingsSummary(): String = when {
    accountEmail == null -> "Wyłączona"
    issue != null || choice != null -> "Wymaga działania"
    else -> accountEmail
}

@Composable
fun SyncScreen(
    state: SyncUiState,
    onConnect: () -> Unit,
    onReconnect: () -> Unit,
    onSyncNow: () -> Unit,
    onOpenChoice: () -> Unit,
    onChoose: (SyncChoice) -> Unit,
    onDismissChoice: () -> Unit,
    onOpenChanges: () -> Unit,
    onRequestDisconnect: () -> Unit,
    onDisconnect: (deleteRemote: Boolean) -> Unit,
    onDismissDisconnect: () -> Unit,
    onExportArchived: (String) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    MakScreenContent(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = MakSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.lg)
    ) {
        if (state.accountEmail == null) {
            MakScreenIntro(
                "Plan zostaje na telefonie i działa bez internetu. Po połączeniu jego kopia trafia do " +
                    "ukrytego folderu aplikacji na Twoim Dysku Google, skąd pobiorą ją inne telefony z tym kontem."
            )
            MakPrimaryAction(text = "Połącz konto Google", onClick = onConnect, enabled = !state.isWorking)
        } else {
            SettingsListSection("Konto") {
                SettingsFieldItem {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)
                        ) {
                            Text(state.accountEmail, style = MaterialTheme.typography.bodyLarge)
                            state.lastSyncLabel?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (state.isWorking) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(start = MakSpacing.md)
                                    .size(24.dp)
                                    .semantics { contentDescription = "Synchronizowanie" },
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                }
            }
            state.choice?.let {
                MakNoteBanner(
                    title = CHOICE_TITLE,
                    subtitle = null,
                    role = MakNoteRole.Warning,
                    action = MakBannerAction("Wybierz wersję", onOpenChoice)
                )
            }
            state.issue?.let { issue ->
                MakNoteBanner(
                    title = null,
                    subtitle = issue,
                    role = MakNoteRole.Error,
                    action = if (state.needsReconnect) {
                        MakBannerAction("Połącz ponownie", onReconnect, enabled = !state.isWorking)
                    } else null
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.md)) {
                MakPrimaryAction(text = "Synchronizuj teraz", onClick = onSyncNow, enabled = !state.isWorking)
                MakSecondaryAction(text = "Wyłącz synchronizację", onClick = onRequestDisconnect, enabled = !state.isWorking)
            }
        }
        state.errorMessage?.let { message ->
            MakNoteBanner(
                title = null,
                subtitle = message,
                role = MakNoteRole.Error,
                dismissAction = MakBannerAction("Zamknij", onDismissError)
            )
        }
        if (state.archive.isNotEmpty()) {
            SettingsListSection("Poprzednie wersje") {
                state.archive.forEachIndexed { index, item ->
                    if (index > 0) SettingsRowDivider()
                    SettingsNavigationRow(
                        title = item.label,
                        value = "Eksportuj do JSON",
                        onClick = { onExportArchived(item.id) }
                    )
                }
            }
            MakHelperText("Wersje zastąpione podczas synchronizacji. Aplikacja przechowuje 10 ostatnich.")
        }
    }

    val choice = state.choice
    if (state.showChoiceDialog && choice != null) {
        MakDialog(
            title = "Którą wersję zachować?",
            description = "Odrzuconą wersję znajdziesz w „Poprzednie wersje”, skąd możesz ją wyeksportować.",
            onDismiss = onDismissChoice,
            dismissAction = MakDialogAction("Później", onDismissChoice),
            confirmAction = MakDialogAction("Zachowaj plan z telefonu", { onChoose(SyncChoice.KEEP_PHONE) }),
            choices = listOf(MakDialogAction("Zachowaj plan z Dysku", { onChoose(SyncChoice.KEEP_DRIVE) }))
        ) {
            // Counts tell little when the content changed, so they give way to the list of differences.
            val withDifferences = choice.differences.isNotEmpty()
            Row(horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm)) {
                VersionCard(
                    title = "Ten telefon",
                    lines = listOf(choice.phoneChanged) + if (withDifferences) emptyList() else choice.phone,
                    modifier = Modifier.weight(1f)
                )
                VersionCard(
                    title = "Dysk Google",
                    lines = listOf(choice.driveChanged) + if (withDifferences) emptyList() else choice.drive,
                    modifier = Modifier.weight(1f)
                )
            }
            if (withDifferences) {
                Text(
                    "Co się różni (${choice.differences.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                DifferenceList(choice.differences.take(DIALOG_DIFFERENCES))
                MakTextAction(text = "Wybierz zmiany", onClick = onOpenChanges)
            }
        }
    }
    if (state.showDisconnectDialog) {
        MakDialog(
            title = "Wyłączyć synchronizację?",
            description = "Plan zostanie na tym telefonie. Jeśli usuniesz kopię z Dysku, inny połączony telefon " +
                "może ją wysłać ponownie.",
            onDismiss = onDismissDisconnect,
            dismissAction = MakDialogAction("Anuluj", onDismissDisconnect),
            confirmAction = MakDialogAction("Wyłącz i zachowaj kopię na Dysku", { onDisconnect(false) }),
            choices = listOf(MakDialogAction("Wyłącz i usuń kopię z Dysku", { onDisconnect(true) }, destructive = true))
        )
    }
}

private const val DIALOG_DIFFERENCES = 3

@Composable
private fun VersionCard(title: String, lines: List<String>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(13.dp))
            .padding(horizontal = MakSpacing.md, vertical = MakSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        lines.forEach {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Differences in a bordered list; shared by the dialog and the screen „Wybierz zmiany”. */
@Composable
internal fun DifferenceList(differences: List<SyncDifferenceUi>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(13.dp))
    ) {
        differences.forEachIndexed { index, difference ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier.padding(horizontal = MakSpacing.md, vertical = MakSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)
            ) {
                DifferenceHeading(difference)
                SideValue("Telefon", difference.phone)
                SideValue("Dysk", difference.drive)
            }
        }
    }
}

@Composable
internal fun DifferenceHeading(difference: SyncDifferenceUi) {
    Column {
        Text(difference.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(difference.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SideValue(side: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm)) {
        Text(
            side,
            modifier = Modifier.widthIn(min = 56.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
