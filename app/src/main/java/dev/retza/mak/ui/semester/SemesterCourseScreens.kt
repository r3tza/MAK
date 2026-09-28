package dev.retza.mak.ui.semester

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import dev.retza.mak.ui.components.FieldError
import dev.retza.mak.ui.components.FieldErrorUi
import dev.retza.mak.ui.components.MakChoiceRow
import dev.retza.mak.ui.components.MakColorDot
import dev.retza.mak.ui.components.MakCourseColorPicker
import dev.retza.mak.ui.components.MakField
import dev.retza.mak.ui.components.MakHelperText
import dev.retza.mak.ui.components.MakPrimaryAction
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakScreenIntro
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakSelectField
import dev.retza.mak.ui.components.MakSpacing
import dev.retza.mak.ui.components.distinctLabels
import dev.retza.mak.ui.programs.StudyProgramEditorUi

@Composable
fun SemesterCourseAddScreen(
    state: SemesterScreenUiState,
    onCourseNameChanged: (String) -> Unit,
    onCourseColorChanged: (String) -> Unit,
    onProgramModeChanged: (CourseProgramModeUi) -> Unit,
    onSelectProgram: (String?) -> Unit,
    onCourseModeChanged: (CourseCalendarModeUi) -> Unit,
    onCourseCalendarChanged: (String) -> Unit,
    onAddCourse: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    MakScreenContent(modifier = modifier.verticalScroll(rememberScrollState())) {
        MakScreenIntro("Wybierz istniejący kierunek albo utwórz nowy.")
        Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    MakChoiceRow(
                        label = "Nowy kierunek",
                        selected = state.courseProgramMode == CourseProgramModeUi.NEW,
                        onClick = { onProgramModeChanged(CourseProgramModeUi.NEW) }
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    MakChoiceRow(
                        label = "Wybierz istniejący",
                        selected = state.courseProgramMode == CourseProgramModeUi.EXISTING,
                        onClick = { onProgramModeChanged(CourseProgramModeUi.EXISTING) }
                    )
                }
            }
            if (state.courseProgramMode == CourseProgramModeUi.NEW) {
                MakField(
                    label = "Nazwa nowego kierunku",
                    value = state.courseNameDraft,
                    onValueChange = onCourseNameChanged,
                    isError = state.courseNameError != null
                )
                FieldError(state.courseNameError?.let(::FieldErrorUi))
                MakCourseColorPicker(
                    selectedColor = state.courseColorDraft,
                    previewName = state.courseNameDraft,
                    onColorSelected = onCourseColorChanged
                )
            } else {
                val programLabels = state.courseProgramOptions.map { it.id }
                    .zip(distinctLabels(state.courseProgramOptions.map { it.name }))
                    .toMap()
                MakSelectField(
                    label = "Istniejący kierunek",
                    value = state.courseProgramId?.let { programLabels[it] }.orEmpty(),
                    options = state.courseProgramOptions,
                    onSelected = { onSelectProgram(it.id) },
                    optionLabel = { programLabels[it.id].orEmpty() },
                    optionLeading = { MakColorDot(it.color) },
                    isError = state.courseNameError != null
                )
                FieldError(state.courseNameError?.let(::FieldErrorUi))
                MakHelperText("Nazwa i kolor istniejącego kierunku są wspólne dla wszystkich semestrów.")
            }

            MakChoiceRow(
                label = "Wspólne daty i tygodnie",
                selected = state.courseCalendarMode == CourseCalendarModeUi.SHARED,
                onClick = { onCourseModeChanged(CourseCalendarModeUi.SHARED) }
            )
            if (state.courseCalendarMode == CourseCalendarModeUi.SHARED && state.calendars.size > 1) {
                MakSelectField(
                    label = "Kalendarz kierunku",
                    value = state.calendars.firstOrNull { it.id == state.courseCalendarId }
                        ?.let(::calendarLabel).orEmpty(),
                    options = state.calendars,
                    onSelected = { onCourseCalendarChanged(it.id) },
                    optionLabel = { calendarLabel(it) }
                )
            }
            MakChoiceRow(
                label = "Osobne daty i tygodnie",
                selected = state.courseCalendarMode == CourseCalendarModeUi.SEPARATE,
                onClick = { onCourseModeChanged(CourseCalendarModeUi.SEPARATE) }
            )
            if (state.courseCalendarMode == CourseCalendarModeUi.SEPARATE) {
                MakHelperText("Powstanie kopia dat, rytmu i korekt wybranego kalendarza.")
            }

            val canAddCourse = when (state.courseProgramMode) {
                CourseProgramModeUi.NEW -> state.courseNameDraft.isNotBlank()
                CourseProgramModeUi.EXISTING -> state.courseProgramId != null
            }
            MakPrimaryAction(
                text = "Dodaj kierunek",
                onClick = onAddCourse,
                enabled = canAddCourse && !state.isAddingCourse
            )
            MakSecondaryAction(text = "Anuluj", onClick = onCancel, enabled = !state.isAddingCourse)
        }
    }
}

