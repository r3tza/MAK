package dev.retza.mak.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import dev.retza.mak.ui.components.FieldError
import dev.retza.mak.ui.components.FieldErrorUi
import dev.retza.mak.ui.components.MakColorPalette
import dev.retza.mak.ui.components.MakDatePickerField
import dev.retza.mak.ui.components.MakField
import dev.retza.mak.ui.components.MakPrimaryAction
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakSectionHeader
import dev.retza.mak.ui.components.MakSelectField
import dev.retza.mak.ui.components.MakStateMessage
import dev.retza.mak.ui.components.ScreenStatus

enum class SetupStep {
    Semester,
    Course,
    Classes
}

enum class SetupField {
    SemesterName,
    StartDate,
    EndDate,
    CourseName
}

data class SetupWizardUiState(
    val step: SetupStep = SetupStep.Semester,
    val semesterName: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val firstWeekLabel: String = "A",
    val courseName: String = "",
    val courseColor: String = "#137B71",
    val errors: Map<SetupField, FieldErrorUi> = emptyMap(),
    val status: ScreenStatus = ScreenStatus.Ready,
    val canSkipClasses: Boolean = true
)

@Composable
fun SetupWizard(
    state: SetupWizardUiState,
    onSemesterNameChanged: (String) -> Unit,
    onStartDateChanged: (String) -> Unit,
    onEndDateChanged: (String) -> Unit,
    onFirstWeekChanged: (String) -> Unit,
    onCourseNameChanged: (String) -> Unit,
    onCourseColorChanged: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onAddClass: () -> Unit,
    onFinish: () -> Unit,
    onReturnToSettings: () -> Unit = {},
    showReturnToSettings: Boolean = false,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (title, subtitle) = when (state.step) {
        SetupStep.Semester -> "Utwórz semestr" to "Krok 1 z 3, najpierw ustaw semestr, potem dodaj kierunek i zajęcia."
        SetupStep.Course -> "Dodaj kierunek" to "Krok 2 z 3. Kierunek oddziela zajęcia w planie."
        SetupStep.Classes -> "Dodaj zajęcia" to "Krok 3 z 3. Semestr i kierunek są gotowe."
    }
    MakScreenContent(modifier = modifier.verticalScroll(rememberScrollState())) {
        MakSectionHeader(
            eyebrow = "Konfiguracja początkowa",
            title = title,
            subtitle = subtitle
        )
        when (state.status) {
            ScreenStatus.Ready -> when (state.step) {
                SetupStep.Semester -> SemesterStep(
                    state = state,
                    onSemesterNameChanged = onSemesterNameChanged,
                    onStartDateChanged = onStartDateChanged,
                    onEndDateChanged = onEndDateChanged,
                    onFirstWeekChanged = onFirstWeekChanged,
                    onNext = onNext,
                    onReturnToSettings = onReturnToSettings,
                    showReturnToSettings = showReturnToSettings
                )

                SetupStep.Course -> CourseStep(
                    state = state,
                    onCourseNameChanged = onCourseNameChanged,
                    onCourseColorChanged = onCourseColorChanged,
                    onNext = onNext,
                    onBack = onBack
                )

                SetupStep.Classes -> ClassesStep(
                    canSkip = state.canSkipClasses,
                    onAddClass = onAddClass,
                    onFinish = onFinish,
                    onBack = onBack
                )
            }

            else -> MakStateMessage(status = state.status, onRetry = onRetry)
        }
    }
}

@Composable
private fun SemesterStep(
    state: SetupWizardUiState,
    onSemesterNameChanged: (String) -> Unit,
    onStartDateChanged: (String) -> Unit,
    onEndDateChanged: (String) -> Unit,
    onFirstWeekChanged: (String) -> Unit,
    onNext: () -> Unit,
    onReturnToSettings: () -> Unit,
    showReturnToSettings: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MakField(
            label = "Nazwa semestru",
            value = state.semesterName,
            onValueChange = onSemesterNameChanged,
            isError = state.errors.containsKey(SetupField.SemesterName)
        )
        FieldError(state.errors[SetupField.SemesterName])
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MakDatePickerField(
                    label = "Od",
                    value = state.startDate,
                    onValueChange = onStartDateChanged,
                    maxDate = state.endDate.toLocalDateOrNull(),
                    isError = state.errors.containsKey(SetupField.StartDate)
                )
                FieldError(state.errors[SetupField.StartDate])
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MakDatePickerField(
                    label = "Do",
                    value = state.endDate,
                    onValueChange = onEndDateChanged,
                    minDate = state.startDate.toLocalDateOrNull(),
                    isError = state.errors.containsKey(SetupField.EndDate)
                )
                FieldError(state.errors[SetupField.EndDate])
            }
        }
        MakSelectField(
            label = "Pierwszy tydzień",
            value = state.firstWeekLabel,
            options = listOf("A", "B"),
            onSelected = onFirstWeekChanged
        )
        MakPrimaryAction(text = "Utwórz semestr", onClick = onNext)
        if (showReturnToSettings) {
            MakSecondaryAction(text = "Wróć do ustawień", onClick = onReturnToSettings)
        }
    }
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

@Composable
private fun CourseStep(
    state: SetupWizardUiState,
    onCourseNameChanged: (String) -> Unit,
    onCourseColorChanged: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MakField(
            label = "Nazwa kierunku",
            value = state.courseName,
            onValueChange = onCourseNameChanged,
            isError = state.errors.containsKey(SetupField.CourseName)
        )
        FieldError(state.errors[SetupField.CourseName])
        MakColorPalette(
            selectedColor = state.courseColor,
            onColorSelected = onCourseColorChanged
        )
        MakPrimaryAction(text = "Zapisz kierunek", onClick = onNext)
        MakSecondaryAction(text = "Wstecz", onClick = onBack)
    }
}

@Composable
private fun ClassesStep(
    canSkip: Boolean,
    onAddClass: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MakPrimaryAction(text = "Dodaj zajęcia", onClick = onAddClass)
        if (canSkip) {
            MakSecondaryAction(text = "Przejdź do Dzisiaj", onClick = onFinish)
        }
        MakSecondaryAction(text = "Wstecz", onClick = onBack)
    }
}
