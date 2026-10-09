package dev.retza.mak.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.koin.core.annotation.KoinViewModel
import dev.retza.mak.domain.ActivePlanData
import dev.retza.mak.domain.ActivePlanInputs
import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.domain.PlanDisplaySettings
import dev.retza.mak.domain.allProgramsHidden
import dev.retza.mak.domain.hiddenAssignments
import dev.retza.mak.domain.countGaps
import dev.retza.mak.domain.collisionLabels
import dev.retza.mak.domain.collisionPartnerNames
import dev.retza.mak.domain.uniqueCollisionCount
import dev.retza.mak.ui.ActivePlanSource
import dev.retza.mak.ui.components.ScreenStatus
import dev.retza.mak.ui.polishLocale
import dev.retza.mak.ui.settings.SettingsPreferences
import dev.retza.mak.ui.toUi
import dev.retza.mak.ui.todayTitleFormatter
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// Wraps the loaded value so "not loaded yet" (null state) differs from "no active semester".
private data class LoadedPlan(val inputs: ActivePlanInputs?)

@KoinViewModel
class TodayViewModel(
    activePlanSource: ActivePlanSource,
    private val preferences: SettingsPreferences,
    private val clock: Clock,
    private val activePlanProvider: ActivePlanProvider
) : ViewModel() {
    private val date = MutableStateFlow(LocalDate.now(clock))

    private val activePlan = activePlanSource.observeActive()
        .map { LoadedPlan(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val today: StateFlow<TodayUiState> = combine(
        activePlan,
        date,
        preferences.gapThresholdMinutes
    ) { loaded, day, thresholdMinutes ->
        if (loaded == null) loadingTodayState() else buildToday(loaded.inputs, day, thresholdMinutes)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = loadingTodayState()
    )

    fun refreshToday() {
        date.value = LocalDate.now(clock)
    }

    private fun buildToday(
        inputs: ActivePlanInputs?,
        day: LocalDate,
        thresholdMinutes: Int
    ): TodayUiState {
        if (inputs == null) return emptyTodayState()
        val data = inputs.data
        val plan = activePlanProvider.resolve(inputs, day)
        val schedule = plan.schedule
        val labels = collisionLabels(plan.collisions)
        val names = collisionPartnerNames(plan.collisions)
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
            items = schedule.occurrences.map { it.toUi(labels[it.id], names[it.id]) },
            hiddenProgramNames = hiddenProgramNames(data, inputs.display),
            allProgramsHidden = data.allProgramsHidden(inputs.display)
        )
    }
}

// Shown until Room answers, so the screen never claims that there is no active semester too early.
private fun loadingTodayState() = TodayUiState(
    dateLabel = "",
    semesterLabel = "",
    weekLabel = "",
    status = ScreenStatus.Loading
)

private fun emptyTodayState() = TodayUiState(
    dateLabel = "Brak aktywnego semestru",
    semesterLabel = "",
    weekLabel = "",
    classCount = 0,
    collisionCount = 0,
    gapCount = 0,
    emptyMessage = "Nie masz jeszcze aktywnego semestru."
)

/** Names of the hidden study programs assigned to the active semester, in assignment order. */
private fun hiddenProgramNames(data: ActivePlanData, display: PlanDisplaySettings): List<String> =
    data.hiddenAssignments(display)
        .mapNotNull { assignment -> data.courses.firstOrNull { it.id == assignment.studyProgramId }?.name }
        .distinct()
