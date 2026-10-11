package dev.retza.mak.ui.freedays

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.retza.mak.data.repository.SemesterConfiguration
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.domain.Holiday
import dev.retza.mak.domain.PolishHolidays
import dev.retza.mak.sync.PlanEditTracker
import dev.retza.mak.text.polishPlural
import dev.retza.mak.ui.AppLocale
import dev.retza.mak.ui.PlanEditSession
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import dev.retza.mak.ui.feedback.launchUiOperation
import dev.retza.mak.ui.shortDateWithYearFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

data class FreeDayGroupUi(
    val ids: List<Long>,
    val title: String,
    val dateLabel: String,
    val calendarsLabel: String
)

data class HolidayOptionUi(val date: LocalDate, val name: String, val detail: String, val checked: Boolean, val enabled: Boolean)

data class HolidaysDialogUi(val intro: String, val options: List<HolidayOptionUi>, val calendarsLabel: String, val isSaving: Boolean = false) {
    val checkedCount: Int get() = options.count { it.checked }
    val confirmLabel: String get() = "Dodaj $checkedCount ${polishPlural(checkedCount, "dzień", "dni", "dni")}"
}

data class FreeDaysUiState(
    val semesterId: Long? = null,
    val groups: List<FreeDayGroupUi> = emptyList(),
    val pendingDeletion: FreeDayGroupUi? = null,
    val isDeleting: Boolean = false,
    val holidays: HolidaysDialogUi? = null
)

private val weekdayFormatter = DateTimeFormatter.ofPattern("EEEE", AppLocale)

private fun singleDayLabel(date: LocalDate): String {
    val weekday = weekdayFormatter.format(date).replaceFirstChar { it.titlecase(AppLocale) }
    return "$weekday, ${date.format(shortDateWithYearFormatter)}"
}

private fun calendarsLabelOf(names: List<String>): String =
    if (names.size == 1) "Kalendarz: ${names.single()}" else "Kalendarze: ${names.joinToString(", ")}"

