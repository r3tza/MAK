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
import dev.retza.mak.domain.collisionLabels
import dev.retza.mak.domain.collisionPartnerNames
import dev.retza.mak.domain.uniqueCollisionCount
import dev.retza.mak.data.ActivePlanSource
import dev.retza.mak.ui.AppLocale
import dev.retza.mak.ui.label
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
    private val clock: Clock,
    private val activePlanProvider: ActivePlanProvider
) : ViewModel() {
    private val date = MutableStateFlow(LocalDate.now(clock))

    private val activePlan = activePlanSource.observeActive()
        .map { LoadedPlan(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val today: StateFlow<TodayUiState> = combine(
        activePlan,
        date
    ) { loaded, day ->
        if (loaded == null) loadingTodayState() else buildToday(loaded.inputs, day)
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
        day: LocalDate
    ): TodayUiState {
        if (inputs == null) return emptyTodayState()
        val data = inputs.data
        val plan = activePlanProvider.resolve(inputs, day)
        val allHidden = data.allProgramsHidden(inputs.display)
        val schedule = plan.schedule
        val labels = collisionLabels(plan.collisions)
        val names = collisionPartnerNames(plan.collisions)
        return TodayUiState(
            dateLabel = day.format(todayTitleFormatter).replaceFirstChar { it.titlecase(AppLocale) },
            semesterLabel = data.semester.name,
            weekLabel = schedule.weekStatus.label(),
            hasActiveSemester = true,
            classCount = schedule.occurrences.size,
            collisionCount = uniqueCollisionCount(plan.collisions),
            gapCount = plan.gapCount,
            items = schedule.occurrences.map { it.toUi(labels[it.id], names[it.id]) },
            // With every program hidden the note on the list says it all; the hint would repeat it.
            hiddenProgramNames = if (allHidden) emptyList() else hiddenProgramNames(data, inputs.display),
            allProgramsHidden = allHidden
        )
    }
}

// Shown until Room answers, so the screen never claims that there is no active semester too early.
private fun loadingTodayState() = TodayUiState(
    dateLabel = "",
    semesterLabel = "",
    weekLabel = "",
    isLoading = true
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
