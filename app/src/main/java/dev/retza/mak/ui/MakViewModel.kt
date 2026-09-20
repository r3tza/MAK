package dev.retza.mak.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.CourseEntity
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.repository.MakRepository
import dev.retza.mak.data.repository.toActivePlanData
import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.domain.OccurrenceChangeKind
import dev.retza.mak.domain.PlannedOccurrence
import dev.retza.mak.domain.Recurrence
import dev.retza.mak.ui.components.CalendarDayUi
import dev.retza.mak.ui.components.CalendarMarkerColor
import dev.retza.mak.ui.components.CalendarMarkerUi
import dev.retza.mak.ui.components.ClassItemUi
import dev.retza.mak.ui.components.FieldErrorUi
import dev.retza.mak.ui.components.ScreenStatus
import dev.retza.mak.ui.components.SemesterUi
import dev.retza.mak.ui.edit.ClassEditViewModel
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import dev.retza.mak.ui.schedule.ScheduleDayUi
import dev.retza.mak.ui.schedule.ScheduleFilterUi
import dev.retza.mak.ui.schedule.conflictLabels
import dev.retza.mak.ui.schedule.ScheduleUiState
import dev.retza.mak.ui.schedule.ScheduleView
import dev.retza.mak.ui.settings.SettingsUiState
import dev.retza.mak.ui.settings.ThemeOptionUi
import dev.retza.mak.ui.setup.SetupField
import dev.retza.mak.ui.setup.SetupStep
import dev.retza.mak.ui.setup.SetupWizardUiState
import dev.retza.mak.ui.today.TodayUiState
import dev.retza.mak.ui.semester.SemesterFormUiState
import dev.retza.mak.ui.semester.SemesterScreenUiState
import dev.retza.mak.ui.semester.WeekOverrideFormUiState
import dev.retza.mak.ui.semester.WeekOverrideScopeUi
import dev.retza.mak.ui.semester.WeekOverrideUi
import dev.retza.mak.ui.semester.WeekTypeUi
import dev.retza.mak.data.entity.WeekOverrideEntity
import dev.retza.mak.export.ExportSnapshot
import dev.retza.mak.export.JsonExportCodec
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

enum class MakDestination {
    Today,
    Schedule,
    EditClass,
    OccurrenceDetails,
    Semester,
    Settings,
    Setup
}

data class MakUiState(
    val destination: MakDestination = MakDestination.Today,
    val requiresSetup: Boolean = true,
    val today: TodayUiState = emptyTodayState(),
    val schedule: ScheduleUiState = emptyScheduleState(),
    val semester: SemesterScreenUiState = SemesterScreenUiState(),
    val themeId: String = "system",
    val settings: SettingsUiState = SettingsUiState(),
    val setup: SetupWizardUiState = SetupWizardUiState(),
    val activeSemesterData: SemesterWithData? = null
)

private data class Controls(
    val destination: MakDestination = MakDestination.Today,
    val scheduleDate: LocalDate,
    val calendarMonth: YearMonth,
    val calendarDate: LocalDate,
    val todayDate: LocalDate,
    val scheduleView: ScheduleView = ScheduleView.List,
    val courseFilterId: String = "all",
    val showCancelled: Boolean = false,
    val setup: SetupWizardUiState = SetupWizardUiState(),
    val forceSetup: Boolean = false,
    val semesterDraft: SemesterScreenUiState = SemesterScreenUiState(),
    val semesterEditId: Long? = null,
    val semesterToDeleteId: String? = null,
    val themeId: String = "system"
)

