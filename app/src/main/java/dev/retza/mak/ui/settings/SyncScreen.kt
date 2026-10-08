package dev.retza.mak.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.retza.mak.sync.SyncChoice
import dev.retza.mak.ui.components.MakDialog
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
                    actions = { MakTextAction(text = "Wybierz wersję", onClick = onOpenChoice) }
                )
            }
            state.issue?.let { issue ->
                MakNoteBanner(
                    title = null,
                    subtitle = issue,
                    role = MakNoteRole.Error,
                    actions = if (state.needsReconnect) {
                        { MakTextAction(text = "Połącz ponownie", onClick = onReconnect, enabled = !state.isWorking) }
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
                actions = { MakTextAction(text = "Zamknij", onClick = onDismissError) }
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
            onDismiss = onDismissChoice
        ) {
            PlanSummaryCard("Ten telefon", choice.phone)
            PlanSummaryCard("Dysk Google", choice.drive)
            MakPrimaryAction(text = "Zachowaj plan z telefonu", onClick = { onChoose(SyncChoice.KEEP_PHONE) })
            MakSecondaryAction(text = "Zachowaj plan z Dysku", onClick = { onChoose(SyncChoice.KEEP_DRIVE) })
            MakSecondaryAction(text = "Później", onClick = onDismissChoice)
        }
    }
    if (state.showDisconnectDialog) {
        MakDialog(
            title = "Wyłączyć synchronizację?",
            description = "Plan zostanie na tym telefonie. Jeśli usuniesz kopię z Dysku, inny połączony telefon " +
                "może ją wysłać ponownie.",
            onDismiss = onDismissDisconnect
        ) {
            MakPrimaryAction(text = "Wyłącz i zachowaj kopię na Dysku", onClick = { onDisconnect(false) })
            MakSecondaryAction(text = "Wyłącz i usuń kopię z Dysku", onClick = { onDisconnect(true) }, destructive = true)
            MakSecondaryAction(text = "Anuluj", onClick = onDismissDisconnect)
        }
    }
}

@Composable
private fun PlanSummaryCard(title: String, lines: List<String>) {
    SettingsListSection(title) {
        SettingsFieldItem {
            Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)) {
                lines.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
            }
        }
    }
}
