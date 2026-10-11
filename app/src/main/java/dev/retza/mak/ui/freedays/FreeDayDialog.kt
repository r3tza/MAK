package dev.retza.mak.ui.freedays

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import dev.retza.mak.domain.WeekType
import dev.retza.mak.ui.components.MakCheckbox
import dev.retza.mak.ui.components.MakDateRangeFields
import dev.retza.mak.ui.components.MakDialog
import dev.retza.mak.ui.components.MakDialogAction
import dev.retza.mak.ui.components.MakField
import dev.retza.mak.ui.components.MakSelectField
import java.time.LocalDate

@Composable
fun FreeDayDialog(
    state: FreeDayEditorUiState,
    onRangeChange: (LocalDate, LocalDate) -> Unit,
    onNameChange: (String) -> Unit,
    onToggleCalendar: (Long) -> Unit,
    onWeekAfterBreakChange: (WeekType?) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    if (!state.isOpen) return
    MakDialog(
        title = state.title,
        onDismiss = onCancel,
        dismissAction = MakDialogAction("Anuluj", onCancel),
        confirmAction = MakDialogAction("Zapisz", onSave, enabled = !state.isSaving)
    ) {
        MakDateRangeFields(
            startDate = state.startDate,
            endDate = state.endDate,
            onRangeChange = onRangeChange,
            minDate = state.minDate,
            maxDate = state.maxDate,
            isError = state.rangeError != null
        )
        state.rangeError?.let { FreeDayErrorText(it) }
        MakField(
            label = "Nazwa (opcjonalnie)",
            value = state.name,
            onValueChange = onNameChange
        )
        if (state.calendars.size > 1) {
            Text(
                "Kalendarze",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Column {
                state.calendars.forEach { option ->
                    MakCheckbox(
                        label = option.label,
                        checked = option.checked,
                        onCheckedChange = { onToggleCalendar(option.id) }
                    )
                }
            }
            state.calendarError?.let { FreeDayErrorText(it) }
        }
        if (state.showsWeekAfterBreak) {
            MakSelectField(
                label = "Po przerwie zacznij od tygodnia",
                value = weekAfterBreakLabel(state.weekAfterBreak),
                options = listOf(null, WeekType.A, WeekType.B),
                onSelected = onWeekAfterBreakChange,
                optionLabel = ::weekAfterBreakLabel
            )
            state.weekAfterBreakDescription?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        state.description?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun weekAfterBreakLabel(type: WeekType?): String = when (type) {
    null -> "Bez zmiany"
    WeekType.A -> "Tydzień A"
    WeekType.B -> "Tydzień B"
}

@Composable
private fun FreeDayErrorText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall
    )
}
