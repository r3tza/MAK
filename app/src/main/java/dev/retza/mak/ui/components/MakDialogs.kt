package dev.retza.mak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import dev.retza.mak.ui.MakDialogMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * The dialog draws its own actions below [content]. [dismissAction] leaves without a change and is a
 * text button. With [confirmAction] alone the two share a row at the end, dismissal first. With
 * [choices] the actions stack: [confirmAction] filled, each choice outlined, dismissal last at the start.
 */
@Composable
fun MakDialog(
    title: String,
    description: String? = null,
    onDismiss: () -> Unit,
    dismissAction: MakDialogAction,
    confirmAction: MakDialogAction? = null,
    choices: List<MakDialogAction> = emptyList(),
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(max = MakDialogMaxWidth)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surface)
                // A low window (landscape tablet, split screen) scrolls instead of cutting off actions.
                .verticalScroll(rememberScrollState())
                .padding(MakSpacing.xl)
                .semantics { paneTitle = title },
            verticalArrangement = Arrangement.spacedBy(MakSpacing.md)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontSize = 20.sp)
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
            MakDialogActions(dismissAction, confirmAction, choices)
        }
    }
}

@Composable
private fun MakDialogActions(
    dismissAction: MakDialogAction,
    confirmAction: MakDialogAction?,
    choices: List<MakDialogAction>
) {
    val dismiss = @Composable {
        MakTextAction(dismissAction.text, dismissAction.onClick, enabled = dismissAction.enabled)
    }
    if (confirmAction != null && choices.isEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MakSpacing.md, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            dismiss()
            MakPrimaryAction(confirmAction.text, confirmAction.onClick, enabled = confirmAction.enabled, fillWidth = false)
        }
    } else {
        confirmAction?.let { MakPrimaryAction(it.text, it.onClick, enabled = it.enabled) }
        choices.forEach {
            MakSecondaryAction(it.text, it.onClick, enabled = it.enabled, destructive = it.destructive)
        }
        dismiss()
    }
}

/** Confirmation of an irreversible deletion; both actions are blocked while [isDeleting]. */
@Composable
fun MakConfirmDeletionDialog(
    title: String,
    text: String,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    confirmLabel: String = "Usuń"
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onCancel() },
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !isDeleting) { Text("Anuluj") }
        }
    )
}

/** Asks before dropping typed values; shared by the class form and the occurrence edit dialog. */
@Composable
fun MakDiscardChangesDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    MakConfirmDeletionDialog(
        title = "Odrzucić zmiany?",
        text = "Wpisane dane nie zostaną zapisane.",
        isDeleting = false,
        onConfirm = onConfirm,
        onCancel = onCancel,
        confirmLabel = "Odrzuć"
    )
}
