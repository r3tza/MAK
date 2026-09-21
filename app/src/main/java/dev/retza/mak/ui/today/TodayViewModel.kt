package dev.retza.mak.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.koin.core.annotation.KoinViewModel
import dev.retza.mak.data.database.SemesterWithData
import dev.retza.mak.data.repository.MakRepository
import dev.retza.mak.data.repository.toActivePlanData
import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.ui.classCountLabel
import dev.retza.mak.ui.polishLocale
import dev.retza.mak.ui.schedule.conflictLabels
import dev.retza.mak.ui.toUi
import dev.retza.mak.ui.todayTitleFormatter
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class TodayViewModel(
    private val repository: MakRepository,
    private val clock: Clock,
    private val activePlanProvider: ActivePlanProvider
) : ViewModel() {
    private val date = MutableStateFlow(LocalDate.now(clock))

    private val activeSemesterData = repository.observeActiveSemester().flatMapLatest { semester ->
        if (semester == null) flowOf(null) else repository.observeSemesterData(semester.id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val today: StateFlow<TodayUiState> = combine(activeSemesterData, date) { data, day ->
        buildToday(data, day)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyTodayState()
    )

    fun refreshToday() {
        date.value = LocalDate.now(clock)
    }

    private fun buildToday(data: SemesterWithData?, day: LocalDate): TodayUiState {
        if (data == null) return emptyTodayState()
        val plan = activePlanProvider.resolve(data.toActivePlanData(), day)
        val schedule = plan.schedule
        val labels = conflictLabels(plan.collisions)
        return TodayUiState(
            dateLabel = day.format(todayTitleFormatter).replaceFirstChar { it.titlecase(polishLocale) },
            semesterLabel = data.semester.name,
            weekLabel = if (schedule.hasMixedWeekTypes) {
                "Różne tygodnie"
            } else {
                schedule.weekType?.let { "Tydzień ${it.name}" } ?: "Poza semestrem"
            },
            summaryLabel = classCountLabel(schedule.occurrences.size),
            items = schedule.occurrences.map { it.toUi(labels[it.id]) }
        )
    }
}

private fun emptyTodayState() = TodayUiState(
    dateLabel = "Brak aktywnego semestru",
    semesterLabel = "",
    weekLabel = "",
    summaryLabel = "0 zajęć",
    emptyMessage = "Nie masz jeszcze aktywnego semestru."
)
