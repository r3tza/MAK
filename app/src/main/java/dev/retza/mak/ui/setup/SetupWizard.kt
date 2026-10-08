package dev.retza.mak.ui.setup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import dev.retza.mak.ui.components.FieldError
import dev.retza.mak.ui.components.MakCourseColorPicker
import dev.retza.mak.ui.components.MakChoiceRow
import dev.retza.mak.ui.components.MakColorDot
import dev.retza.mak.ui.components.MakHelperText
import dev.retza.mak.ui.components.MakNoteBanner
import dev.retza.mak.ui.components.MakNoteRole
import dev.retza.mak.ui.components.MakDatePickerField
import dev.retza.mak.ui.components.MakField
import dev.retza.mak.ui.components.MakPrimaryAction
import dev.retza.mak.ui.components.MakFieldPair
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakSectionHeader
import dev.retza.mak.ui.components.MakSelectField
import dev.retza.mak.ui.components.MakTextAction
import dev.retza.mak.ui.components.MakStateMessage
import dev.retza.mak.ui.components.ScreenStatus
import dev.retza.mak.ui.components.distinctLabels

@Composable
fun SetupWizard(
    state: SetupWizardUiState,
    onSemesterNameChanged: (String) -> Unit,
    onStartDateChanged: (String) -> Unit,
    onEndDateChanged: (String) -> Unit,
    onFirstWeekChanged: (String) -> Unit,
    onCourseNameChanged: (String) -> Unit,
    onCourseColorChanged: (String) -> Unit,
    onProgramModeChanged: (SetupProgramMode) -> Unit,
    onProgramSelected: (Long) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onAddClass: () -> Unit,
    onActivateAndAddClass: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    onReturnToSettings: () -> Unit = {},
    showReturnToSettings: Boolean = false,
    onRetry: () -> Unit,
    onAddAnotherProgram: () -> Unit = {},
    onCalendarModeChanged: (SetupCalendarMode) -> Unit = {}
) {
    val (title, subtitle) = when (state.step) {
        SetupStep.Semester -> "Utwórz semestr" to "Krok 1 z 3, najpierw ustaw semestr, potem dodaj kierunek i zajęcia."
        SetupStep.Course -> if (state.isAddingAnotherProgram) {
            "Dodaj kolejny kierunek" to "Krok 2 z 3. Kierunek dołączy do semestru „${state.semesterName.trim()}”."
        } else {
            "Dodaj kierunek" to "Krok 2 z 3. Kierunek oddziela zajęcia w planie."
        }
        SetupStep.Classes -> if (state.semesterProgramNames.size > 1) {
            "Dodaj zajęcia" to "Krok 3 z 3. Semestr i kierunki są gotowe."
        } else {
            "Dodaj zajęcia" to "Krok 3 z 3. Semestr i kierunek są gotowe."
        }
    }
    // The system back gesture does what the step's visible exit action does. On the first step
    // without "Wróć do ustawień" it stays with navigation, which closes the wizard.
    val exitsWizard = state.step == SetupStep.Semester && !state.isAddingAnotherProgram
    BackHandler(enabled = state.status == ScreenStatus.Ready && (!exitsWizard || showReturnToSettings)) {
        when {
            state.isSaving || state.isActivating -> Unit
            exitsWizard -> onReturnToSettings()
            else -> onBack()
        }
    }
    MakScreenContent(modifier = modifier.verticalScroll(rememberScrollState())) {
        MakSectionHeader(
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
                    onProgramModeChanged = onProgramModeChanged,
                    onProgramSelected = onProgramSelected,
                    onCalendarModeChanged = onCalendarModeChanged,
                    onNext = onNext,
                    onBack = onBack
                )

                SetupStep.Classes -> if (state.isSemesterActive) {
                    ClassesStep(
                        programNames = state.semesterProgramNames,
                        canSkip = state.canSkipClasses,
                        onAddClass = onAddClass,
                        onAddAnotherProgram = onAddAnotherProgram,
                        onFinish = onFinish,
                        onBack = onBack
                    )
                } else {
                    InactiveSemesterClassesStep(
                        programNames = state.semesterProgramNames,
                        isActivating = state.isActivating,
                        onActivateAndAddClass = onActivateAndAddClass,
                        onAddAnotherProgram = onAddAnotherProgram,
                        onFinish = onReturnToSettings,
                        onBack = onBack
                    )
                }
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
        MakFieldPair(
            first = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MakDatePickerField(
                        label = "Od",
                        value = state.startDate,
                        onValueChange = onStartDateChanged,
                        maxDate = state.endDate.toLocalDateOrNull(),
                        isError = state.errors.containsKey(SetupField.StartDate)
                    )
                    FieldError(state.errors[SetupField.StartDate])
                }
            },
            second = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
        )
        MakSelectField(
            label = "Pierwszy tydzień",
            value = "Tydzień ${state.firstWeekLabel}",
            options = listOf("Tydzień A", "Tydzień B"),
            // The wizard state keeps only the letter.
            onSelected = { onFirstWeekChanged(it.last().toString()) }
        )
        MakPrimaryAction(text = "Utwórz semestr", onClick = onNext)
        if (showReturnToSettings) {
            MakTextAction(text = "Wróć do ustawień", onClick = onReturnToSettings)
        }
    }
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

