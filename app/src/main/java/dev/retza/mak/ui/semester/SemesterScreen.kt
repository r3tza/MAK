package dev.retza.mak.ui.semester

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import dev.retza.mak.ui.components.FieldError
import dev.retza.mak.ui.components.FieldErrorUi
import dev.retza.mak.ui.components.MakChoiceRow
import dev.retza.mak.ui.components.MakEmptyState
import dev.retza.mak.ui.components.MakField
import dev.retza.mak.ui.components.MakHelperText
import dev.retza.mak.ui.components.MakNoteBanner
import dev.retza.mak.ui.components.MakPrimaryAction
import dev.retza.mak.ui.components.MakRowTitle
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakSectionHeader
import dev.retza.mak.ui.components.MakSelectField
import dev.retza.mak.ui.components.MakStateMessage
import dev.retza.mak.ui.components.MakTag
import dev.retza.mak.ui.components.MakTextAction
import dev.retza.mak.ui.components.ScreenStatus

enum class WeekTypeUi {
    A,
    B
}

enum class WeekOverrideScopeUi {
    ONE_WEEK,
    FROM_WEEK
}

data class SemesterFormUiState(
    val name: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val firstWeek: WeekTypeUi = WeekTypeUi.A,
    val nameError: String? = null,
    val startDateError: String? = null,
    val endDateError: String? = null,
    val dateRangeError: String? = null,
    val isSaving: Boolean = false
)

data class WeekOverrideUi(
    val id: String,
    val weekStartDate: String,
    val weekType: WeekTypeUi,
    val scope: WeekOverrideScopeUi
)

data class WeekOverrideFormUiState(
    val id: String? = null,
    val weekStartDate: String = "",
    val weekType: WeekTypeUi = WeekTypeUi.A,
    val scope: WeekOverrideScopeUi = WeekOverrideScopeUi.ONE_WEEK,
    val weekStartDateError: String? = null,
    val isSaving: Boolean = false
) {
    val isEditing: Boolean
        get() = id != null
}

data class SemesterScreenUiState(
    val semester: SemesterFormUiState = SemesterFormUiState(),
    val overrides: List<WeekOverrideUi> = emptyList(),
    val overrideForm: WeekOverrideFormUiState = WeekOverrideFormUiState(),
    val courses: List<Pair<String, String>> = emptyList(),
    val courseNameDraft: String = "",
    val courseColorDraft: String = "#137b71",
    val status: ScreenStatus = ScreenStatus.Ready
)

