package dev.retza.mak.ui.freedays

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.retza.mak.data.repository.SemesterConfiguration
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.sync.PlanEditTracker
import dev.retza.mak.text.polishPlural
import dev.retza.mak.ui.AppLocale
import dev.retza.mak.ui.PlanEditSession
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import dev.retza.mak.ui.feedback.launchUiOperation
import dev.retza.mak.ui.shortDateWithYearFormatter
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

data class FreeDaysUiState(
    val semesterId: Long? = null,
    val groups: List<FreeDayGroupUi> = emptyList(),
    val pendingDeletion: FreeDayGroupUi? = null,
    val isDeleting: Boolean = false
)

private val weekdayFormatter = DateTimeFormatter.ofPattern("EEEE", AppLocale)

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

    private val editSession = PlanEditSession(
        tracker = planEditTracker,
        scope = viewModelScope,
        refreshBase = { true },
        hasDraft = { state.value.pendingDeletion != null }
    )

    init {
        val id = semesterId
        if (id != null) {
            viewModelScope.launch {
                semesterRepository.observeSemesterConfiguration(id).collect { loaded ->
                    state.update { it.copy(groups = loaded?.toGroupUis().orEmpty()) }
                }
            }
        }
        viewModelScope.launch {
            state.map { it.pendingDeletion != null }.distinctUntilChanged().collect { hasDraft ->
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

    private fun SemesterConfiguration.toGroupUis(): List<FreeDayGroupUi> =
        groupFreeDays(freeDays).map { group ->
            val calendarNames = group.calendarIds.map { calendarName(it) }
            FreeDayGroupUi(
                ids = group.ids,
                title = group.name ?: "Dni wolne",
                dateLabel = dateLabelOf(group),
                calendarsLabel = if (calendarNames.size == 1) {
                    "Kalendarz: ${calendarNames.single()}"
                } else {
                    "Kalendarze: ${calendarNames.joinToString(", ")}"
                }
            )
        }


    private fun dateLabelOf(group: FreeDayGroup): String {
        if (group.startDate == group.endDate) {
            val weekday = weekdayFormatter.format(group.startDate).replaceFirstChar { it.titlecase(AppLocale) }
            return "$weekday, ${group.startDate.format(shortDateWithYearFormatter)}"
        }
        val start = group.startDate.format(shortDateWithYearFormatter)
        val end = group.endDate.format(shortDateWithYearFormatter)
        val days = group.dayCount
        return "$start - $end, $days ${polishPlural(days, "dzień", "dni", "dni")}"
    }
}
