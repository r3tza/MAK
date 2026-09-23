package dev.retza.mak.ui.semester

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.ui.text.font.FontWeight
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import dev.retza.mak.ui.components.FieldError
import dev.retza.mak.ui.components.FieldErrorUi
import dev.retza.mak.ui.components.MakChoiceRow
import dev.retza.mak.ui.components.MakColorPalette
import dev.retza.mak.ui.components.MakDatePickerField
import dev.retza.mak.ui.components.MakEmptyState
import dev.retza.mak.ui.components.MakActionMenu
import dev.retza.mak.ui.components.MakField
import dev.retza.mak.ui.components.MakHelperText
import dev.retza.mak.ui.components.MakNoteBanner
import dev.retza.mak.ui.components.MakPrimaryAction
import dev.retza.mak.ui.components.MakRowTitle
import dev.retza.mak.ui.components.MakFieldPair
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakScreenIntro
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakSelectField
import dev.retza.mak.ui.components.MakSpacing
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
    val isSaving: Boolean = false,
    val isOpen: Boolean = false
) {
    val isEditing: Boolean
        get() = id != null
}

enum class CourseCalendarModeUi {
    SHARED,
    SEPARATE
}

enum class CourseProgramModeUi {
    NEW,
    EXISTING
}

data class SemesterCourseUi(
    val assignmentId: String,
    val programId: String,
    val name: String,
    val color: String,
    val calendarId: String,
    val calendarLabel: String,
    val sharesCalendar: Boolean
)

data class SemesterCalendarUi(
    val id: String,
    val startDate: String,
    val endDate: String,
    val firstWeek: WeekTypeUi,
    val courseNames: List<String>
)

data class SemesterProgramOptionUi(
    val id: String,
    val name: String,
    val color: String
)

data class ReconnectCalendarUi(
    val assignmentId: String,
    val calendarId: String,
    val programName: String,
    val sourceBecomesUnused: Boolean
)

data class SemesterScreenUiState(
    val semester: SemesterFormUiState = SemesterFormUiState(),
    val overrides: List<WeekOverrideUi> = emptyList(),
    val overrideForm: WeekOverrideFormUiState = WeekOverrideFormUiState(),
    val overrideCount: Int = 0,
    val courseItems: List<SemesterCourseUi> = emptyList(),
    val calendars: List<SemesterCalendarUi> = emptyList(),
    val selectedCalendarId: String? = null,
    val courseNameDraft: String = "",
    val courseColorDraft: String = "#137b71",
    val courseCalendarMode: CourseCalendarModeUi = CourseCalendarModeUi.SHARED,
    val courseCalendarId: String? = null,
    val courseProgramMode: CourseProgramModeUi = CourseProgramModeUi.NEW,
    val courseProgramId: String? = null,
    val courseProgramOptions: List<SemesterProgramOptionUi> = emptyList(),
    val courseNameError: String? = null,
    val isAddingCourse: Boolean = false,
    val isDeletingCourse: Boolean = false,
    val isDeletingOverride: Boolean = false,
    val isDeletingCalendar: Boolean = false,
    val isSeparatingCalendar: Boolean = false,
    val isReconnectingCalendar: Boolean = false,
    val pendingReconnect: ReconnectCalendarUi? = null,
    val reconnectError: String? = null,
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
    onOpenCourses: () -> Unit,
    onOpenOverrides: () -> Unit,
    onOpenCalendars: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    MakScreenContent(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        MakScreenIntro("Zakres dat, pierwszy tydzień A/B, kierunki i ręczne korekty tygodni.")

        when (state.status) {
            ScreenStatus.Ready -> {
                SemesterForm(
                    state = state.semester,
                    showDates = state.calendars.size <= 1,
                    onNameChanged = onSemesterNameChanged,
                    onStartDateChanged = onSemesterStartDateChanged,
                    onEndDateChanged = onSemesterEndDateChanged,
                    onFirstWeekChanged = onSemesterFirstWeekChanged,
                    onSave = onSaveSemester
                )
                if (state.calendars.size > 1) {
                    MakNoteBanner(
                        title = "Różne kalendarze",
                        subtitle = "Kierunki używają różnych zakresów dat i rytmów A/B. " +
                            "Zmień daty na ekranie Kalendarze."
                    )
                }
                SemesterNavigationRow(
                    title = "Kierunki",
                    description = "Kierunki i ich kalendarze.",
                    count = state.courseItems.size,
                    onClick = onOpenCourses
                )
                SemesterNavigationRow(
                    title = "Korekty tygodni",
                    description = "Ręczne oznaczenia tygodni A/B.",
                    count = state.overrideCount,
                    onClick = onOpenOverrides
                )
                if (state.calendars.size > 1) {
                    SemesterNavigationRow(
                        title = "Kalendarze",
                        description = "Zakresy dat i rytmy A/B kierunków.",
                        count = state.calendars.size,
                        onClick = onOpenCalendars
                    )
                }
            }

            else -> MakStateMessage(status = state.status, onRetry = onRetry)
        }
    }
}

