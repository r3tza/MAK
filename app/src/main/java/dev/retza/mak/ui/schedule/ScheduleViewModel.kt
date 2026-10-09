package dev.retza.mak.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.data.repository.WeekOverrideRecord
import dev.retza.mak.domain.ActivePlanData
import dev.retza.mak.domain.ActivePlanInputs
import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.domain.OccurrenceChangeKind
import dev.retza.mak.domain.PlannedOccurrence
import dev.retza.mak.domain.Recurrence
import dev.retza.mak.domain.WeekOverrideScope
import dev.retza.mak.domain.WeekType
import dev.retza.mak.domain.cancelledOccurrences
import dev.retza.mak.domain.collisionLabels
import dev.retza.mak.domain.collisionPartnerNames
import dev.retza.mak.ui.ActivePlanSource
import dev.retza.mak.ui.classCountLabel
import dev.retza.mak.ui.components.CalendarDayUi
import dev.retza.mak.ui.components.CalendarLegendUi
import dev.retza.mak.ui.components.CalendarMarkerUi
import dev.retza.mak.ui.components.ClassItemUi
import dev.retza.mak.ui.components.ScreenStatus
import dev.retza.mak.ui.dayNames
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import dev.retza.mak.ui.fullDateFormatter
import dev.retza.mak.ui.monthFormatter
import dev.retza.mak.ui.polishLocale
import dev.retza.mak.ui.semester.WeekOverrideScopeUi
import dev.retza.mak.ui.semester.WeekTypeUi
import dev.retza.mak.ui.shortDateFormatter
import dev.retza.mak.ui.shortDayNames
import dev.retza.mak.ui.toUi
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

sealed interface ScheduleEffect {
    data class OpenNewClassEditor(val date: LocalDate) : ScheduleEffect
}

private data class ScheduleControls(
    val today: LocalDate,
    val scheduleDate: LocalDate,
    val calendarMonth: YearMonth,
    val calendarDate: LocalDate,
    val scheduleView: ScheduleView = ScheduleView.List,
    val courseFilterId: String = "all",
    val showCancelled: Boolean = false
)

