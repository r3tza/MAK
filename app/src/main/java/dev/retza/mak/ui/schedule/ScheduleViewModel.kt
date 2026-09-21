package dev.retza.mak.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.koin.core.annotation.KoinViewModel
import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.WeekOverrideEntity
import dev.retza.mak.data.entity.WeekOverrideScope
import dev.retza.mak.data.entity.WeekType
import dev.retza.mak.data.repository.MakRepository
import dev.retza.mak.data.repository.toActivePlanData
import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.domain.OccurrenceChangeKind
import dev.retza.mak.domain.PlannedOccurrence
import dev.retza.mak.domain.Recurrence
import dev.retza.mak.ui.calendarForAssignment
import dev.retza.mak.ui.classCountLabel
import dev.retza.mak.ui.components.CalendarDayUi
import dev.retza.mak.ui.components.CalendarMarkerColor
import dev.retza.mak.ui.components.CalendarMarkerUi
import dev.retza.mak.ui.components.ClassItemUi
import dev.retza.mak.ui.components.ScreenStatus
import dev.retza.mak.ui.dayNames
import dev.retza.mak.ui.fullDateFormatter
import dev.retza.mak.ui.monthFormatter
import dev.retza.mak.ui.polishLocale
import dev.retza.mak.ui.semester.WeekOverrideScopeUi
import dev.retza.mak.ui.semester.WeekTypeUi
import dev.retza.mak.ui.shortDateFormatter
import dev.retza.mak.ui.toUi
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ScheduleEffect {
    data class OpenNewClassEditor(val date: LocalDate) : ScheduleEffect
}