/** Lists the free day groups of one semester and deletes them. Adding and editing go through [FreeDayEditorViewModel]. */
@KoinViewModel
class FreeDaysViewModel(
    savedState: SavedStateHandle,
    private val semesterRepository: SemesterRepository,
    private val feedbackSink: FeedbackSink,
    planEditTracker: PlanEditTracker
) : ViewModel() {
    // The route argument is text; a value that is not a number leaves the screen empty.
    private val semesterId: Long? = savedState.get<String>("semesterId")?.toLongOrNull()

    private val state = MutableStateFlow(FreeDaysUiState(semesterId = semesterId))
    val freeDays: StateFlow<FreeDaysUiState> = state.asStateFlow()

    // The last loaded configuration; the holiday dialog is built from it.
    private var configuration: SemesterConfiguration? = null

    private val editSession = PlanEditSession(
        tracker = planEditTracker,
        scope = viewModelScope,
        refreshBase = { true },
        hasDraft = { state.value.pendingDeletion != null || state.value.holidays != null }
    )

    init {
        val id = semesterId
        if (id != null) {
            viewModelScope.launch {
                semesterRepository.observeSemesterConfiguration(id).collect { loaded ->
                    configuration = loaded
                    state.update { it.copy(groups = loaded?.toGroupUis().orEmpty()) }
                }
            }
        }
        viewModelScope.launch {
            state.map { it.pendingDeletion != null || it.holidays != null }.distinctUntilChanged().collect { hasDraft ->
                if (!hasDraft) editSession.releaseIfNoDraft()
            }
        }
    }

    override fun onCleared() {
        editSession.close()
    }

    fun requestDeletion(ids: List<Long>) = editSession.run {
        val group = state.value.groups.firstOrNull { it.ids == ids } ?: return@run
        state.update { it.copy(pendingDeletion = group) }
    }

    fun cancelDeletion() {
        if (state.value.isDeleting) return
        state.update { it.copy(pendingDeletion = null) }
    }

    fun confirmDeletion() {
        if (state.value.isDeleting) return
        val ids = state.value.pendingDeletion?.ids ?: return
        state.update { it.copy(isDeleting = true) }
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się usunąć dni wolnych.",
            onFinish = {
                state.update { it.copy(isDeleting = false) }
            }
        ) {
            semesterRepository.deleteFreeDays(ids)
            state.update { it.copy(pendingDeletion = null) }
            feedbackSink.publish(UiFeedback("Usunięto dni wolne", UiFeedbackKind.Success))
        }
    }

    fun openHolidays() = editSession.run {
        val dialog = configuration?.toHolidaysDialog() ?: return@run
        state.update { it.copy(holidays = dialog) }
    }

    fun toggleHoliday(date: LocalDate) {
        state.update { current ->
            val dialog = current.holidays ?: return@update current
            if (dialog.isSaving) return@update current
            current.copy(
                holidays = dialog.copy(
                    options = dialog.options.map { option ->
                        if (option.date == date && option.enabled) option.copy(checked = !option.checked) else option
                    }
                )
            )
        }
    }

    fun cancelHolidays() {
        if (state.value.holidays?.isSaving == true) return
        state.update { it.copy(holidays = null) }
    }

    fun addHolidays() {
        val dialog = state.value.holidays ?: return
        val semesterId = state.value.semesterId ?: return
        val config = configuration ?: return
        if (dialog.isSaving || dialog.checkedCount == 0) return
        val holidays = dialog.options.filter { it.checked }.map { Holiday(it.date, it.name) }
        state.update { it.copy(holidays = dialog.copy(isSaving = true)) }
        viewModelScope.launchUiOperation(
            feedbackSink = feedbackSink,
            errorMessage = "Nie udało się dodać świąt ustawowych.",
            onFinish = {
                state.update { current -> current.copy(holidays = current.holidays?.copy(isSaving = false)) }
            }
        ) {
            semesterRepository.addHolidays(semesterId, holidays, config.calendars.map { it.id })
            state.update { it.copy(holidays = null) }
            feedbackSink.publish(UiFeedback("Dodano święta ustawowe", UiFeedbackKind.Success))
        }
    }

    /** Holidays from the first to the last calendar day; a holiday covered by a free day in every calendar is off. */
    private fun SemesterConfiguration.toHolidaysDialog(): HolidaysDialogUi? {
        if (calendars.isEmpty()) return null
        val calendarIds = calendars.map { it.id }
        val start = calendars.minOf { it.startDate }
        val end = calendars.maxOf { it.endDate }
        val options = PolishHolidays.between(start, end).map { holiday ->
            val coveringDays = calendarIds.map { calendarId ->
                freeDays.firstOrNull { it.academicCalendarId == calendarId && holiday.date in it.startDate..it.endDate }
            }
            val coveredEverywhere = coveringDays.all { it != null }
            val coveringName = coveringDays.firstOrNull()?.name ?: "Dni wolne"
            HolidayOptionUi(
                date = holiday.date,
                name = holiday.name,
                detail = if (coveredEverywhere) "Już w „$coveringName”" else singleDayLabel(holiday.date),
                checked = !coveredEverywhere,
                enabled = !coveredEverywhere
            )
        }
        return HolidaysDialogUi(
            intro = "Święta w czasie semestru, ${start.format(shortDateWithYearFormatter)} - " +
                "${end.format(shortDateWithYearFormatter)}. Odznacz te, w które uczelnia prowadzi zajęcia.",
            options = options,
            calendarsLabel = calendarsLabelOf(calendars.map { calendarName(it.id) })
        )
    }

    private fun SemesterConfiguration.toGroupUis(): List<FreeDayGroupUi> =
        groupFreeDays(freeDays).map { group ->
            val calendarNames = group.calendarIds.map { calendarName(it) }
            FreeDayGroupUi(
                ids = group.ids,
                title = group.name ?: "Dni wolne",
                dateLabel = dateLabelOf(group),
                calendarsLabel = calendarsLabelOf(calendarNames)
            )
        }


    private fun dateLabelOf(group: FreeDayGroup): String {
        if (group.startDate == group.endDate) return singleDayLabel(group.startDate)
        val start = group.startDate.format(shortDateWithYearFormatter)
        val end = group.endDate.format(shortDateWithYearFormatter)
        val days = group.dayCount
        return "$start - $end, $days ${polishPlural(days, "dzień", "dni", "dni")}"
    }
}
