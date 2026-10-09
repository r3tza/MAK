package dev.retza.mak.ui.programs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.retza.mak.ui.components.FieldError
import dev.retza.mak.ui.components.MakCourseColorPicker
import dev.retza.mak.ui.components.FieldErrorUi
import dev.retza.mak.ui.components.MakColorDot
import dev.retza.mak.ui.components.MakConfirmDeletionDialog
import dev.retza.mak.ui.components.MakEmptyState
import dev.retza.mak.ui.components.MakField
import dev.retza.mak.ui.components.MakHelperText
import dev.retza.mak.ui.components.MakPrimaryAction
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakScreenIntro
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakSpacing
import dev.retza.mak.ui.components.distinctLabels

@Composable
fun StudyProgramsScreen(
    state: StudyProgramsUiState,
    onOpenProgram: (Long) -> Unit,
    onVisibleChange: (id: Long, visible: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    MakScreenContent(modifier = modifier.verticalScroll(rememberScrollState())) {
        MakScreenIntro(
            "Odznacz kierunek, aby ukryć jego zajęcia na tym telefonie. " +
                "Nazwa i kolor są wspólne dla wszystkich semestrów."
        )
        if (state.programs.isEmpty()) {
            MakEmptyState("Kierunki pojawią się po skonfigurowaniu planu.")
        } else {
            val labels = distinctLabels(state.programs.map { it.name })
            Column(modifier = Modifier.fillMaxWidth()) {
                state.programs.forEachIndexed { index, program ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    StudyProgramRow(
                        label = labels[index],
                        color = program.color,
                        isHidden = program.isHidden,
                        onVisibleChange = { visible -> onVisibleChange(program.id, visible) },
                        onClick = { onOpenProgram(program.id) }
                    )
                }
            }
        }
    }
}

/** The checkbox shows or hides the program on this phone; the rest of the row opens its editor. */
@Composable
private fun StudyProgramRow(
    label: String,
    color: String,
    isHidden: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The default checkbox already has a 48 dp touch target.
        Checkbox(
            checked = !isHidden,
            onCheckedChange = onVisibleChange,
            modifier = Modifier.semantics { contentDescription = "Pokazuj $label" }
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clickable(onClick = onClick)
                .clearAndSetSemantics {
                    contentDescription = if (isHidden) "$label, ukryty na tym telefonie, edytuj" else "$label, edytuj"
                    role = Role.Button
                }
                .padding(vertical = MakSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm)
        ) {
            MakColorDot(color = color)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (isHidden) {
                    Text(
                        text = "Ukryty na tym telefonie",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StudyProgramEditScreen(
    editor: StudyProgramEditorUi,
    onNameChanged: (String) -> Unit,
    onColorChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onRequestDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    MakScreenContent(modifier = modifier.verticalScroll(rememberScrollState())) {
        MakScreenIntro("Zmiana będzie widoczna we wszystkich semestrach, w planie i w widgecie.")
        Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.md)) {
            MakField(
                label = "Nazwa kierunku",
                value = editor.name,
                onValueChange = onNameChanged,
                isError = editor.nameError != null
            )
            FieldError(editor.nameError?.let(::FieldErrorUi))
            MakCourseColorPicker(
                selectedColor = editor.color,
                previewName = editor.name,
                onColorSelected = onColorChanged
            )
            MakPrimaryAction(text = "Zapisz kierunek", onClick = onSave, enabled = !editor.isSaving)
            MakSecondaryAction(text = "Anuluj", onClick = onCancel, enabled = !editor.isSaving)
        }
        // Deletion is a separate section, away from the form actions.
        Column(
            modifier = Modifier.padding(top = MakSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)
        ) {
            MakSecondaryAction(
                text = "Usuń kierunek",
                onClick = onRequestDelete,
                enabled = editor.canDelete,
                destructive = true
            )
            if (editor.usedInSemesters.isNotEmpty()) {
                MakHelperText(
                    "Kierunek jest używany w semestrach: ${editor.usedInSemesters.joinToString(", ")}. " +
                        "Aby go usunąć, najpierw usuń go z tych semestrów na ekranie Kierunki semestru."
                )
            }
        }
    }
    if (editor.showDeleteConfirmation) {
        MakConfirmDeletionDialog(
            title = "Usunąć kierunek?",
            text = "Kierunek ${editor.name.trim()} zniknie z listy kierunków. Tej operacji nie można cofnąć.",
            isDeleting = editor.isDeleting,
            onConfirm = onConfirmDelete,
            onCancel = onCancelDelete
        )
    }
}
