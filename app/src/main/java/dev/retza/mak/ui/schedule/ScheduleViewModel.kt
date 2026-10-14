package dev.retza.mak.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.data.repository.WeekOverrideRecord
import dev.retza.mak.domain.ActivePlanData
import dev.retza.mak.domain.ActivePlanInputs
import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.domain.FreeDay
import dev.retza.mak.domain.ResolvedSchedule
import dev.retza.mak.domain.OccurrenceStatus
import dev.retza.mak.domain.PlannedOccurrence
import dev.retza.mak.domain.WeekOverrideScope
import dev.retza.mak.domain.WeekStatus
import dev.retza.mak.domain.WeekType
import dev.retza.mak.domain.weekStart
import dev.retza.mak.domain.collisionLabels
import dev.retza.mak.domain.collisionPartnerNames
import dev.retza.mak.domain.SemesterProgram
import dev.retza.mak.domain.allProgramsHidden
import dev.retza.mak.domain.visibleAssignments
import dev.retza.mak.domain.visibleTo
import dev.retza.mak.domain.VisiblePlanData
import dev.retza.mak.data.ActivePlanSource
import dev.retza.mak.ui.AppLocale
import dev.retza.mak.ui.classCountLabel
import dev.retza.mak.ui.components.CalendarDayUi
import dev.retza.mak.ui.components.CalendarLegendUi
import dev.retza.mak.ui.components.CalendarMarkerUi
import dev.retza.mak.ui.components.ClassItemUi
import dev.retza.mak.ui.components.cardBadge
import dev.retza.mak.ui.components.toUi
import dev.retza.mak.ui.dayNames
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.launchUiOperation
import dev.retza.mak.ui.fullDateFormatter
import dev.retza.mak.ui.label
import dev.retza.mak.ui.listNote
import dev.retza.mak.ui.monthFormatter
import dev.retza.mak.ui.shortDateFormatter
import dev.retza.mak.ui.shortDayNames
import dev.retza.mak.domain.toLocalDateOrNull
import dev.retza.mak.ui.toUi
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

sealed interface ScheduleEffect {
    data class OpenNewClassEditor(val date: LocalDate) : ScheduleEffect
    data class OpenFreeDayEditor(val semesterId: Long, val date: LocalDate) : ScheduleEffect
}

private data class ScheduleControls(
    val today: LocalDate,
    val scheduleDate: LocalDate,
    val calendarMonth: YearMonth,
    val calendarDate: LocalDate,
    val scheduleView: ScheduleView = ScheduleView.List,
    val courseFilterId: Long? = null,
    val showCancelled: Boolean = false
)

private data class GridKey(val month: YearMonth, val filter: Long?, val today: LocalDate, val view: ScheduleView)

