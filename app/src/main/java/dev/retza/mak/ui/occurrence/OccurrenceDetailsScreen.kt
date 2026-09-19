package dev.retza.mak.ui.occurrence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.retza.mak.ui.components.MakDialog
import dev.retza.mak.ui.components.MakFactRow
import dev.retza.mak.ui.components.MakField
import dev.retza.mak.ui.components.MakHelperText
import dev.retza.mak.ui.components.MakPrimaryAction
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakSectionHeader
import dev.retza.mak.ui.components.MakTag

@Composable
fun OccurrenceDetailsScreen(
    state: OccurrenceDetailsUiState,
    onEditBaseClass: () -> Unit,
    onDeleteBaseClass: () -> Unit,
    onRequestDeleteBaseClass: () -> Unit,
    onDismissDeleteConfirmation: () -> Unit,
    onCancelOccurrence: () -> Unit,
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
    MakScreenContent(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        MakSectionHeader(
            eyebrow = "Termin",
            title = state.subjectName.ifBlank { state.title },
            subtitle = "Zmiana dotyczy tylko wybranego wystąpienia zajęć."
        )
        StatusTag(state)
        Facts(state)
        OccurrenceForm(
            state = state,
            onTargetDateDraftChanged = onTargetDateDraftChanged,
            onStartTimeDraftChanged = onStartTimeDraftChanged,
            onEndTimeDraftChanged = onEndTimeDraftChanged,
            onRoomDraftChanged = onRoomDraftChanged,
            onCancelOccurrence = onCancelOccurrence,
            onChangeOccurrence = onChangeOccurrence,
            onMoveOccurrence = onMoveOccurrence,
            onRestoreOccurrence = onRestoreOccurrence,
            onBack = onBack
        )
        NotesBlock(
            state = state,
            onOccurrenceNoteDraftChanged = onOccurrenceNoteDraftChanged,
            onSaveOccurrenceNote = onSaveOccurrenceNote,
            onDeleteOccurrenceNote = onDeleteOccurrenceNote
        )
        BaseClassActions(
            state = state,
            onEditBaseClass = onEditBaseClass,
            onRequestDeleteBaseClass = onRequestDeleteBaseClass
        )
    }

    if (state.showDeleteConfirmation) {
        MakDialog(
            title = "Usuń zajęcia",
            description = "Usunięcie wpisu usunie wszystkie jego wystąpienia. Tej operacji nie można cofnąć.",
            onDismiss = onDismissDeleteConfirmation
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MakSecondaryAction(text = "Anuluj", onClick = onDismissDeleteConfirmation, modifier = Modifier.weight(1f))
                MakPrimaryAction(text = "Usuń zajęcia", onClick = onDeleteBaseClass, modifier = Modifier.weight(1f))
            }
        }
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
    MakTag(text = label, modifier = Modifier.padding(bottom = 14.dp))
}

@Composable
private fun Facts(state: OccurrenceDetailsUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
    onCancelOccurrence: () -> Unit,
    onChangeOccurrence: () -> Unit,
    onMoveOccurrence: () -> Unit,
    onRestoreOccurrence: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.canChangeOccurrence || state.canMoveOccurrence) {
            MakField(
                label = "Data",
                value = state.targetDateDraft,
                onValueChange = onTargetDateDraftChanged,
                placeholder = "RRRR-MM-DD",
                enabled = state.canMoveOccurrence || state.canChangeOccurrence
            )
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                MakField(
                    label = "Od",
                    value = state.startTimeDraft,
                    onValueChange = onStartTimeDraftChanged,
                    modifier = Modifier.weight(1f),
                    enabled = state.canChangeOccurrence
                )
                MakField(
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
        if (state.canRestoreOccurrence) {
            MakPrimaryAction(text = "Przywróć termin", onClick = onRestoreOccurrence)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (state.canCancelOccurrence) {
                MakSecondaryAction(
                    text = "Odwołaj termin",
                    onClick = onCancelOccurrence,
                    modifier = Modifier.weight(1f),
                    destructive = true
                )
            }
            MakSecondaryAction(text = "Zamknij", onClick = onBack, modifier = Modifier.weight(1f))
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
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

@Composable
private fun BaseClassActions(
    state: OccurrenceDetailsUiState,
    onEditBaseClass: () -> Unit,
    onRequestDeleteBaseClass: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.canEditBaseClass) {
            MakSecondaryAction(text = "Edytuj bazowe zajęcia", onClick = onEditBaseClass)
        }
        if (state.canDeleteBaseClass) {
            MakSecondaryAction(
                text = "Usuń zajęcia",
                onClick = onRequestDeleteBaseClass,
                destructive = true
            )
        }
    }
}
