package dev.retza.mak.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import dev.retza.mak.ui.components.FieldError
import dev.retza.mak.ui.components.FieldErrorUi
import dev.retza.mak.ui.components.MakDatePickerField
import dev.retza.mak.ui.components.MakExpandableSection
import dev.retza.mak.ui.components.MakField
import dev.retza.mak.ui.components.MakFieldPair
import dev.retza.mak.ui.components.MakPrimaryAction
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakSectionHeader
import dev.retza.mak.ui.components.MakSelectField
import dev.retza.mak.ui.components.MakSpacing
import dev.retza.mak.ui.components.MakStateMessage
import dev.retza.mak.ui.components.ScreenStatus
import dev.retza.mak.ui.components.MakTimePickerField

enum class ClassEditField {
    Name,
    Course,
    Type,
    Day,
    StartTime,
    EndTime,
    Recurrence,
    Date
}

data class RecurrenceOptionUi(
    val id: String,
    val label: String
)

data class ClassCourseOptionUi(
    val id: String,
    val label: String
)

data class HiddenDataWarningUi(
    val changeCount: Int,
    val noteCount: Int
)

data class ClassEditUiState(
    val title: String = "Dodaj zajęcia",
    val name: String = "",
    val courseName: String = "",
    val semesterProgramId: String? = null,
    val type: String = "",
    val dayLabel: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val recurrenceId: String = "every_week",
    val recurrenceLabel: String = "Co tydzień",
    val occurrenceDate: String = "",
    val semesterStartDate: String? = null,
    val semesterEndDate: String? = null,
    val room: String = "",
    val building: String = "",
    val group: String = "",
    val teacher: String = "",
    val note: String = "",
    val courseOptions: List<ClassCourseOptionUi> = emptyList(),
    val typeOptions: List<String> = emptyList(),
    val dayOptions: List<String> = emptyList(),
    val recurrenceOptions: List<RecurrenceOptionUi> = emptyList(),
    val errors: Map<ClassEditField, FieldErrorUi> = emptyMap(),
    val status: ScreenStatus = ScreenStatus.Ready,
    val isSaving: Boolean = false,
    val pendingHiddenData: HiddenDataWarningUi? = null
)

