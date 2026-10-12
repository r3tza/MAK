package dev.retza.mak.ui.freedays

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.retza.mak.data.repository.SemesterConfiguration
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.domain.WeekType
import dev.retza.mak.domain.firstMondayAfter
import dev.retza.mak.domain.inclusiveDays
import dev.retza.mak.sync.PlanEditTracker
import dev.retza.mak.ui.PlanEditSession
import dev.retza.mak.ui.daysLabel
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import dev.retza.mak.ui.feedback.launchUiOperation
import dev.retza.mak.ui.shortDateWithYearFormatter
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

data class FreeDayCalendarOptionUi(
    val id: Long,
    val label: String,
    val checked: Boolean
)

data class FreeDayEditorUiState(
    val isOpen: Boolean = false,
    val isEditing: Boolean = false,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val name: String = "",
    val calendars: List<FreeDayCalendarOptionUi> = emptyList(),
    val minDate: LocalDate? = null,
    val maxDate: LocalDate? = null,
    val rangeError: String? = null,
    val calendarError: String? = null,
    val isSaving: Boolean = false,
    val weekAfterBreak: WeekType? = null
) {
    val title: String
        get() = if (isEditing) "Edytuj dni wolne" else "Dodaj dni wolne"

    /** Offered for any range, also a single day, once both dates are chosen. */
    val showsWeekAfterBreak: Boolean
        get() = startDate != null && endDate != null

    val weekAfterBreakDescription: String?
        get() {
            val end = endDate ?: return null
            val type = weekAfterBreak ?: return null
            if (!showsWeekAfterBreak) return null
            val monday = firstMondayAfter(end).format(shortDateWithYearFormatter)
            return "Od $monday tydzień ${type.name}, potem na zmianę. " +
                "Powstanie korekta „Od tego tygodnia”."
        }

    /** Null until both dates are chosen; counts both ends of the range. */
    val description: String?
        get() {
            val start = startDate ?: return null
            val end = endDate ?: return null
            return daysLabel(inclusiveDays(start, end))
        }
}

/**
 * Edits one group of free days across the calendars of a semester. A group is one row per calendar in the
 * repository; editing replaces every row of the group in one write.
 */
@KoinViewModel
class FreeDayEditorViewModel(
    private val semesterRepository: SemesterRepository,
    private val feedbackSink: FeedbackSink,
    planEditTracker: PlanEditTracker
) : ViewModel() {
    private val state = MutableStateFlow(FreeDayEditorUiState())
    val editor: StateFlow<FreeDayEditorUiState> = state.asStateFlow()

    private var semesterId: Long? = null
    private var replacedIds: List<Long> = emptyList()

    private val editSession = PlanEditSession(
        tracker = planEditTracker,
        scope = viewModelScope,
        refreshBase = { true },
        hasDraft = { state.value.isOpen }
    )

    override fun onCleared() {
        editSession.close()
    }

    fun startNew(semesterId: Long, date: LocalDate?) = editSession.run {
        val configuration = loadConfiguration(semesterId) ?: return@run
        this.semesterId = semesterId
        replacedIds = emptyList()
        state.value = configuration.toOpenState(
            isEditing = false,
            startDate = date,
            endDate = date,
            name = "",
            checkedCalendarIds = configuration.calendars.map { it.id }.toSet()
        )
    }

    fun startEdit(semesterId: Long, freeDayIds: List<Long>) = editSession.run {
        val configuration = loadConfiguration(semesterId) ?: return@run
        val group = configuration.freeDays.filter { it.id in freeDayIds }
        val first = group.firstOrNull() ?: return@run
        this.semesterId = semesterId
        replacedIds = group.map { it.id }
        state.value = configuration.toOpenState(
            isEditing = true,
            startDate = first.startDate,
            endDate = first.endDate,
            name = first.name.orEmpty(),
            checkedCalendarIds = group.map { it.academicCalendarId }.toSet()
        )
    }

    fun updateRange(start: LocalDate, end: LocalDate) {
        state.update { it.copy(startDate = start, endDate = end, rangeError = null) }
    }

    fun updateName(name: String) {
        state.update { it.copy(name = name) }
    }

    fun toggleCalendar(id: Long) {
        state.update { current ->
            current.copy(
                calendars = current.calendars.map {
                    if (it.id == id) it.copy(checked = !it.checked) else it
                },
                calendarError = null
            )
        }
    }

    fun updateWeekAfterBreak(type: WeekType?) {
        state.update { it.copy(weekAfterBreak = type) }
    }

    fun cancel() {
        state.value = FreeDayEditorUiState()
        viewModelScope.launch { editSession.releaseIfNoDraft() }
    }

    fun save() {
        val current = state.value
        if (!current.isOpen || current.isSaving) return
        val semesterId = semesterId ?: return
        val start = current.startDate
        val end = current.endDate
        if (start == null || end == null) {
            state.update { it.copy(rangeError = "Wybierz daty.") }
            return
        }
        val checkedIds = current.calendars.filter { it.checked }.map { it.id }
        if (checkedIds.isEmpty()) {
            state.update { it.copy(calendarError = "Wybierz co najmniej jeden kalendarz.") }
            return
        }
        state.update { it.copy(isSaving = true, rangeError = null, calendarError = null) }
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się zapisać dni wolnych.",
            onFinish = {
                state.update { it.copy(isSaving = false) }
            }
        ) {
            semesterRepository.saveFreeDayGroup(
                semesterId = semesterId,
                replacedIds = replacedIds,
                startDate = start,
                endDate = end,
                name = current.name.trim().ifEmpty { null },
                calendarIds = checkedIds,
                weekAfterBreak = current.weekAfterBreak.takeIf { current.showsWeekAfterBreak }
            )
            val message = if (current.isEditing) "Zapisano dni wolne" else "Dodano dni wolne"
            state.value = FreeDayEditorUiState()
            feedbackSink.publish(UiFeedback(message, UiFeedbackKind.Success))
            editSession.releaseIfNoDraft()
        }
    }

    /** Null when the semester is gone or has no calendar to attach free days to. */
    private suspend fun loadConfiguration(semesterId: Long): SemesterConfiguration? =
        semesterRepository.observeSemesterConfiguration(semesterId).first()
            ?.takeIf { it.calendars.isNotEmpty() }

    private fun SemesterConfiguration.toOpenState(
        isEditing: Boolean,
        startDate: LocalDate?,
        endDate: LocalDate?,
        name: String,
        checkedCalendarIds: Set<Long>
    ) = FreeDayEditorUiState(
        isOpen = true,
        isEditing = isEditing,
        startDate = startDate,
        endDate = endDate,
        name = name,
        calendars = calendars.sortedBy { it.id }.map { calendar ->
            FreeDayCalendarOptionUi(
                id = calendar.id,
                label = calendarName(calendar.id),
                checked = calendar.id in checkedCalendarIds
            )
        },
        minDate = calendars.minOf { it.startDate },
        maxDate = calendars.maxOf { it.endDate }
    )

}
