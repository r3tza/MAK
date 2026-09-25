package dev.retza.mak.ui.occurrence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import dev.retza.mak.ui.components.FieldError
import dev.retza.mak.ui.components.FieldErrorUi
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
    onOpenOccurrenceEdit: () -> Unit,
    onDismissOccurrenceEdit: () -> Unit,
    onSaveOccurrenceChange: () -> Unit,
    onRestoreOccurrence: () -> Unit,
    onOccurrenceNoteDraftChanged: (String) -> Unit,
    onSharedNoteDraftChanged: (String) -> Unit,
    onTargetDateDraftChanged: (String) -> Unit,
    onStartTimeDraftChanged: (String) -> Unit,
    onEndTimeDraftChanged: (String) -> Unit,
    onRoomDraftChanged: (String) -> Unit,
    onSaveSharedNote: () -> Unit,
    onSaveOccurrenceNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        MakScreenContent(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            OccurrenceHeader(title = state.subjectName.ifBlank { state.title })
            StatusTag(state)
            Facts(state)
            NotesBlock(
                state = state,
                onOccurrenceNoteDraftChanged = onOccurrenceNoteDraftChanged,
                onSharedNoteDraftChanged = onSharedNoteDraftChanged,
                onSaveSharedNote = onSaveSharedNote,
                onSaveOccurrenceNote = onSaveOccurrenceNote
            )
        }
        OccurrenceBottomActions(
            state = state,
            onOpenOccurrenceEdit = onOpenOccurrenceEdit,
            onRestoreOccurrence = onRestoreOccurrence
        )
    }

    if (state.showEditDialog) {
        OccurrenceEditDialog(
            state = state,
            onTargetDateDraftChanged = onTargetDateDraftChanged,
            onStartTimeDraftChanged = onStartTimeDraftChanged,
            onEndTimeDraftChanged = onEndTimeDraftChanged,
            onRoomDraftChanged = onRoomDraftChanged,
            onDismiss = onDismissOccurrenceEdit,
            onSave = onSaveOccurrenceChange
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
    onOpenOccurrenceEdit: () -> Unit,
    onRestoreOccurrence: () -> Unit
) {
    val canChange = state.canChangeOccurrence || state.canMoveOccurrence
    if (!state.canRestoreOccurrence && !canChange) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MakSpacing.lg, vertical = MakSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)
    ) {
        if (state.canRestoreOccurrence) {
            MakPrimaryAction(
                text = "Przywróć termin",
                onClick = onRestoreOccurrence,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            MakPrimaryAction(
                text = "Zmień termin",
                onClick = onOpenOccurrenceEdit,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OccurrenceEditDialog(
    state: OccurrenceDetailsUiState,
    onTargetDateDraftChanged: (String) -> Unit,
    onStartTimeDraftChanged: (String) -> Unit,
    onEndTimeDraftChanged: (String) -> Unit,
    onRoomDraftChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    MakDialog(
        title = "Edytuj ten termin",
        description = "Zmiany dotyczą tylko tego terminu. Pozostałe wystąpienia zajęć pozostaną bez zmian.",
        onDismiss = { if (!state.isSaving) onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MakSpacing.md)
        ) {
            MakDatePickerField(
                label = "Data",
                value = state.targetDateDraft,
                onValueChange = onTargetDateDraftChanged,
                minDate = state.semesterStartDate?.toLocalDateOrNull(),
                maxDate = state.semesterEndDate?.toLocalDateOrNull(),
                isError = state.draftErrors.containsKey(OccurrenceEditField.Date)
            )
            FieldError(state.draftErrors[OccurrenceEditField.Date])
            Row(horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                MakTimePickerField(
                    label = "Od",
                    value = state.startTimeDraft,
                    onValueChange = onStartTimeDraftChanged,
                    modifier = Modifier.weight(1f)
                )
                MakTimePickerField(
                    label = "Do",
                    value = state.endTimeDraft,
                    onValueChange = onEndTimeDraftChanged,
                    modifier = Modifier.weight(1f),
                    isError = state.draftErrors.containsKey(OccurrenceEditField.EndTime)
                )
            }
            FieldError(state.draftErrors[OccurrenceEditField.EndTime])
            MakField(
                label = "Sala",
                value = state.roomDraft,
                onValueChange = onRoomDraftChanged
            )
        }
        FieldError(state.draftError?.let(::FieldErrorUi))
        Row(horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm), modifier = Modifier.fillMaxWidth()) {
            MakSecondaryAction(
                text = "Anuluj",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                enabled = !state.isSaving
            )
            MakPrimaryAction(
                text = "Zapisz",
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = state.canSaveOccurrenceEdit && !state.isSaving
            )
        }
    }
}

@Composable
private fun OccurrenceHeader(title: String) {
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
private fun NotesBlock(
    state: OccurrenceDetailsUiState,
    onOccurrenceNoteDraftChanged: (String) -> Unit,
    onSharedNoteDraftChanged: (String) -> Unit,
    onSaveSharedNote: () -> Unit,
    onSaveOccurrenceNote: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.md)
    ) {
        Text(
            text = "Notatki",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        MakField(
            label = "Notatka dla wszystkich terminów",
            value = state.sharedNoteDraft,
            onValueChange = onSharedNoteDraftChanged,
            singleLine = false,
            minLines = 3,
            isError = state.sharedNoteError != null
        )
        MakHelperText("Wspólna dla każdego wystąpienia tych zajęć.")
        FieldError(state.sharedNoteError?.let(::FieldErrorUi))
        if (state.canSaveSharedNote || state.isSavingSharedNote) {
            MakPrimaryAction(
                text = "Zapisz notatkę dla wszystkich terminów",
                onClick = onSaveSharedNote,
                enabled = !state.isSavingSharedNote
            )
        }
        MakField(
            label = "Notatka tylko dla tej daty",
            value = state.occurrenceNoteDraft,
            onValueChange = onOccurrenceNoteDraftChanged,
            singleLine = false,
            minLines = 3,
            isError = state.occurrenceNoteError != null
        )
        MakHelperText("Dotyczy tylko tego terminu i przechodzi z nim po przeniesieniu.")
        FieldError(state.occurrenceNoteError?.let(::FieldErrorUi))
        if (state.canSaveOccurrenceNote || state.isSavingOccurrenceNote) {
            MakPrimaryAction(
                text = "Zapisz notatkę dla tej daty",
                onClick = onSaveOccurrenceNote,
                enabled = !state.isSavingOccurrenceNote
            )
        }
    }
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()