@Composable
private fun CourseStep(
    state: SetupWizardUiState,
    onCourseNameChanged: (String) -> Unit,
    onCourseColorChanged: (String) -> Unit,
    onProgramModeChanged: (SetupProgramMode) -> Unit,
    onProgramSelected: (Long) -> Unit,
    onCalendarModeChanged: (SetupCalendarMode) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val programOptions = state.availableProgramOptions
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (programOptions.isNotEmpty() && !state.isProgramChoiceLocked) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MakChoiceRow(
                    label = "Nowy kierunek",
                    selected = state.programMode == SetupProgramMode.New,
                    onClick = { onProgramModeChanged(SetupProgramMode.New) },
                    modifier = Modifier.weight(1f)
                )
                MakChoiceRow(
                    label = "Wybierz istniejący",
                    selected = state.programMode == SetupProgramMode.Existing,
                    onClick = { onProgramModeChanged(SetupProgramMode.Existing) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        when (state.programMode) {
            SetupProgramMode.New -> {
                MakField(
                    label = "Nazwa kierunku",
                    value = state.courseName,
                    onValueChange = onCourseNameChanged,
                    isError = state.errors.containsKey(SetupField.CourseName)
                )
                FieldError(state.errors[SetupField.CourseName])
                MakCourseColorPicker(
                    selectedColor = state.courseColor,
                    previewName = state.courseName,
                    onColorSelected = onCourseColorChanged
                )
            }

            SetupProgramMode.Existing -> {
                val selected = programOptions.firstOrNull { it.id == state.selectedProgramId }
                if (state.isProgramChoiceLocked) {
                    MakNoteBanner(
                        title = "Kierunek: ${selected?.name.orEmpty()}",
                        subtitle = LOCKED_PROGRAM_NOTE,
                        role = MakNoteRole.Neutral
                    )
                } else {
                    val labels = programOptions.map { it.id }
                        .zip(distinctLabels(programOptions.map { it.name }))
                        .toMap()
                    MakSelectField(
                        label = "Istniejący kierunek",
                        value = selected?.let { labels[it.id] }.orEmpty(),
                        options = programOptions,
                        onSelected = { onProgramSelected(it.id) },
                        optionLabel = { labels[it.id].orEmpty() },
                        optionLeading = { MakColorDot(it.color) },
                        isError = state.errors.containsKey(SetupField.CourseProgram)
                    )
                    FieldError(state.errors[SetupField.CourseProgram])
                    MakHelperText(
                        "Kierunek jest współdzielony między semestrami, więc jego nazwa i kolor się nie zmienią."
                    )
                }
            }
        }
        if (state.isProgramChoiceLocked && state.programMode == SetupProgramMode.New) {
            MakHelperText(LOCKED_PROGRAM_NOTE)
        }
        if (state.isAddingAnotherProgram) {
            CalendarModeChoice(selected = state.calendarMode, onSelected = onCalendarModeChanged)
        }
        MakPrimaryAction(text = "Zapisz kierunek", onClick = onNext, enabled = !state.isSaving)
        MakTextAction(text = "Wstecz", onClick = onBack, enabled = !state.isSaving)
    }
}

private const val LOCKED_PROGRAM_NOTE =
    "Kierunek jest już zapisany w tym semestrze. Kolejny kierunek dodasz w następnym kroku."

@Composable
private fun CalendarModeChoice(
    selected: SetupCalendarMode,
    onSelected: (SetupCalendarMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Tygodnie A/B",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        MakChoiceRow(
            label = "Wspólne z pierwszym kierunkiem",
            selected = selected == SetupCalendarMode.Shared,
            onClick = { onSelected(SetupCalendarMode.Shared) }
        )
        MakChoiceRow(
            label = "Osobne dla tego kierunku",
            selected = selected == SetupCalendarMode.Separate,
            onClick = { onSelected(SetupCalendarMode.Separate) }
        )
        MakHelperText(
            when (selected) {
                SetupCalendarMode.Shared -> "Kierunki mają ten sam rytm A/B i te same korekty tygodni."
                SetupCalendarMode.Separate ->
                    "Kierunek dostaje własny kalendarz z tymi samymi datami. Jego tygodnie A/B skorygujesz osobno w ustawieniach semestru."
            }
        )
    }
}

@Composable
private fun SemesterProgramsNote(programNames: List<String>) {
    if (programNames.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = if (programNames.size == 1) "Kierunek w semestrze" else "Kierunki w semestrze",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = programNames.joinToString(", "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InactiveSemesterClassesStep(
    programNames: List<String>,
    isActivating: Boolean,
    onActivateAndAddClass: () -> Unit,
    onAddAnotherProgram: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MakNoteBanner(
            title = "Semestr zapisany",
            subtitle = "Zacznie obowiązywać, gdy wybierzesz go jako aktywny w ustawieniach.",
            role = MakNoteRole.Neutral
        )
        SemesterProgramsNote(programNames)
        MakPrimaryAction(
            text = "Ustaw jako aktywny i dodaj zajęcia",
            onClick = onActivateAndAddClass,
            enabled = !isActivating
        )
        MakSecondaryAction(text = "Dodaj kolejny kierunek", onClick = onAddAnotherProgram, enabled = !isActivating)
        MakSecondaryAction(text = "Zakończ", onClick = onFinish, enabled = !isActivating)
        MakTextAction(text = "Wstecz", onClick = onBack, enabled = !isActivating)
    }
}

@Composable
private fun ClassesStep(
    programNames: List<String>,
    canSkip: Boolean,
    onAddClass: () -> Unit,
    onAddAnotherProgram: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SemesterProgramsNote(programNames)
        MakPrimaryAction(text = "Dodaj zajęcia", onClick = onAddClass)
        MakSecondaryAction(text = "Dodaj kolejny kierunek", onClick = onAddAnotherProgram)
        if (canSkip) {
            MakSecondaryAction(text = "Przejdź do Dzisiaj", onClick = onFinish)
        }
        MakTextAction(text = "Wstecz", onClick = onBack)
    }
}
