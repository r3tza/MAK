package dev.retza.mak.ui.semester

import dev.retza.mak.ui.components.DefaultCourseColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.retza.mak.ui.components.FieldError
import dev.retza.mak.ui.components.FieldErrorUi
import dev.retza.mak.ui.components.MakActionMenu
import dev.retza.mak.ui.components.MakChoiceRow
import dev.retza.mak.ui.components.MakColorDot
import dev.retza.mak.ui.components.MakConfirmDeletionDialog
import dev.retza.mak.ui.components.MakDatePickerField
import dev.retza.mak.ui.components.MakEmptyState
import dev.retza.mak.ui.components.MakField
import dev.retza.mak.ui.components.MakFieldPair
import dev.retza.mak.ui.components.MakHelperText
import dev.retza.mak.ui.components.MakNoteBanner
import dev.retza.mak.ui.components.MakNoteRole
import dev.retza.mak.ui.components.MakPrimaryAction
import dev.retza.mak.ui.components.MakRowTitle
import dev.retza.mak.ui.components.MakScreenContent
import dev.retza.mak.ui.components.MakScreenIntro
import dev.retza.mak.ui.components.MakSecondaryAction
import dev.retza.mak.ui.components.MakSelectField
import dev.retza.mak.ui.components.distinctLabels
import dev.retza.mak.ui.components.MakSpacing
import dev.retza.mak.ui.components.MakStateMessage
import dev.retza.mak.ui.components.MakTextAction
import dev.retza.mak.ui.components.makRowFocus
import dev.retza.mak.ui.polishPlural
import dev.retza.mak.ui.components.ScreenStatus
import dev.retza.mak.ui.components.parseHexColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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