@OptIn(ExperimentalCoroutinesApi::class)
class MakViewModel(
    private val repository: MakRepository,
    private val feedbackSink: FeedbackSink,
    private val classEditViewModel: ClassEditViewModel,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val activePlanProvider: ActivePlanProvider = ActivePlanProvider()
) : ViewModel() {
    private val today = LocalDate.now(clock)
    private val controls = MutableStateFlow(
        Controls(
            scheduleDate = today,
            calendarMonth = YearMonth.from(today),
            calendarDate = today,
            todayDate = today
        )
    )

    private val semesters = repository.observeSemesters()
    private val activeSemesterData = repository.observeActiveSemester().flatMapLatest { semester ->
        if (semester == null) flowOf(null) else repository.observeSemesterData(semester.id)
    }

    val uiState = combine(semesters, activeSemesterData, controls) { semesterList, activeData, control ->
        buildState(semesterList, activeData, control)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MakUiState()
    )

    init {
        viewModelScope.launch {
            combine(repository.observeSemesters(), repository.observeActiveSemester()) { list, active ->
                list to active
            }.collect { (list, active) ->
                if (active == null && list.isNotEmpty()) {
                    repository.setActiveSemester(list.first().id)
                }
            }
        }
    }

    fun publishFeedback(kind: UiFeedbackKind, message: String) {
        feedbackSink.publish(UiFeedback(message, kind))
    }

    fun navigate(destination: MakDestination) {
        controls.update { it.copy(destination = destination) }
    }

    fun refreshToday() {
        controls.update { it.copy(todayDate = LocalDate.now(clock)) }
    }

    fun updateSetup(transform: (SetupWizardUiState) -> SetupWizardUiState) {
        controls.update { it.copy(setup = transform(it.setup)) }
    }

    fun setupNext() {
        val setup = uiState.value.setup
        when (setup.step) {
            SetupStep.Semester -> saveSetupSemester(setup)
            SetupStep.Course -> saveSetupCourse(setup)
            SetupStep.Classes -> finishSetup()
        }
    }

    fun setupBack() {
        controls.update { control ->
            val data = uiState.value.activeSemesterData
            val previous = when {
                data != null && data.courses.isNotEmpty() -> SetupStep.Classes
                data != null -> SetupStep.Course
                else -> SetupStep.Semester
            }
            control.copy(setup = control.setup.copy(step = previous, errors = emptyMap()))
        }
    }

    fun finishSetup() {
        controls.update { it.copy(forceSetup = false, destination = MakDestination.Today) }
    }

    fun cancelSetup() {
        controls.update { it.copy(forceSetup = false, destination = MakDestination.Settings) }
    }

    fun startSemesterSetup() {
        controls.update {
            it.copy(
                forceSetup = true,
                destination = MakDestination.Setup,
                setup = SetupWizardUiState()
            )
        }
    }

    fun openNewClassForSelectedCalendarDay() {
        classEditViewModel.openNew(controls.value.calendarDate)
    }

    fun selectScheduleView(view: ScheduleView) {
        controls.update { it.copy(scheduleView = view) }
    }

    fun changeWeek(amount: Long) {
        controls.update { it.copy(scheduleDate = it.scheduleDate.plusWeeks(amount)) }
    }

    fun selectScheduleDay(id: String) {
        id.toLocalDateOrNull()?.let { date -> controls.update { it.copy(scheduleDate = date) } }
    }

    fun selectCourseFilter(id: String) {
        controls.update { it.copy(courseFilterId = id) }
    }

    fun changeMonth(amount: Long) {
        controls.update { control ->
            val month = control.calendarMonth.plusMonths(amount)
            control.copy(calendarMonth = month, calendarDate = month.atDay(1))
        }
    }

    fun selectCalendarDay(id: String) {
        id.toLocalDateOrNull()?.let { date ->
            controls.update { it.copy(calendarDate = date, calendarMonth = YearMonth.from(date)) }
        }
    }

    fun setShowCancelled(value: Boolean) {
        controls.update { it.copy(showCancelled = value) }
    }

    fun saveVisibleWeekOverride(weekType: WeekTypeUi, scope: WeekOverrideScopeUi) {
        val data = uiState.value.activeSemesterData ?: return
        val monday = controls.value.scheduleDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val entityScope = dev.retza.mak.data.entity.WeekOverrideScope.valueOf(scope.name)
        val existing = data.weekOverrides.firstOrNull {
            it.weekStartDate == monday && it.scope == entityScope
        }
        viewModelScope.launch {
            repository.saveWeekOverride(
                WeekOverrideEntity(
                    id = existing?.id ?: 0,
                    semesterId = data.semester.id,
                    weekStartDate = monday,
                    weekType = dev.retza.mak.data.entity.WeekType.valueOf(weekType.name),
                    scope = entityScope
                )
            )
        }
    }

    fun clearVisibleWeekOverride(scope: WeekOverrideScopeUi) {
        val data = uiState.value.activeSemesterData ?: return
        val monday = controls.value.scheduleDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val entityScope = dev.retza.mak.data.entity.WeekOverrideScope.valueOf(scope.name)
        val existing = data.weekOverrides.firstOrNull {
            it.weekStartDate == monday && it.scope == entityScope
        } ?: return
        viewModelScope.launch { repository.deleteWeekOverride(existing.id) }
    }

    fun selectSemester(id: String) {
        id.toLongOrNull()?.let { semesterId ->
            controls.update { it.copy(courseFilterId = "all") }
            viewModelScope.launch { repository.setActiveSemester(semesterId) }
        }
    }

    fun requestSemesterDeletion(id: String) {
        controls.update { it.copy(semesterToDeleteId = id) }
    }

    fun cancelSemesterDeletion() {
        controls.update { it.copy(semesterToDeleteId = null) }
    }

    fun confirmSemesterDeletion() {
        val id = controls.value.semesterToDeleteId?.toLongOrNull() ?: return
        val remaining = uiState.value.settings.semesters.firstOrNull { it.id != id.toString() }
        viewModelScope.launch {
            repository.deleteSemester(id)
            if (remaining != null) repository.setActiveSemester(remaining.id.toLong())
            else repository.clearActiveSemester()
            controls.update {
                it.copy(
                    semesterToDeleteId = null,
                    destination = if (remaining == null) MakDestination.Setup else MakDestination.Settings,
                    forceSetup = remaining == null,
                    setup = if (remaining == null) SetupWizardUiState() else it.setup
                )
            }
        }
    }

    fun selectTheme(id: String) {
        controls.update { it.copy(themeId = id) }
    }

    fun openSemesterConfiguration(id: String) {
        val semesterId = id.toLongOrNull() ?: return
        viewModelScope.launch {
            repository.setActiveSemester(semesterId)
            val data = repository.observeSemesterData(semesterId).first() ?: return@launch
            controls.update {
                it.copy(
                    destination = MakDestination.Semester,
                    semesterEditId = semesterId,
                    semesterDraft = SemesterScreenUiState(
                        semester = SemesterFormUiState(
                            name = data.semester.name,
                            startDate = data.semester.startDate.toString(),
                            endDate = data.semester.endDate.toString(),
                            firstWeek = WeekTypeUi.valueOf(data.semester.firstWeekType.name)
                        ),
                        overrides = data.weekOverrides.map { override ->
                            WeekOverrideUi(
                                override.id.toString(),
                                override.weekStartDate.toString(),
                                WeekTypeUi.valueOf(override.weekType.name),
                                WeekOverrideScopeUi.valueOf(override.scope.name)
                            )
                        },
                        courses = data.courses.map { course -> course.id.toString() to course.name }
                    )
                )
            }
        }
    }

    fun updateSemester(transform: (SemesterScreenUiState) -> SemesterScreenUiState) {
        controls.update { it.copy(semesterDraft = transform(it.semesterDraft)) }
    }

    fun saveSemesterConfiguration() {
        val id = controls.value.semesterEditId ?: return
        val draft = controls.value.semesterDraft.semester
        val start = draft.startDate.toLocalDateOrNull()
        val end = draft.endDate.toLocalDateOrNull()
        if (draft.name.isBlank() || start == null || end == null || end.isBefore(start)) {
            updateSemester {
                it.copy(semester = draft.copy(
                    nameError = if (draft.name.isBlank()) "Podaj nazwę semestru." else null,
                    startDateError = if (start == null) "Podaj poprawną datę." else null,
                    endDateError = if (end == null) "Podaj poprawną datę." else null,
                    dateRangeError = if (start != null && end != null && end.isBefore(start)) "Koniec nie może być wcześniejszy od początku." else null
                ))
            }
            return
        }
        viewModelScope.launch {
            repository.saveSemester(
                SemesterEntity(id, draft.name.trim(), start, end, dev.retza.mak.data.entity.WeekType.valueOf(draft.firstWeek.name), true)
            )
            controls.update { it.copy(destination = MakDestination.Settings) }
        }
    }

    fun newWeekOverride() = updateSemester {
        it.copy(overrideForm = WeekOverrideFormUiState(isOpen = true))
    }

    fun editWeekOverride(id: String) {
        val item = controls.value.semesterDraft.overrides.firstOrNull { it.id == id } ?: return
        updateSemester {
            it.copy(
                overrideForm = WeekOverrideFormUiState(
                    id = item.id,
                    weekStartDate = item.weekStartDate,
                    weekType = item.weekType,
                    scope = item.scope,
                    isOpen = true
                )
            )
        }
    }

    fun cancelWeekOverrideEdit() = updateSemester {
        it.copy(overrideForm = WeekOverrideFormUiState())
    }

    fun saveWeekOverride() {
        val semesterId = controls.value.semesterEditId ?: return
        val form = controls.value.semesterDraft.overrideForm
        val date = form.weekStartDate.toLocalDateOrNull()
        if (date == null || date.dayOfWeek != DayOfWeek.MONDAY) {
            updateSemester { it.copy(overrideForm = form.copy(weekStartDateError = "Wybierz poniedziałek.")) }
            return
        }
        viewModelScope.launch {
            repository.saveWeekOverride(
                WeekOverrideEntity(
                    id = form.id?.toLongOrNull() ?: 0,
                    semesterId = semesterId,
                    weekStartDate = date,
                    weekType = dev.retza.mak.data.entity.WeekType.valueOf(form.weekType.name),
                    scope = dev.retza.mak.data.entity.WeekOverrideScope.valueOf(form.scope.name)
                )
            )
            updateSemester { it.copy(overrideForm = WeekOverrideFormUiState()) }
        }
    }

    fun deleteWeekOverride(id: String) {
        id.toLongOrNull()?.let { overrideId -> viewModelScope.launch { repository.deleteWeekOverride(overrideId) } }
        updateSemester { it.copy(overrideForm = WeekOverrideFormUiState()) }
    }

    fun addCourse() {
        val data = uiState.value.activeSemesterData ?: return
        val draft = controls.value.semesterDraft
        if (draft.courseNameDraft.isBlank()) return
        viewModelScope.launch {
            repository.saveCourse(
                CourseEntity(
                    semesterId = data.semester.id,
                    name = draft.courseNameDraft.trim(),
                    color = draft.courseColorDraft.ifBlank { "#137b71" }
                )
            )
            controls.update { it.copy(semesterDraft = it.semesterDraft.copy(courseNameDraft = "")) }
        }
    }

    fun deleteCourse(id: String) {
        id.toLongOrNull()?.let { courseId -> viewModelScope.launch { repository.deleteCourse(courseId) } }
    }

    fun exportJson(onReady: (ByteArray) -> Unit) {
        viewModelScope.launch {
            onReady(JsonExportCodec.encode(ExportSnapshot.from(repository.getAllSemesterData())))
        }
    }

    private fun saveSetupSemester(setup: SetupWizardUiState) {
        val start = setup.startDate.toLocalDateOrNull()
        val end = setup.endDate.toLocalDateOrNull()
        val errors = buildMap {
            if (setup.semesterName.isBlank()) put(SetupField.SemesterName, FieldErrorUi("Podaj nazwę semestru."))
            if (start == null) put(SetupField.StartDate, FieldErrorUi("Wybierz poprawną datę."))
            if (end == null || start != null && end.isBefore(start)) {
                put(SetupField.EndDate, FieldErrorUi("Data końca nie może być wcześniejsza od początku."))
            }
        }
        if (errors.isNotEmpty() || start == null || end == null) {
            controls.update { it.copy(setup = setup.copy(errors = errors)) }
            return
        }
        viewModelScope.launch {
            repository.saveSemester(
                SemesterEntity(
                    name = setup.semesterName.trim(),
                    startDate = start,
                    endDate = end,
                    firstWeekType = dev.retza.mak.data.entity.WeekType.valueOf(setup.firstWeekLabel),
                    isActive = true
                )
            )
            controls.update {
                it.copy(setup = setup.copy(step = SetupStep.Course, errors = emptyMap()))
            }
        }
    }

    private fun saveSetupCourse(setup: SetupWizardUiState) {
        val semesterId = uiState.value.activeSemesterData?.semester?.id
        val errors = if (setup.courseName.isBlank()) {
            mapOf(SetupField.CourseName to FieldErrorUi("Podaj nazwę kierunku."))
        } else emptyMap()
        if (semesterId == null || errors.isNotEmpty()) {
            controls.update { it.copy(setup = setup.copy(errors = errors)) }
            return
        }
        viewModelScope.launch {
            repository.saveCourse(
                CourseEntity(
                    semesterId = semesterId,
                    name = setup.courseName.trim(),
                    color = setup.courseColor.ifBlank { "#137b71" }
                )
            )
            controls.update {
                it.copy(setup = setup.copy(step = SetupStep.Classes, errors = emptyMap()))
            }
        }
    }

    private fun buildState(
        semesterList: List<SemesterEntity>,
        activeData: SemesterWithData?,
        control: Controls
    ): MakUiState {
        val requiresSetup = control.forceSetup || semesterList.isEmpty() ||
            activeData != null && activeData.courses.isEmpty()
        val destination = if (requiresSetup) MakDestination.Setup else control.destination
        return MakUiState(
            destination = destination,
            requiresSetup = requiresSetup,
            today = buildToday(activeData, control.todayDate),
            schedule = buildSchedule(activeData, control),
            themeId = control.themeId,
            semester = control.semesterDraft.copy(
                overrides = activeData?.weekOverrides?.map { override ->
                    WeekOverrideUi(
                        override.id.toString(),
                        override.weekStartDate.toString(),
                        WeekTypeUi.valueOf(override.weekType.name),
                        WeekOverrideScopeUi.valueOf(override.scope.name)
                    )
                }.orEmpty(),
                courses = activeData?.courses?.map { it.id.toString() to it.name }.orEmpty()
            ),
            settings = buildSettings(semesterList, activeData, control),
            setup = if (activeData != null && activeData.courses.isEmpty()) {
                control.setup.copy(step = SetupStep.Course)
            } else control.setup,
            activeSemesterData = activeData
        )
    }

    private fun buildToday(data: SemesterWithData?, date: LocalDate): TodayUiState {
        if (data == null) return emptyTodayState()
        val plan = activePlan(data, date)
        val schedule = plan.schedule
        val labels = conflictLabels(plan.collisions)
        return TodayUiState(
            dateLabel = date.format(todayTitleFormatter).replaceFirstChar { it.titlecase(polishLocale) },
            semesterLabel = data.semester.name,
            weekLabel = schedule.weekType?.let { "Tydzień ${it.name}" } ?: "Poza semestrem",
            summaryLabel = classCountLabel(schedule.occurrences.size),
            items = schedule.occurrences.map { it.toUi(labels[it.id]) }
        )
    }

    private fun buildSchedule(data: SemesterWithData?, control: Controls): ScheduleUiState {
        if (data == null) return emptyScheduleState()
        val selectedPlan = activePlan(data, control.scheduleDate)
        val selected = selectedPlan.schedule
        val filtered = selected.occurrences.filter {
            control.courseFilterId == "all" || it.classItem.courseId == control.courseFilterId
        }
        val cancelled = if (control.showCancelled) cancelledItems(data, control.scheduleDate) else emptyList()
        val labels = conflictLabels(selectedPlan.collisions)
        val monday = control.scheduleDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val currentWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val calendarDays = calendarDates(control.calendarMonth).map { date ->
            val occurrences = activePlan(data, date).schedule.occurrences.filter {
                control.courseFilterId == "all" || it.classItem.courseId == control.courseFilterId
            }
            CalendarDayUi(
                id = date.toString(),
                dayLabel = date.dayOfMonth.toString(),
                accessibilityLabel = calendarAccessibilityLabel(date, occurrences),
                isInCurrentMonth = YearMonth.from(date) == control.calendarMonth,
                isToday = date == today,
                isSelected = date == control.calendarDate,
                markers = occurrences.take(3).map {
                    CalendarMarkerUi(
                        id = it.id,
                        contentDescription = calendarOccurrenceLabel(it),
                        colorToken = markerColor(data, it)
                    )
                }
            )
        }
        val calendarPlan = activePlan(data, control.calendarDate)
        val calendarSchedule = calendarPlan.schedule
        val calendarFiltered = calendarSchedule.occurrences.filter {
            control.courseFilterId == "all" || it.classItem.courseId == control.courseFilterId
        }
        val calendarLabels = conflictLabels(calendarPlan.collisions)
        return ScheduleUiState(
            view = control.scheduleView,
            weekRangeLabel = "${monday.format(shortDateFormatter)} - ${monday.plusDays(6).format(shortDateFormatter)}",
            weekSubtitle = if (monday == currentWeekMonday) "Bieżący tydzień" else data.semester.name,
            weekTypeLabel = selected.weekType?.let { "Tydzień ${it.name}" } ?: "Poza semestrem",
            weekSourceLabel = if (selected.correction == null) "Wyliczony automatycznie" else "Korekta ręczna",
            weekType = selected.weekType?.let { WeekTypeUi.valueOf(it.name) },
            days = (0L..6L).map { offset ->
                val date = monday.plusDays(offset)
                ScheduleDayUi(
                    id = date.toString(),
                    shortLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, polishLocale),
                    dateLabel = date.dayOfMonth.toString(),
                    accessibilityLabel = date.format(fullDateFormatter),
                    isSelected = date == control.scheduleDate,
                    isEnabled = !date.isBefore(data.semester.startDate) && !date.isAfter(data.semester.endDate)
                )
            },
            filters = listOf(ScheduleFilterUi("all", "Wszystkie", control.courseFilterId == "all")) +
                data.courses.map {
                    ScheduleFilterUi(it.id.toString(), it.name, control.courseFilterId == it.id.toString())
                },
            selectedDayLabel = dayNames[control.scheduleDate.dayOfWeek].orEmpty(),
            selectedDayCountLabel = classCountLabel(filtered.size + cancelled.size),
            items = filtered.map { it.toUi(labels[it.id]) } + cancelled,
            calendarMonthLabel = control.calendarMonth.format(monthFormatter),
            calendarDays = calendarDays,
            calendarSelectedDayLabel = control.calendarDate.format(fullDateFormatter),
            calendarSelectedDayCountLabel = classCountLabel(calendarFiltered.size),
            calendarItems = calendarFiltered.map { it.toUi(calendarLabels[it.id]) } +
                if (control.showCancelled) cancelledItems(data, control.calendarDate) else emptyList(),
            showCancelled = control.showCancelled,
            hasOneWeekCorrection = data.weekOverrides.any {
                it.weekStartDate == monday && it.scope == dev.retza.mak.data.entity.WeekOverrideScope.ONE_WEEK
            },
            hasFromWeekCorrection = data.weekOverrides.any {
                it.weekStartDate == monday && it.scope == dev.retza.mak.data.entity.WeekOverrideScope.FROM_WEEK
            }
        )
    }

    private fun buildSettings(
        semesterList: List<SemesterEntity>,
        activeData: SemesterWithData?,
        control: Controls
    ): SettingsUiState = SettingsUiState(
        semesters = semesterList.map { semester ->
            val isActive = semester.id == activeData?.semester?.id
            SemesterUi(
                id = semester.id.toString(),
                name = semester.name,
                dateRangeLabel = "${semester.startDate.format(shortDateFormatter)} - ${semester.endDate.format(shortDateFormatter)}",
                firstWeekLabel = "Pierwszy tydzień ${semester.firstWeekType.name}",
                courseCountLabel = if (isActive) "${activeData.courses.size} kierunków" else "Dane odizolowane",
                classCountLabel = if (isActive) classCountLabel(activeData.classes.size) else "Osobny plan",
                isActive = isActive
            )
        },
        activeSemesterId = activeData?.semester?.id?.toString(),
        themeOptions = listOf(
            ThemeOptionUi("system", "Systemowy", control.themeId == "system"),
            ThemeOptionUi("light", "Jasny", control.themeId == "light"),
            ThemeOptionUi("dark", "Ciemny", control.themeId == "dark")
        ),
        semesterToDeleteId = control.semesterToDeleteId
    )

    private fun cancelledItems(data: SemesterWithData, date: LocalDate): List<ClassItemUi> =
        data.occurrenceChanges.filter {
            it.originalDate == date && it.kind == dev.retza.mak.data.entity.OccurrenceChangeKind.CANCELLED
        }.mapNotNull { change ->
            val item = data.classes.firstOrNull { it.id == change.classId } ?: return@mapNotNull null
            val course = data.courses.firstOrNull { it.id == item.courseId }
            val teacher = data.teachers.firstOrNull { it.id == item.teacherId }
            ClassItemUi(
                id = "${item.id}:$date",
                name = item.name,
                type = item.type,
                courseName = course?.name.orEmpty(),
                courseColor = course?.color,
                startTime = item.startTime.toString(),
                endTime = item.endTime.toString(),
                room = item.room?.trim()?.ifEmpty { null },
                building = item.building,
                teacherName = teacher?.name,
                note = item.classNote,
                statusBadge = "Odwołane",
                isCancelled = true
            )
        }

    private fun activePlan(data: SemesterWithData, date: LocalDate) =
        activePlanProvider.resolve(data.toActivePlanData(), date)

    class Factory(
        private val repository: MakRepository,
        private val feedbackSink: FeedbackSink,
        private val classEditViewModel: ClassEditViewModel
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MakViewModel::class.java))
            return MakViewModel(repository, feedbackSink, classEditViewModel) as T
        }
    }
}

