package dev.retza.mak.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.retza.mak.data.database.SemesterWithData
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
import dev.retza.mak.ui.components.ScreenStatus
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
import dev.retza.mak.ui.settings.SettingsViewModel
import dev.retza.mak.ui.settings.toId
import dev.retza.mak.ui.today.TodayUiState
import dev.retza.mak.ui.semester.WeekOverrideScopeUi
import dev.retza.mak.ui.semester.WeekTypeUi
import dev.retza.mak.data.entity.WeekOverrideEntity
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
    val hasLoadedData: Boolean = false,
    val today: TodayUiState = emptyTodayState(),
    val schedule: ScheduleUiState = emptyScheduleState(),
    val themeId: String = "system",
    val settings: SettingsUiState = SettingsUiState(),
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
    val showCancelled: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class MakViewModel(
    private val repository: MakRepository,
    private val feedbackSink: FeedbackSink,
    private val classEditViewModel: ClassEditViewModel,
    private val settingsViewModel: SettingsViewModel,
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

    val uiState = combine(
        semesters,
        activeSemesterData,
        controls,
        settingsViewModel.settings,
        settingsViewModel.themeMode
    ) { semesterList, activeData, control, settings, theme ->
        buildState(semesterList, activeData, control, settings, theme.toId())
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

    fun selectSemester(id: String) = settingsViewModel.selectSemester(id)

    fun requestSemesterDeletion(id: String) = settingsViewModel.requestSemesterDeletion(id)

    fun cancelSemesterDeletion() = settingsViewModel.cancelSemesterDeletion()

    fun confirmSemesterDeletion() = settingsViewModel.confirmSemesterDeletion()

    fun selectTheme(id: String) = settingsViewModel.selectTheme(id)

    fun exportJson(onReady: (ByteArray) -> Unit) = settingsViewModel.exportJson(onReady)

    private fun buildState(
        semesterList: List<SemesterEntity>,
        activeData: SemesterWithData?,
        control: Controls,
        settings: SettingsUiState,
        themeId: String
    ): MakUiState {
        val requiresSetup = semesterList.isEmpty() ||
            activeData != null && activeData.courses.isEmpty()
        val destination = if (requiresSetup) MakDestination.Setup else control.destination
        return MakUiState(
            destination = destination,
            requiresSetup = requiresSetup,
            hasLoadedData = true,
            today = buildToday(activeData, control.todayDate),
            schedule = buildSchedule(activeData, control),
            themeId = themeId,
            settings = settings,
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
        private val classEditViewModel: ClassEditViewModel,
        private val settingsViewModel: SettingsViewModel
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MakViewModel::class.java))
            return MakViewModel(repository, feedbackSink, classEditViewModel, settingsViewModel) as T
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
