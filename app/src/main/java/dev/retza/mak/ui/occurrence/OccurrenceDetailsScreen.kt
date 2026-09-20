package dev.retza.mak.ui.occurrence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.navigationBarsPadding
import java.time.LocalDate
import dev.retza.mak.ui.components.MakDialog
import dev.retza.mak.ui.components.MakDatePickerField
import dev.retza.mak.ui.components.MakFactRow
import dev.retza.mak.ui.components.MakField
import dev.retza.mak.ui.components.MakHelperText
import dev.retza.mak.ui.components.MakPrimaryAction
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakSpacing
import dev.retza.mak.ui.components.MakTag
import dev.retza.mak.ui.components.MakTimePickerField

@Composable
fun OccurrenceDetailsScreen(
    state: OccurrenceDetailsUiState,
    onDeleteBaseClass: () -> Unit,
    onDismissDeleteConfirmation: () -> Unit,
    onChangeOccurrence: () -> Unit,
    onMoveOccurrence: () -> Unit,
    onRestoreOccurrence: () -> Unit,
    onOccurrenceNoteDraftChanged: (String) -> Unit,
    onTargetDateDraftChanged: (String) -> Unit,
    onStartTimeDraftChanged: (String) -> Unit,
    onEndTimeDraftChanged: (String) -> Unit,
    onRoomDraftChanged: (String) -> Unit,
    onSaveOccurrenceNote: () -> Unit,
    onDeleteOccurrenceNote: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showChangeForm by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxSize()) {
        MakScreenContent(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            OccurrenceHeader(
                title = state.subjectName.ifBlank { state.title },
                subtitle = "Zmiana dotyczy tylko wybranego wystąpienia zajęć."
            )
            StatusTag(state)
            Facts(state)
            NotesBlock(
                state = state,
                onOccurrenceNoteDraftChanged = onOccurrenceNoteDraftChanged,
                onSaveOccurrenceNote = onSaveOccurrenceNote,
                onDeleteOccurrenceNote = onDeleteOccurrenceNote
            )
            if (showChangeForm) {
                OccurrenceForm(
                    state = state,
                    onTargetDateDraftChanged = onTargetDateDraftChanged,
                    onStartTimeDraftChanged = onStartTimeDraftChanged,
                    onEndTimeDraftChanged = onEndTimeDraftChanged,
                    onRoomDraftChanged = onRoomDraftChanged,
                    onChangeOccurrence = onChangeOccurrence,
                    onMoveOccurrence = onMoveOccurrence
                )
            }
        }
        OccurrenceBottomActions(
            state = state,
            showChangeForm = showChangeForm,
            onShowChangeForm = { showChangeForm = true },
            onRestoreOccurrence = onRestoreOccurrence,
            onBack = onBack
        )
    }

    if (state.showDeleteConfirmation) {
        MakDialog(
            title = "Usuń zajęcia",
            description = "Usunięcie wpisu usunie wszystkie jego wystąpienia. Tej operacji nie można cofnąć.",
            onDismiss = onDismissDeleteConfirmation
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                MakSecondaryAction(text = "Anuluj", onClick = onDismissDeleteConfirmation, modifier = Modifier.weight(1f))
                MakSecondaryAction(
                    text = "Usuń zajęcia",
                    onClick = onDeleteBaseClass,
                    modifier = Modifier.weight(1f),
                    destructive = true
                )
            }
        }
    }
}

