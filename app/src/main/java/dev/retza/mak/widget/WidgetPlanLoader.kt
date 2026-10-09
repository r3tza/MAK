package dev.retza.mak.widget

import dev.retza.mak.data.repository.SemesterRepository
import dev.retza.mak.domain.ActivePlanInputs
import dev.retza.mak.domain.ActivePlanProvider
import dev.retza.mak.domain.allProgramsHidden
import dev.retza.mak.ui.ActivePlanSource
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
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
    private val presenter: WidgetPresenter = WidgetPresenter()
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
                    flowOf(WidgetUiState.NoActiveSemester(dateLabel()))
                } else {
                    activePlanSource.observe(semester.id).map(::present)
                }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(WidgetUiState.Error(dateLabel()))
            }

    private fun present(inputs: ActivePlanInputs?): WidgetUiState {
        if (inputs == null) return WidgetUiState.Error(dateLabel())
        if (inputs.data.allProgramsHidden(inputs.display)) return WidgetUiState.AllProgramsHidden(dateLabel())
        val date = LocalDate.now(clock)
        val plan = activePlanProvider.resolve(inputs, date)
        return presenter.present(date, inputs.data.semester.name, plan, LocalTime.now(clock))
    }

    private fun dateLabel(): String = widgetDateLabel(LocalDate.now(clock))
}
