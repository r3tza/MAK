package dev.retza.mak.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.koin.core.annotation.KoinViewModel
import dev.retza.mak.data.repository.ScheduleRepository
import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.domain.ActivePlanData
import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.domain.countGaps
import dev.retza.mak.domain.uniqueCollisionCount
import dev.retza.mak.ui.polishLocale
import dev.retza.mak.ui.schedule.conflictLabels
import dev.retza.mak.ui.settings.SettingsPreferences
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
    private val semesterRepository: SemesterRepository,
    private val scheduleRepository: ScheduleRepository,
    private val preferences: SettingsPreferences,
    private val clock: Clock,
    private val activePlanProvider: ActivePlanProvider
) : ViewModel() {
    private val date = MutableStateFlow(LocalDate.now(clock))

    private val activePlanData = semesterRepository.observeActiveSemester()
        .flatMapLatest { semester ->
            if (semester == null) flowOf(null) else scheduleRepository.observeActivePlanData(semester.id)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val today: StateFlow<TodayUiState> = combine(
        activePlanData,
        date,
        preferences.gapThresholdMinutes
    ) { data, day, thresholdMinutes ->
        buildToday(data, day, thresholdMinutes)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyTodayState()
    )

    fun refreshToday() {
        date.value = LocalDate.now(clock)
    }

    private fun buildToday(
        data: ActivePlanData?,
        day: LocalDate,
        thresholdMinutes: Int
    ): TodayUiState {
        if (data == null) return emptyTodayState()
        val plan = activePlanProvider.resolve(data, day)
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
            hasActiveSemester = true,
            classCount = schedule.occurrences.size,
            collisionCount = uniqueCollisionCount(plan.collisions),
            gapCount = countGaps(schedule.occurrences, thresholdMinutes.toLong()),
            items = schedule.occurrences.map { it.toUi(labels[it.id]) }
        )
    }
}

private fun emptyTodayState() = TodayUiState(
    dateLabel = "Brak aktywnego semestru",
    semesterLabel = "",
    weekLabel = "",
    classCount = 0,
    collisionCount = 0,
    gapCount = 0,
    emptyMessage = "Nie masz jeszcze aktywnego semestru."
)
