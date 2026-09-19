package dev.retza.mak.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.retza.mak.ui.components.MakDialog
import dev.retza.mak.ui.components.MakEmptyState
import dev.retza.mak.ui.components.MakNoteBanner
import dev.retza.mak.ui.components.MakPrimaryAction
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakSectionHeader
import dev.retza.mak.ui.components.MakSelectField
import dev.retza.mak.ui.components.MakStateMessage
import dev.retza.mak.ui.components.SemesterUi
import dev.retza.mak.ui.components.ScreenStatus

data class ThemeOptionUi(
    val id: String,
    val label: String,
    val isSelected: Boolean = false
)

data class SettingsUiState(
    val semesters: List<SemesterUi> = emptyList(),
    val activeSemesterId: String? = null,
    val themeOptions: List<ThemeOptionUi> = emptyList(),
    val notificationsLabel: String = "Obsługiwane przez aplikację, treść i moment wysyłki do ustalenia",
    val notificationsDetails: String = "Treść i moment wysyłki zostaną ustalone.",
    val semesterToDeleteId: String? = null,
    val status: ScreenStatus = ScreenStatus.Ready
)

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onSemesterSelected: (String) -> Unit,
    onAddSemester: () -> Unit,
    onConfigureSemester: (String) -> Unit,
    onDeleteSemester: (String) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onThemeSelected: (String) -> Unit,
    onExport: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = state.semesters.firstOrNull { it.id == state.activeSemesterId }
    MakScreenContent(modifier = modifier.verticalScroll(rememberScrollState())) {
        MakSectionHeader(
            eyebrow = "Ustawienia",
            title = "Semestry i wygląd",
            subtitle = "Wybierz plan, który ma być aktywny na ekranie i w powiadomieniach."
        )
        when (state.status) {
            ScreenStatus.Ready -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.semesters.isEmpty()) {
                    MakEmptyState("Dodaj semestr, aby rozpocząć pracę z planem.")
                } else {
                    MakSelectField(
                        label = "Aktywny semestr",
                        value = active?.name.orEmpty(),
                        options = state.semesters.map { it.name },
                        onSelected = { name ->
                            state.semesters.firstOrNull { it.name == name }?.id?.let(onSemesterSelected)
                        }
                    )
                    if (active != null) {
                        MakNoteBanner(
                            title = active.name,
                            subtitle = "${active.dateRangeLabel}, ${active.firstWeekLabel.lowercase()}"
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                        MakSecondaryAction(
                            text = "Konfiguruj semestr",
                            onClick = { active?.id?.let(onConfigureSemester) },
                            modifier = Modifier.weight(1f),
                            enabled = active != null
                        )
                        MakSecondaryAction(
                            text = "Dodaj semestr",
                            onClick = onAddSemester,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    MakSecondaryAction(
                        text = "Usuń wybrany semestr",
                        onClick = { active?.id?.let(onDeleteSemester) },
                        enabled = active != null,
                        destructive = true
                    )
                }
                MakSelectField(
                    label = "Motyw",
                    value = state.themeOptions.firstOrNull { it.isSelected }?.label.orEmpty(),
                    options = state.themeOptions.map { it.label },
                    onSelected = { label ->
                        state.themeOptions.firstOrNull { it.label == label }?.id?.let(onThemeSelected)
                    }
                )
                MakNoteBanner(
                    title = "Powiadomienia",
                    subtitle = state.notificationsLabel
                )
                MakSecondaryAction(text = "Eksportuj plan do JSON", onClick = onExport)
            }

            else -> MakStateMessage(status = state.status, onRetry = onRetry)
        }
    }
    if (state.semesterToDeleteId != null) {
        MakDialog(
            title = "Usuń semestr",
            description = "Usunięcie semestru usunie jego plan i dane. Tej operacji nie można cofnąć.",
            onDismiss = onCancelDelete
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MakSecondaryAction(text = "Anuluj", onClick = onCancelDelete, modifier = Modifier.weight(1f))
                MakPrimaryAction(text = "Usuń", onClick = onConfirmDelete, modifier = Modifier.weight(1f))
            }
        }
    }
}