@Composable
fun SemesterScreen(
    state: SemesterScreenUiState,
    onSemesterNameChanged: (String) -> Unit,
    onSemesterStartDateChanged: (String) -> Unit,
    onSemesterEndDateChanged: (String) -> Unit,
    onSemesterFirstWeekChanged: (WeekTypeUi) -> Unit,
    onSaveSemester: () -> Unit,
    onOverrideWeekStartDateChanged: (String) -> Unit,
    onOverrideWeekTypeChanged: (WeekTypeUi) -> Unit,
    onOverrideScopeChanged: (WeekOverrideScopeUi) -> Unit,
    onNewOverride: () -> Unit,
    onEditOverride: (String) -> Unit,
    onSaveOverride: () -> Unit,
    onDeleteOverride: (String) -> Unit,
    onCancelOverrideEdit: () -> Unit,
    onCourseNameChanged: (String) -> Unit,
    onCourseColorChanged: (String) -> Unit,
    onAddCourse: () -> Unit,
    onDeleteCourse: (String) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    MakScreenContent(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        MakSectionHeader(
            eyebrow = "Semestr",
            title = "Zakres i tygodnie A/B",
            subtitle = "Zakres dat, kierunki i ręczne korekty tygodni."
        )

        when (state.status) {
            ScreenStatus.Ready -> {
                SemesterForm(
                    state = state.semester,
                    onNameChanged = onSemesterNameChanged,
                    onStartDateChanged = onSemesterStartDateChanged,
                    onEndDateChanged = onSemesterEndDateChanged,
                    onFirstWeekChanged = onSemesterFirstWeekChanged,
                    onSave = onSaveSemester
                )
                CoursesBlock(
                    courses = state.courses,
                    courseNameDraft = state.courseNameDraft,
                    courseColorDraft = state.courseColorDraft,
                    onCourseNameChanged = onCourseNameChanged,
                    onCourseColorChanged = onCourseColorChanged,
                    onAddCourse = onAddCourse,
                    onDeleteCourse = onDeleteCourse
                )
                WeekOverridesSection(
                    overrides = state.overrides,
                    form = state.overrideForm,
                    onWeekStartDateChanged = onOverrideWeekStartDateChanged,
                    onWeekTypeChanged = onOverrideWeekTypeChanged,
                    onScopeChanged = onOverrideScopeChanged,
                    onNewOverride = onNewOverride,
                    onEditOverride = onEditOverride,
                    onSaveOverride = onSaveOverride,
                    onDeleteOverride = onDeleteOverride,
                    onCancelEdit = onCancelOverrideEdit
                )
                MakSecondaryAction(
                    text = "Wróć do ustawień",
                    onClick = onBack,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            else -> MakStateMessage(status = state.status, onRetry = onRetry)
        }
    }
}

@Composable
private fun SemesterForm(
    state: SemesterFormUiState,
    onNameChanged: (String) -> Unit,
    onStartDateChanged: (String) -> Unit,
    onEndDateChanged: (String) -> Unit,
    onFirstWeekChanged: (WeekTypeUi) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier.padding(bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MakField(
            label = "Nazwa semestru",
            value = state.name,
            onValueChange = onNameChanged,
            isError = state.nameError != null
        )
        FieldError(state.nameError?.let(::FieldErrorUi))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            MakField(
                label = "Od",
                value = state.startDate,
                onValueChange = onStartDateChanged,
                placeholder = "RRRR-MM-DD",
                isError = state.startDateError != null,
                modifier = Modifier.weight(1f)
            )
            MakField(
                label = "Do",
                value = state.endDate,
                onValueChange = onEndDateChanged,
                placeholder = "RRRR-MM-DD",
                isError = state.endDateError != null,
                modifier = Modifier.weight(1f)
            )
        }
        FieldError(state.startDateError?.let(::FieldErrorUi))
        FieldError(state.endDateError?.let(::FieldErrorUi))
        FieldError(state.dateRangeError?.let(::FieldErrorUi))
        MakSelectField(
            label = "Pierwszy tydzień",
            value = "Tydzień ${state.firstWeek.name}",
            options = listOf("Tydzień A", "Tydzień B"),
            onSelected = { onFirstWeekChanged(if (it.endsWith("B")) WeekTypeUi.B else WeekTypeUi.A) }
        )
        MakHelperText("Podaj daty w formacie RRRR-MM-DD. Pierwszy tydzień zaczyna się w poniedziałek tygodnia zawierającego datę rozpoczęcia.")
        MakPrimaryAction(
            text = "Zapisz semestr",
            onClick = onSave,
            enabled = !state.isSaving
        )
    }
}

@Composable
private fun CoursesBlock(
    courses: List<Pair<String, String>>,
    courseNameDraft: String,
    courseColorDraft: String,
    onCourseNameChanged: (String) -> Unit,
    onCourseColorChanged: (String) -> Unit,
    onAddCourse: () -> Unit,
    onDeleteCourse: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MakRowTitle(title = "Kierunki", meta = if (courses.isEmpty()) "Brak" else "${courses.size}")
        if (courses.isEmpty()) {
            MakEmptyState("Dodaj kierunek, aby przypisać zajęcia.")
        } else {
            courses.forEach { (id, name) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    MakTextAction(text = "Usuń", onClick = { onDeleteCourse(id) })
                }
            }
        }
        MakField(
            label = "Nazwa nowego kierunku",
            value = courseNameDraft,
            onValueChange = onCourseNameChanged
        )
        MakField(
            label = "Kolor kierunku, opcjonalnie",
            value = courseColorDraft,
            onValueChange = onCourseColorChanged,
            placeholder = "#137b71"
        )
        MakSecondaryAction(
            text = "Dodaj kierunek",
            onClick = onAddCourse,
            enabled = courseNameDraft.isNotBlank()
        )
    }
}

