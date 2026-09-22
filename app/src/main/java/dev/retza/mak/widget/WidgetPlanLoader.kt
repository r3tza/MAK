package dev.retza.mak.widget

import dev.retza.mak.data.repository.ScheduleRepository
import dev.retza.mak.domain.ActivePlanProvider
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.first

class WidgetPlanLoader(
    private val scheduleRepository: ScheduleRepository,
    private val activePlanProvider: ActivePlanProvider,
    private val clock: Clock,
    private val presenter: WidgetPresenter = WidgetPresenter()
) {
    suspend fun load(): WidgetUiState {
        val date = LocalDate.now(clock)
        val dateLabel = widgetDateLabel(date)
        return runCatching {
            val planData = scheduleRepository.observeActivePlanData().first()
                ?: return@runCatching WidgetUiState.NoActiveSemester(dateLabel)
            val plan = activePlanProvider.resolve(planData, date)
            presenter.present(date, planData.semester.name, plan)
        }.getOrElse {
            WidgetUiState.Error(dateLabel)
        }
    }
}
