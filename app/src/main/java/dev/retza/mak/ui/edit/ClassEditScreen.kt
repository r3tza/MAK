package dev.retza.mak.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.retza.mak.ui.components.FieldError
import dev.retza.mak.ui.components.FieldErrorUi
import dev.retza.mak.ui.components.MakField
import dev.retza.mak.ui.components.MakHelperText
import dev.retza.mak.ui.components.MakPrimaryAction
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakSectionHeader
import dev.retza.mak.ui.components.MakSelectField
import dev.retza.mak.ui.components.MakStateMessage
import dev.retza.mak.ui.components.ScreenStatus

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

data class ClassEditUiState(
    val title: String = "Dodaj zajęcia",
    val name: String = "",
    val courseName: String = "",
    val type: String = "",
    val dayLabel: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val recurrenceId: String = "every_week",
    val recurrenceLabel: String = "Co tydzień",
    val occurrenceDate: String = "",
    val room: String = "",
    val building: String = "",
    val group: String = "",
    val teacher: String = "",
    val note: String = "",
    val courseOptions: List<String> = emptyList(),
    val typeOptions: List<String> = emptyList(),
    val dayOptions: List<String> = emptyList(),
    val recurrenceOptions: List<RecurrenceOptionUi> = emptyList(),
    val errors: Map<ClassEditField, FieldErrorUi> = emptyMap(),
    val status: ScreenStatus = ScreenStatus.Ready
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
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    MakScreenContent(modifier = modifier.verticalScroll(rememberScrollState())) {
        MakSectionHeader(
            eyebrow = "Nowy wpis",
            title = state.title,
            subtitle = "Najpierw termin i przedmiot. Resztę możesz uzupełnić później."
        )
        when (state.status) {
            ScreenStatus.Ready -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MakField(
                    label = "Nazwa przedmiotu",
                    value = state.name,
                    onValueChange = onNameChanged,
                    placeholder = "np. Projektowanie interfejsów",
                    isError = state.errors.containsKey(ClassEditField.Name)
                )
                FieldError(state.errors[ClassEditField.Name])
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                    MakSelectField(
                        label = "Kierunek",
                        value = state.courseName,
                        options = state.courseOptions,
                        onSelected = onCourseChanged,
                        modifier = Modifier.weight(1f),
                        isError = state.errors.containsKey(ClassEditField.Course)
                    )
                    MakSelectField(
                        label = "Typ",
                        value = state.type,
                        options = state.typeOptions,
                        onSelected = onTypeChanged,
                        modifier = Modifier.weight(1f),
                        isError = state.errors.containsKey(ClassEditField.Type)
                    )
                }
                FieldError(state.errors[ClassEditField.Course])
                FieldError(state.errors[ClassEditField.Type])
                MakSelectField(
                    label = "Dzień tygodnia",
                    value = state.dayLabel,
                    options = state.dayOptions,
                    onSelected = onDayChanged,
                    isError = state.errors.containsKey(ClassEditField.Day)
                )
                FieldError(state.errors[ClassEditField.Day])
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                    MakField(
                        label = "Od",
                        value = state.startTime,
                        onValueChange = onStartTimeChanged,
                        placeholder = "14:00",
                        isError = state.errors.containsKey(ClassEditField.StartTime),
                        modifier = Modifier.weight(1f)
                    )
                    MakField(
                        label = "Do",
                        value = state.endTime,
                        onValueChange = onEndTimeChanged,
                        placeholder = "15:30",
                        isError = state.errors.containsKey(ClassEditField.EndTime),
                        modifier = Modifier.weight(1f)
                    )
                }
                FieldError(state.errors[ClassEditField.StartTime])
                FieldError(state.errors[ClassEditField.EndTime])
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
                    MakField(
                        label = "Data zajęć",
                        value = state.occurrenceDate,
                        onValueChange = onOccurrenceDateChanged,
                        placeholder = "RRRR-MM-DD",
                        isError = state.errors.containsKey(ClassEditField.Date)
                    )
                    FieldError(state.errors[ClassEditField.Date])
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                    MakField(
                        label = "Sala",
                        value = state.room,
                        onValueChange = onRoomChanged,
                        placeholder = "np. L204",
                        modifier = Modifier.weight(1f)
                    )
                    MakField(
                        label = "Prowadzący",
                        value = state.teacher,
                        onValueChange = onTeacherChanged,
                        placeholder = "Opcjonalnie",
                        modifier = Modifier.weight(1f)
                    )
                }
                MakField(
                    label = "Budynek, opcjonalnie",
                    value = state.building,
                    onValueChange = onBuildingChanged
                )
                MakField(
                    label = "Grupa, opcjonalnie",
                    value = state.group,
                    onValueChange = onGroupChanged
                )
                MakField(
                    label = "Notatka, opcjonalnie",
                    value = state.note,
                    onValueChange = onNoteChanged,
                    singleLine = false,
                    minLines = 3
                )
                MakPrimaryAction(
                    text = if (state.title.startsWith("Edytuj")) "Zapisz zajęcia" else "Dodaj do planu",
                    onClick = onSave
                )
                MakSecondaryAction(text = "Anuluj", onClick = onCancel)
                MakHelperText("Koniec musi być późniejszy niż początek tego samego dnia.")
            }

            else -> MakStateMessage(status = state.status, onRetry = onRetry)
        }
    }
}