private fun markerColor(
    data: SemesterWithData,
    occurrence: PlannedOccurrence
): CalendarMarkerColor {
    if (occurrence.occurrenceChange != null) return CalendarMarkerColor.Error
    val index = data.courses.indexOfFirst { it.id.toString() == occurrence.course?.id }
    return if (index > 0) CalendarMarkerColor.Secondary else CalendarMarkerColor.Primary
}

private fun calendarAccessibilityLabel(
    date: LocalDate,
    occurrences: List<PlannedOccurrence>
): String {
    val details = occurrences.take(3).joinToString("; ") { calendarOccurrenceLabel(it) }
    return buildString {
        append(date.format(fullDateFormatter))
        append(", ")
        append(classCountLabel(occurrences.size))
        if (details.isNotBlank()) {
            append(", ")
            append(details)
        }
    }
}

private fun calendarOccurrenceLabel(occurrence: PlannedOccurrence): String {
    val status = when {
        occurrence.occurrenceChange?.kind == OccurrenceChangeKind.CANCELLED -> ", odwołane"
        occurrence.occurrenceChange?.kind == OccurrenceChangeKind.MODIFIED -> ", zmienione"
        occurrence.classItem.recurrence == Recurrence.ONCE -> ", jednorazowe"
        else -> ""
    }
    return "${occurrence.name}, ${occurrence.startTime} - ${occurrence.endTime}$status"
}