private data class ScheduleControls(
    val scheduleDate: LocalDate,
    val calendarMonth: YearMonth,
    val calendarDate: LocalDate,
    val scheduleView: ScheduleView = ScheduleView.List,
    val courseFilterId: String = "all",
    val showCancelled: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class ScheduleViewModel(
    private val repository: MakRepository,
    private val clock: Clock,
    private val activePlanProvider: ActivePlanProvider
) : ViewModel() {
    private val today = LocalDate.now(clock)
    private val controls = MutableStateFlow(
        ScheduleControls(
            scheduleDate = today,
            calendarMonth = YearMonth.from(today),
            calendarDate = today
        )
    )

    private val activeSemesterData = repository.observeActiveSemester().flatMapLatest { semester ->
        if (semester == null) flowOf(null) else repository.observeSemesterData(semester.id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val schedule: StateFlow<ScheduleUiState> = combine(activeSemesterData, controls) { data, control ->
        buildSchedule(data, control)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyScheduleState()
    )

    private val effectsChannel = Channel<ScheduleEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    fun selectView(view: ScheduleView) = controls.update { it.copy(scheduleView = view) }

    fun changeWeek(amount: Long) = controls.update { it.copy(scheduleDate = it.scheduleDate.plusWeeks(amount)) }

    fun selectDay(id: String) {
        id.toLocalDateOrNull()?.let { date -> controls.update { it.copy(scheduleDate = date) } }
    }

    fun selectCourseFilter(id: String) = controls.update { it.copy(courseFilterId = id) }

    fun changeMonth(amount: Long) = controls.update { control ->
        val month = control.calendarMonth.plusMonths(amount)
        control.copy(calendarMonth = month, calendarDate = month.atDay(1))
    }

    fun selectCalendarDay(id: String) {
        id.toLocalDateOrNull()?.let { date ->
            controls.update { it.copy(calendarDate = date, calendarMonth = YearMonth.from(date)) }
        }
    }

    fun setShowCancelled(value: Boolean) = controls.update { it.copy(showCancelled = value) }

    fun openNewClassForSelectedCalendarDay() {
        effectsChannel.trySend(ScheduleEffect.OpenNewClassEditor(controls.value.calendarDate))
    }

    fun saveVisibleWeekOverride(weekType: WeekTypeUi, scope: WeekOverrideScopeUi) {
        val data = activeSemesterData.value ?: return
        val calendarId = visibleWeekCalendarId(data) ?: return
        val monday = controls.value.scheduleDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val entityScope = WeekOverrideScope.valueOf(scope.name)
        val existing = data.weekOverrides.firstOrNull {
            it.academicCalendarId == calendarId &&
                it.weekStartDate == monday && it.scope == entityScope
        }
        viewModelScope.launch {
            repository.saveWeekOverride(
                WeekOverrideEntity(
                    id = existing?.id ?: 0,
                    semesterId = data.semester.id,
                    academicCalendarId = calendarId,
                    weekStartDate = monday,
                    weekType = WeekType.valueOf(weekType.name),
                    scope = entityScope
                )
            )
        }
    }

    fun clearVisibleWeekOverride(scope: WeekOverrideScopeUi) {
        val data = activeSemesterData.value ?: return
        val calendarId = visibleWeekCalendarId(data) ?: return
        val monday = controls.value.scheduleDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val entityScope = WeekOverrideScope.valueOf(scope.name)
        val existing = data.weekOverrides.firstOrNull {
            it.academicCalendarId == calendarId &&
                it.weekStartDate == monday && it.scope == entityScope
        } ?: return
        viewModelScope.launch { repository.deleteWeekOverride(existing.id) }
    }

    private fun visibleWeekCalendarId(data: SemesterWithData): Long? {
        val filterId = controls.value.courseFilterId
        if (filterId == "all") return data.academicCalendars.singleOrNull()?.id
        val assignment = data.semesterPrograms.firstOrNull { it.id.toString() == filterId } ?: return null
        return data.calendarForAssignment(assignment.id)?.id
    }

    private fun buildSchedule(data: SemesterWithData?, control: ScheduleControls): ScheduleUiState {
        if (data == null) return emptyScheduleState()
        val activeFilter = control.courseFilterId.takeIf { id ->
            id == "all" || data.semesterPrograms.any { it.id.toString() == id }
        } ?: "all"
        val selectedPlan = activePlan(data, control.scheduleDate)
        val selected = selectedPlan.schedule
        val filtered = selected.occurrences.filter {
            activeFilter == "all" || it.classItem.semesterProgramId == activeFilter
        }
        val cancelled = if (control.showCancelled) cancelledItems(data, control.scheduleDate) else emptyList()
        val labels = conflictLabels(selectedPlan.collisions)
        val monday = control.scheduleDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val currentWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val calendarDays = calendarDates(control.calendarMonth).map { date ->
            val occurrences = activePlan(data, date).schedule.occurrences.filter {
                activeFilter == "all" || it.classItem.semesterProgramId == activeFilter
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
            activeFilter == "all" || it.classItem.semesterProgramId == activeFilter
        }
        val calendarLabels = conflictLabels(calendarPlan.collisions)
        val relevantCalendarIds = if (activeFilter == "all") {
            data.academicCalendars.mapTo(mutableSetOf()) { it.id }
        } else {
            setOfNotNull(
                data.semesterPrograms.firstOrNull { it.id.toString() == activeFilter }
                    ?.let { data.calendarForAssignment(it.id)?.id }
            )
        }
        return ScheduleUiState(
            view = control.scheduleView,
            weekRangeLabel = "${monday.format(shortDateFormatter)} - ${monday.plusDays(6).format(shortDateFormatter)}",
            weekSubtitle = if (monday == currentWeekMonday) "Bieżący tydzień" else data.semester.name,
            weekTypeLabel = if (selected.hasMixedWeekTypes) {
                "Różne tygodnie"
            } else {
                selected.weekType?.let { "Tydzień ${it.name}" } ?: "Poza semestrem"
            },
            weekSourceLabel = when {
                selected.hasMixedWeekTypes -> "Różne kalendarze"
                selected.correction == null -> "Wyliczony automatycznie"
                else -> "Korekta ręczna"
            },
            weekType = if (selected.hasMixedWeekTypes) {
                null
            } else {
                selected.weekType?.let { WeekTypeUi.valueOf(it.name) }
            },
            days = (0L..6L).map { offset ->
                val date = monday.plusDays(offset)
                ScheduleDayUi(
                    id = date.toString(),
                    shortLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, polishLocale),
                    dateLabel = date.dayOfMonth.toString(),
                    accessibilityLabel = date.format(fullDateFormatter),
                    isSelected = date == control.scheduleDate,
                    isEnabled = data.academicCalendars.any { calendar ->
                        !date.isBefore(calendar.startDate) && !date.isAfter(calendar.endDate)
                    }
                )
            },
            filters = listOf(ScheduleFilterUi("all", "Wszystkie", activeFilter == "all")) +
                data.semesterPrograms.map { assignment ->
                    val program = data.studyPrograms.firstOrNull { it.id == assignment.studyProgramId }
                    ScheduleFilterUi(
                        assignment.id.toString(),
                        program?.name.orEmpty(),
                        activeFilter == assignment.id.toString()
                    )
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
                it.academicCalendarId in relevantCalendarIds &&
                    it.weekStartDate == monday && it.scope == WeekOverrideScope.ONE_WEEK
            },
            hasFromWeekCorrection = data.weekOverrides.any {
                it.academicCalendarId in relevantCalendarIds &&
                    it.weekStartDate == monday && it.scope == WeekOverrideScope.FROM_WEEK
            }
        )
    }

    private fun cancelledItems(data: SemesterWithData, date: LocalDate): List<ClassItemUi> =
        data.occurrenceChanges.filter {
            it.originalDate == date && it.kind == dev.retza.mak.data.entity.OccurrenceChangeKind.CANCELLED
        }.mapNotNull { change ->
            val item = data.classes.firstOrNull { it.id == change.classId } ?: return@mapNotNull null
            val assignment = data.semesterPrograms.firstOrNull { it.id == item.semesterProgramId }
            val program = assignment?.let { link ->
                data.studyPrograms.firstOrNull { it.id == link.studyProgramId }
            }
            ClassItemUi(
                id = "${item.id}:$date",
                name = item.name,
                type = item.type,
                courseName = program?.name.orEmpty(),
                courseColor = program?.color,
                startTime = item.startTime.toString(),
                endTime = item.endTime.toString(),
                room = item.room?.trim()?.ifEmpty { null },
                building = item.building,
                teacherName = item.teacherName,
                note = item.classNote,
                statusBadge = "Odwołane",
                isCancelled = true
            )
        }

    private fun activePlan(data: SemesterWithData, date: LocalDate) =
        activePlanProvider.resolve(data.toActivePlanData(), date)
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

private fun emptyScheduleState() = ScheduleUiState(
    weekRangeLabel = "",
    weekSubtitle = "",
    weekTypeLabel = "",
    weekSourceLabel = "",
    status = ScreenStatus.Ready
)

private fun markerColor(
    data: SemesterWithData,
    occurrence: PlannedOccurrence
): CalendarMarkerColor {
    if (occurrence.occurrenceChange != null) return CalendarMarkerColor.Error
    val index = data.studyPrograms.indexOfFirst { it.id.toString() == occurrence.studyProgram?.id }
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

private fun calendarDates(month: YearMonth): List<LocalDate> {
    val first = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0L until 42L).map(first::plusDays)
}
