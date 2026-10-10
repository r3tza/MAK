package dev.retza.mak.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.retza.mak.domain.Recurrence
import dev.retza.mak.text.polishPlural
import dev.retza.mak.ui.toLocalDateOrNull
import dev.retza.mak.ui.dayNames
import dev.retza.mak.ui.components.FieldError
import dev.retza.mak.ui.components.FieldErrorUi
import dev.retza.mak.ui.components.MakDatePickerField
import dev.retza.mak.ui.components.MakDialog
import dev.retza.mak.ui.components.MakDialogAction
import dev.retza.mak.ui.components.MakHelperText
import dev.retza.mak.ui.components.MakExpandableSection
import dev.retza.mak.ui.components.MakField
import dev.retza.mak.ui.components.MakFieldPair
import dev.retza.mak.ui.components.MakPrimaryAction
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakScreenIntro
import dev.retza.mak.ui.components.MakSelectField
import dev.retza.mak.ui.components.MakSpacing
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

internal val classTypeOptions = listOf("Wykład", "Ćwiczenia", "Laboratorium", "Projekt", "Seminarium", "Inne")

fun Recurrence.formLabel(): String = when (this) {
    Recurrence.EVERY_WEEK -> "Co tydzień"
    Recurrence.A_WEEK -> "Tydzień A"
    Recurrence.B_WEEK -> "Tydzień B"
    Recurrence.ONCE -> "Jednorazowo"
}

data class ClassCourseOptionUi(
    val id: Long,
    val label: String,
    // Hidden on this phone: it can be chosen, but its classes stay out of the plan.
    val isHidden: Boolean = false
)

data class HiddenDataWarningUi(
    val changeCount: Int,
    val noteCount: Int
)

data class ClassEditUiState(
    val title: String = "Dodaj zajęcia",
    val name: String = "",
    val courseName: String = "",
    val semesterProgramId: Long? = null,
    val type: String = "",
    val dayLabel: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val recurrence: Recurrence = Recurrence.EVERY_WEEK,
    val occurrenceDate: String = "",
    val semesterStartDate: String? = null,
    val semesterEndDate: String? = null,
    val room: String = "",
    val building: String = "",
    val group: String = "",
    val teacher: String = "",
    val note: String = "",
    val courseOptions: List<ClassCourseOptionUi> = emptyList(),
    val errors: Map<ClassEditField, FieldErrorUi> = emptyMap(),
    val isSaving: Boolean = false,
    val pendingHiddenData: HiddenDataWarningUi? = null
)

@Composable
fun ClassEditScreen(
    state: ClassEditUiState,
    onNameChanged: (String) -> Unit,
    onCourseChanged: (Long) -> Unit,
    onTypeChanged: (String) -> Unit,
    onDayChanged: (String) -> Unit,
    onStartTimeChanged: (String) -> Unit,
    onEndTimeChanged: (String) -> Unit,
    onRecurrenceChanged: (Recurrence) -> Unit,
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
    modifier: Modifier = Modifier
) {
    state.pendingHiddenData?.let { warning ->
        MakDialog(
            title = "Część danych przestanie być widoczna",
            description = hiddenDataMessage(warning),
            onDismiss = { if (!state.isSaving) onDismissHiddenData() },
            dismissAction = MakDialogAction("Anuluj", onDismissHiddenData, enabled = !state.isSaving),
            confirmAction = MakDialogAction("Zapisz", onConfirmHiddenData, enabled = !state.isSaving)
        )
    }
    var showMoreOptions by remember { mutableStateOf(false) }
    MakScreenContent(modifier = modifier.verticalScroll(rememberScrollState())) {
        MakScreenIntro("Najpierw termin i przedmiot. Resztę możesz uzupełnić później.")
        Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.md)) {
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
                            optionTrailing = { option ->
                                if (option.isHidden) {
                                    Text(
                                        text = "ukryty",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            isError = state.errors.containsKey(ClassEditField.Course)
                        )
                        FieldError(state.errors[ClassEditField.Course])
                        if (state.courseOptions.any { it.id == state.semesterProgramId && it.isHidden }) {
                            MakHelperText(
                                text = "Ten kierunek jest ukryty na tym telefonie. Zajęcia zapiszą się, " +
                                    "ale nie pojawią się w planie, dopóki go nie włączysz.",
                                icon = Icons.Outlined.Info
                            )
                        }
                    }
                },
                second = {
                    Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)) {
                        MakSelectField(
                            label = "Typ",
                            value = state.type,
                            options = classTypeOptions,
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
                options = dayNames.values.toList(),
                onSelected = onDayChanged,
                isError = state.errors.containsKey(ClassEditField.Day)
            )
            FieldError(state.errors[ClassEditField.Day])
            MakFieldPair(
                first = {
                    Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)) {
                        MakTimePickerField(
                            label = "Od",
                            value = state.startTime,
                            onValueChange = onStartTimeChanged,
                            isError = state.errors.containsKey(ClassEditField.StartTime)
                        )
                        FieldError(state.errors[ClassEditField.StartTime])
                    }
                },
                second = {
                    Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)) {
                        MakTimePickerField(
                            label = "Do",
                            value = state.endTime,
                            onValueChange = onEndTimeChanged,
                            isError = state.errors.containsKey(ClassEditField.EndTime)
                        )
                        FieldError(state.errors[ClassEditField.EndTime])
                    }
                }
            )
            MakSelectField(
                label = "Powtarzanie",
                value = state.recurrence.formLabel(),
                options = Recurrence.entries,
                onSelected = onRecurrenceChanged,
                optionLabel = { it.formLabel() },
                isError = state.errors.containsKey(ClassEditField.Recurrence)
            )
            if (state.recurrence == Recurrence.ONCE) {
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
                    label = "Notatka do zajęć",
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
    }
}


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