@Composable
fun ClassEditScreen(
    state: ClassEditUiState,
    onNameChanged: (String) -> Unit,
    onCourseChanged: (String) -> Unit,
    onTypeChanged: (String) -> Unit,
    onDayChanged: (String) -> Unit,
    onStartTimeChanged: (String) -> Unit,
    onEndTimeChanged: (String) -> Unit,
    onRecurrenceChanged: (String) -> Unit,
    onOccurrenceDateChanged: (String) -> Unit,
    onRoomChanged: (String) -> Unit,
    onBuildingChanged: (String) -> Unit,
    onGroupChanged: (String) -> Unit,
    onTeacherChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onSave: () -> Unit,
    onConfirmHiddenData: () -> Unit,
    onDismissHiddenData: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    state.pendingHiddenData?.let { warning ->
        AlertDialog(
            onDismissRequest = { if (!state.isSaving) onDismissHiddenData() },
            title = { Text("Część danych przestanie być widoczna") },
            text = { Text(hiddenDataMessage(warning)) },
            confirmButton = {
                TextButton(onClick = onConfirmHiddenData, enabled = !state.isSaving) { Text("Zapisz") }
            },
            dismissButton = {
                TextButton(onClick = onDismissHiddenData, enabled = !state.isSaving) { Text("Anuluj") }
            }
        )
    }
    var showMoreOptions by remember { mutableStateOf(false) }
    MakScreenContent(modifier = modifier.verticalScroll(rememberScrollState())) {
        MakSectionHeader(
            eyebrow = null,
            title = state.title,
            subtitle = "Najpierw termin i przedmiot. Resztę możesz uzupełnić później."
        )
        when (state.status) {
            ScreenStatus.Ready -> Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.md)) {
                MakField(
                    label = "Nazwa przedmiotu",
                    value = state.name,
                    onValueChange = onNameChanged,
                    placeholder = "np. Projektowanie interfejsów",
                    isError = state.errors.containsKey(ClassEditField.Name)
                )
                FieldError(state.errors[ClassEditField.Name])
                MakFieldPair(
                    first = {
                        Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)) {
                            MakSelectField(
                                label = "Kierunek",
                                value = state.courseName,
                                options = state.courseOptions,
                                onSelected = { option -> onCourseChanged(option.id) },
                                optionLabel = { it.label },
                                isError = state.errors.containsKey(ClassEditField.Course)
                            )
                            FieldError(state.errors[ClassEditField.Course])
                        }
                    },
                    second = {
                        Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)) {
                            MakSelectField(
                                label = "Typ",
                                value = state.type,
                                options = state.typeOptions,
                                onSelected = onTypeChanged,
                                isError = state.errors.containsKey(ClassEditField.Type)
                            )
                            FieldError(state.errors[ClassEditField.Type])
                        }
                    }
                )
                MakSelectField(
                    label = "Dzień tygodnia",
                    value = state.dayLabel,
                    options = state.dayOptions,
                    onSelected = onDayChanged,
                    isError = state.errors.containsKey(ClassEditField.Day)
                )
                FieldError(state.errors[ClassEditField.Day])
                Row(horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)) {
                        MakTimePickerField(
                            label = "Od",
                            value = state.startTime,
                            onValueChange = onStartTimeChanged,
                            isError = state.errors.containsKey(ClassEditField.StartTime)
                        )
                        FieldError(state.errors[ClassEditField.StartTime])
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)) {
                        MakTimePickerField(
                            label = "Do",
                            value = state.endTime,
                            onValueChange = onEndTimeChanged,
                            isError = state.errors.containsKey(ClassEditField.EndTime)
                        )
                        FieldError(state.errors[ClassEditField.EndTime])
                    }
                }
                MakSelectField(
                    label = "Powtarzanie",
                    value = state.recurrenceLabel,
                    options = state.recurrenceOptions.map { it.label },
                    onSelected = { label ->
                        state.recurrenceOptions.firstOrNull { it.label == label }?.id?.let(onRecurrenceChanged)
                    },
                    isError = state.errors.containsKey(ClassEditField.Recurrence)
                )
                if (state.recurrenceId == "once") {
                    MakDatePickerField(
                        label = "Data zajęć",
                        value = state.occurrenceDate,
                        onValueChange = onOccurrenceDateChanged,
                        minDate = state.semesterStartDate?.toLocalDateOrNull(),
                        maxDate = state.semesterEndDate?.toLocalDateOrNull(),
                        isError = state.errors.containsKey(ClassEditField.Date)
                    )
                    FieldError(state.errors[ClassEditField.Date])
                }
                MakExpandableSection(
                    label = "więcej opcji",
                    expanded = showMoreOptions,
                    onExpandedChange = { showMoreOptions = it }
                ) {
                    MakField(
                        label = "Sala",
                        value = state.room,
                        onValueChange = onRoomChanged,
                        placeholder = "np. L204"
                    )
                    MakField(
                        label = "Prowadzący",
                        value = state.teacher,
                        onValueChange = onTeacherChanged
                    )
                    MakField(
                        label = "Budynek",
                        value = state.building,
                        onValueChange = onBuildingChanged
                    )
                    MakField(
                        label = "Grupa",
                        value = state.group,
                        onValueChange = onGroupChanged
                    )
                    MakField(
                        label = "Notatka",
                        value = state.note,
                        onValueChange = onNoteChanged,
                        singleLine = false,
                        minLines = 3
                    )
                }
                MakPrimaryAction(
                    text = if (state.title.startsWith("Edytuj")) "Zapisz zajęcia" else "Dodaj do planu",
                    onClick = onSave,
                    enabled = !state.isSaving
                )
                MakSecondaryAction(text = "Anuluj", onClick = onCancel)
            }

            else -> MakStateMessage(status = state.status, onRetry = onRetry)
        }
    }
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

internal fun hiddenDataMessage(warning: HiddenDataWarningUi): String {
    val parts = buildList {
        if (warning.changeCount > 0) {
            add(
                "${warning.changeCount} " +
                    polishPlural(warning.changeCount, "zmiana terminu", "zmiany terminów", "zmian terminów")
            )
        }
        if (warning.noteCount > 0) {
            add("${warning.noteCount} " + polishPlural(warning.noteCount, "notatka", "notatki", "notatek"))
        }
    }
    return "Po zapisie przestaną być widoczne dane przypięte do dotychczasowych terminów: " +
        parts.joinToString(" i ") + ". Dane zostaną zachowane i wrócą, jeśli przywrócisz poprzedni termin."
}

private fun polishPlural(count: Int, one: String, few: String, many: String): String = when {
    count == 1 -> one
    count % 10 in 2..4 && count % 100 !in 12..14 -> few
    else -> many
}