@Composable
private fun SemesterNavigationRow(
    title: String,
    description: String,
    count: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = MakSpacing.md, vertical = MakSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                text = "$description Liczba: $count.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = "Otwórz $title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SemesterCoursesScreen(
    state: SemesterScreenUiState,
    onCourseNameChanged: (String) -> Unit,
    onCourseColorChanged: (String) -> Unit,
    onProgramModeChanged: (CourseProgramModeUi) -> Unit,
    onSelectProgram: (String?) -> Unit,
    onCourseModeChanged: (CourseCalendarModeUi) -> Unit,
    onCourseCalendarChanged: (String) -> Unit,
    onAddCourse: () -> Unit,
    onDeleteCourse: (String) -> Unit,
    onSeparateCourse: (String) -> Unit,
    onRequestReconnect: (String, String) -> Unit,
    onConfirmReconnect: () -> Unit,
    onCancelReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    MakScreenContent(modifier = modifier.verticalScroll(rememberScrollState())) {
        MakScreenIntro("Kierunki mogą współdzielić kalendarz albo używać własnego.")
        CoursesBlock(
            state = state,
            onCourseNameChanged = onCourseNameChanged,
            onCourseColorChanged = onCourseColorChanged,
            onProgramModeChanged = onProgramModeChanged,
            onSelectProgram = onSelectProgram,
            onCourseModeChanged = onCourseModeChanged,
            onCourseCalendarChanged = onCourseCalendarChanged,
            onAddCourse = onAddCourse,
            onDeleteCourse = onDeleteCourse,
            onSeparateCourse = onSeparateCourse,
            onRequestReconnect = onRequestReconnect
        )
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
}

@Composable
fun SemesterWeekOverridesScreen(
    state: SemesterScreenUiState,
    onCalendarSelected: (String) -> Unit,
    onWeekStartDateChanged: (String) -> Unit,
    onWeekTypeChanged: (WeekTypeUi) -> Unit,
    onScopeChanged: (WeekOverrideScopeUi) -> Unit,
    onNewOverride: () -> Unit,
    onEditOverride: (String) -> Unit,
    onSaveOverride: () -> Unit,
    onDeleteOverride: (String) -> Unit,
    onCancelOverrideEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    MakScreenContent(modifier = modifier.verticalScroll(rememberScrollState())) {
        MakScreenIntro("Ustaw oznaczenie tylko dla jednego tygodnia albo od wybranego tygodnia w przyszłość.")
        if (state.calendars.size > 1) {
            MakSelectField(
                label = "Kalendarz",
                value = state.calendars
                    .firstOrNull { it.id == state.selectedCalendarId }
                    ?.let(::calendarLabel)
                    .orEmpty(),
                options = state.calendars,
                onSelected = { onCalendarSelected(it.id) },
                optionLabel = { calendarLabel(it) }
            )
        }
        WeekOverridesSection(
            overrides = state.overrides,
            form = state.overrideForm,
            onWeekStartDateChanged = onWeekStartDateChanged,
            onWeekTypeChanged = onWeekTypeChanged,
            onScopeChanged = onScopeChanged,
            onNewOverride = onNewOverride,
            onEditOverride = onEditOverride,
            onSaveOverride = onSaveOverride,
            onDeleteOverride = onDeleteOverride,
            onCancelEdit = onCancelOverrideEdit
        )
    }
}

@Composable
fun SemesterCalendarsScreen(
    state: SemesterScreenUiState,
    onCalendarSelected: (String) -> Unit,
    onStartDateChanged: (String) -> Unit,
    onEndDateChanged: (String) -> Unit,
    onFirstWeekChanged: (WeekTypeUi) -> Unit,
    onSaveCalendar: () -> Unit,
    onDeleteCalendar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    MakScreenContent(modifier = modifier.verticalScroll(rememberScrollState())) {
        MakScreenIntro("Zakres dat i rytm A/B każdego kalendarza.")
        if (state.calendars.size > 1) {
            MakSelectField(
                label = "Edytowany kalendarz",
                value = state.calendars
                    .firstOrNull { it.id == state.selectedCalendarId }
                    ?.let(::calendarLabel)
                    .orEmpty(),
                options = state.calendars,
                onSelected = { onCalendarSelected(it.id) },
                optionLabel = { calendarLabel(it) }
            )
        }
        CalendarDateFields(
            state = state.semester,
            onStartDateChanged = onStartDateChanged,
            onEndDateChanged = onEndDateChanged,
            onFirstWeekChanged = onFirstWeekChanged
        )
        FieldError(state.semester.dateRangeError?.let(::FieldErrorUi))
        MakPrimaryAction(
            text = "Zapisz kalendarz",
            onClick = onSaveCalendar,
            enabled = !state.semester.isSaving
        )
        state.calendars.forEach { calendar ->
            CalendarCard(
                calendar = calendar,
                isDeleting = state.isDeletingCalendar,
                onDelete = { onDeleteCalendar(calendar.id) }
            )
        }
    }
}

@Composable
private fun CalendarCard(
    calendar: SemesterCalendarUi,
    isDeleting: Boolean,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${calendar.startDate} - ${calendar.endDate}", fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Pierwszy tydzień: ${calendar.firstWeek.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MakTag(text = "Tydzień ${calendar.firstWeek.name}")
        }
        Text(
            text = if (calendar.courseNames.isEmpty()) {
                "Brak przypisanych kierunków"
            } else {
                "Kierunki: ${calendar.courseNames.joinToString(", ")}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (calendar.courseNames.isEmpty()) {
            MakTextAction(text = "Usuń", onClick = onDelete, enabled = !isDeleting)
        }
    }
}

@Composable
private fun CalendarDateFields(
    state: SemesterFormUiState,
    onStartDateChanged: (String) -> Unit,
    onEndDateChanged: (String) -> Unit,
    onFirstWeekChanged: (WeekTypeUi) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.md)) {
        MakFieldPair(
            first = {
                Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)) {
                    MakDatePickerField(
                        label = "Od",
                        value = state.startDate,
                        onValueChange = onStartDateChanged,
                        maxDate = state.endDate.toLocalDateOrNull(),
                        isError = state.startDateError != null
                    )
                    FieldError(state.startDateError?.let(::FieldErrorUi))
                }
            },
            second = {
                Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.xs)) {
                    MakDatePickerField(
                        label = "Do",
                        value = state.endDate,
                        onValueChange = onEndDateChanged,
                        minDate = state.startDate.toLocalDateOrNull(),
                        isError = state.endDateError != null
                    )
                    FieldError(state.endDateError?.let(::FieldErrorUi))
                }
            }
        )
        MakSelectField(
            label = "Pierwszy tydzień",
            value = "Tydzień ${state.firstWeek.name}",
            options = listOf("Tydzień A", "Tydzień B"),
            onSelected = { onFirstWeekChanged(if (it.endsWith("B")) WeekTypeUi.B else WeekTypeUi.A) }
        )
    }
}

