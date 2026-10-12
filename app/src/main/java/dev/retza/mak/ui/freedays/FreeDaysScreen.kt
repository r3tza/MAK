package dev.retza.mak.ui.freedays

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.retza.mak.ui.components.MakCheckboxBox
import dev.retza.mak.ui.components.MakConfirmDeletionDialog
import dev.retza.mak.ui.components.MakDialog
import dev.retza.mak.ui.components.MakDialogAction
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakScreenIntro
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakSpacing
import dev.retza.mak.ui.components.MakTextAction
import java.time.LocalDate

@Composable
fun FreeDaysScreen(
    state: FreeDaysUiState,
    onAdd: () -> Unit,
    onEdit: (List<Long>) -> Unit,
    onDelete: (List<Long>) -> Unit,
    onConfirmDeletion: () -> Unit,
    onCancelDeletion: () -> Unit,
    onOpenHolidays: () -> Unit,
    onToggleHoliday: (LocalDate) -> Unit,
    onAddHolidays: () -> Unit,
    onCancelHolidays: () -> Unit,
    modifier: Modifier = Modifier
) {
    state.pendingDeletion?.let { group ->
        MakConfirmDeletionDialog(
            title = "Usunąć dni wolne?",
            text = "Usuniesz „${group.title}”, ${group.dateLabel}. Zajęcia cykliczne w tych dniach wrócą do planu.",
            isDeleting = state.isDeleting,
            onConfirm = onConfirmDeletion,
            onCancel = onCancelDeletion
        )
    }
    state.holidays?.let { dialog ->
        HolidaysDialog(
            dialog = dialog,
            onToggle = onToggleHoliday,
            onAdd = onAddHolidays,
            onCancel = onCancelHolidays
        )
    }
    MakScreenContent(modifier = modifier.verticalScroll(rememberScrollState())) {
        MakScreenIntro("W dni wolne zajęcia cykliczne się nie odbywają. Terminy jednorazowe i przeniesione zostają.")
        MakSecondaryAction(
            text = "Dodaj dni wolne",
            onClick = onAdd,
            icon = Icons.Outlined.Add
        )
        MakSecondaryAction(
            text = "Dodaj święta ustawowe",
            onClick = onOpenHolidays,
            modifier = Modifier.padding(top = MakSpacing.sm)
        )
        Column(
            modifier = Modifier.padding(top = MakSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)
        ) {
            state.groups.forEach { group ->
                FreeDayGroupCard(
                    group = group,
                    onEdit = { onEdit(group.ids) },
                    onDelete = { onDelete(group.ids) }
                )
            }
        }
    }
}

@Composable
private fun HolidaysDialog(
    dialog: HolidaysDialogUi,
    onToggle: (LocalDate) -> Unit,
    onAdd: () -> Unit,
    onCancel: () -> Unit
) {
    MakDialog(
        title = "Święta ustawowe",
        description = dialog.intro,
        onDismiss = onCancel,
        dismissAction = MakDialogAction("Anuluj", onCancel, enabled = !dialog.isSaving),
        confirmAction = MakDialogAction(
            text = dialog.confirmLabel,
            onClick = onAdd,
            enabled = dialog.checkedCount > 0 && !dialog.isSaving
        )
    ) {
        if (dialog.options.isEmpty()) {
            Text(
                "W czasie semestru nie ma świąt ustawowych.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        dialog.options.forEach { option ->
            HolidayOptionRow(
                option = option,
                enabled = option.enabled && !dialog.isSaving,
                onToggle = { onToggle(option.date) }
            )
        }
        Text(
            text = dialog.calendarsLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HolidayOptionRow(
    option: HolidayOptionUi,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    // One toggleable row, so a screen reader announces the holiday, its date and its state once.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .toggleable(value = option.checked, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics(mergeDescendants = true) {
                stateDescription = if (option.checked) "Zaznaczone" else "Niezaznaczone"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MakSpacing.md)
    ) {
        MakCheckboxBox(checked = option.checked, focused = false)
        Column {
            Text(option.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = option.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FreeDayGroupCard(
    group: FreeDayGroupUi,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)
    ) {
        Column {
            Text(group.title, fontWeight = FontWeight.SemiBold)
            Text(
                text = group.dateLabel,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = group.calendarsLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MakSpacing.xs)) {
            MakTextAction("Edytuj", onEdit)
            MakTextAction("Usuń", onDelete, destructive = true)
        }
    }
}