@Composable
fun SemesterCourseEditScreen(
    state: SemesterScreenUiState,
    editor: StudyProgramEditorUi,
    onNameChanged: (String) -> Unit,
    onColorChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onSeparateCourse: (String) -> Unit,
    onRequestReconnect: (String, String) -> Unit,
    onConfirmReconnect: () -> Unit,
    onCancelReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val course = state.courseItems.firstOrNull { it.programId == editor.id?.toString() }
    MakScreenContent(modifier = modifier.verticalScroll(rememberScrollState())) {
        Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.md)) {
            MakField(
                label = "Nazwa kierunku",
                value = editor.name,
                onValueChange = onNameChanged,
                isError = editor.nameError != null
            )
            FieldError(editor.nameError?.let(::FieldErrorUi))

            Text(
                text = "Kalendarz w tym semestrze",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = course?.let {
                    "${it.calendarLabel} (${if (it.sharesCalendar) "wspólny" else "osobny"})"
                } ?: "Ładowanie kalendarza",
                style = MaterialTheme.typography.bodyMedium
            )
            if (state.calendars.size > 1 && course != null) {
                MakSelectField(
                    label = "Kalendarz",
                    value = state.calendars.firstOrNull { it.id == course.calendarId }
                        ?.let(::calendarLabel).orEmpty(),
                    options = state.calendars,
                    onSelected = { onRequestReconnect(course.assignmentId, it.id) },
                    optionLabel = { calendarLabel(it) }
                )
            }
            if (course?.sharesCalendar == true) {
                MakSecondaryAction(
                    text = "Rozdziel kalendarz",
                    onClick = { onSeparateCourse(course.assignmentId) },
                    enabled = !state.isSeparatingCalendar
                )
                MakHelperText("Kierunek dostanie własną kopię dat, rytmu A/B i korekt.")
            }
            MakHelperText("Zmiana kalendarza zapisuje się od razu.")

            MakCourseColorPicker(
                selectedColor = editor.color,
                previewName = editor.name,
                onColorSelected = onColorChanged
            )
            MakHelperText("Nazwa i kolor zmienią się we wszystkich semestrach, w planie i w widgecie.")
            MakPrimaryAction(
                text = "Zapisz kierunek",
                onClick = onSave,
                enabled = editor.id != null && !editor.isSaving
            )
            MakSecondaryAction(text = "Anuluj", onClick = onCancel, enabled = !editor.isSaving)
        }
    }
    state.pendingReconnect?.let { reconnect ->
        ReconnectCalendarDialog(
            reconnect = reconnect,
            isSaving = state.isReconnectingCalendar,
            error = state.reconnectError,
            onConfirm = onConfirmReconnect,
            onCancel = onCancelReconnect
        )
    }
}