@Composable
private fun SemesterForm(
    state: SemesterFormUiState,
    showDates: Boolean,
    onNameChanged: (String) -> Unit,
    onStartDateChanged: (String) -> Unit,
    onEndDateChanged: (String) -> Unit,
    onFirstWeekChanged: (WeekTypeUi) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier.padding(bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.md)
    ) {
        MakField(
            label = "Nazwa semestru",
            value = state.name,
            onValueChange = onNameChanged,
            isError = state.nameError != null
        )
        FieldError(state.nameError?.let(::FieldErrorUi))
        if (showDates) {
            CalendarDateFields(
                state = state,
                onStartDateChanged = onStartDateChanged,
                onEndDateChanged = onEndDateChanged,
                onFirstWeekChanged = onFirstWeekChanged
            )
            FieldError(state.dateRangeError?.let(::FieldErrorUi))
        }
        MakPrimaryAction(
            text = "Zapisz semestr",
            onClick = onSave,
            enabled = !state.isSaving
        )
    }
}

@Composable
private fun CoursesBlock(
    state: SemesterScreenUiState,
    onCourseNameChanged: (String) -> Unit,
    onCourseColorChanged: (String) -> Unit,
    onProgramModeChanged: (CourseProgramModeUi) -> Unit,
    onSelectProgram: (String?) -> Unit,
    onCourseModeChanged: (CourseCalendarModeUi) -> Unit,
    onCourseCalendarChanged: (String) -> Unit,
    onAddCourse: () -> Unit,
    onDeleteCourse: (String) -> Unit,
    onSeparateCourse: (String) -> Unit,
    onRequestReconnect: (String, String) -> Unit
) {
    Column(
        modifier = Modifier.padding(bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.md)
    ) {
        MakRowTitle(
            title = "Przypisane kierunki",
            meta = if (state.courseItems.isEmpty()) "Brak" else "${state.courseItems.size}"
        )
        if (state.courseItems.isEmpty()) {
            MakEmptyState("Dodaj kierunek, aby przypisać zajęcia.")
        } else {
            state.courseItems.forEach { course ->
                CourseRow(
                    course = course,
                    calendars = state.calendars,
                    canReconnect = state.calendars.size > 1,
                    isDeletingCourse = state.isDeletingCourse,
                    isSeparatingCalendar = state.isSeparatingCalendar,
                    onDelete = { onDeleteCourse(course.assignmentId) },
                    onSeparate = { onSeparateCourse(course.assignmentId) },
                    onReconnect = { calendarId -> onRequestReconnect(course.assignmentId, calendarId) }
                )
            }
        }

        MakNoteBanner(
            title = "Kierunek w semestrze",
            subtitle = "Wybierz istniejący kierunek albo utwórz nowy."
        )
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
            MakColorPalette(
                selectedColor = state.courseColorDraft,
                onColorSelected = onCourseColorChanged
            )
        } else {
            MakSelectField(
                label = "Istniejący kierunek",
                value = state.courseProgramOptions
                    .firstOrNull { it.id == state.courseProgramId }
                    ?.name
                    .orEmpty(),
                options = state.courseProgramOptions,
                onSelected = { onSelectProgram(it.id) },
                optionLabel = { it.name },
                isError = state.courseNameError != null
            )
            FieldError(state.courseNameError?.let(::FieldErrorUi))
            MakHelperText(
                "Kierunek jest współdzielony między semestrami. Nazwę i kolor zmienia się w kierunku, nie tutaj."
            )
        }

        MakChoiceRow(
            label = "Wspólne daty i tygodnie",
            selected = state.courseCalendarMode == CourseCalendarModeUi.SHARED,
            onClick = { onCourseModeChanged(CourseCalendarModeUi.SHARED) }
        )
        if (state.courseCalendarMode == CourseCalendarModeUi.SHARED && state.calendars.size > 1) {
            MakSelectField(
                label = "Kalendarz kierunku",
                value = state.calendars
                    .firstOrNull { it.id == state.courseCalendarId }
                    ?.let(::calendarLabel)
                    .orEmpty(),
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
        MakSecondaryAction(
            text = "Dodaj kierunek",
            onClick = onAddCourse,
            enabled = canAddCourse && !state.isAddingCourse
        )
    }
}

@Composable
private fun CourseRow(
    course: SemesterCourseUi,
    calendars: List<SemesterCalendarUi>,
    canReconnect: Boolean,
    isDeletingCourse: Boolean,
    isSeparatingCalendar: Boolean,
    onDelete: () -> Unit,
    onSeparate: () -> Unit,
    onReconnect: (String) -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(course.name, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (course.sharesCalendar) {
                        "${course.calendarLabel} (wspólny)"
                    } else {
                        "${course.calendarLabel} (osobny)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MakTextAction(
                text = "Usuń",
                onClick = onDelete,
                enabled = !isDeletingCourse
            )
        }
        if (canReconnect) {
            MakSelectField(
                label = "Kalendarz",
                value = calendars
                    .firstOrNull { it.id == course.calendarId }
                    ?.let(::calendarLabel)
                    .orEmpty(),
                options = calendars,
                onSelected = { onReconnect(it.id) },
                optionLabel = { calendarLabel(it) }
            )
        }
        if (course.sharesCalendar) {
            MakTextAction(
                text = "Rozdziel kalendarz",
                onClick = onSeparate,
                enabled = !isSeparatingCalendar
            )
        }
    }
}