data class CourseDeletionUi(
    val assignmentId: String,
    val programName: String,
    val classCount: Int
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
    val courseColorDraft: String = DefaultCourseColor,
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
    val pendingCourseDeletion: CourseDeletionUi? = null,
    val pendingOverrideDeletion: WeekOverrideUi? = null,
    val pendingCalendarDeletion: SemesterCalendarUi? = null,
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
                            "Zmień daty na ekranie Kalendarze.",
                        role = MakNoteRole.Neutral
                    )
                }
                SemesterNavigationRow(
                    title = "Kierunki",
                    countLabel = state.courseItems.size.let {
                        "$it ${polishPlural(it, "kierunek", "kierunki", "kierunków")}"
                    },
                    onClick = onOpenCourses
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = MakSpacing.md),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SemesterNavigationRow(
                    title = "Korekty tygodni",
                    countLabel = state.overrideCount.let {
                        "$it ${polishPlural(it, "korekta", "korekty", "korekt")}"
                    },
                    onClick = onOpenOverrides
                )
                if (state.calendars.size > 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = MakSpacing.md),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    SemesterNavigationRow(
                        title = "Kalendarze",
                        countLabel = state.calendars.size.let {
                            "$it ${polishPlural(it, "kalendarz", "kalendarze", "kalendarzy")}"
                        },
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
    countLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .makRowFocus()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = MakSpacing.md, vertical = MakSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MakSpacing.sm)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = countLabel,
                style = MaterialTheme.typography.bodyMedium,
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
    onAddCourse: () -> Unit,
    onEditCourse: (String) -> Unit,
    onDeleteCourse: (String) -> Unit,
    onConfirmCourseDeletion: () -> Unit,
    onCancelCourseDeletion: () -> Unit,
    modifier: Modifier = Modifier
) {
    MakScreenContent(modifier = modifier.verticalScroll(rememberScrollState())) {
        MakScreenIntro("Kierunki mogą współdzielić kalendarz albo używać własnego.")
        CoursesBlock(
            state = state,
            onAddCourse = onAddCourse,
            onEditCourse = onEditCourse,
            onDeleteCourse = onDeleteCourse
        )
        state.pendingCourseDeletion?.let { deletion ->
            CourseDeletionDialog(
                deletion = deletion,
                isDeleting = state.isDeletingCourse,
                onConfirm = onConfirmCourseDeletion,
                onCancel = onCancelCourseDeletion
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
    onConfirmOverrideDeletion: () -> Unit,
    onCancelOverrideDeletion: () -> Unit,
    modifier: Modifier = Modifier
) {
    state.pendingOverrideDeletion?.let { override ->
        MakConfirmDeletionDialog(
            title = "Usunąć korektę tygodnia?",
            text = "Korekta tygodnia od ${override.weekStartDate.asLongDate()} zostanie usunięta. " +
                "Rytm A/B wróci do automatycznego wyliczenia.",
            isDeleting = state.isDeletingOverride,
            onConfirm = onConfirmOverrideDeletion,
            onCancel = onCancelOverrideDeletion
        )
    }
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
    onConfirmCalendarDeletion: () -> Unit,
    onCancelCalendarDeletion: () -> Unit,
    modifier: Modifier = Modifier
) {
    state.pendingCalendarDeletion?.let { calendar ->
        MakConfirmDeletionDialog(
            title = "Usunąć kalendarz?",
            text = "Kalendarz ${calendar.startDate.asCalendarDate()} - ${calendar.endDate.asCalendarDate()} " +
                "nie jest używany przez żaden kierunek i zostanie usunięty razem ze swoimi korektami.",
            isDeleting = state.isDeletingCalendar,
            onConfirm = onConfirmCalendarDeletion,
            onCancel = onCancelCalendarDeletion
        )
    }
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
                Text(
                    "${calendar.startDate.asCalendarDate()} - ${calendar.endDate.asCalendarDate()}",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Pierwszy tydzień: ${calendar.firstWeek.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
    onAddCourse: () -> Unit,
    onEditCourse: (String) -> Unit,
    onDeleteCourse: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(MakSpacing.md)
    ) {
        MakRowTitle(
            title = "Przypisane kierunki",
            meta = if (state.courseItems.isEmpty()) null else "${state.courseItems.size}"
        )
        MakSecondaryAction(
            text = "Dodaj kierunek",
            icon = Icons.Outlined.Add,
            onClick = onAddCourse
        )
        if (state.courseItems.isEmpty()) {
            MakEmptyState("Dodaj kierunek, aby przypisać zajęcia.")
        } else {
            state.courseItems.forEach { course ->
                CourseRow(
                    course = course,
                    isDeletingCourse = state.isDeletingCourse,
                    onEdit = { onEditCourse(course.assignmentId) },
                    onDelete = { onDeleteCourse(course.assignmentId) }
                )
            }
        }
    }
}

@Composable
private fun CourseRow(
    course: SemesterCourseUi,
    isDeletingCourse: Boolean,
    onEdit: () -> Unit,
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
            MakColorDot(course.color)
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
            Column {
                MakTextAction(text = "Edytuj", onClick = onEdit)
                MakTextAction(
                    text = "Usuń",
                    onClick = onDelete,
                    enabled = !isDeletingCourse,
                    destructive = true
                )
            }
        }
    }
}

@Composable
internal fun ReconnectCalendarDialog(
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

@Composable
private fun CourseDeletionDialog(
    deletion: CourseDeletionUi,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    MakConfirmDeletionDialog(
        title = "Usunąć kierunek z semestru?",
        text = courseDeletionMessage(deletion),
        isDeleting = isDeleting,
        onConfirm = onConfirm,
        onCancel = onCancel
    )
}

internal fun courseDeletionMessage(deletion: CourseDeletionUi): String = buildString {
    append("Kierunek ${deletion.programName} zostanie usunięty z tego semestru.")
    if (deletion.classCount > 0) {
        append(" Liczba usuwanych zajęć: ${deletion.classCount}. Razem z nimi znikną ich notatki i zmiany terminów.")
    }
    append(" Kierunek pozostanie dostępny w innych semestrach. Tej operacji nie można cofnąć.")
}

internal fun calendarLabel(calendar: SemesterCalendarUi): String {
    val courses = distinctLabels(calendar.courseNames)
    val coursePart = if (courses.isEmpty()) "brak kierunków" else courses.joinToString(", ")
    val range = "${calendar.startDate.asCalendarDate()} - ${calendar.endDate.asCalendarDate()}"
    return "$range, tydzień ${calendar.firstWeek.name}, $coursePart"
}

private fun String.asLongDate(): String =
    toLocalDateOrNull()?.format(polishShortDateFormatter) ?: this

private fun String.asCalendarDate(): String =
    toLocalDateOrNull()?.format(calendarDateFormatter) ?: this

private val calendarDateFormatter =
    java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.forLanguageTag("pl-PL"))

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
                Text("Tydzień ${item.weekType.name}", fontWeight = FontWeight.SemiBold)
                val weekStart = item.weekStartDate.toLocalDateOrNull()?.format(polishShortDateFormatter)
                    ?: item.weekStartDate
                Text(
                    text = when (item.scope) {
                        WeekOverrideScopeUi.ONE_WEEK -> "$weekStart, tylko ten tydzień"
                        WeekOverrideScopeUi.FROM_WEEK -> "Od $weekStart"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MakActionMenu(
                actions = listOf(
                    "Edytuj" to onEdit,
                    "Usuń" to onDelete
                )
            )
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
    Column(verticalArrangement = Arrangement.spacedBy(MakSpacing.md)) {
        Text(
            text = if (state.isEditing) "Edytuj korektę" else "Dodaj korektę",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() }
        )
        MakDatePickerField(
            label = "Dzień w tygodniu korekty",
            value = state.weekStartDate,
            onValueChange = onWeekStartDateChanged,
            isError = state.weekStartDateError != null
        )
        FieldError(state.weekStartDateError?.let(::FieldErrorUi))
        state.weekStartDate.toLocalDateOrNull()?.let { monday ->
            MakHelperText("Korekta obejmuje tydzień od poniedziałku ${monday.format(polishShortDateFormatter)}.")
        }
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
            MakSecondaryAction(
                text = "Anuluj",
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                enabled = !state.isSaving
            )
            MakPrimaryAction(
                text = if (state.isEditing) "Zapisz zmiany" else "Dodaj korektę",
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = !state.isSaving
            )
        }
    }
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

private val polishShortDateFormatter =
    DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("pl-PL"))