@KoinViewModel
class ScheduleViewModel(
    private val semesterRepository: SemesterRepository,
    activePlanSource: ActivePlanSource,
    private val clock: Clock,
    private val activePlanProvider: ActivePlanProvider,
    private val feedbackSink: FeedbackSink
) : ViewModel() {
    // Not in ScheduleUiState: the week dialog closes as soon as the user taps a button.
    private var weekOverrideOperationRunning = false
    private val controls = LocalDate.now(clock).let { today ->
        MutableStateFlow(
            ScheduleControls(
                today = today,
                scheduleDate = today,
                calendarMonth = YearMonth.from(today),
                calendarDate = today
            )
        )
    }

    private val activePlan = activePlanSource.observeActive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // The grid resolves 42 days, so it is rebuilt only when what it shows changes, and only in the calendar view.
    private val calendarGrid: StateFlow<List<CalendarDayUi>> = combine(
        activePlan,
        controls.map { GridKey(it.calendarMonth, it.courseFilterId, it.today, it.scheduleView) }.distinctUntilChanged()
    ) { inputs, key ->
        if (inputs == null || key.view != ScheduleView.Calendar) {
            emptyList()
        } else {
            val visible = inputs.data.visibleTo(inputs.display)
            val activeFilter = activeFilter(inputs, key.filter)
            calendarGrid(
                visible,
                key.month,
                activeFilter,
                viewCalendarIds(visible.data, activeFilter),
                key.today
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val schedule: StateFlow<ScheduleUiState> = combine(activePlan, controls, calendarGrid) { inputs, control, grid ->
        buildSchedule(inputs, control, grid)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyScheduleState()
    )

    private val effectsChannel = Channel<ScheduleEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    fun selectView(view: ScheduleView) = controls.update { it.copy(scheduleView = view) }

    /**
     * Called when the app returns to the foreground. After midnight the plan follows the new day,
     * but a week or day the user picked on purpose stays selected.
     */
    fun refreshToday() = controls.update { control ->
        val newToday = LocalDate.now(clock)
        if (newToday == control.today) return@update control
        val followList = control.scheduleDate == control.today
        val followCalendar = control.calendarDate == control.today
        control.copy(
            today = newToday,
            scheduleDate = if (followList) newToday else control.scheduleDate,
            calendarDate = if (followCalendar) newToday else control.calendarDate,
            calendarMonth = if (followCalendar) YearMonth.from(newToday) else control.calendarMonth
        )
    }

    fun changeWeek(amount: Long) = controls.update { it.copy(scheduleDate = it.scheduleDate.plusWeeks(amount)) }

    fun selectDay(id: String) {
        id.toLocalDateOrNull()?.let { date -> controls.update { it.copy(scheduleDate = date) } }
    }

    fun selectCourseFilter(id: Long?) = controls.update { it.copy(courseFilterId = id) }

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

    fun showDate(id: String) {
        id.toLocalDateOrNull()?.let { date ->
            controls.update {
                it.copy(scheduleDate = date, calendarDate = date, calendarMonth = YearMonth.from(date))
            }
        }
    }

    fun openNewClassForSelectedCalendarDay() {
        effectsChannel.trySend(ScheduleEffect.OpenNewClassEditor(controls.value.calendarDate))
    }

    fun markSelectedCalendarDayFree() {
        val data = activePlan.value?.data ?: return
        effectsChannel.trySend(ScheduleEffect.OpenFreeDayEditor(data.semester.id, controls.value.calendarDate))
    }

    fun saveVisibleWeekOverride(weekType: WeekType, scope: WeekOverrideScope) {
        if (weekOverrideOperationRunning) return
        val data = activePlan.value?.data ?: return
        val calendarId = visibleWeekCalendarId(data) ?: return
        if (!schedule.value.canCorrectWeek) return
        val monday = controls.value.scheduleDate.weekStart()
        weekOverrideOperationRunning = true
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się zapisać korekty tygodnia.",
            onFinish = { weekOverrideOperationRunning = false }
        ) {
            semesterRepository.saveWeekOverride(
                WeekOverrideRecord(
                    id = 0,
                    semesterId = data.semester.id,
                    academicCalendarId = calendarId,
                    weekStartDate = monday,
                    weekType = weekType,
                    scope = scope
                )
            )
        }
    }

    fun clearVisibleWeekOverride(scope: WeekOverrideScope) {
        if (weekOverrideOperationRunning) return
        val data = activePlan.value?.data ?: return
        val calendarId = visibleWeekCalendarId(data) ?: return
        val monday = controls.value.scheduleDate.weekStart()
        weekOverrideOperationRunning = true
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się usunąć korekty tygodnia.",
            onFinish = { weekOverrideOperationRunning = false }
        ) {
            semesterRepository.deleteWeekOverride(calendarId, monday, scope)
        }
    }

    private fun visibleWeekCalendarId(
        data: ActivePlanData,
        filterId: Long? = controls.value.courseFilterId
    ): Long? {
        if (filterId == null) return data.calendars.singleOrNull()?.id
        val assignment = data.semesterPrograms.firstOrNull { it.id == filterId } ?: return null
        return data.calendars.firstOrNull { it.id == assignment.academicCalendarId }?.id
    }

    private fun matchesFilter(occurrence: PlannedOccurrence, activeFilter: Long?): Boolean =
        activeFilter == null || occurrence.classItem.semesterProgramId == activeFilter

    private fun activeFilter(inputs: ActivePlanInputs, requested: Long?): Long? {
        val visible = inputs.data.visibleTo(inputs.display)
        val programs = visible.data.visibleAssignments(inputs.display)
        return requested?.takeIf { id -> programs.any { it.id == id } }
    }

    /** Calendars whose classes the view shows: the filtered assignment's calendar, or every calendar. */
    private fun viewCalendarIds(data: ActivePlanData, activeFilter: Long?): Set<Long> =
        if (activeFilter == null) {
            data.calendars.mapTo(mutableSetOf()) { it.id }
        } else {
            setOfNotNull(data.semesterPrograms.firstOrNull { it.id == activeFilter }?.academicCalendarId)
        }

    private fun calendarGrid(
        visible: VisiblePlanData,
        month: YearMonth,
        activeFilter: Long?,
        calendarIds: Set<Long>,
        today: LocalDate
    ): List<CalendarDayUi> {
        val days = calendarDates(month).map { date ->
            val schedule = activePlanProvider.schedule(visible, date)
            // Days of the grid show only markers, so their collisions are not needed.
            val occurrences = schedule.occurrences.filter { matchesFilter(it, activeFilter) }
            val cancelled = schedule.cancelled.filter { matchesFilter(it, activeFilter) }
            val shown = occurrences + cancelled
            val isFreeDay = schedule.viewFreeDay(calendarIds) != null
            CalendarDayUi(
                id = date.toString(),
                dayLabel = date.dayOfMonth.toString(),
                accessibilityLabel = calendarAccessibilityLabel(date, occurrences, cancelled, isFreeDay),
                isInCurrentMonth = YearMonth.from(date) == month,
                isToday = date == today,
                isSelected = false,
                markers = shown.take(if (shown.size > 5) 4 else 5).map {
                    CalendarMarkerUi(
                        id = it.id,
                        contentDescription = calendarOccurrenceLabel(it),
                        isChanged = it.occurrenceChange != null && it.status != OccurrenceStatus.CANCELLED,
                        isCancelled = it.status == OccurrenceStatus.CANCELLED,
                        colorHex = it.studyProgram?.color
                    )
                },
                hasMoreMarkers = shown.size > 5,
                isFreeDay = isFreeDay
            )
        }
        // The grid is a week-row layout of seven columns, so a band joins only neighbours in the same row.
        return days.mapIndexed { index, day ->
            val column = index % 7
            day.copy(
                joinsPrevious = day.isFreeDay && column > 0 && days[index - 1].isFreeDay,
                joinsNext = day.isFreeDay && column < 6 && days.getOrNull(index + 1)?.isFreeDay == true
            )
        }
    }

    private fun buildSchedule(
        inputs: ActivePlanInputs?,
        control: ScheduleControls,
        grid: List<CalendarDayUi>
    ): ScheduleUiState {
        if (inputs == null) return emptyScheduleState()
        // Hidden study programs are neither filter options, legend entries, cancelled classes nor markers.
        val visible = inputs.data.visibleTo(inputs.display)
        val data = visible.data
        val programs = data.visibleAssignments(inputs.display)
        val activeFilter = activeFilter(inputs, control.courseFilterId)
        val selectedPlan = activePlanProvider.resolve(visible, control.scheduleDate, inputs.display)
        val selected = selectedPlan.schedule
        val filtered = selected.occurrences.filter { matchesFilter(it, activeFilter) }
        val cancelled = if (control.showCancelled) cancelledItems(selected.cancelled, activeFilter) else emptyList()
        val labels = collisionLabels(selectedPlan.collisions)
        val names = collisionPartnerNames(selectedPlan.collisions)
        val monday = control.scheduleDate.weekStart()
        val currentWeekMonday = control.today.weekStart()
        val calendarDays = grid.map { it.copy(isSelected = it.id == control.calendarDate.toString()) }
        // The selected day's classes are built only in the calendar view.
        val calendarPlan = when {
            control.scheduleView != ScheduleView.Calendar -> null
            control.calendarDate == control.scheduleDate -> selectedPlan
            else -> activePlanProvider.resolve(visible, control.calendarDate, inputs.display)
        }
        val calendarFiltered = calendarPlan?.schedule?.occurrences?.filter { matchesFilter(it, activeFilter) }.orEmpty()
        val calendarLabels = calendarPlan?.let { collisionLabels(it.collisions) }.orEmpty()
        val calendarNames = calendarPlan?.let { collisionPartnerNames(it.collisions) }.orEmpty()
        val relevantCalendarIds = viewCalendarIds(data, activeFilter)
        // The week row follows the calendar of the active filter, or the single calendar when there is no filter.
        val weekCalendarId = visibleWeekCalendarId(data, activeFilter)
        val shownWeek = weekCalendarId?.let { selected.weeksByCalendarId[it] }
        val weekOutside = if (weekCalendarId == null) {
            selected.weekStatus == WeekStatus.OutsideSemester
        } else {
            shownWeek == null
        }
        val relevantWeeks = selected.weeksByCalendarId.filterKeys { it in relevantCalendarIds }.values
        return ScheduleUiState(
            view = control.scheduleView,
            weekRangeLabel = "${monday.format(shortDateFormatter)} - ${monday.plusDays(6).format(shortDateFormatter)}",
            weekSubtitle = if (monday == currentWeekMonday) "Bieżący tydzień" else data.semester.name,
            weekTypeLabel = when {
                weekCalendarId == null -> selected.weekStatus.label()
                shownWeek == null -> WeekStatus.OutsideSemester.label()
                else -> WeekStatus.Single(shownWeek.weekType).label()
            },
            weekSourceLabel = when {
                weekOutside -> ""
                weekCalendarId == null && selected.weekStatus == WeekStatus.Mixed -> "Różne kalendarze"
                relevantWeeks.any { it.overrideSource != null } -> "Korekta ręczna"
                else -> "Wyliczony automatycznie"
            },
            weekType = shownWeek?.weekType,
            days = (0L..6L).map { offset ->
                val date = monday.plusDays(offset)
                ScheduleDayUi(
                    id = date.toString(),
                    shortLabel = shortDayNames.getValue(date.dayOfWeek),
                    dateLabel = date.dayOfMonth.toString(),
                    accessibilityLabel = date.format(fullDateFormatter),
                    isSelected = date == control.scheduleDate,
                    isEnabled = data.calendars.any { calendar ->
                        date in calendar
                    }
                )
            },
            filters = listOf(ScheduleFilterUi(null, "Wszystkie", activeFilter == null)) +
                programs.map { assignment ->
                    val program = data.courses.firstOrNull { it.id == assignment.studyProgramId }
                    ScheduleFilterUi(
                        assignment.id,
                        program?.name.orEmpty(),
                        activeFilter == assignment.id
                    )
                },
            selectedDayLabel = dayNames[control.scheduleDate.dayOfWeek].orEmpty(),
            // Counts only classes that take place, like "Dzisiaj" and the widget.
            selectedDayCountLabel = classCountLabel(filtered.size),
            items = filtered.map { it.toUi(labels[it.id], names[it.id]) } + cancelled,
            calendarMonthLabel = control.calendarMonth.format(monthFormatter),
            calendarDays = calendarDays,
            calendarLegend = calendarLegend(
                data,
                programs,
                activeFilter,
                hasFreeDay = calendarDays.any { it.isFreeDay },
                hasCancelled = calendarDays.any { it.markers.any(CalendarMarkerUi::isCancelled) }
            ),
            freeDay = selected.viewFreeDay(relevantCalendarIds)?.listNote(),
            calendarFreeDay = calendarPlan?.schedule?.viewFreeDay(relevantCalendarIds)?.listNote(),
            allProgramsHidden = inputs.data.allProgramsHidden(inputs.display),
            // Capitalized like the list heading; fullDateFormatter stays lowercase for screen readers.
            calendarSelectedDayLabel = control.calendarDate.format(fullDateFormatter)
                .replaceFirstChar { it.titlecase(AppLocale) },
            calendarSelectedDayCountLabel = classCountLabel(calendarFiltered.size),
            calendarItems = if (calendarPlan == null) {
                emptyList()
            } else {
                calendarFiltered.map { it.toUi(calendarLabels[it.id], calendarNames[it.id]) } +
                    if (control.showCancelled) cancelledItems(calendarPlan.schedule.cancelled, activeFilter) else emptyList()
            },
            showCancelled = control.showCancelled,
            hasOneWeekCorrection = data.weekOverrides.any {
                it.academicCalendarId in relevantCalendarIds &&
                    it.weekStartDate == monday && it.scope == WeekOverrideScope.ONE_WEEK
            },
            canCorrectWeek = weekCalendarId != null && shownWeek != null,
            weekCorrectionHint = if (weekCalendarId == null && !weekOutside) {
                "Wybierz kierunek w polu „Kierunek”, aby zmienić tydzień."
            } else {
                null
            },
            emptyDayMessage = if (activeFilter == null) {
                "Brak zajęć w tym dniu."
            } else {
                "Brak zajęć w tym dniu dla wybranego kierunku."
            },
            hasFromWeekCorrection = data.weekOverrides.any {
                it.academicCalendarId in relevantCalendarIds &&
                    it.weekStartDate == monday && it.scope == WeekOverrideScope.FROM_WEEK
            }
        )
    }

    private fun cancelledItems(cancelled: List<PlannedOccurrence>, activeFilter: Long?): List<ClassItemUi> =
        cancelled
            .filter { matchesFilter(it, activeFilter) }
            .map { it.toUi(conflictLabel = null) }
}


private fun emptyScheduleState() = ScheduleUiState(
    weekRangeLabel = "",
    weekSubtitle = "",
    weekTypeLabel = "",
    weekSourceLabel = ""
)

/** The free day of the view: it exists only when every calendar in [calendarIds] has one on this date. */
private fun ResolvedSchedule.viewFreeDay(calendarIds: Set<Long>): FreeDay? =
    freeDayOf(calendarIds.filter { it in weeksByCalendarId })

private fun calendarLegend(
    data: ActivePlanData,
    programs: List<SemesterProgram>,
    activeFilter: Long?,
    hasFreeDay: Boolean,
    hasCancelled: Boolean
): List<CalendarLegendUi> =
    programs
        .filter { activeFilter == null || it.id == activeFilter }
        .mapNotNull { assignment -> data.courses.firstOrNull { it.id == assignment.studyProgramId } }
        .distinctBy { it.id }
        .map { CalendarLegendUi(label = it.name, colorHex = it.color) } +
        CalendarLegendUi(label = "Zmieniony termin", isChange = true) +
        (if (hasCancelled) listOf(CalendarLegendUi(label = "Odwołany termin", isCancelled = true)) else emptyList()) +
        if (hasFreeDay) listOf(CalendarLegendUi(label = "Dzień wolny", isFreeDay = true)) else emptyList()

private fun calendarAccessibilityLabel(
    date: LocalDate,
    occurrences: List<PlannedOccurrence>,
    cancelled: List<PlannedOccurrence>,
    isFreeDay: Boolean
): String {
    val details = (occurrences + cancelled).take(3).joinToString("; ") { calendarOccurrenceLabel(it) }
    return buildString {
        append(date.format(fullDateFormatter))
        if (isFreeDay) append(", dzień wolny")
        append(", ")
        append(classCountLabel(occurrences.size))
        if (details.isNotBlank()) {
            append(", ")
            append(details)
        }
    }
}

private fun calendarOccurrenceLabel(occurrence: PlannedOccurrence): String {
    val status = occurrence.status.toUi().cardBadge()?.let { ", ${it.lowercase()}" }.orEmpty()
    return "${occurrence.name}, ${occurrence.startTime} - ${occurrence.endTime}$status"
}

private fun calendarDates(month: YearMonth): List<LocalDate> {
    val first = month.atDay(1).weekStart()
    return (0L until 42L).map(first::plusDays)
}