private fun PlannedOccurrence.toUi(conflictLabel: String?): ClassItemUi {
    val cancelled = occurrenceChange?.kind == OccurrenceChangeKind.CANCELLED
    val modified = occurrenceChange?.kind == OccurrenceChangeKind.MODIFIED
    val oneOff = classItem.recurrence == Recurrence.ONCE
    return ClassItemUi(
        id = id,
        name = name,
        type = classItem.type,
        courseName = course?.name.orEmpty(),
        courseColor = course?.color,
        startTime = startTime.toString(),
        endTime = endTime.toString(),
        room = room?.trim()?.ifEmpty { null },
        building = building,
        teacherName = teacher?.name,
        weekLabel = when (classItem.recurrence) {
            Recurrence.A_WEEK -> "Tydzień A"
            Recurrence.B_WEEK -> "Tydzień B"
            Recurrence.ONCE -> "Jednorazowe"
            Recurrence.EVERY_WEEK -> null
        },
        note = occurrenceNoteBody ?: classNote,
        statusBadge = when {
            cancelled -> "Odwołane"
            oneOff -> "Jednorazowe"
            modified -> "Zmienione"
            else -> null
        },
        isCancelled = cancelled,
        isModified = modified,
        isOneOff = oneOff,
        conflictLabel = conflictLabel
    )
}