@Composable
private fun ReconnectCalendarDialog(
    reconnect: ReconnectCalendarUi,
    isSaving: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isSaving) onCancel() },
        title = { Text("Połączyć kalendarze?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)) {
                Text("Kierunek ${reconnect.programName} będzie używał wybranego kalendarza.")
                if (reconnect.sourceBecomesUnused) {
                    Text(
                        "Dotychczasowy kalendarz nie będzie już używany i zostanie usunięty " +
                            "razem ze swoimi korektami. Korekty nie zostaną przeniesione."
                    )
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isSaving) { Text("Połącz") }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !isSaving) { Text("Anuluj") }
        }
    )
}

private fun calendarLabel(calendar: SemesterCalendarUi): String {
    val courses = uniqueCourseNames(calendar.courseNames)
    val coursePart = if (courses.isEmpty()) "brak kierunków" else courses.joinToString(", ")
    return "${calendar.startDate} - ${calendar.endDate}, tydzień ${calendar.firstWeek.name}, $coursePart"
}

private fun uniqueCourseNames(names: List<String>): List<String> {
    val totals = names.groupingBy { it }.eachCount()
    val seen = mutableMapOf<String, Int>()
    return names.map { name ->
        if (totals.getValue(name) > 1) {
            val index = (seen[name] ?: 0) + 1
            seen[name] = index
            "$name ($index)"
        } else {
            name
        }
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
    Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Zapisane korekty", fontWeight = FontWeight.SemiBold, fontSize = MaterialTheme.typography.titleMedium.fontSize)
            MakTextAction(text = "Dodaj", onClick = onNewOverride)
        }
        MakHelperText("Każda korekta dotyczy poniedziałku wybranego tygodnia.")
        if (form.isOpen && form.id == null) {
            WeekOverrideForm(
                state = form,
                onWeekStartDateChanged = onWeekStartDateChanged,
                onWeekTypeChanged = onWeekTypeChanged,
                onScopeChanged = onScopeChanged,
                onSave = onSaveOverride,
                onCancel = onCancelEdit
            )
        }
        if (overrides.isEmpty()) {
            MakEmptyState("Automatyczne oznaczenie tygodni A/B działa bez ręcznych zmian.")
        } else {
            overrides.forEach { override ->
                WeekOverrideCard(
                    item = override,
                    onEdit = { onEditOverride(override.id) },
                    onDelete = { onDeleteOverride(override.id) }
                )
                if (form.isOpen && form.id == override.id) {
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
        }
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
        verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.weekStartDate.toLocalDateOrNull()?.format(polishShortDateFormatter)
                        ?: item.weekStartDate,
                    fontWeight = FontWeight.SemiBold
                )
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
        MakActionMenu(
            actions = listOf(
                "Edytuj" to onEdit,
                "Usuń" to onDelete
            )
        )
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
    Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.md)) {
        MakNoteBanner(
            title = if (state.isEditing) "Edytuj korektę" else "Dodaj korektę",
            subtitle = "Oznaczenie A albo B dla wybranego poniedziałku."
        )
        MakDatePickerField(
            label = "Poniedziałek tygodnia",
            value = state.weekStartDate,
            onValueChange = onWeekStartDateChanged,
            isError = state.weekStartDateError != null
        )
        FieldError(state.weekStartDateError?.let(::FieldErrorUi))
        MakSelectField(
            label = "Oznaczenie tygodnia",
            value = "Tydzień ${state.weekType.name}",
            options = listOf("Tydzień A", "Tydzień B"),
            onSelected = { onWeekTypeChanged(if (it.endsWith("B")) WeekTypeUi.B else WeekTypeUi.A) }
        )
        Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.sm)) {
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
            horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm)
        ) {
            MakPrimaryAction(
                text = if (state.isEditing) "Zapisz zmiany" else "Dodaj korektę",
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = !state.isSaving
            )
            MakSecondaryAction(
                text = "Anuluj",
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                enabled = !state.isSaving
            )
        }
    }
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

private val polishShortDateFormatter =
    DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("pl-PL"))
