package dev.retza.mak.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.repository.MakRepository
import dev.retza.mak.data.repository.toActivePlanData
import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.ui.edit.ClassEditViewModel
import dev.retza.mak.ui.feedback.FeedbackSink
import dev.retza.mak.ui.feedback.UiFeedback
import dev.retza.mak.ui.feedback.UiFeedbackKind
import dev.retza.mak.ui.schedule.conflictLabels
import dev.retza.mak.ui.today.TodayUiState
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    val activeSemesterData: SemesterWithData? = null
)

private data class Controls(
    val destination: MakDestination = MakDestination.Today,
    val todayDate: LocalDate
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
    private val controls = MutableStateFlow(Controls(todayDate = today))

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

    private fun buildState(
        semesterList: List<SemesterEntity>,
        activeData: SemesterWithData?,
        control: Controls
    ): MakUiState {
        val requiresSetup = semesterList.isEmpty() ||
            activeData != null && activeData.courses.isEmpty()
        val destination = if (requiresSetup) MakDestination.Setup else control.destination
        return MakUiState(
            destination = destination,
            requiresSetup = requiresSetup,
            hasLoadedData = true,
            today = buildToday(activeData, control.todayDate),
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

private fun emptyTodayState() = TodayUiState(
    dateLabel = "Brak aktywnego semestru",
    semesterLabel = "",
    weekLabel = "",
    summaryLabel = "0 zajęć"
)