@KoinViewModel
class ScheduleViewModel(
    private val semesterRepository: SemesterRepository,
    activePlanSource: ActivePlanSource,
    private val clock: Clock,
    private val activePlanProvider: ActivePlanProvider,
    private val feedbackSink: FeedbackSink
) : ViewModel() {
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

    val schedule: StateFlow<ScheduleUiState> = combine(activePlan, controls) { inputs, control ->
        buildSchedule(inputs, control)
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

    fun saveVisibleWeekOverride(weekType: WeekTypeUi, scope: WeekOverrideScopeUi) {
        val data = activePlan.value?.data ?: return
        val calendarId = visibleWeekCalendarId(data) ?: return
        val monday = controls.value.scheduleDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val entityScope = WeekOverrideScope.valueOf(scope.name)
        val existing = data.weekOverrides.firstOrNull {
            it.academicCalendarId == calendarId.toString() &&
                it.weekStartDate == monday && it.scope == entityScope
        }
        viewModelScope.launch {
            runReportingFailure("Nie udało się zapisać korekty tygodnia.") {
                semesterRepository.saveWeekOverride(
                    WeekOverrideRecord(
                        id = existing?.id?.toLongOrNull() ?: 0,
                        semesterId = data.semester.id.toLong(),
                        academicCalendarId = calendarId,
                        weekStartDate = monday,
                        weekType = WeekType.valueOf(weekType.name),
                        scope = entityScope
                    )
                )
            }
        }
    }

    fun clearVisibleWeekOverride(scope: WeekOverrideScopeUi) {
        val data = activePlan.value?.data ?: return
        val calendarId = visibleWeekCalendarId(data) ?: return
        val monday = controls.value.scheduleDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val entityScope = WeekOverrideScope.valueOf(scope.name)
        val existing = data.weekOverrides.firstOrNull {
            it.academicCalendarId == calendarId.toString() &&
                it.weekStartDate == monday && it.scope == entityScope
        } ?: return
        viewModelScope.launch {
            runReportingFailure("Nie udało się usunąć korekty tygodnia.") {
                semesterRepository.deleteWeekOverride(existing.id.toLong())
            }
        }
    }

    private suspend fun runReportingFailure(message: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            feedbackSink.publish(UiFeedback(message, UiFeedbackKind.Error))
        }
    }

    private fun visibleWeekCalendarId(
        data: ActivePlanData,
        filterId: String = controls.value.courseFilterId
    ): Long? {
        if (filterId == "all") return data.calendars.singleOrNull()?.id?.toLongOrNull()
        val assignment = data.semesterPrograms.firstOrNull { it.id == filterId } ?: return null
        return data.calendars.firstOrNull { it.id == assignment.academicCalendarId }?.id?.toLongOrNull()
    }

    private fun buildSchedule(
        inputs: ActivePlanInputs?,
        control: ScheduleControls
    ): ScheduleUiState {
        if (inputs == null) return emptyScheduleState()
        val data = inputs.data
        val activeFilter = control.courseFilterId.takeIf { id ->
            id == "all" || data.semesterPrograms.any { it.id == id }
        } ?: "all"
        val selectedPlan = activePlanProvider.resolve(inputs, control.scheduleDate)
        val selected = selectedPlan.schedule
        val filtered = selected.occurrences.filter {
            activeFilter == "all" || it.classItem.semesterProgramId == activeFilter
        }
        val cancelled = if (control.showCancelled) cancelledItems(data, control.scheduleDate, activeFilter) else emptyList()
        val labels = collisionLabels(selectedPlan.collisions)
        val names = collisionPartnerNames(selectedPlan.collisions)
        val monday = control.scheduleDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val currentWeekMonday = control.today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val calendarDays = calendarDates(control.calendarMonth).map { date ->
            // Days of the grid show only markers, so their collisions are not needed.
            val occurrences = activePlanProvider.schedule(data, date).occurrences.filter {
                activeFilter == "all" || it.classItem.semesterProgramId == activeFilter
            }
            CalendarDayUi(
                id = date.toString(),
                dayLabel = date.dayOfMonth.toString(),
                accessibilityLabel = calendarAccessibilityLabel(date, occurrences),
                isInCurrentMonth = YearMonth.from(date) == control.calendarMonth,
                isToday = date == control.today,
                isSelected = date == control.calendarDate,
                markers = occurrences.take(if (occurrences.size > 5) 4 else 5).map {
                    CalendarMarkerUi(
                        id = it.id,
                        contentDescription = calendarOccurrenceLabel(it),
                        isChanged = it.occurrenceChange != null,
                        colorHex = it.studyProgram?.color
                    )
                },
                hasMoreMarkers = occurrences.size > 5
            )
        }
        val calendarPlan = if (control.calendarDate == control.scheduleDate) {
            selectedPlan
        } else {
            activePlanProvider.resolve(inputs, control.calendarDate)
        }
        val calendarSchedule = calendarPlan.schedule
        val calendarFiltered = calendarSchedule.occurrences.filter {
            activeFilter == "all" || it.classItem.semesterProgramId == activeFilter
        }
        val calendarLabels = collisionLabels(calendarPlan.collisions)
        val calendarNames = collisionPartnerNames(calendarPlan.collisions)
        val relevantCalendarIds = if (activeFilter == "all") {
            data.calendars.mapTo(mutableSetOf()) { it.id }
        } else {
            setOfNotNull(
                data.semesterPrograms.firstOrNull { it.id == activeFilter }?.academicCalendarId
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
                    shortLabel = shortDayNames.getValue(date.dayOfWeek),
                    dateLabel = date.dayOfMonth.toString(),
                    accessibilityLabel = date.format(fullDateFormatter),
                    isSelected = date == control.scheduleDate,
                    isEnabled = data.calendars.any { calendar ->
                        !date.isBefore(calendar.startDate) && !date.isAfter(calendar.endDate)
                    }
                )
            },
            filters = listOf(ScheduleFilterUi("all", "Wszystkie", activeFilter == "all")) +
                data.semesterPrograms.map { assignment ->
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
            calendarLegend = calendarLegend(data, activeFilter),
            // Capitalized like the list heading; fullDateFormatter stays lowercase for screen readers.
            calendarSelectedDayLabel = control.calendarDate.format(fullDateFormatter)
                .replaceFirstChar { it.titlecase(polishLocale) },
            calendarSelectedDayCountLabel = classCountLabel(calendarFiltered.size),
            calendarItems = calendarFiltered.map { it.toUi(calendarLabels[it.id], calendarNames[it.id]) } +
                if (control.showCancelled) cancelledItems(data, control.calendarDate, activeFilter) else emptyList(),
            showCancelled = control.showCancelled,
            hasOneWeekCorrection = data.weekOverrides.any {
                it.academicCalendarId in relevantCalendarIds &&
                    it.weekStartDate == monday && it.scope == WeekOverrideScope.ONE_WEEK
            },
            canCorrectWeek = visibleWeekCalendarId(data, activeFilter) != null,
            hasFromWeekCorrection = data.weekOverrides.any {
                it.academicCalendarId in relevantCalendarIds &&
                    it.weekStartDate == monday && it.scope == WeekOverrideScope.FROM_WEEK
            }
        )
    }

    private fun cancelledItems(data: ActivePlanData, date: LocalDate, activeFilter: String): List<ClassItemUi> =
        cancelledOccurrences(data, date)
            .filter { activeFilter == "all" || it.classItem.semesterProgramId == activeFilter }
            .map { cancelled ->
                val item = cancelled.classItem
                ClassItemUi(
                    id = "${item.id}:$date",
                    name = item.name,
                    type = item.type,
                    courseName = cancelled.studyProgram?.name.orEmpty(),
                    courseColor = cancelled.studyProgram?.color,
                    startTime = item.startTime.toString(),
                    endTime = item.endTime.toString(),
                    room = item.room?.trim()?.ifEmpty { null },
                    building = item.building,
                    teacherName = item.teacherName,
                    classNote = item.classNote?.takeIf { it.isNotBlank() },
                    occurrenceNote = cancelled.occurrenceNote?.body?.takeIf { it.isNotBlank() },
                    statusBadge = "Odwołane",
                    isCancelled = true
                )
            }
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

private fun emptyScheduleState() = ScheduleUiState(
    weekRangeLabel = "",
    weekSubtitle = "",
    weekTypeLabel = "",
    weekSourceLabel = "",
    status = ScreenStatus.Ready
)

private fun calendarLegend(data: ActivePlanData, activeFilter: String): List<CalendarLegendUi> {
    val courses = data.semesterPrograms
        .filter { activeFilter == "all" || it.id == activeFilter }
        .mapNotNull { assignment -> data.courses.firstOrNull { it.id == assignment.studyProgramId } }
        .distinctBy { it.id }
        .map { CalendarLegendUi(label = it.name, colorHex = it.color) }
    return courses + CalendarLegendUi(label = "Zmieniony termin", isChange = true)
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