private fun emptyTodayState() = TodayUiState(
    dateLabel = "Brak aktywnego semestru",
    semesterLabel = "",
    weekLabel = "",
    summaryLabel = "0 zajęć"
)

private fun emptyScheduleState() = ScheduleUiState(
    weekRangeLabel = "",
    weekSubtitle = "",
    weekTypeLabel = "",
    weekSourceLabel = "",
    status = ScreenStatus.Ready
)

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

private fun String.toLocalTimeOrNull(): LocalTime? = runCatching { LocalTime.parse(this) }.getOrNull()

private fun calendarDates(month: YearMonth): List<LocalDate> {
    val first = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0L until 42L).map(first::plusDays)
}

private fun classCountLabel(count: Int): String = when {
    count == 1 -> "1 zajęcie"
    count in 2..4 -> "$count zajęcia"
    else -> "$count zajęć"
}

private val polishLocale = Locale.forLanguageTag("pl-PL")
private val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", polishLocale)
private val todayTitleFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", polishLocale)
private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", polishLocale)
private val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", polishLocale)
private val dayNames = linkedMapOf(
    DayOfWeek.MONDAY to "Poniedziałek",
    DayOfWeek.TUESDAY to "Wtorek",
    DayOfWeek.WEDNESDAY to "Środa",
    DayOfWeek.THURSDAY to "Czwartek",
    DayOfWeek.FRIDAY to "Piątek",
    DayOfWeek.SATURDAY to "Sobota",
    DayOfWeek.SUNDAY to "Niedziela"
)