@Composable
private fun OccurrenceBottomActions(
    state: OccurrenceDetailsUiState,
    showChangeForm: Boolean,
    onShowChangeForm: () -> Unit,
    onRestoreOccurrence: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = MakSpacing.lg, vertical = MakSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)
    ) {
        if (!showChangeForm) {
            if (state.canRestoreOccurrence) {
                MakPrimaryAction(
                    text = "Przywróć termin",
                    onClick = onRestoreOccurrence,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (state.canChangeOccurrence || state.canMoveOccurrence) {
                MakPrimaryAction(
                    text = "Zmień termin",
                    onClick = onShowChangeForm,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        MakSecondaryAction(
            text = "Zamknij",
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OccurrenceHeader(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MakSpacing.xs, vertical = MakSpacing.sm)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = MakSpacing.sm)
        )
    }
}

@Composable
private fun StatusTag(state: OccurrenceDetailsUiState) {
    val label = state.statusLabel ?: when (state.status) {
        OccurrenceStatusUi.Scheduled -> "Zaplanowane"
        OccurrenceStatusUi.Cancelled -> "Odwołane"
        OccurrenceStatusUi.Changed -> "Zmienione"
        OccurrenceStatusUi.Moved -> "Przeniesione"
        OccurrenceStatusUi.OneOff -> "Jednorazowe"
    }
    MakTag(text = label, modifier = Modifier.padding(bottom = MakSpacing.md))
}

@Composable
private fun Facts(state: OccurrenceDetailsUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)
    ) {
        MakFactRow("Data", state.dateLabel)
        MakFactRow("Godziny", "${state.startTime} - ${state.endTime}")
        MakFactRow("Typ zajęć", state.typeLabel)
        MakFactRow("Kierunek", state.courseName)
        if (!state.room.isNullOrBlank()) MakFactRow("Sala", state.room)
        if (!state.building.isNullOrBlank()) MakFactRow("Budynek", state.building)
        if (!state.teacherName.isNullOrBlank()) MakFactRow("Prowadzący", state.teacherName)
        if (!state.groupName.isNullOrBlank()) MakFactRow("Grupa", state.groupName)
        if (!state.weekLabel.isNullOrBlank()) MakFactRow("Tydzień", state.weekLabel)
        if (!state.originalDateLabel.isNullOrBlank()) MakFactRow("Data bazowa", state.originalDateLabel)
        if (!state.targetDateLabel.isNullOrBlank()) MakFactRow("Nowa data", state.targetDateLabel)
    }
}

@Composable
private fun OccurrenceForm(
    state: OccurrenceDetailsUiState,
    onTargetDateDraftChanged: (String) -> Unit,
    onStartTimeDraftChanged: (String) -> Unit,
    onEndTimeDraftChanged: (String) -> Unit,
    onRoomDraftChanged: (String) -> Unit,
    onChangeOccurrence: () -> Unit,
    onMoveOccurrence: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.md)
    ) {
        if (state.canChangeOccurrence || state.canMoveOccurrence) {
            MakDatePickerField(
                label = "Data",
                value = state.targetDateDraft,
                onValueChange = onTargetDateDraftChanged,
                minDate = state.semesterStartDate?.toLocalDateOrNull(),
                maxDate = state.semesterEndDate?.toLocalDateOrNull(),
                enabled = state.canMoveOccurrence || state.canChangeOccurrence
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                MakTimePickerField(
                    label = "Od",
                    value = state.startTimeDraft,
                    onValueChange = onStartTimeDraftChanged,
                    modifier = Modifier.weight(1f),
                    enabled = state.canChangeOccurrence
                )
                MakTimePickerField(
                    label = "Do",
                    value = state.endTimeDraft,
                    onValueChange = onEndTimeDraftChanged,
                    modifier = Modifier.weight(1f),
                    enabled = state.canChangeOccurrence
                )
            }
            MakField(
                label = "Sala",
                value = state.roomDraft,
                onValueChange = onRoomDraftChanged,
                enabled = state.canChangeOccurrence
            )
        }
        if (state.canChangeOccurrence) {
            MakPrimaryAction(text = "Zapisz zmianę", onClick = onChangeOccurrence)
        }
        if (state.canMoveOccurrence) {
            MakSecondaryAction(text = "Przenieś termin", onClick = onMoveOccurrence)
        }
    }
}

@Composable
private fun NotesBlock(
    state: OccurrenceDetailsUiState,
    onOccurrenceNoteDraftChanged: (String) -> Unit,
    onSaveOccurrenceNote: () -> Unit,
    onDeleteOccurrenceNote: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.md)
    ) {
        MakHelperText(
            if (state.sharedNote.isNullOrBlank()) {
                "Brak wspólnej notatki do zajęć."
            } else {
                "Notatka do zajęć: ${state.sharedNote}"
            }
        )
        MakField(
            label = "Notatka do wystąpienia",
            value = state.occurrenceNoteDraft,
            onValueChange = onOccurrenceNoteDraftChanged,
            enabled = state.canEditOccurrenceNote,
            singleLine = false,
            minLines = 3
        )
        MakHelperText("Dotyczy tylko daty ${state.dateLabel}.")
        if (state.canEditOccurrenceNote) {
            MakPrimaryAction(
                text = "Zapisz notatkę dla daty",
                onClick = onSaveOccurrenceNote,
                enabled = state.occurrenceNoteDraft.isNotBlank()
            )
            if (!state.occurrenceNote.isNullOrBlank()) {
                MakSecondaryAction(
                    text = "Usuń notatkę dla daty",
                    onClick = onDeleteOccurrenceNote
                )
            }
        }
    }
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()
