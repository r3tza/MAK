package dev.retza.mak.widget

import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.domain.ActivePlanInputs
import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.domain.allProgramsHidden
import dev.retza.mak.domain.shownPlanDate
import dev.retza.mak.domain.tomorrowSwitchTime
import dev.retza.mak.data.ActivePlanSource
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class WidgetPlanLoader(
    private val semesterRepository: SemesterRepository,
    private val activePlanSource: ActivePlanSource,
    private val activePlanProvider: ActivePlanProvider,
    private val clock: Clock,
    private val presenter: WidgetPresenter = WidgetPresenter(),
    private val daySwitchScheduler: WidgetDaySwitchScheduler = WidgetDaySwitchScheduler {}
) {
    /**
     * The widget state again after every change of the plan or of its settings. A running Glance session
     * only recomposes on update, so it has to follow the data itself.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun states(): Flow<WidgetUiState> =
        semesterRepository.observeActiveSemester()
            .flatMapLatest { semester ->
                if (semester == null) {
                    daySwitchScheduler.schedule(null)
                    flowOf(WidgetUiState.NoActiveSemester(todayLabel()))
                } else {
                    activePlanSource.observe(semester.id).map(::present)
                }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                daySwitchScheduler.schedule(null)
                emit(WidgetUiState.Error(todayLabel()))
            }

    private fun present(inputs: ActivePlanInputs?): WidgetUiState {
        val now = LocalDateTime.now(clock)
        val today = now.toLocalDate()
        if (inputs == null) {
            daySwitchScheduler.schedule(null)
            return WidgetUiState.Error(widgetDateLabel(today))
        }
        if (inputs.data.allProgramsHidden(inputs.display)) {
            daySwitchScheduler.schedule(null)
            return WidgetUiState.AllProgramsHidden(widgetDateLabel(today))
        }
        val todayPlan = activePlanProvider.resolve(inputs, today)
        val enabled = inputs.display.showTomorrowInEvening
        val switchTime = tomorrowSwitchTime(todayPlan.schedule.occurrences)
        val shown = shownPlanDate(today, now.toLocalTime(), switchTime, enabled)
        val semesterName = inputs.data.semester.name
        if (shown == today) {
            // Only a time still ahead is planned, because a past switch time would already show tomorrow.
            daySwitchScheduler.schedule(if (enabled && switchTime != null) today.atTime(switchTime) else null)
            return presenter.present(today, semesterName, todayPlan, now.toLocalTime())
        }
        daySwitchScheduler.schedule(null)
        val tomorrowPlan = activePlanProvider.resolve(inputs, shown)
        return presenter.present(shown, semesterName, tomorrowPlan, now = null, isTomorrow = true)
    }

    private fun todayLabel(): String = widgetDateLabel(LocalDate.now(clock))
}
