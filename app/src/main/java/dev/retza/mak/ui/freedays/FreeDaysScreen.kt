package dev.retza.mak.ui.freedays

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.retza.mak.ui.components.MakConfirmDeletionDialog
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakScreenIntro
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakSpacing
import dev.retza.mak.ui.components.MakTextAction

@Composable
fun FreeDaysScreen(
    state: FreeDaysUiState,
    onAdd: () -> Unit,
    onEdit: (List<Long>) -> Unit,
    onDelete: (List<Long>) -> Unit,
    onConfirmDeletion: () -> Unit,
    onCancelDeletion: () -> Unit,
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
    MakScreenContent(modifier = modifier.verticalScroll(rememberScrollState())) {
        MakScreenIntro("W dni wolne zajęcia cykliczne się nie odbywają. Terminy jednorazowe i przeniesione zostają.")
        MakSecondaryAction(
            text = "Dodaj dni wolne",
            onClick = onAdd,
            icon = Icons.Outlined.Add
        )
        Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)) {
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