@Composable
private fun WeekOverridesSection(
    overrides: List<WeekOverrideUi>,
    form: WeekOverrideFormUiState,
    onWeekStartDateChanged: (String) -> Unit,
    onWeekTypeChanged: (WeekTypeUi) -> Unit,
    onScopeChanged: (WeekOverrideScopeUi) -> Unit,
    onNewOverride: () -> Unit,
    onEditOverride: (String) -> Unit,
    onSaveOverride: () -> Unit,
    onDeleteOverride: (String) -> Unit,
    onCancelEdit: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Korekty tygodni A/B", fontWeight = FontWeight.SemiBold, fontSize = MaterialTheme.typography.titleMedium.fontSize)
            MakTextAction(text = "Dodaj", onClick = onNewOverride)
        }
        MakHelperText("Każda korekta dotyczy poniedziałku wybranego tygodnia.")
        if (overrides.isEmpty()) {
            MakEmptyState("Automatyczne oznaczenie tygodni A/B działa bez ręcznych zmian.")
        } else {
            overrides.forEach { override ->
                WeekOverrideCard(
                    item = override,
                    onEdit = { onEditOverride(override.id) },
                    onDelete = { onDeleteOverride(override.id) }
                )
            }
        }
        WeekOverrideForm(
            state = form,
            onWeekStartDateChanged = onWeekStartDateChanged,
            onWeekTypeChanged = onWeekTypeChanged,
            onScopeChanged = onScopeChanged,
            onSave = onSaveOverride,
            onCancel = onCancelEdit
        )
    }
}

@Composable
private fun WeekOverrideCard(
    item: WeekOverrideUi,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.weekStartDate, fontWeight = FontWeight.SemiBold)
                Text(
                    text = when (item.scope) {
                        WeekOverrideScopeUi.ONE_WEEK -> "Tylko ten tydzień"
                        WeekOverrideScopeUi.FROM_WEEK -> "Od tego tygodnia"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MakTag(text = "Tydzień ${item.weekType.name}")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MakSecondaryAction(text = "Edytuj", onClick = onEdit, modifier = Modifier.weight(1f))
            MakSecondaryAction(text = "Usuń", onClick = onDelete, modifier = Modifier.weight(1f), destructive = true)
        }
    }
}

@Composable
private fun WeekOverrideForm(
    state: WeekOverrideFormUiState,
    onWeekStartDateChanged: (String) -> Unit,
    onWeekTypeChanged: (WeekTypeUi) -> Unit,
    onScopeChanged: (WeekOverrideScopeUi) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MakNoteBanner(
            title = if (state.isEditing) "Edytuj korektę" else "Dodaj korektę",
            subtitle = "Oznaczenie A albo B dla wybranego poniedziałku."
        )
        MakField(
            label = "Poniedziałek tygodnia",
            value = state.weekStartDate,
            onValueChange = onWeekStartDateChanged,
            placeholder = "RRRR-MM-DD",
            isError = state.weekStartDateError != null
        )
        FieldError(state.weekStartDateError?.let(::FieldErrorUi))
        MakSelectField(
            label = "Oznaczenie tygodnia",
            value = "Tydzień ${state.weekType.name}",
            options = listOf("Tydzień A", "Tydzień B"),
            onSelected = { onWeekTypeChanged(if (it.endsWith("B")) WeekTypeUi.B else WeekTypeUi.A) }
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WeekOverrideScopeUi.entries.forEach { scope ->
                val label = when (scope) {
                    WeekOverrideScopeUi.ONE_WEEK -> "Tylko ten tydzień"
                    WeekOverrideScopeUi.FROM_WEEK -> "Od tego tygodnia w przyszłość"
                }
                MakChoiceRow(
                    label = label,
                    selected = state.scope == scope,
                    onClick = { onScopeChanged(scope) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MakPrimaryAction(
                text = if (state.isEditing) "Zapisz zmiany" else "Dodaj korektę",
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = !state.isSaving
            )
            if (state.isEditing) {
                MakSecondaryAction(
                    text = "Anuluj",
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isSaving
                )
            }
        }
    }
}
