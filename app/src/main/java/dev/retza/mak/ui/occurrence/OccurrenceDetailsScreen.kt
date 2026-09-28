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
import dev.retza.mak.ui.components.oneOffTint
import dev.retza.mak.ui.components.modifiedTint
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.Icon
import androidx.compose.material.icons.outlined.LooksOne
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import dev.retza.mak.ui.components.MakFieldPair
import dev.retza.mak.ui.components.FieldError
import dev.retza.mak.ui.components.FieldErrorUi
import dev.retza.mak.ui.components.MakDialog
import dev.retza.mak.ui.components.MakDatePickerField
import dev.retza.mak.ui.components.MakFactRow
import dev.retza.mak.ui.components.MakField
import dev.retza.mak.ui.components.MakHelperText
import dev.retza.mak.ui.components.MakConflictNote
import dev.retza.mak.ui.components.MakPrimaryAction
import dev.retza.mak.ui.components.MakEmptyState
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakSpacing
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
    if (state.notFound) {
        MakScreenContent(modifier = modifier.fillMaxSize()) {
            MakEmptyState("Nie znaleziono tych zajęć.")
        }
        return
    }
    Column(modifier = modifier.fillMaxSize()) {
        MakScreenContent(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            OccurrenceHeader(title = state.subjectName.ifBlank { state.title })
            StatusSummary(state)
            state.conflictLabel?.let { label ->
                MakConflictNote(
                    label = label,
                    partners = state.conflictWith,
                    modifier = Modifier.padding(bottom = MakSpacing.md)
                )
            }
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
            MakFieldPair(
                first = {
                    MakTimePickerField(
                        label = "Od",
                        value = state.startTimeDraft,
                        onValueChange = onStartTimeDraftChanged
                    )
                },
                second = {
                    MakTimePickerField(
                        label = "Do",
                        value = state.endTimeDraft,
                        onValueChange = onEndTimeDraftChanged,
                        isError = state.draftErrors.containsKey(OccurrenceEditField.EndTime)
                    )
                }
            )
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
private fun StatusSummary(state: OccurrenceDetailsUiState) {
    val changeLines = state.changeLines()
    if (state.status == OccurrenceStatusUi.Scheduled && changeLines.isEmpty()) return
    val (icon, tint) = when (state.status) {
        OccurrenceStatusUi.Cancelled -> Icons.Outlined.EventBusy to MaterialTheme.colorScheme.error
        OccurrenceStatusUi.Changed, OccurrenceStatusUi.Moved -> Icons.Outlined.EditCalendar to modifiedTint()
        OccurrenceStatusUi.OneOff -> Icons.Outlined.LooksOne to oneOffTint()
        OccurrenceStatusUi.Scheduled -> Icons.Outlined.Event to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = MakSpacing.md)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Text(
                text = state.statusLabel ?: state.status.label(),
                color = tint,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        changeLines.forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
            label = "Notatka do zajęć",
            value = state.sharedNoteDraft,
            onValueChange = onSharedNoteDraftChanged,
            singleLine = false,
            minLines = 3,
            isError = state.sharedNoteError != null
        )
        MakHelperText("Wspólna dla każdego wystąpienia tych zajęć.")
        FieldError(state.sharedNoteError?.let(::FieldErrorUi))
        if (state.canSaveSharedNote || state.isSavingSharedNote) {
            MakSecondaryAction(
                text = "Zapisz notatkę do zajęć",
                onClick = onSaveSharedNote,
                enabled = !state.isSavingSharedNote
            )
        }
        MakField(
            label = "Notatka do terminu",
            value = state.occurrenceNoteDraft,
            onValueChange = onOccurrenceNoteDraftChanged,
            singleLine = false,
            minLines = 3,
            isError = state.occurrenceNoteError != null
        )
        MakHelperText("Dotyczy tylko tego terminu i przechodzi z nim po przeniesieniu.")
        FieldError(state.occurrenceNoteError?.let(::FieldErrorUi))
        if (state.canSaveOccurrenceNote || state.isSavingOccurrenceNote) {
            MakSecondaryAction(
                text = "Zapisz notatkę do terminu",
                onClick = onSaveOccurrenceNote,
                enabled = !state.isSavingOccurrenceNote
            )
        }
    }
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()
